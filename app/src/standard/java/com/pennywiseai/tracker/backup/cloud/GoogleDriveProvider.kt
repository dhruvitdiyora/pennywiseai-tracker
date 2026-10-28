package com.pennywiseai.tracker.backup.cloud

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.ByteArrayContent
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.security.SecureRandom

/** Short-lived OAuth access tokens for the `drive.appdata` scope. */
interface DriveTokenSource {
    /** @throws CloudBackupException AUTH_REQUIRED when the user must grant access again. */
    suspend fun token(): String
    suspend fun invalidate(token: String)
}

/**
 * Google Drive REST v3 against the hidden, app-private `appDataFolder`. The
 * app can only see files it created there; the user can't browse them in
 * Drive (they can delete them from Drive settings → Manage apps).
 */
class GoogleDriveProvider(
    private val tokens: DriveTokenSource,
    private val client: HttpClient,
) : CloudStorageProvider {

    override val type = CloudProviderType.GOOGLE_DRIVE

    override suspend fun verify() {
        val response = authorized(HttpMethod.Get, "$API/files") {
            parameter("spaces", APP_DATA)
            parameter("pageSize", 1)
            parameter("fields", "files(id)")
        }
        if (response.status.value !in 200..299) fail(response)
    }

    override suspend fun list(): List<RemoteSnapshot> {
        val result = mutableListOf<RemoteSnapshot>()
        var pageToken: String? = null
        var pages = 0
        do {
            val response = authorized(HttpMethod.Get, "$API/files") {
                parameter("spaces", APP_DATA)
                parameter("q", "name contains 'pennywise-backup-' and trashed = false")
                parameter("fields", "nextPageToken,files(id,name,size)")
                parameter("pageSize", 200)
                pageToken?.let { parameter("pageToken", it) }
            }
            if (response.status.value !in 200..299) fail(response)
            val page = json.decodeFromString(FileList.serializer(), bodyText(response))
            page.files.forEach { file ->
                val createdAt = SnapshotNames.parseTimestamp(file.name) ?: return@forEach
                if (!isValidId(file.id)) return@forEach
                result += RemoteSnapshot(file.id, file.name, createdAt, file.size?.toLongOrNull())
            }
            pageToken = page.nextPageToken
        } while (pageToken != null && ++pages < MAX_PAGES)
        return result
    }

    override suspend fun upload(name: String, bytes: ByteArray): RemoteSnapshot {
        val createdAt = SnapshotNames.parseTimestamp(name) ?: throw IllegalArgumentException("not a snapshot name")
        val boundary = "pennywise-" + ByteArray(12).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) }
        val metadata = """{"name":"$name","parents":["$APP_DATA"]}"""
        val body = ByteArrayOutputStream(bytes.size + 512).apply {
            write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n".toByteArray())
            write("--$boundary\r\nContent-Type: application/octet-stream\r\n\r\n".toByteArray())
            write(bytes)
            write("\r\n--$boundary--\r\n".toByteArray())
        }.toByteArray()
        // Multipart upload is a single request: the file appears complete or not at all.
        val response = authorized(HttpMethod.Post, "$UPLOAD/files") {
            parameter("uploadType", "multipart")
            parameter("fields", "id,name,size")
            setBody(ByteArrayContent(body, ContentType("multipart", "related").withParameter("boundary", boundary)))
        }
        if (response.status.value !in 200..299) fail(response)
        val file = json.decodeFromString(DriveFile.serializer(), bodyText(response))
        return RemoteSnapshot(file.id, name, createdAt, file.size?.toLongOrNull() ?: bytes.size.toLong())
    }

    override suspend fun download(snapshot: RemoteSnapshot, maxBytes: Long): ByteArray {
        requireValidId(snapshot.id)
        return withToken { token ->
            try {
                client.prepareRequest {
                    configure(HttpMethod.Get, "$API/files/${snapshot.id}", token)
                    parameter("alt", "media")
                }.execute { response ->
                    if (response.status.value == 401) return@execute null
                    if (response.status.value !in 200..299) fail(response)
                    val declared = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                    if (declared != null && declared > maxBytes) throw CloudBackupException(CloudBackupError.TOO_LARGE)
                    readCapped(response.bodyAsChannel(), maxBytes)
                }
            } catch (e: CloudBackupException) {
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw CloudBackupException(CloudBackupError.NETWORK, e)
            }
        }
    }

    override suspend fun delete(snapshot: RemoteSnapshot) {
        requireValidId(snapshot.id)
        val response = authorized(HttpMethod.Delete, "$API/files/${snapshot.id}")
        if (response.status.value !in 200..299 && response.status.value != 404) fail(response)
    }

    /** Runs [block] with a token; on a 401 (block returns null) invalidates it and tries once more. */
    private suspend fun <T : Any> withToken(block: suspend (String) -> T?): T {
        val first = tokens.token()
        block(first)?.let { return it }
        tokens.invalidate(first)
        return block(tokens.token()) ?: throw CloudBackupException(CloudBackupError.AUTH_REQUIRED)
    }

    private suspend fun authorized(
        method: HttpMethod,
        target: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = withToken { token ->
        val response = try {
            client.request {
                configure(method, target, token)
                block()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw CloudBackupException(CloudBackupError.NETWORK, e)
        }
        response.takeUnless { it.status.value == 401 }
    }

    private fun HttpRequestBuilder.configure(method: HttpMethod, target: String, token: String) {
        this.method = method
        url(target)
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private suspend fun bodyText(response: HttpResponse): String =
        String(readCapped(response.bodyAsChannel(), MAX_JSON_BYTES), Charsets.UTF_8)

    private suspend fun fail(response: HttpResponse): Nothing {
        val status = response.status.value
        val reason = if (status == 403) runCatching { bodyText(response) }.getOrDefault("") else ""
        throw CloudBackupException(errorFor(status, reason))
    }

    private fun isValidId(id: String) = ID_PATTERN.matches(id)

    private fun requireValidId(id: String) {
        if (!isValidId(id)) throw CloudBackupException(CloudBackupError.NOT_FOUND)
    }

    @Serializable
    private data class FileList(
        val files: List<DriveFile> = emptyList(),
        val nextPageToken: String? = null,
    )

    @Serializable
    private data class DriveFile(
        val id: String = "",
        val name: String = "",
        @SerialName("size") val size: String? = null,
    )

    companion object {
        const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private const val APP_DATA = "appDataFolder"
        private const val MAX_PAGES = 20
        private const val MAX_JSON_BYTES = 2L * 1024 * 1024
        private val ID_PATTERN = Regex("^[A-Za-z0-9_-]{1,200}$")
        private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

        internal fun errorFor(status: Int, body: String): CloudBackupError = when {
            status == 401 -> CloudBackupError.AUTH_REQUIRED
            status == 403 && (body.contains("storageQuotaExceeded") || body.contains("quotaExceeded\"")) ->
                CloudBackupError.STORAGE_FULL
            status == 403 && (body.contains("accessNotConfigured") || body.contains("SERVICE_DISABLED")) ->
                CloudBackupError.DRIVE_NOT_CONFIGURED
            status == 403 && body.contains("RateLimitExceeded", ignoreCase = true) -> CloudBackupError.SERVER
            status == 403 -> CloudBackupError.AUTH_FAILED
            status == 404 -> CloudBackupError.NOT_FOUND
            status == 413 -> CloudBackupError.TOO_LARGE
            status == 429 || status in 500..599 -> CloudBackupError.SERVER
            else -> CloudBackupError.UNKNOWN
        }
    }
}

