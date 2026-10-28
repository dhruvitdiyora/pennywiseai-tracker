package com.pennywiseai.tracker.ui.screens.settings.cloud

import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.backup.cloud.CloudBackupConfig
import com.pennywiseai.tracker.backup.cloud.CloudBackupError
import com.pennywiseai.tracker.backup.cloud.CloudBackupException
import com.pennywiseai.tracker.backup.cloud.CloudBackupFrequency
import com.pennywiseai.tracker.backup.cloud.CloudBackupManager
import com.pennywiseai.tracker.backup.cloud.CloudBackupScheduler
import com.pennywiseai.tracker.backup.cloud.CloudBackupSettings
import com.pennywiseai.tracker.backup.cloud.CloudProviderType
import com.pennywiseai.tracker.backup.cloud.DriveConnectResult
import com.pennywiseai.tracker.backup.cloud.RemoteSnapshot
import com.pennywiseai.tracker.data.backup.ImportResult
import com.pennywiseai.tracker.data.backup.ImportStrategy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CloudBusy { BACKING_UP, RESTORING, LOADING, CONNECTING, WORKING }

sealed interface CloudMessage {
    data object BackupDone : CloudMessage
    data class Connected(val needsPassphrase: Boolean) : CloudMessage
    data object Disconnected : CloudMessage
    data object PassphraseSaved : CloudMessage
    data object PassphraseRemoved : CloudMessage
    data object SnapshotDeleted : CloudMessage
    data object AutoNeedsSetup : CloudMessage
    data class Restored(val result: ImportResult.Success) : CloudMessage
    data class Failed(val error: CloudBackupError) : CloudMessage
}

/** A restore that needs the user to type the passphrase (none stored, or the stored one didn't open it). */
data class PassphrasePrompt(
    val snapshot: RemoteSnapshot,
    val strategy: ImportStrategy,
    val wrongAttempt: Boolean,
)

data class CloudBackupUiState(
    val config: CloudBackupConfig = CloudBackupConfig(),
    val driveAvailable: Boolean = false,
    val driveAccount: String? = null,
    val hasPassphrase: Boolean = false,
    val busy: CloudBusy? = null,
    /** Non-null while the snapshot sheet is open. */
    val snapshots: List<RemoteSnapshot>? = null,
    val message: CloudMessage? = null,
    val pendingConsent: PendingIntent? = null,
    val passphrasePrompt: PassphrasePrompt? = null,
)

@HiltViewModel
class CloudBackupViewModel @Inject constructor(
    private val manager: CloudBackupManager,
    private val settings: CloudBackupSettings,
    private val scheduler: CloudBackupScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(
        CloudBackupUiState(
            driveAvailable = manager.isDriveAvailable,
            driveAccount = manager.driveAccountName(),
            hasPassphrase = safeHasPassphrase(),
        )
    )
    val state: StateFlow<CloudBackupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.config.collect { config -> _state.update { it.copy(config = config) } }
        }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    // ---- Providers --------------------------------------------------------------------

    fun connectDrive() = run(CloudBusy.CONNECTING) {
        handleDrive(manager.connectDrive())
    }

    fun onDriveConsentLaunched() = _state.update { it.copy(pendingConsent = null) }

    fun onDriveConsentResult(resultOk: Boolean, data: Intent?) {
        _state.update { it.copy(busy = null) }
        if (!resultOk) return
        run(CloudBusy.CONNECTING) { handleDrive(manager.completeDriveConnect(data)) }
    }

    private fun handleDrive(result: DriveConnectResult) {
        when (result) {
            is DriveConnectResult.Connected -> _state.update {
                it.copy(driveAccount = result.accountName, message = CloudMessage.Connected(!it.hasPassphrase))
            }
            // Keep the busy state until the consent screen returns.
            is DriveConnectResult.NeedsConsent -> _state.update { it.copy(pendingConsent = result.pendingIntent) }
            is DriveConnectResult.Failed -> _state.update { it.copy(message = CloudMessage.Failed(result.error)) }
        }
    }

    fun connectWebDav(url: String, username: String, password: String) = run(CloudBusy.CONNECTING) {
        manager.connectWebDav(url, username, password)
        _state.update { it.copy(driveAccount = null, message = CloudMessage.Connected(!it.hasPassphrase)) }
    }

    fun disconnect() = run(CloudBusy.WORKING) {
        scheduler.cancel()
        manager.disconnect()
        _state.update { it.copy(driveAccount = null, snapshots = null, message = CloudMessage.Disconnected) }
    }

    // ---- Passphrase -------------------------------------------------------------------

    fun setPassphrase(passphrase: CharArray) = run(CloudBusy.WORKING) {
        try {
            manager.setPassphrase(passphrase)
        } finally {
            passphrase.fill('\u0000')
        }
        _state.update { it.copy(hasPassphrase = true, message = CloudMessage.PassphraseSaved) }
    }

    fun removePassphrase() = run(CloudBusy.WORKING) {
        manager.removePassphrase()
        if (_state.value.config.autoBackupEnabled) {
            settings.setAutoBackup(false)
            scheduler.cancel()
        }
        _state.update { it.copy(hasPassphrase = false, message = CloudMessage.PassphraseRemoved) }
    }

    // ---- Backup -----------------------------------------------------------------------

    fun backupNow() = run(CloudBusy.BACKING_UP) {
        manager.backupNow()
        _state.update { it.copy(message = CloudMessage.BackupDone) }
    }

    fun setAutoBackup(enabled: Boolean) = launchQuiet {
        val current = _state.value
        if (enabled && (current.config.provider == CloudProviderType.NONE || !current.hasPassphrase)) {
            _state.update { it.copy(message = CloudMessage.AutoNeedsSetup) }
            return@launchQuiet
        }
        settings.setAutoBackup(enabled)
        if (enabled) scheduler.schedule(current.config.frequency) else scheduler.cancel()
    }

    fun setFrequency(frequency: CloudBackupFrequency) = launchQuiet {
        settings.setFrequency(frequency)
        if (_state.value.config.autoBackupEnabled) scheduler.schedule(frequency)
    }

    fun setKeepCount(count: Int) = launchQuiet { settings.setKeepCount(count) }

    fun setIncludeReceipts(include: Boolean) = launchQuiet { settings.setIncludeReceipts(include) }

    // ---- Restore ----------------------------------------------------------------------

    fun openSnapshots() = run(CloudBusy.LOADING) {
        val snapshots = manager.listSnapshots()
        _state.update { it.copy(snapshots = snapshots) }
    }

    fun closeSnapshots() = _state.update { it.copy(snapshots = null) }

    fun deleteSnapshot(snapshot: RemoteSnapshot) = run(CloudBusy.WORKING) {
        manager.deleteSnapshot(snapshot)
        _state.update { state ->
            state.copy(
                snapshots = state.snapshots?.filterNot { it.id == snapshot.id },
                message = CloudMessage.SnapshotDeleted,
            )
        }
    }

    /**
     * Restores with the [strategy] the user already confirmed. Without a stored
     * passphrase, or when the stored one doesn't open this snapshot, asks for one.
     */
    fun restore(snapshot: RemoteSnapshot, strategy: ImportStrategy, passphrase: CharArray? = null) {
        if (passphrase == null && !_state.value.hasPassphrase) {
            _state.update { it.copy(passphrasePrompt = PassphrasePrompt(snapshot, strategy, wrongAttempt = false)) }
            return
        }
        run(CloudBusy.RESTORING, onError = { error ->
            if (error == CloudBackupError.WRONG_PASSPHRASE_OR_DAMAGED) {
                _state.update {
                    it.copy(passphrasePrompt = PassphrasePrompt(snapshot, strategy, wrongAttempt = passphrase != null))
                }
                true
            } else {
                false
            }
        }) {
            val result = manager.restore(snapshot, strategy, passphrase)
            _state.update { it.copy(snapshots = null, passphrasePrompt = null, message = CloudMessage.Restored(result)) }
        }
    }

    fun dismissPassphrasePrompt() = _state.update { it.copy(passphrasePrompt = null) }

    // ---- Plumbing ---------------------------------------------------------------------

    private fun safeHasPassphrase(): Boolean = runCatching { manager.hasPassphrase() }.getOrDefault(false)

    /**
     * Runs [block] with a busy indicator; a [CloudBackupException] becomes a
     * message unless [onError] handles it (returns true).
     */
    private fun run(
        busy: CloudBusy,
        onError: (CloudBackupError) -> Boolean = { false },
        block: suspend () -> Unit,
    ) {
        if (_state.value.busy != null) return
        _state.update { it.copy(busy = busy) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: CloudBackupException) {
                if (!onError(e.error)) _state.update { it.copy(message = CloudMessage.Failed(e.error)) }
            } catch (e: Exception) {
                _state.update { it.copy(message = CloudMessage.Failed(CloudBackupError.UNKNOWN)) }
            } finally {
                // A pending consent screen keeps the indicator until it returns.
                _state.update { if (it.pendingConsent != null) it else it.copy(busy = null) }
            }
        }
    }

    private fun launchQuiet(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
