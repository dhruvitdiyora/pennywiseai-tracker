package com.pennywiseai.tracker.backup.cloud

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.TextContent
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CancellationException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLDecoder
import java.util.Base64
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader

/** Validation for user-entered WebDAV addresses. */
object WebDavUrls {
    /**
     * Parses [input] as the base folder URL. Only `https://` is accepted, so
     * neither the Basic credentials nor the (encrypted) file ever travel in
     * the clear; credentials embedded in the URL are refused so they can't
     * end up in plain preferences or logs.
     *
     * @throws CloudBackupException INSECURE_URL
     */
    fun parseBaseUrl(input: String): Url {
        val trimmed = input.trim()
        if (!trimmed.startsWith("https://", ignoreCase = true)) insecure()
        // Check the authority on the raw text: the URL parser fills in defaults (e.g. localhost).
        val authority = trimmed.substring("https://".length).substringBefore('/').substringBefore('?').substringBefore('#')
        if (authority.isBlank() || authority.contains('@') || authority.contains(' ')) insecure()
        val url = runCatching { Url(trimmed) }.getOrElse { insecure() }
        if (url.protocol != URLProtocol.HTTPS) insecure()
        if (url.host.isBlank() || !authority.lowercase().startsWith(url.host.lowercase())) insecure()
        if (url.user != null || url.password != null) insecure()
        if (url.encodedQuery.isNotEmpty() || url.fragment.isNotEmpty()) insecure()
        return URLBuilder(url).apply {
            if (!encodedPath.endsWith("/")) encodedPath += "/"
        }.build()
    }

    private fun insecure(): Nothing = throw CloudBackupException(CloudBackupError.INSECURE_URL)
}

/** WebDAV credentials, kept only in [CloudSecretStore]. */
data class WebDavCredentials(val baseUrl: String, val username: String, val password: String) {
    override fun toString(): String = "WebDavCredentials(<redacted>)"
}

/**
 * WebDAV storage under `<base>/PennyWise/`. Works with any RFC 4918 server
 * (Nextcloud, ownCloud, Synology, Apache mod_dav, …).
 *
 * The [client] must not follow redirects: a redirect could forward the Basic
 * credentials to another host or downgrade to http. Redirects are surfaced as
 * a server error instead.
 */
class WebDavProvider(
    credentials: WebDavCredentials,
    private val client: HttpClient,
) : CloudStorageProvider {

    override val type = CloudProviderType.WEBDAV

    private val folderUrl: Url = URLBuilder(WebDavUrls.parseBaseUrl(credentials.baseUrl)).apply {
        encodedPath += "$FOLDER_NAME/"
    }.build()

    private val authorization = "Basic " + Base64.getEncoder().encodeToString(
        "${credentials.username}:${credentials.password}".toByteArray(Charsets.UTF_8)
    )

    override suspend fun verify() {
        ensureFolder()
        val response = send(PROPFIND, folderUrl) {
            header("Depth", "0")
            setBody(TextContent(PROPFIND_BODY, ContentType.Application.Xml))
        }
        if (response.status.value != 207 && response.status.value !in 200..299) fail(response)
    }

    override suspend fun list(): List<RemoteSnapshot> {
        val response = send(PROPFIND, folderUrl) {
            header("Depth", "1")
            setBody(TextContent(PROPFIND_BODY, ContentType.Application.Xml))
        }
        if (response.status.value == 404) return emptyList()
        if (response.status.value != 207) fail(response)
        val xml = String(readCapped(response.bodyAsChannel(), MAX_LISTING_BYTES), Charsets.UTF_8)
        return WebDavXml.parseMultistatus(xml).mapNotNull { entry ->
            if (entry.isCollection) return@mapNotNull null
            val name = entry.fileName ?: return@mapNotNull null
            val createdAt = SnapshotNames.parseTimestamp(name) ?: return@mapNotNull null
            RemoteSnapshot(id = name, name = name, createdAt = createdAt, sizeBytes = entry.contentLength)
        }
    }

    override suspend fun upload(name: String, bytes: ByteArray): RemoteSnapshot {
        val createdAt = SnapshotNames.parseTimestamp(name)
            ?: throw IllegalArgumentException("not a snapshot name")
        ensureFolder()
        // Upload under a temporary name, then MOVE into place: an interrupted
        // upload never leaves a truncated file that looks like a snapshot.
        val partUrl = fileUrl(name + PART_SUFFIX)
        val finalUrl = fileUrl(name)
        val put = send(HttpMethod.Put, partUrl) {
            setBody(ByteArrayContent(bytes, ContentType.Application.OctetStream))
        }
        if (put.status.value !in 200..299) {
            runCatching { send(HttpMethod.Delete, partUrl) }
            fail(put)
        }
        val move = send(MOVE, partUrl) {
            header("Destination", finalUrl.toString())
            header("Overwrite", "F")
        }
        if (move.status.value !in 200..299) {
            runCatching { send(HttpMethod.Delete, partUrl) }
            fail(move)
        }
        return RemoteSnapshot(id = name, name = name, createdAt = createdAt, sizeBytes = bytes.size.toLong())
    }

    override suspend fun download(snapshot: RemoteSnapshot, maxBytes: Long): ByteArray {
        requireSnapshotName(snapshot.id)
        return try {
            client.prepareRequest {
                configure(HttpMethod.Get, fileUrl(snapshot.id))
            }.execute { response ->
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

    override suspend fun delete(snapshot: RemoteSnapshot) {
        requireSnapshotName(snapshot.id)
        val response = send(HttpMethod.Delete, fileUrl(snapshot.id))
        if (response.status.value !in 200..299 && response.status.value != 404) fail(response)
    }

    private suspend fun ensureFolder() {
        val response = send(MKCOL, folderUrl)
        when (response.status.value) {
            in 200..299, 405 -> Unit // created, or already exists
            409 -> throw CloudBackupException(CloudBackupError.NOT_FOUND) // parent folder missing
            else -> fail(response)
        }
    }

    private fun fileUrl(name: String): Url = URLBuilder(folderUrl).apply { encodedPath += name }.build()

    private fun requireSnapshotName(name: String) {
        if (!SnapshotNames.isSnapshot(name)) throw CloudBackupException(CloudBackupError.NOT_FOUND)
    }

    private fun HttpRequestBuilder.configure(method: HttpMethod, target: Url) {
        this.method = method
        url(target)
        header(HttpHeaders.Authorization, authorization)
    }

    private suspend fun send(
        method: HttpMethod,
        target: Url,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = try {
        client.request {
            configure(method, target)
            block()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw CloudBackupException(CloudBackupError.NETWORK, e)
    }

    private fun fail(response: HttpResponse): Nothing = throw CloudBackupException(errorFor(response.status.value))

    companion object {
        const val FOLDER_NAME = "PennyWise"
        private const val PART_SUFFIX = ".part"
        private const val MAX_LISTING_BYTES = 4L * 1024 * 1024
        private val PROPFIND = HttpMethod("PROPFIND")
        private val MKCOL = HttpMethod("MKCOL")
        private val MOVE = HttpMethod("MOVE")
        private const val PROPFIND_BODY = """<?xml version="1.0" encoding="utf-8"?>""" +
            """<d:propfind xmlns:d="DAV:"><d:prop><d:resourcetype/><d:getcontentlength/></d:prop></d:propfind>"""

        internal fun errorFor(status: Int): CloudBackupError = when (status) {
            401, 403 -> CloudBackupError.AUTH_FAILED
            404, 409, 410 -> CloudBackupError.NOT_FOUND
            413 -> CloudBackupError.TOO_LARGE
            507 -> CloudBackupError.STORAGE_FULL
            408, 429 -> CloudBackupError.NETWORK
            in 300..399 -> CloudBackupError.SERVER // redirects are refused, never followed
            in 500..599 -> CloudBackupError.SERVER
            else -> CloudBackupError.UNKNOWN
        }
    }
}

/** Reads at most [maxBytes] from [channel]; one byte more fails with TOO_LARGE. */
internal suspend fun readCapped(channel: ByteReadChannel, maxBytes: Long): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(64 * 1024)
    var total = 0L
    while (true) {
        val n = channel.readAvailable(buffer, 0, buffer.size)
        if (n < 0) break
        if (n == 0) {
            if (channel.isClosedForRead) break
            continue
        }
        total += n
        if (total > maxBytes) throw CloudBackupException(CloudBackupError.TOO_LARGE)
        out.write(buffer, 0, n)
    }
    channel.closedCause?.let { throw IOException(it) }
    return out.toByteArray()
}

internal object WebDavXml {
    data class Entry(val href: String, val isCollection: Boolean, val contentLength: Long?) {
        /** Last path segment, URL-decoded. */
        val fileName: String?
            get() = href.trimEnd('/').substringAfterLast('/').takeIf { it.isNotEmpty() }
                ?.let { runCatching { URLDecoder.decode(it.replace("+", "%2B"), "UTF-8") }.getOrNull() }
    }

    /** Parses a PROPFIND multistatus. Documents with a DTD are rejected (no entity expansion). */
    fun parseMultistatus(xml: String): List<Entry> {
        if (xml.contains("<!DOCTYPE", ignoreCase = true) || xml.contains("<!ENTITY", ignoreCase = true)) {
            throw CloudBackupException(CloudBackupError.SERVER)
        }
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }
        val document = try {
            factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        } catch (e: Exception) {
            throw CloudBackupException(CloudBackupError.SERVER, e)
        }
        val responses = document.getElementsByTagNameNS(DAV, "response")
        return (0 until responses.length).mapNotNull { i ->
            val response = responses.item(i) as? Element ?: return@mapNotNull null
            val href = response.firstText("href")?.trim() ?: return@mapNotNull null
            val isCollection = response.getElementsByTagNameNS(DAV, "collection").length > 0
            val length = response.firstText("getcontentlength")?.trim()?.toLongOrNull()
            Entry(href, isCollection, length)
        }
    }

    private fun Element.firstText(localName: String): String? =
        getElementsByTagNameNS(DAV, localName).takeIf { it.length > 0 }?.item(0)?.textContent

    private const val DAV = "DAV:"
}
