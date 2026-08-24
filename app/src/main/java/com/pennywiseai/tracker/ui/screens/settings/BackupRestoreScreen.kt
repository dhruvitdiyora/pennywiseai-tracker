package com.pennywiseai.tracker.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.icons.iconsax.ExportArrow02
import com.pennywiseai.tracker.ui.icons.iconsax.Folder2
import com.pennywiseai.tracker.ui.icons.iconsax.Danger
import com.pennywiseai.tracker.ui.icons.iconsax.Iconsax
import com.pennywiseai.tracker.ui.icons.iconsax.ImportArrow01
import com.pennywiseai.tracker.ui.icons.iconsax.Sync
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.blue_dark
import com.pennywiseai.tracker.ui.theme.blue_light
import com.pennywiseai.tracker.ui.theme.cyan_dark
import com.pennywiseai.tracker.ui.theme.cyan_light
import com.pennywiseai.tracker.ui.theme.green_dark
import com.pennywiseai.tracker.ui.theme.green_light
import com.pennywiseai.tracker.ui.theme.orange_dark
import com.pennywiseai.tracker.ui.theme.orange_light
import com.pennywiseai.tracker.ui.theme.purple_dark
import com.pennywiseai.tracker.ui.theme.purple_light
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(onNavigateBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val enabled by viewModel.scheduledFolderBackupEnabled.collectAsStateWithLifecycle(initialValue = false)
    val lastBackup by viewModel.scheduledFolderBackupLastTimestamp.collectAsStateWithLifecycle(initialValue = null)
    val lastError by viewModel.scheduledFolderBackupLastError.collectAsStateWithLifecycle(initialValue = null)
    val folderName by viewModel.scheduledFolderBackupFolderName.collectAsStateWithLifecycle(initialValue = null)
    val message by viewModel.importExportMessage.collectAsStateWithLifecycle()
    val exportedFile by viewModel.exportedBackupFile.collectAsStateWithLifecycle()
    val requestFolderPicker by viewModel.requestFolderPicker.collectAsStateWithLifecycle()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(viewModel::importBackup) }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { it?.let(viewModel::saveBackupToFile) }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { it?.let(viewModel::onBackupFolderSelected) }
    LaunchedEffect(requestFolderPicker) { if (requestFolderPicker) { folderLauncher.launch(null); viewModel.onFolderPickerLaunched() } }
    val scroll = TopAppBarDefaults.pinnedScrollBehavior(); val haze = remember { HazeState() }
    val lastBackupText = lastBackup?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")) }
    val isStale = lastBackup != null && System.currentTimeMillis() - lastBackup!! > 3 * 24 * 60 * 60 * 1_000L
    Scaffold(topBar = { CustomTitleTopAppBar(title = "Backup & Restore", scrollBehaviorSmall = scroll, scrollBehaviorLarge = scroll, hazeState = haze, hasBackButton = true, navigationContent = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().hazeSource(haze).background(MaterialTheme.colorScheme.background).overScrollVertical().verticalScroll(rememberScrollState()).padding(padding).padding(Dimensions.Padding.content), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.sm)) {
            SectionHeaderV2("Backup")
            SettingsGroup {
                SettingsNavItem(Iconsax.ExportArrow02, blue_light, blue_dark, "Export Data", "Create a backup file", viewModel::exportBackup, ListItemPosition.Top)
                SettingsSwitchRow(Iconsax.Sync, purple_light, purple_dark, "Automatic backup", folderName?.let { "$it - ${lastBackupText ?: "Waiting for first backup"}" } ?: "Back up daily to a folder your cloud app syncs", enabled, viewModel::setScheduledFolderBackupEnabled, ListItemPosition.Middle)
                if (enabled && (isStale || lastError != null)) {
                    SettingsNavItem(Iconsax.Danger, orange_light, orange_dark, "Backup needs attention", lastError ?: "No backup since $lastBackupText. The folder may no longer be accessible.", viewModel::requestChangeBackupFolder, ListItemPosition.Middle, trailingText = "Re-select")
                }
                if (enabled) {
                    SettingsNavItem(Iconsax.ExportArrow02, green_light, green_dark, "Back Up Now", lastBackupText?.let { "Last backup: $it" } ?: "Run a backup to your folder now", { viewModel.backupToFolderNow() }, ListItemPosition.Middle)
                    SettingsNavItem(Iconsax.Folder2, orange_light, orange_dark, "Change Backup Folder", "Pick a different folder for automatic backups", viewModel::requestChangeBackupFolder, ListItemPosition.Middle)
                }
                SettingsNavItem(Iconsax.ImportArrow01, cyan_light, cyan_dark, "Import Data", "Restore data from a backup", { importLauncher.launch("*/*") }, ListItemPosition.Bottom)
            }
    if (enabled) Text("Choose or re-select a folder from the automatic backup row in Settings.", style = MaterialTheme.typography.bodySmall)
        }
    }
    message?.let { status ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::clearImportExportMessage,
            title = { Text("Backup status") },
            text = { Text(status) },
            confirmButton = {
                if (exportedFile != null && status.contains("Choose")) {
                    TextButton(onClick = {
                        val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy_MM_dd_HHmmss"))
                        saveLauncher.launch("PennyWise_Backup_$timestamp.pennywisebackup")
                        viewModel.clearImportExportMessage()
                    }) { Text("Save to Files") }
                } else {
                    TextButton(onClick = viewModel::clearImportExportMessage) { Text("OK") }
                }
            },
            dismissButton = if (exportedFile != null && status.contains("Choose")) {
                { TextButton(onClick = { viewModel.shareBackup(); viewModel.clearImportExportMessage() }) { Text("Share") } }
            } else null
        )
    }
}
