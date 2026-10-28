package com.pennywiseai.tracker.backup.cloud

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cloudBackupDataStore by preferencesDataStore(name = "cloud_backup_settings")

enum class CloudBackupFrequency(val days: Long) { DAILY(1), WEEKLY(7) }

/** Non-secret cloud backup state. Secrets live in [CloudSecretStore]; this holds no credentials. */
data class CloudBackupConfig(
    val provider: CloudProviderType = CloudProviderType.NONE,
    /** Host of the WebDAV server, shown in the UI. Not a secret, never logged. */
    val webDavHost: String? = null,
    val autoBackupEnabled: Boolean = false,
    val frequency: CloudBackupFrequency = CloudBackupFrequency.DAILY,
    val keepCount: Int = CloudRetention.DEFAULT_KEEP,
    val includeReceipts: Boolean = false,
    val lastSuccessAt: Long? = null,
    val lastAttemptAt: Long? = null,
    val lastError: CloudBackupError? = null,
)

/**
 * Not part of the PennyWise backup file (connection settings belong to this
 * device), and not in Android Auto Backup.
 */
@Singleton
class CloudBackupSettings @Inject constructor(@ApplicationContext private val context: Context) {

    private val store get() = context.cloudBackupDataStore

    val config: Flow<CloudBackupConfig> = store.data.map { it.toConfig() }

    suspend fun current(): CloudBackupConfig = config.first()

    suspend fun setProvider(provider: CloudProviderType, webDavHost: String? = null) = store.edit {
        it[PROVIDER] = provider.name
        if (webDavHost != null) it[WEBDAV_HOST] = webDavHost else it.remove(WEBDAV_HOST)
        it.remove(LAST_ERROR)
    }

    suspend fun setAutoBackup(enabled: Boolean) = store.edit { it[AUTO] = enabled }
    suspend fun setFrequency(frequency: CloudBackupFrequency) = store.edit { it[FREQUENCY] = frequency.name }
    suspend fun setKeepCount(count: Int) = store.edit { it[KEEP] = count.coerceIn(1, 50) }
    suspend fun setIncludeReceipts(include: Boolean) = store.edit { it[RECEIPTS] = include }

    suspend fun recordSuccess(at: Long) = store.edit {
        it[LAST_SUCCESS] = at
        it[LAST_ATTEMPT] = at
        it.remove(LAST_ERROR)
    }

    suspend fun recordFailure(at: Long, error: CloudBackupError) = store.edit {
        it[LAST_ATTEMPT] = at
        it[LAST_ERROR] = error.name
    }

    suspend fun clearConnection() = store.edit {
        it[PROVIDER] = CloudProviderType.NONE.name
        it[AUTO] = false
        it.remove(WEBDAV_HOST)
        it.remove(LAST_ERROR)
    }

    private fun Preferences.toConfig() = CloudBackupConfig(
        provider = this[PROVIDER]?.let { name -> CloudProviderType.entries.firstOrNull { it.name == name } }
            ?: CloudProviderType.NONE,
        webDavHost = this[WEBDAV_HOST],
        autoBackupEnabled = this[AUTO] ?: false,
        frequency = this[FREQUENCY]?.let { name -> CloudBackupFrequency.entries.firstOrNull { it.name == name } }
            ?: CloudBackupFrequency.DAILY,
        keepCount = this[KEEP] ?: CloudRetention.DEFAULT_KEEP,
        includeReceipts = this[RECEIPTS] ?: false,
        lastSuccessAt = this[LAST_SUCCESS],
        lastAttemptAt = this[LAST_ATTEMPT],
        lastError = CloudBackupError.fromName(this[LAST_ERROR]),
    )

    private companion object {
        val PROVIDER = stringPreferencesKey("provider")
        val WEBDAV_HOST = stringPreferencesKey("webdav_host")
        val AUTO = booleanPreferencesKey("auto_backup")
        val FREQUENCY = stringPreferencesKey("frequency")
        val KEEP = intPreferencesKey("keep_count")
        val RECEIPTS = booleanPreferencesKey("include_receipts")
        val LAST_SUCCESS = longPreferencesKey("last_success_at")
        val LAST_ATTEMPT = longPreferencesKey("last_attempt_at")
        val LAST_ERROR = stringPreferencesKey("last_error")
    }
}
