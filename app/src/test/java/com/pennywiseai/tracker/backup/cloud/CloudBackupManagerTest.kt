package com.pennywiseai.tracker.backup.cloud

import com.pennywiseai.tracker.data.backup.BackupExporter
import com.pennywiseai.tracker.data.backup.BackupImporter
import com.pennywiseai.tracker.data.backup.ExportBytesResult
import com.pennywiseai.tracker.data.backup.ExportPrivacy
import com.pennywiseai.tracker.data.backup.ImportResult
import com.pennywiseai.tracker.data.backup.ImportStrategy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CloudBackupManagerTest {

    /** Stand-in for the Keystore cipher; secrecy of this layer is covered by CloudSecretStoreTest. */
    private object PassThroughCipher : SecretCipher {
        override fun seal(plain: ByteArray, associatedData: ByteArray) = associatedData + 0 + plain
        override fun open(sealed: ByteArray, associatedData: ByteArray): ByteArray {
            require(sealed.copyOfRange(0, associatedData.size).contentEquals(associatedData))
            return sealed.copyOfRange(associatedData.size + 1, sealed.size)
        }
    }

    private class MapStore : SealedValueStore {
        val map = mutableMapOf<String, String>()
        override fun read(key: String) = map[key]
        override fun write(key: String, value: String): Boolean { map[key] = value; return true }
        override fun remove(keys: Collection<String>): Boolean { keys.forEach(map::remove); return true }
    }

    private class FakeProvider : CloudStorageProvider {
        override val type = CloudProviderType.WEBDAV
        val files = linkedMapOf<String, ByteArray>()
        var failUpload: CloudBackupError? = null
        override suspend fun verify() = Unit
        override suspend fun list() = files.keys.map { RemoteSnapshot(it, it, SnapshotNames.parseTimestamp(it)!!, files[it]!!.size.toLong()) }
        override suspend fun upload(name: String, bytes: ByteArray): RemoteSnapshot {
            failUpload?.let { throw CloudBackupException(it) }
            files[name] = bytes
            return RemoteSnapshot(name, name, SnapshotNames.parseTimestamp(name)!!, bytes.size.toLong())
        }
        override suspend fun download(snapshot: RemoteSnapshot, maxBytes: Long) = files.getValue(snapshot.id)
        override suspend fun delete(snapshot: RemoteSnapshot) { files.remove(snapshot.id) }
    }

    private val plaintext = """{"_format":"PennyWise Backup v1.3","note":"MERCHANT-XYZ"}""".toByteArray()
    private val exporter = mockk<BackupExporter>()
    private val importer = mockk<BackupImporter>()
    private val settings = mockk<CloudBackupSettings>(relaxed = true)
    private val drive = mockk<GoogleDriveFeature>(relaxed = true)
    private val secrets = CloudSecretStore(PassThroughCipher, MapStore())
    private val crypto = CloudBackupCrypto(iterations = CloudBackupCrypto.MIN_ITERATIONS)
    private val provider = FakeProvider()
    private var now = Instant.parse("2026-10-27T10:00:00Z")
    private var config = CloudBackupConfig(provider = CloudProviderType.WEBDAV, keepCount = 2)

    private val manager = CloudBackupManager(
        exporter, importer, settings, secrets, crypto, drive,
        webDavFactory = { provider },
        clock = { now },
    )

    init {
        coEvery { settings.current() } answers { config }
        coEvery { exporter.exportBackupBytes(any(), any()) } returns ExportBytesResult.Success(plaintext)
        secrets.put(CloudSecretStore.WEBDAV_URL, "https://dav.example.test/")
        secrets.put(CloudSecretStore.WEBDAV_USERNAME, "me")
        secrets.put(CloudSecretStore.WEBDAV_PASSWORD, "pw")
    }

    private fun assertError(error: CloudBackupError, block: suspend () -> Unit) {
        val e = assertThrows(CloudBackupException::class.java) { runBlocking { block() } }
        assertEquals(error, e.error)
    }

    @Test
    fun backupNow_uploadsOnlyEncryptedEnvelope_andRecordsSuccess() = runBlocking {
        manager.setPassphrase("a long enough passphrase".toCharArray())
        val snapshot = manager.backupNow()
        val uploaded = provider.files.getValue(snapshot.id)
        assertArrayEquals(CloudBackupCrypto.MAGIC, uploaded.copyOfRange(0, 4))
        assertFalse(String(uploaded, Charsets.ISO_8859_1).contains("MERCHANT-XYZ"))
        assertArrayEquals(plaintext, crypto.open(uploaded, "a long enough passphrase".toCharArray()))
        coVerify { exporter.exportBackupBytes(ExportPrivacy.FULL, false) }
        coVerify { settings.recordSuccess(now.toEpochMilli()) }
    }

    @Test
    fun backupNow_appliesRetention() = runBlocking {
        manager.setPassphrase("a long enough passphrase".toCharArray())
        repeat(4) {
            manager.backupNow()
            now = now.plusSeconds(3600)
        }
        assertEquals(2, provider.files.size)
        val newest = provider.files.keys.map { SnapshotNames.parseTimestamp(it)!! }.sorted()
        assertEquals(listOf(Instant.parse("2026-10-27T12:00:00Z"), Instant.parse("2026-10-27T13:00:00Z")), newest)
    }

    @Test
    fun backupNow_withoutPassphrase_uploadsNothing() {
        assertError(CloudBackupError.PASSPHRASE_NOT_SET) { manager.backupNow() }
        assertTrue(provider.files.isEmpty())
        coVerify { settings.recordFailure(any(), CloudBackupError.PASSPHRASE_NOT_SET) }
    }

    @Test
    fun backupNow_notConnected_fails() {
        config = config.copy(provider = CloudProviderType.NONE)
        manager.setPassphrase("a long enough passphrase".toCharArray())
        assertError(CloudBackupError.NOT_CONNECTED) { manager.backupNow() }
    }

    @Test
    fun backupNow_providerError_isRecorded() {
        manager.setPassphrase("a long enough passphrase".toCharArray())
        provider.failUpload = CloudBackupError.NETWORK
        assertError(CloudBackupError.NETWORK) { manager.backupNow() }
        coVerify { settings.recordFailure(any(), CloudBackupError.NETWORK) }
    }

    @Test
    fun shortPassphrase_isRejected() {
        assertError(CloudBackupError.PASSPHRASE_TOO_SHORT) { manager.setPassphrase("short".toCharArray()) }
        assertFalse(manager.hasPassphrase())
    }

    @Test
    fun restore_importsDecryptedBytes_withChosenStrategy() = runBlocking {
        manager.setPassphrase("a long enough passphrase".toCharArray())
        val snapshot = manager.backupNow()
        val bytes = slot<ByteArray>()
        coEvery { importer.importBackupBytes(capture(bytes), ImportStrategy.REPLACE_ALL) } returns
            ImportResult.Success(1, 2, 0)
        val result = manager.restore(snapshot, ImportStrategy.REPLACE_ALL)
        assertEquals(1, result.importedTransactions)
        assertArrayEquals(plaintext, bytes.captured)
    }

    @Test
    fun restore_wrongPassphrase_neverReachesImporter() = runBlocking {
        manager.setPassphrase("a long enough passphrase".toCharArray())
        val snapshot = manager.backupNow()
        assertError(CloudBackupError.WRONG_PASSPHRASE_OR_DAMAGED) {
            manager.restore(snapshot, ImportStrategy.MERGE, "a different passphrase".toCharArray())
        }
        coVerify(exactly = 0) { importer.importBackupBytes(any(), any()) }
    }

    @Test
    fun restore_tamperedSnapshot_neverReachesImporter() = runBlocking {
        manager.setPassphrase("a long enough passphrase".toCharArray())
        val snapshot = manager.backupNow()
        val stored = provider.files.getValue(snapshot.id)
        stored[stored.size - 3] = (stored[stored.size - 3].toInt() xor 1).toByte()
        assertError(CloudBackupError.WRONG_PASSPHRASE_OR_DAMAGED) { manager.restore(snapshot, ImportStrategy.MERGE) }
        coVerify(exactly = 0) { importer.importBackupBytes(any(), any()) }
    }

    @Test
    fun disconnect_clearsCredentials_butKeepsPassphrase() = runBlocking {
        manager.setPassphrase("a long enough passphrase".toCharArray())
        manager.disconnect()
        assertFalse(secrets.contains(CloudSecretStore.WEBDAV_URL))
        assertFalse(secrets.contains(CloudSecretStore.WEBDAV_USERNAME))
        assertFalse(secrets.contains(CloudSecretStore.WEBDAV_PASSWORD))
        assertTrue(manager.hasPassphrase())
        coVerify { settings.clearConnection() }
    }

    @Test
    fun connectWebDav_refusesHttp_andStoresNothing() {
        manager.disconnectBlocking()
        assertError(CloudBackupError.INSECURE_URL) { manager.connectWebDav("http://dav.example.test/", "me", "pw") }
        assertFalse(secrets.contains(CloudSecretStore.WEBDAV_PASSWORD))
    }

    private fun CloudBackupManager.disconnectBlocking() = runBlocking { disconnect() }
}
