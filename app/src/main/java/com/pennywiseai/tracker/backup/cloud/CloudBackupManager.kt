package com.pennywiseai.tracker.backup.cloud

import android.content.Intent
import android.util.Log
import com.pennywiseai.tracker.data.backup.BackupExporter
import com.pennywiseai.tracker.data.backup.BackupImporter
import com.pennywiseai.tracker.data.backup.ExportBytesResult
import com.pennywiseai.tracker.data.backup.ExportPrivacy
import com.pennywiseai.tracker.data.backup.ImportResult
import com.pennywiseai.tracker.data.backup.ImportStrategy
import com.pennywiseai.tracker.di.CloudBackupHttpClient
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud backup orchestration. [BackupExporter] / [BackupImporter] stay the only
 * producers and consumers of backup bytes; this class seals them with
 * [CloudBackupCrypto] and moves the opaque envelope through a
 * [CloudStorageProvider]. There is no code path that uploads plaintext.
 *
 * Logs carry error codes only — never credentials, tokens, URLs, paths or content.
 */
@Singleton
class CloudBackupManager internal constructor(
    private val exporter: BackupExporter,
    private val importer: BackupImporter,
    private val settings: CloudBackupSettings,
    private val secrets: CloudSecretStore,
    private val crypto: CloudBackupCrypto,
    private val drive: GoogleDriveFeature,
    private val webDavFactory: (WebDavCredentials) -> CloudStorageProvider,
    private val clock: () -> Instant,
) {
    @Inject constructor(
        exporter: BackupExporter,
        importer: BackupImporter,
        settings: CloudBackupSettings,
        secrets: CloudSecretStore,
        crypto: CloudBackupCrypto,
        drive: GoogleDriveFeature,
        @CloudBackupHttpClient httpClient: HttpClient,
    ) : this(
        exporter, importer, settings, secrets, crypto, drive,
        webDavFactory = { WebDavProvider(it, httpClient) },
        clock = Instant::now,
    )

    /** One backup / restore / delete at a time, across the UI and the worker. */
    private val operationLock = Mutex()

    val isDriveAvailable: Boolean get() = drive.isAvailable

    /** The connected Google account, for display; null if unknown or unreadable. */
    fun driveAccountName(): String? = runCatching { secrets.get(CloudSecretStore.DRIVE_ACCOUNT) }.getOrNull()

    // ---- Passphrase ----------------------------------------------------------------------

    fun hasPassphrase(): Boolean = secrets.contains(CloudSecretStore.PASSPHRASE)

    fun setPassphrase(passphrase: CharArray) {
        if (passphrase.size < CloudBackupCrypto.MIN_PASSPHRASE_LENGTH) {
            throw CloudBackupException(CloudBackupError.PASSPHRASE_TOO_SHORT)
        }
        secrets.put(CloudSecretStore.PASSPHRASE, String(passphrase))
    }

    fun removePassphrase() = secrets.remove(CloudSecretStore.PASSPHRASE)

    // ---- Connections ---------------------------------------------------------------------

    /** Validates https, checks the server and credentials, then stores them (sealed). */
    suspend fun connectWebDav(url: String, username: String, password: String) {
        val base = WebDavUrls.parseBaseUrl(url)
        val credentials = WebDavCredentials(base.toString(), username, password)
        webDavFactory(credentials).verify()
        val previous = settings.current().provider
        if (previous == CloudProviderType.GOOGLE_DRIVE) runCatching { drive.disconnect() }
        secrets.put(CloudSecretStore.WEBDAV_URL, credentials.baseUrl)
        secrets.put(CloudSecretStore.WEBDAV_USERNAME, username)
        secrets.put(CloudSecretStore.WEBDAV_PASSWORD, password)
        settings.setProvider(CloudProviderType.WEBDAV, webDavHost = base.host)
    }

    suspend fun connectDrive(): DriveConnectResult = onDriveResult(drive.connect())

    suspend fun completeDriveConnect(data: Intent?): DriveConnectResult = onDriveResult(drive.completeConnect(data))

    private suspend fun onDriveResult(result: DriveConnectResult): DriveConnectResult {
        if (result is DriveConnectResult.Connected) {
            try {
                drive.provider().verify()
            } catch (e: CloudBackupException) {
                return DriveConnectResult.Failed(e.error)
            }
            clearWebDavSecrets()
            settings.setProvider(CloudProviderType.GOOGLE_DRIVE)
        }
        return result
    }

    /** Signs out of / forgets the active provider and stops automatic backups. The passphrase stays. */
    suspend fun disconnect() {
        if (settings.current().provider == CloudProviderType.GOOGLE_DRIVE) {
            runCatching { drive.disconnect() }
        }
        clearWebDavSecrets()
        settings.clearConnection()
    }

    private fun clearWebDavSecrets() = secrets.remove(
        CloudSecretStore.WEBDAV_URL,
        CloudSecretStore.WEBDAV_USERNAME,
        CloudSecretStore.WEBDAV_PASSWORD,
    )

    private suspend fun activeProvider(): CloudStorageProvider = when (settings.current().provider) {
        CloudProviderType.NONE -> throw CloudBackupException(CloudBackupError.NOT_CONNECTED)
        CloudProviderType.GOOGLE_DRIVE -> drive.provider()
        CloudProviderType.WEBDAV -> {
            val url = secrets.get(CloudSecretStore.WEBDAV_URL)
            val user = secrets.get(CloudSecretStore.WEBDAV_USERNAME)
            val password = secrets.get(CloudSecretStore.WEBDAV_PASSWORD)
            if (url == null || user == null || password == null) {
                throw CloudBackupException(CloudBackupError.NOT_CONNECTED)
            }
            webDavFactory(WebDavCredentials(url, user, password))
        }
    }

    private fun storedPassphrase(): CharArray =
        secrets.get(CloudSecretStore.PASSPHRASE)?.toCharArray()
            ?: throw CloudBackupException(CloudBackupError.PASSPHRASE_NOT_SET)

    // ---- Operations ----------------------------------------------------------------------

    /**
     * Export → encrypt → upload → apply retention. Records the outcome (time
     * and error code) for the status row. Retention failures don't fail the
     * backup: the new snapshot is already safe.
     */
    suspend fun backupNow(): RemoteSnapshot = operationLock.withLock {
        recordingOutcome {
            val config = settings.current()
            val provider = activeProvider()
            val passphrase = storedPassphrase()
            val plaintext = when (val export = exporter.exportBackupBytes(ExportPrivacy.FULL, config.includeReceipts)) {
                is ExportBytesResult.Success -> export.bytes
                is ExportBytesResult.Error -> throw CloudBackupException(CloudBackupError.EXPORT_FAILED)
            }
            val envelope = withContext(Dispatchers.Default) {
                try {
                    crypto.seal(plaintext, passphrase)
                } catch (e: OutOfMemoryError) {
                    throw CloudBackupException(CloudBackupError.TOO_LARGE)
                } finally {
                    passphrase.fill('\u0000')
                }
            }
            check(envelope.copyOfRange(0, CloudBackupCrypto.MAGIC.size).contentEquals(CloudBackupCrypto.MAGIC))
            if (envelope.size > CloudBackupCrypto.MAX_ENVELOPE_BYTES) {
                throw CloudBackupException(CloudBackupError.TOO_LARGE)
            }
            val uploaded = provider.upload(SnapshotNames.newName(clock()), envelope)
            applyRetention(provider, config.keepCount, uploaded.id)
            uploaded
        }
    }

    private suspend fun applyRetention(provider: CloudStorageProvider, keep: Int, protectedId: String) {
        try {
            val stale = CloudRetention.toDelete(provider.list(), keep, protectedId)
            stale.forEach { provider.delete(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: CloudBackupException) {
            Log.w(TAG, "Retention skipped: ${e.error}")
        }
    }

    suspend fun listSnapshots(): List<RemoteSnapshot> =
        activeProvider().list().sortedByDescending { it.createdAt }

    /**
     * Download → authenticate + decrypt → import with the user's [strategy].
     * Nothing is imported unless the GCM tag verifies.
     *
     * @param passphrase a passphrase typed for this restore (e.g. on a new
     * phone, or for a snapshot made before a passphrase change); null uses the stored one.
     */
    suspend fun restore(
        snapshot: RemoteSnapshot,
        strategy: ImportStrategy,
        passphrase: CharArray? = null,
    ): ImportResult.Success = operationLock.withLock {
        val provider = activeProvider()
        val key = passphrase ?: storedPassphrase()
        val envelope = provider.download(snapshot, CloudBackupCrypto.MAX_ENVELOPE_BYTES)
        val plaintext = withContext(Dispatchers.Default) {
            try {
                crypto.open(envelope, key)
            } catch (e: OutOfMemoryError) {
                throw CloudBackupException(CloudBackupError.TOO_LARGE)
            } finally {
                key.fill('\u0000')
            }
        }
        when (val result = importer.importBackupBytes(plaintext, strategy)) {
            is ImportResult.Success -> result
            is ImportResult.Error -> throw CloudBackupException(CloudBackupError.IMPORT_FAILED)
        }
    }

    suspend fun deleteSnapshot(snapshot: RemoteSnapshot) = operationLock.withLock {
        activeProvider().delete(snapshot)
    }

    private suspend fun <T> recordingOutcome(block: suspend () -> T): T {
        try {
            val result = block()
            settings.recordSuccess(clock().toEpochMilli())
            return result
        } catch (e: CancellationException) {
            throw e
        } catch (e: CloudBackupException) {
            settings.recordFailure(clock().toEpochMilli(), e.error)
            Log.w(TAG, "Cloud backup failed: ${e.error}")
            throw e
        } catch (e: Exception) {
            settings.recordFailure(clock().toEpochMilli(), CloudBackupError.UNKNOWN)
            Log.w(TAG, "Cloud backup failed: ${e.javaClass.simpleName}")
            throw CloudBackupException(CloudBackupError.UNKNOWN, e)
        }
    }

    private companion object {
        const val TAG = "CloudBackup"
    }
}
