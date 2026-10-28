package com.pennywiseai.tracker.backup.cloud

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDriveProviderTest {

    private class FakeTokens : DriveTokenSource {
        var issued = 0
        val invalidated = mutableListOf<String>()
        override suspend fun token() = "token-${++issued}"
        override suspend fun invalidate(token: String) { invalidated += token }
    }

    private val tokens = FakeTokens()
    private val requests = mutableListOf<HttpRequestData>()
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val name = "pennywise-backup-20261027T101530Z-1a2b3c.pwenc"

    @Test
    fun list_usesAppDataFolder_andFiltersForeignNames() = runBlocking {
        val p = GoogleDriveProvider(tokens, HttpClient(MockEngine { request ->
            requests += request
            respond(
                """{"files":[{"id":"abc_1","name":"$name","size":"42"},{"id":"zz","name":"other.txt"}]}""",
                HttpStatusCode.OK, json,
            )
        }))
        val list = p.list()
        assertEquals(listOf("abc_1"), list.map { it.id })
        assertEquals(42L, list.single().sizeBytes)
        val url = requests.single().url
        assertEquals("appDataFolder", url.parameters["spaces"])
        assertEquals("Bearer token-1", requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun expiredToken_isInvalidatedAndRetriedOnce() = runBlocking {
        val p = GoogleDriveProvider(tokens, HttpClient(MockEngine { request ->
            requests += request
            if (request.headers[HttpHeaders.Authorization] == "Bearer token-1") respond("", HttpStatusCode.Unauthorized)
            else respond("""{"files":[]}""", HttpStatusCode.OK, json)
        }))
        assertTrue(p.list().isEmpty())
        assertEquals(listOf("token-1"), tokens.invalidated)
        assertEquals(2, requests.size)
    }

    @Test
    fun persistent401_needsSignIn() {
        val p = GoogleDriveProvider(tokens, HttpClient(MockEngine { respond("", HttpStatusCode.Unauthorized) }))
        val e = assertThrows(CloudBackupException::class.java) { runBlocking { p.verify() } }
        assertEquals(CloudBackupError.AUTH_REQUIRED, e.error)
    }

    @Test
    fun errorMapping() {
        assertEquals(CloudBackupError.STORAGE_FULL, GoogleDriveProvider.errorFor(403, """{"reason":"storageQuotaExceeded"}"""))
        assertEquals(CloudBackupError.DRIVE_NOT_CONFIGURED, GoogleDriveProvider.errorFor(403, """{"reason":"accessNotConfigured"}"""))
        assertEquals(CloudBackupError.AUTH_FAILED, GoogleDriveProvider.errorFor(403, "{}"))
        assertEquals(CloudBackupError.SERVER, GoogleDriveProvider.errorFor(503, ""))
        assertEquals(CloudBackupError.NOT_FOUND, GoogleDriveProvider.errorFor(404, ""))
    }

    @Test
    fun scope_isAppDataOnly() {
        assertEquals("https://www.googleapis.com/auth/drive.appdata", GoogleDriveProvider.SCOPE_DRIVE_APPDATA)
    }
}
