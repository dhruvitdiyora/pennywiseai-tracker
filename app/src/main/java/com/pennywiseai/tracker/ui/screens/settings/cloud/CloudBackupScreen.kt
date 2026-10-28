package com.pennywiseai.tracker.ui.screens.settings.cloud

import android.app.Activity
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.backup.cloud.CloudBackupCrypto
import com.pennywiseai.tracker.backup.cloud.CloudBackupError
import com.pennywiseai.tracker.backup.cloud.CloudBackupFrequency
import com.pennywiseai.tracker.backup.cloud.CloudProviderType
import com.pennywiseai.tracker.backup.cloud.CloudRetention
import com.pennywiseai.tracker.backup.cloud.RemoteSnapshot
import com.pennywiseai.tracker.data.backup.ImportStrategy
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.SecuritySafe
import com.pennywiseai.tracker.ui.screens.settings.SettingsIconRow
import com.pennywiseai.tracker.ui.screens.settings.SettingsNavItem
import com.pennywiseai.tracker.ui.screens.settings.SettingsSection
import com.pennywiseai.tracker.ui.screens.settings.SettingsSubScreen
import com.pennywiseai.tracker.ui.screens.settings.SettingsSwitchRow
import com.pennywiseai.tracker.ui.screens.settings.glassDialog
import com.pennywiseai.tracker.ui.screens.settings.glassSheet
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.amber_dark
import com.pennywiseai.tracker.ui.theme.amber_light
import com.pennywiseai.tracker.ui.theme.blue_dark
import com.pennywiseai.tracker.ui.theme.blue_light
import com.pennywiseai.tracker.ui.theme.cyan_dark
import com.pennywiseai.tracker.ui.theme.cyan_light
import com.pennywiseai.tracker.ui.theme.green_dark
import com.pennywiseai.tracker.ui.theme.green_light
import com.pennywiseai.tracker.ui.theme.indigo_dark
import com.pennywiseai.tracker.ui.theme.indigo_light
import com.pennywiseai.tracker.ui.theme.purple_dark
import com.pennywiseai.tracker.ui.theme.purple_light
import com.pennywiseai.tracker.ui.theme.red_dark
import com.pennywiseai.tracker.ui.theme.red_light
import com.pennywiseai.tracker.ui.theme.teal_dark
import com.pennywiseai.tracker.ui.theme.teal_light
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Settings → Backup → Cloud backup, in the Cashiro settings style: a tonal
 * glass hero, then grouped glass rows for provider, encryption, backup and
 * restore. All work happens in [CloudBackupViewModel]; this file only shows
 * state and asks for confirmation.
 */
@Composable
fun CloudBackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: CloudBackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config = state.config

    var showWebDav by remember { mutableStateOf(false) }
    var showPassphrase by remember { mutableStateOf(false) }
    var confirmRemovePassphrase by remember { mutableStateOf(false) }
    var confirmDisconnect by remember { mutableStateOf<CloudProviderType?>(null) }
    var showFrequency by remember { mutableStateOf(false) }
    var showKeep by remember { mutableStateOf(false) }
    var restoreChoice by remember { mutableStateOf<RemoteSnapshot?>(null) }
    var confirmReplace by remember { mutableStateOf<RemoteSnapshot?>(null) }
    var confirmDelete by remember { mutableStateOf<RemoteSnapshot?>(null) }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDriveConsentResult(result.resultCode == Activity.RESULT_OK, result.data)
    }
    LaunchedEffect(state.pendingConsent) {
        state.pendingConsent?.let { pending ->
            viewModel.onDriveConsentLaunched()
            consentLauncher.launch(IntentSenderRequest.Builder(pending).build())
        }
    }

    val busy = state.busy != null
    val connected = config.provider != CloudProviderType.NONE

    SettingsSubScreen(
        title = stringResource(R.string.cloud_backup_title),
        backContentDescription = stringResource(R.string.settings_back),
        onNavigateBack = onNavigateBack,
    ) {
        CloudHero()

        state.busy?.let { BusyBanner(it) }

        SettingsSection(title = stringResource(R.string.cloud_backup_provider_section)) {
            GroupedList {
                val rows = if (state.driveAvailable) 2 else 1
                if (state.driveAvailable) {
                    val driveConnected = config.provider == CloudProviderType.GOOGLE_DRIVE
                    SettingsNavItem(
                        icon = Icons.Default.AddToDrive,
                        iconBgColor = green_light,
                        iconTint = green_dark,
                        title = stringResource(R.string.cloud_backup_drive_title),
                        subtitle = when {
                            driveConnected && state.driveAccount != null ->
                                stringResource(R.string.cloud_backup_drive_connected_as, state.driveAccount!!)
                            driveConnected -> stringResource(R.string.cloud_backup_connected)
                            else -> stringResource(R.string.cloud_backup_drive_subtitle)
                        },
                        trailingText = stringResource(
                            if (driveConnected) R.string.cloud_backup_action_disconnect else R.string.cloud_backup_action_connect
                        ),
                        onClick = {
                            if (busy) return@SettingsNavItem
                            if (driveConnected) confirmDisconnect = CloudProviderType.GOOGLE_DRIVE
                            else viewModel.connectDrive()
                        },
                        position = ListItemPosition.from(0, rows),
                    )
                }
                val webDavConnected = config.provider == CloudProviderType.WEBDAV
                SettingsNavItem(
                    icon = Icons.Default.Dns,
                    iconBgColor = blue_light,
                    iconTint = blue_dark,
                    title = stringResource(R.string.cloud_backup_webdav_title),
                    subtitle = if (webDavConnected) {
                        stringResource(R.string.cloud_backup_webdav_connected_to, config.webDavHost.orEmpty())
                    } else {
                        stringResource(R.string.cloud_backup_webdav_subtitle)
                    },
                    trailingText = stringResource(
                        if (webDavConnected) R.string.cloud_backup_action_disconnect else R.string.cloud_backup_action_connect
                    ),
                    onClick = {
                        if (busy) return@SettingsNavItem
                        if (webDavConnected) confirmDisconnect = CloudProviderType.WEBDAV else showWebDav = true
                    },
                    position = ListItemPosition.from(rows - 1, rows),
                )
            }
        }

        SettingsSection(title = stringResource(R.string.cloud_backup_encryption_section)) {
            GroupedList {
                SettingsNavItem(
                    icon = Icons.Default.Key,
                    iconBgColor = purple_light,
                    iconTint = purple_dark,
                    title = stringResource(R.string.cloud_backup_passphrase_title),
                    subtitle = stringResource(
                        if (state.hasPassphrase) R.string.cloud_backup_passphrase_set else R.string.cloud_backup_passphrase_not_set
                    ),
                    onClick = { if (!busy) showPassphrase = true },
                    position = ListItemPosition.Top,
                )
                SettingsIconRow(
                    icon = Icons.Default.Warning,
                    iconContainerColor = amber_light,
                    iconContentColor = amber_dark,
                    title = stringResource(R.string.cloud_backup_passphrase_warning_title),
                    subtitle = stringResource(R.string.cloud_backup_passphrase_warning_body),
                    subtitleMaxLines = 8,
                    position = ListItemPosition.Bottom,
                    minHeight = Dimensions.Component.listItemMinHeightTwoLine,
                )
            }
        }

        SettingsSection(title = stringResource(R.string.cloud_backup_backup_section)) {
            GroupedList {
                SettingsNavItem(
                    icon = Icons.Default.Backup,
                    iconBgColor = teal_light,
                    iconTint = teal_dark,
                    title = stringResource(R.string.cloud_backup_now_title),
                    subtitle = statusLine(config.lastSuccessAt, config.lastError),
                    onClick = { if (!busy) viewModel.backupNow() },
                    position = ListItemPosition.Top,
                )
                SettingsSwitchRow(
                    icon = Icons.Default.CloudSync,
                    iconBgColor = indigo_light,
                    iconTint = indigo_dark,
                    title = stringResource(R.string.cloud_backup_auto_title),
                    subtitle = stringResource(R.string.cloud_backup_auto_subtitle),
                    checked = config.autoBackupEnabled,
                    onCheckedChange = viewModel::setAutoBackup,
                    position = ListItemPosition.Middle,
                )
                if (config.autoBackupEnabled) {
                    SettingsNavItem(
                        icon = Icons.Default.Schedule,
                        iconBgColor = indigo_light,
                        iconTint = indigo_dark,
                        title = stringResource(R.string.cloud_backup_frequency_title),
                        subtitle = stringResource(config.frequency.label()),
                        onClick = { showFrequency = true },
                        position = ListItemPosition.Middle,
                    )
                }
                SettingsNavItem(
                    icon = Icons.Default.Inventory2,
                    iconBgColor = cyan_light,
                    iconTint = cyan_dark,
                    title = stringResource(R.string.cloud_backup_keep_title),
                    subtitle = stringResource(R.string.cloud_backup_keep_subtitle),
                    trailingText = config.keepCount.toString(),
                    onClick = { showKeep = true },
                    position = ListItemPosition.Middle,
                )
                SettingsSwitchRow(
                    icon = Icons.Default.Receipt,
                    iconBgColor = purple_light,
                    iconTint = purple_dark,
                    title = stringResource(R.string.cloud_backup_receipts_title),
                    subtitle = stringResource(R.string.cloud_backup_receipts_subtitle),
                    checked = config.includeReceipts,
                    onCheckedChange = viewModel::setIncludeReceipts,
                    position = ListItemPosition.Bottom,
                )
            }
        }

        SettingsSection(title = stringResource(R.string.cloud_backup_restore_section)) {
            GroupedList {
                SettingsNavItem(
                    icon = Icons.Default.Restore,
                    iconBgColor = red_light,
                    iconTint = red_dark,
                    title = stringResource(R.string.cloud_backup_restore_title),
                    subtitle = stringResource(R.string.cloud_backup_restore_subtitle),
                    onClick = { if (!busy) viewModel.openSnapshots() },
                    position = ListItemPosition.Single,
                )
            }
        }
    }

    // ---- Dialogs ---------------------------------------------------------------------

    if (showWebDav) {
        WebDavDialog(
            onDismiss = { showWebDav = false },
            onConnect = { url, user, password ->
                showWebDav = false
                viewModel.connectWebDav(url, user, password)
            },
        )
    }

    if (showPassphrase) {
        PassphraseDialog(
            hasPassphrase = state.hasPassphrase,
            onDismiss = { showPassphrase = false },
            onSave = {
                showPassphrase = false
                viewModel.setPassphrase(it)
            },
            onRemove = {
                showPassphrase = false
                confirmRemovePassphrase = true
            },
        )
    }

    if (confirmRemovePassphrase) {
        ConfirmDialog(
            title = stringResource(R.string.cloud_backup_passphrase_remove_confirm_title),
            body = stringResource(R.string.cloud_backup_passphrase_remove_confirm_body),
            confirm = stringResource(R.string.cloud_backup_passphrase_remove),
            destructive = true,
            onConfirm = {
                confirmRemovePassphrase = false
                viewModel.removePassphrase()
            },
            onDismiss = { confirmRemovePassphrase = false },
        )
    }

    confirmDisconnect?.let { provider ->
        val name = stringResource(
            if (provider == CloudProviderType.GOOGLE_DRIVE) R.string.cloud_backup_drive_title else R.string.cloud_backup_webdav_title
        )
        ConfirmDialog(
            title = stringResource(R.string.cloud_backup_disconnect_title, name),
            body = stringResource(R.string.cloud_backup_disconnect_body),
            confirm = stringResource(R.string.cloud_backup_action_disconnect),
            destructive = true,
            onConfirm = {
                confirmDisconnect = null
                viewModel.disconnect()
            },
            onDismiss = { confirmDisconnect = null },
        )
    }

    if (showFrequency) {
        ChoiceDialog(
            title = stringResource(R.string.cloud_backup_frequency_title),
            options = CloudBackupFrequency.entries,
            selected = config.frequency,
            label = { stringResource(it.label()) },
            onSelect = {
                showFrequency = false
                viewModel.setFrequency(it)
            },
            onDismiss = { showFrequency = false },
        )
    }

    if (showKeep) {
        ChoiceDialog(
            title = stringResource(R.string.cloud_backup_keep_title),
            options = CloudRetention.KEEP_OPTIONS,
            selected = config.keepCount,
            label = { it.toString() },
            onSelect = {
                showKeep = false
                viewModel.setKeepCount(it)
            },
            onDismiss = { showKeep = false },
        )
    }

    state.snapshots?.let { snapshots ->
        SnapshotSheet(
            snapshots = snapshots,
            busy = busy,
            onDismiss = viewModel::closeSnapshots,
            onRestore = { restoreChoice = it },
            onDelete = { confirmDelete = it },
        )
    }

    restoreChoice?.let { snapshot ->
        RestoreChoiceDialog(
            snapshot = snapshot,
            onMerge = {
                restoreChoice = null
                viewModel.restore(snapshot, ImportStrategy.MERGE)
            },
            onReplace = {
                restoreChoice = null
                confirmReplace = snapshot
            },
            onDismiss = { restoreChoice = null },
        )
    }

    confirmReplace?.let { snapshot ->
        ConfirmDialog(
            title = stringResource(R.string.cloud_backup_replace_confirm_title),
            body = stringResource(R.string.cloud_backup_replace_confirm_body, formatInstant(snapshot.createdAt)),
            confirm = stringResource(R.string.cloud_backup_replace_confirm_action),
            destructive = true,
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onConfirm = {
                confirmReplace = null
                viewModel.restore(snapshot, ImportStrategy.REPLACE_ALL)
            },
            onDismiss = { confirmReplace = null },
        )
    }

    confirmDelete?.let { snapshot ->
        ConfirmDialog(
            title = stringResource(R.string.cloud_backup_snapshot_delete_title),
            body = stringResource(R.string.cloud_backup_snapshot_delete_body, formatInstant(snapshot.createdAt)),
            confirm = stringResource(R.string.cloud_backup_snapshot_delete),
            destructive = true,
            onConfirm = {
                confirmDelete = null
                viewModel.deleteSnapshot(snapshot)
            },
            onDismiss = { confirmDelete = null },
        )
    }

    state.passphrasePrompt?.let { prompt ->
        PassphrasePromptDialog(
            wrongAttempt = prompt.wrongAttempt,
            onDismiss = viewModel::dismissPassphrasePrompt,
            onSubmit = { passphrase ->
                viewModel.dismissPassphrasePrompt()
                viewModel.restore(prompt.snapshot, prompt.strategy, passphrase)
            },
        )
    }

    state.message?.let { message ->
        AlertDialog(
            modifier = Modifier.glassDialog(),
            containerColor = Color.Transparent,
            onDismissRequest = viewModel::dismissMessage,
            title = { Text(stringResource(R.string.cloud_backup_result_title)) },
            text = { Text(messageText(message)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissMessage) { Text(stringResource(R.string.settings_action_ok)) }
            },
        )
    }

    // Snapshots belong to the connected provider; disconnecting closes the sheet.
    LaunchedEffect(connected) { if (!connected) viewModel.closeSnapshots() }
}

@Composable
private fun CloudHero() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        tint = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(
                icon = Iconax.SecuritySafe,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = stringResource(R.string.cloud_backup_hero_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.cloud_backup_hero_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun BusyBanner(busy: CloudBusy) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = stringResource(
                    when (busy) {
                        CloudBusy.BACKING_UP -> R.string.cloud_backup_busy_backing_up
                        CloudBusy.RESTORING -> R.string.cloud_backup_busy_restoring
                        CloudBusy.LOADING -> R.string.cloud_backup_busy_loading
                        CloudBusy.CONNECTING -> R.string.cloud_backup_busy_connecting
                        CloudBusy.WORKING -> R.string.cloud_backup_busy_working
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun statusLine(lastSuccessAt: Long?, lastError: CloudBackupError?): String = when {
    lastError != null -> stringResource(R.string.cloud_backup_last_failed, stringResource(lastError.message()))
    lastSuccessAt != null -> stringResource(R.string.cloud_backup_last_success, formatInstant(Instant.ofEpochMilli(lastSuccessAt)))
    else -> stringResource(R.string.cloud_backup_never)
}

private fun formatInstant(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .format(instant.atZone(ZoneId.systemDefault()))

private fun CloudBackupFrequency.label(): Int = when (this) {
    CloudBackupFrequency.DAILY -> R.string.cloud_backup_frequency_daily
    CloudBackupFrequency.WEEKLY -> R.string.cloud_backup_frequency_weekly
}

internal fun CloudBackupError.message(): Int = when (this) {
    CloudBackupError.NOT_CONNECTED -> R.string.cloud_backup_error_not_connected
    CloudBackupError.PROVIDER_UNAVAILABLE -> R.string.cloud_backup_error_provider_unavailable
    CloudBackupError.DRIVE_NOT_CONFIGURED -> R.string.cloud_backup_error_drive_not_configured
    CloudBackupError.AUTH_REQUIRED -> R.string.cloud_backup_error_auth_required
    CloudBackupError.AUTH_FAILED -> R.string.cloud_backup_error_auth_failed
    CloudBackupError.INSECURE_URL -> R.string.cloud_backup_error_insecure_url
    CloudBackupError.PASSPHRASE_NOT_SET -> R.string.cloud_backup_error_passphrase_not_set
    CloudBackupError.PASSPHRASE_TOO_SHORT -> R.string.cloud_backup_error_passphrase_too_short
    CloudBackupError.SECURE_STORAGE_UNAVAILABLE -> R.string.cloud_backup_error_secure_storage
    CloudBackupError.WRONG_PASSPHRASE_OR_DAMAGED -> R.string.cloud_backup_error_wrong_passphrase
    CloudBackupError.UNSUPPORTED_FORMAT -> R.string.cloud_backup_error_unsupported
    CloudBackupError.TOO_LARGE -> R.string.cloud_backup_error_too_large
    CloudBackupError.STORAGE_FULL -> R.string.cloud_backup_error_storage_full
    CloudBackupError.NOT_FOUND -> R.string.cloud_backup_error_not_found
    CloudBackupError.NETWORK -> R.string.cloud_backup_error_network
    CloudBackupError.SERVER -> R.string.cloud_backup_error_server
    CloudBackupError.EXPORT_FAILED -> R.string.cloud_backup_error_export
    CloudBackupError.IMPORT_FAILED -> R.string.cloud_backup_error_import
    CloudBackupError.UNKNOWN -> R.string.cloud_backup_error_unknown
}

@Composable
private fun messageText(message: CloudMessage): String = when (message) {
    CloudMessage.BackupDone -> stringResource(R.string.cloud_backup_result_backup_done)
    is CloudMessage.Connected -> stringResource(
        if (message.needsPassphrase) R.string.cloud_backup_result_connected else R.string.cloud_backup_result_connected_ready
    )
    CloudMessage.Disconnected -> stringResource(R.string.cloud_backup_result_disconnected)
    CloudMessage.PassphraseSaved -> stringResource(R.string.cloud_backup_result_passphrase_saved)
    CloudMessage.PassphraseRemoved -> stringResource(R.string.cloud_backup_result_passphrase_removed)
    CloudMessage.SnapshotDeleted -> stringResource(R.string.cloud_backup_result_deleted)
    CloudMessage.AutoNeedsSetup -> stringResource(R.string.cloud_backup_auto_needs_setup)
    is CloudMessage.Restored -> stringResource(
        R.string.cloud_backup_result_restored,
        message.result.importedTransactions,
        message.result.importedCategories,
        message.result.skippedDuplicates,
    )
    is CloudMessage.Failed -> stringResource(message.error.message())
}

@Composable
private fun WebDavDialog(onDismiss: () -> Unit, onConnect: (String, String, String) -> Unit) {
    var url by remember { mutableStateOf("https://") }
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val urlLooksSecure = url.trim().startsWith("https://", ignoreCase = true) && url.trim().length > "https://".length
    AlertDialog(
        modifier = Modifier.glassDialog(),
        containerColor = Color.Transparent,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cloud_backup_webdav_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.cloud_backup_webdav_url)) },
                    supportingText = {
                        Text(
                            stringResource(
                                if (urlLooksSecure || url == "https://") R.string.cloud_backup_webdav_url_hint
                                else R.string.cloud_backup_error_insecure_url
                            )
                        )
                    },
                    isError = !urlLooksSecure && url != "https://",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text(stringResource(R.string.cloud_backup_webdav_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.cloud_backup_webdav_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConnect(url.trim(), user, password) },
                enabled = urlLooksSecure && user.isNotBlank() && password.isNotEmpty(),
            ) { Text(stringResource(R.string.cloud_backup_action_connect)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_action_cancel)) }
        },
    )
}

@Composable
private fun PassphraseDialog(
    hasPassphrase: Boolean,
    onDismiss: () -> Unit,
    onSave: (CharArray) -> Unit,
    onRemove: () -> Unit,
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    val min = CloudBackupCrypto.MIN_PASSPHRASE_LENGTH
    val tooShort = first.isNotEmpty() && first.length < min
    val mismatch = second.isNotEmpty() && first != second
    AlertDialog(
        modifier = Modifier.glassDialog(),
        containerColor = Color.Transparent,
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Key, contentDescription = null) },
        title = { Text(stringResource(R.string.cloud_backup_passphrase_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(stringResource(R.string.cloud_backup_passphrase_dialog_body, min), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.cloud_backup_passphrase_warning_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (hasPassphrase) {
                    Text(
                        stringResource(R.string.cloud_backup_passphrase_change_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = first,
                    onValueChange = { first = it },
                    label = { Text(stringResource(R.string.cloud_backup_passphrase_new)) },
                    isError = tooShort,
                    supportingText = if (tooShort) {
                        { Text(stringResource(R.string.cloud_backup_passphrase_too_short, min)) }
                    } else null,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = second,
                    onValueChange = { second = it },
                    label = { Text(stringResource(R.string.cloud_backup_passphrase_confirm)) },
                    isError = mismatch,
                    supportingText = if (mismatch) {
                        { Text(stringResource(R.string.cloud_backup_passphrase_mismatch)) }
                    } else null,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (hasPassphrase) {
                    TextButton(
                        onClick = onRemove,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.cloud_backup_passphrase_remove)) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(first.toCharArray()) },
                enabled = first.length >= min && first == second,
            ) { Text(stringResource(R.string.settings_action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_action_cancel)) }
        },
    )
}

@Composable
private fun PassphrasePromptDialog(wrongAttempt: Boolean, onDismiss: () -> Unit, onSubmit: (CharArray) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        modifier = Modifier.glassDialog(),
        containerColor = Color.Transparent,
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Key, contentDescription = null) },
        title = { Text(stringResource(R.string.cloud_backup_passphrase_prompt_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    stringResource(
                        if (wrongAttempt) R.string.cloud_backup_passphrase_prompt_wrong
                        else R.string.cloud_backup_passphrase_prompt_body
                    ),
                    color = if (wrongAttempt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.cloud_backup_passphrase_new)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(value.toCharArray()) }, enabled = value.isNotEmpty()) {
                Text(stringResource(R.string.cloud_backup_action_restore))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_action_cancel)) }
        },
    )
}

@Composable
private fun RestoreChoiceDialog(
    snapshot: RemoteSnapshot,
    onMerge: () -> Unit,
    onReplace: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.glassDialog(),
        containerColor = Color.Transparent,
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.History, contentDescription = null) },
        title = { Text(stringResource(R.string.cloud_backup_restore_choice_title, formatInstant(snapshot.createdAt))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(stringResource(R.string.cloud_backup_restore_choice_body))
                ChoiceBlock(
                    title = stringResource(R.string.cloud_backup_restore_merge),
                    body = stringResource(R.string.cloud_backup_restore_merge_body),
                    onClick = onMerge,
                )
                ChoiceBlock(
                    title = stringResource(R.string.cloud_backup_restore_replace),
                    body = stringResource(R.string.cloud_backup_restore_replace_body),
                    onClick = onReplace,
                    destructive = true,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_action_cancel)) }
        },
    )
}

@Composable
private fun ChoiceBlock(title: String, body: String, onClick: () -> Unit, destructive: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SnapshotSheet(
    snapshots: List<RemoteSnapshot>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onRestore: (RemoteSnapshot) -> Unit,
    onDelete: (RemoteSnapshot) -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.glassSheet(),
        containerColor = Color.Transparent,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                stringResource(R.string.cloud_backup_snapshots_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (snapshots.isEmpty()) {
                Text(
                    stringResource(R.string.cloud_backup_snapshots_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(snapshots, key = { it.id }) { snapshot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !busy, role = Role.Button) { onRestore(snapshot) }
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            IconTile(
                                icon = Icons.Default.Restore,
                                containerColor = teal_light,
                                contentColor = teal_dark,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(formatInstant(snapshot.createdAt), style = MaterialTheme.typography.bodyLarge)
                                snapshot.sizeBytes?.let { size ->
                                    Text(
                                        Formatter.formatShortFileSize(context, size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            IconButton(onClick = { onDelete(snapshot) }, enabled = !busy) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.cloud_backup_snapshot_delete),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.glassDialog(),
        containerColor = Color.Transparent,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == selected, role = Role.RadioButton) { onSelect(option) }
                            .padding(vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(label(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_action_cancel)) }
        },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        modifier = Modifier.glassDialog(),
        containerColor = Color.Transparent,
        onDismissRequest = onDismiss,
        icon = icon,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = if (destructive) ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                else ButtonDefaults.textButtonColors(),
            ) { Text(confirm) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_action_cancel)) }
        },
    )
}
