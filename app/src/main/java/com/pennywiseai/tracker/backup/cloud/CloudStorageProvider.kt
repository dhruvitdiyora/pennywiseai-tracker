package com.pennywiseai.tracker.backup.cloud

import java.security.SecureRandom
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class CloudProviderType { NONE, GOOGLE_DRIVE, WEBDAV }

/** One encrypted backup file on a provider. [id] is provider-specific (Drive file id, WebDAV file name). */
data class RemoteSnapshot(
    val id: String,
    val name: String,
    val createdAt: Instant,
    val sizeBytes: Long?,
)

/**
 * A place that stores opaque, already-encrypted backup files. Providers never
 * see backup content: [CloudBackupManager] hands them sealed envelopes only.
 * Every failure is a [CloudBackupException].
 */
interface CloudStorageProvider {
    val type: CloudProviderType

    /** Checks the connection and credentials, creating the backup folder if needed. */
    suspend fun verify()

    /** PennyWise snapshots only (see [SnapshotNames]); other files are ignored. */
    suspend fun list(): List<RemoteSnapshot>

    suspend fun upload(name: String, bytes: ByteArray): RemoteSnapshot

    /** Fails with TOO_LARGE as soon as more than [maxBytes] arrive. */
    suspend fun download(snapshot: RemoteSnapshot, maxBytes: Long): ByteArray

    suspend fun delete(snapshot: RemoteSnapshot)
}

/**
 * Snapshot file names: `pennywise-backup-20261027T101530Z-1a2b3c.pwenc`.
 *
 * The UTC timestamp orders snapshots (lexically and chronologically) without
 * trusting provider metadata; the random suffix keeps two devices backing up in
 * the same second from overwriting each other. Only names that match are
 * listed, restored or deleted by retention, so other files in the folder are
 * never touched.
 */
object SnapshotNames {
    private const val PREFIX = "pennywise-backup-"
    const val EXTENSION = ".pwenc"
    private val PATTERN = Regex("""^pennywise-backup-(\d{8}T\d{6}Z)-([0-9a-f]{6})\.pwenc$""")
    private val FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
    private val random = SecureRandom()

    fun newName(now: Instant): String {
        val suffix = ByteArray(3).also(random::nextBytes).joinToString("") { "%02x".format(it) }
        return PREFIX + FORMAT.format(now) + "-" + suffix + EXTENSION
    }

    /** The creation time encoded in [name], or null if it isn't a PennyWise snapshot name. */
    fun parseTimestamp(name: String): Instant? {
        val match = PATTERN.matchEntire(name) ?: return null
        return runCatching { Instant.from(FORMAT.parse(match.groupValues[1])) }.getOrNull()
    }

    fun isSnapshot(name: String): Boolean = parseTimestamp(name) != null
}

object CloudRetention {
    val KEEP_OPTIONS = listOf(3, 5, 10, 20)
    const val DEFAULT_KEEP = 5

    /**
     * Snapshots to delete so that only the newest [keep] remain. [protectedId]
     * (the file just uploaded) is never returned, even if clocks disagree.
     */
    fun toDelete(snapshots: List<RemoteSnapshot>, keep: Int, protectedId: String?): List<RemoteSnapshot> {
        val limit = keep.coerceAtLeast(1)
        val newestFirst = snapshots
            .filter { SnapshotNames.isSnapshot(it.name) }
            .sortedWith(compareByDescending<RemoteSnapshot> { it.createdAt }.thenByDescending { it.name })
        val kept = newestFirst.take(limit).map { it.id }.toMutableSet()
        if (protectedId != null && protectedId !in kept) {
            // Keep the fresh upload in place of the oldest otherwise-kept one.
            newestFirst.take(limit).lastOrNull()?.let { kept.remove(it.id) }
            kept.add(protectedId)
        }
        return newestFirst.filter { it.id !in kept }
    }
}
