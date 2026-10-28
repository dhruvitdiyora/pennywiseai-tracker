package com.pennywiseai.tracker.backup.cloud

import com.pennywiseai.tracker.worker.CloudBackupWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CloudRetentionAndUrlTest {

    private fun snapshot(epochSecond: Long): RemoteSnapshot {
        val at = Instant.ofEpochSecond(epochSecond)
        val name = SnapshotNames.newName(at)
        return RemoteSnapshot(id = "id-$epochSecond", name = name, createdAt = at, sizeBytes = 10)
    }

    // ---- Snapshot names ------------------------------------------------------------

    @Test
    fun snapshotName_roundTripsTimestamp_andIsUniquePerCall() {
        val at = Instant.parse("2026-10-27T10:15:30Z")
        val a = SnapshotNames.newName(at)
        val b = SnapshotNames.newName(at)
        assertTrue(a.startsWith("pennywise-backup-20261027T101530Z-"))
        assertTrue(a.endsWith(".pwenc"))
        assertEquals(at, SnapshotNames.parseTimestamp(a))
        assertTrue(a != b) // two devices in the same second don't collide
    }

    @Test
    fun foreignNames_areNotSnapshots() {
        listOf(
            "PennyWise_Backup.pennywisebackup",
            "pennywise-backup-20261027T101530Z-1a2b3c.pwenc.part",
            "pennywise-backup-20261027T101530Z-1A2B3C.pwenc",
            "../pennywise-backup-20261027T101530Z-1a2b3c.pwenc",
            "notes.txt",
        ).forEach { assertNull(it, SnapshotNames.parseTimestamp(it)) }
    }

    // ---- Retention -----------------------------------------------------------------

    @Test
    fun retention_keepsNewestN() {
        val all = (1L..8L).map { snapshot(it * 1000) }.shuffled()
        val deleted = CloudRetention.toDelete(all, keep = 3, protectedId = "id-8000")
        assertEquals((1L..5L).map { "id-${it * 1000}" }.toSet(), deleted.map { it.id }.toSet())
    }

    @Test
    fun retention_neverDeletesJustUploaded_evenWithSkewedClock() {
        // The fresh upload's timestamp is older than others (another device's clock runs ahead).
        val all = listOf(snapshot(100), snapshot(5000), snapshot(6000), snapshot(7000))
        val deleted = CloudRetention.toDelete(all, keep = 2, protectedId = "id-100")
        assertFalse(deleted.any { it.id == "id-100" })
        assertEquals(2, all.size - deleted.size)
    }

    @Test
    fun retention_ignoresForeignFiles_andNoopsWhenUnderLimit() {
        val foreign = RemoteSnapshot("x", "my-notes.txt", Instant.EPOCH, 1)
        val all = listOf(snapshot(1), snapshot(2), foreign)
        assertTrue(CloudRetention.toDelete(all, keep = 5, protectedId = null).isEmpty())
        assertFalse(CloudRetention.toDelete(all, keep = 1, protectedId = null).contains(foreign))
    }

    @Test
    fun retention_keepAtLeastOne() {
        val all = listOf(snapshot(1), snapshot(2))
        assertEquals(listOf("id-1"), CloudRetention.toDelete(all, keep = 0, protectedId = null).map { it.id })
    }

    // ---- https enforcement ---------------------------------------------------------

    private fun assertInsecure(url: String) {
        val e = assertThrows(url, CloudBackupException::class.java) { WebDavUrls.parseBaseUrl(url) }
        assertEquals(CloudBackupError.INSECURE_URL, e.error)
    }

    @Test
    fun webDavUrl_rejectsPlainHttpAndOtherSchemes() {
        assertInsecure("http://dav.example.test/remote.php/dav/files/me/")
        assertInsecure("HTTP://dav.example.test/")
        assertInsecure("ftp://dav.example.test/")
        assertInsecure("dav.example.test/files")
        assertInsecure("webdav://dav.example.test/")
        assertInsecure("")
        assertInsecure("https://")
    }

    @Test
    fun webDavUrl_rejectsEmbeddedCredentialsQueryAndFragment() {
        assertInsecure("https://me:pw@dav.example.test/")
        assertInsecure("https://me@dav.example.test/")
        assertInsecure("https://dav.example.test/?token=abc")
        assertInsecure("https://dav.example.test/#frag")
    }

    @Test
    fun webDavUrl_acceptsHttps_andNormalisesTrailingSlash() {
        assertEquals(
            "https://dav.example.test/remote.php/dav/files/me/",
            WebDavUrls.parseBaseUrl("  https://dav.example.test/remote.php/dav/files/me  ").toString(),
        )
        assertEquals("https://dav.example.test:8443/", WebDavUrls.parseBaseUrl("HTTPS://dav.example.test:8443").toString())
    }

    // ---- Worker retry bound --------------------------------------------------------

    @Test
    fun worker_retriesOnlyTransientErrors_boundedAttempts() {
        assertTrue(CloudBackupWorker.shouldRetry(CloudBackupError.NETWORK.retryable, 0))
        assertTrue(CloudBackupWorker.shouldRetry(CloudBackupError.SERVER.retryable, 1))
        assertFalse(CloudBackupWorker.shouldRetry(CloudBackupError.NETWORK.retryable, 2))
        assertFalse(CloudBackupWorker.shouldRetry(CloudBackupError.AUTH_REQUIRED.retryable, 0))
        assertFalse(CloudBackupWorker.shouldRetry(CloudBackupError.PASSPHRASE_NOT_SET.retryable, 0))
        assertFalse(CloudBackupWorker.shouldRetry(CloudBackupError.WRONG_PASSPHRASE_OR_DAMAGED.retryable, 0))
    }
}
