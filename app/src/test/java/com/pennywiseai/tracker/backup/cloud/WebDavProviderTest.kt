package com.pennywiseai.tracker.backup.cloud

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WebDavProviderTest {

    private val credentials = WebDavCredentials("https://dav.example.test/files/me", "me", "app-pass")
    private val requests = mutableListOf<HttpRequestData>()

    private fun provider(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): WebDavProvider {
        val engine = MockEngine { request ->
            requests += request
            handler(request)
        }
        return WebDavProvider(credentials, HttpClient(engine) { followRedirects = false; expectSuccess = false })
    }

    private fun assertError(error: CloudBackupError, block: suspend () -> Unit) {
        val e = assertThrows(CloudBackupException::class.java) { runBlocking { block() } }
        assertEquals(error, e.error)
    }

    private val name = "pennywise-backup-20261027T101530Z-1a2b3c.pwenc"
    private val older = "pennywise-backup-20261001T000000Z-abcdef.pwenc"

    private val multistatus = """<?xml version="1.0" encoding="utf-8"?>
        <d:multistatus xmlns:d="DAV:">
          <d:response><d:href>/files/me/PennyWise/</d:href>
            <d:propstat><d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop></d:propstat></d:response>
          <d:response><d:href>/files/me/PennyWise/$name</d:href>
            <d:propstat><d:prop><d:resourcetype/><d:getcontentlength>1234</d:getcontentlength></d:prop></d:propstat></d:response>
          <d:response><d:href>https://dav.example.test/files/me/PennyWise/$older</d:href>
            <d:propstat><d:prop><d:resourcetype/><d:getcontentlength>99</d:getcontentlength></d:prop></d:propstat></d:response>
          <d:response><d:href>/files/me/PennyWise/$name.part</d:href>
            <d:propstat><d:prop><d:resourcetype/></d:prop></d:propstat></d:response>
          <d:response><d:href>/files/me/PennyWise/holiday%20photo.jpg</d:href>
            <d:propstat><d:prop><d:resourcetype/></d:prop></d:propstat></d:response>
        </d:multistatus>"""

    @Test
    fun list_returnsOnlySnapshots_withSizes() = runBlocking {
        val p = provider { respond(multistatus, HttpStatusCode.MultiStatus) }
        val list = p.list().sortedBy { it.createdAt }
        assertEquals(listOf(older, name), list.map { it.name })
        assertEquals(1234L, list.last().sizeBytes)
        assertEquals(Instant.parse("2026-10-27T10:15:30Z"), list.last().createdAt)
        val request = requests.single()
        assertEquals("PROPFIND", request.method.value)
        assertEquals("https://dav.example.test/files/me/PennyWise/", request.url.toString())
        assertEquals("1", request.headers["Depth"])
        assertTrue(request.headers[HttpHeaders.Authorization]!!.startsWith("Basic "))
    }

    @Test
    fun list_missingFolder_isEmpty() = runBlocking {
        val p = provider { respond("", HttpStatusCode.NotFound) }
        assertTrue(p.list().isEmpty())
    }

    @Test
    fun list_rejectsDoctype() {
        val evil = """<?xml version="1.0"?><!DOCTYPE x [<!ENTITY e SYSTEM "file:///etc/passwd">]><d:multistatus xmlns:d="DAV:"/>"""
        val p = provider { respond(evil, HttpStatusCode.MultiStatus) }
        assertError(CloudBackupError.SERVER) { p.list() }
    }

    @Test
    fun upload_putsPartFileThenMovesIntoPlace() = runBlocking {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val p = provider { request ->
            when (request.method.value) {
                "MKCOL" -> respond("", HttpStatusCode.MethodNotAllowed) // already exists
                "PUT" -> respond("", HttpStatusCode.Created)
                "MOVE" -> respond("", HttpStatusCode.Created)
                else -> respond("", HttpStatusCode.BadRequest)
            }
        }
        val uploaded = p.upload(name, bytes)
        assertEquals(name, uploaded.id)
        assertEquals(listOf("MKCOL", "PUT", "MOVE"), requests.map { it.method.value })
        val put = requests[1]
        assertTrue(put.url.toString().endsWith("/PennyWise/$name.part"))
        assertArrayEquals(bytes, (put.body as OutgoingContent.ByteArrayContent).bytes())
        val move = requests[2]
        assertEquals("https://dav.example.test/files/me/PennyWise/$name", move.headers["Destination"])
        assertEquals("F", move.headers["Overwrite"])
    }

    @Test
    fun upload_failedPut_cleansUpPart_andReportsStorageFull() {
        val p = provider { request ->
            when (request.method.value) {
                "MKCOL" -> respond("", HttpStatusCode.Created)
                "PUT" -> respond("", HttpStatusCode(507, "Insufficient Storage"))
                else -> respond("", HttpStatusCode.NoContent)
            }
        }
        assertError(CloudBackupError.STORAGE_FULL) { p.upload(name, byteArrayOf(1)) }
        assertEquals("DELETE", requests.last().method.value)
    }

    @Test
    fun badCredentials_areAuthFailed() {
        val p = provider { respond("", HttpStatusCode.Unauthorized) }
        assertError(CloudBackupError.AUTH_FAILED) { p.verify() }
    }

    @Test
    fun redirects_areNotFollowed() {
        val p = provider {
            respond("", HttpStatusCode.MovedPermanently, headersOf(HttpHeaders.Location, "http://elsewhere.test/"))
        }
        assertError(CloudBackupError.SERVER) { p.list() }
        assertEquals(1, requests.size)
    }

    @Test
    fun download_returnsBytes_andEnforcesCap() {
        val body = ByteArray(5000) { it.toByte() }
        val p = provider { respond(body, HttpStatusCode.OK) }
        val snapshot = RemoteSnapshot(name, name, Instant.EPOCH, null)
        assertArrayEquals(body, runBlocking { p.download(snapshot, maxBytes = 5000) })
        assertError(CloudBackupError.TOO_LARGE) { p.download(snapshot, maxBytes = 4999) }
    }

    @Test
    fun download_refusesNonSnapshotIds() {
        val p = provider { respond("", HttpStatusCode.OK) }
        assertError(CloudBackupError.NOT_FOUND) {
            p.download(RemoteSnapshot("../../etc/passwd", "x", Instant.EPOCH, null), 10)
        }
        assertTrue(requests.isEmpty())
    }

    @Test
    fun httpBaseUrl_isRefusedBeforeAnyRequest() {
        val e = assertThrows(CloudBackupException::class.java) {
            WebDavProvider(credentials.copy(baseUrl = "http://dav.example.test/"), HttpClient(MockEngine { respond("") }))
        }
        assertEquals(CloudBackupError.INSECURE_URL, e.error)
    }
}
