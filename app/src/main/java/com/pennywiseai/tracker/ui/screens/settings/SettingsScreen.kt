package com.pennywiseai.tracker.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import coil.compose.AsyncImage
import com.pennywiseai.tracker.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.core.Constants
import com.pennywiseai.tracker.ui.UiText
import com.pennywiseai.tracker.ui.components.AvatarHelper
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.SupportDevelopmentDialog
import com.pennywiseai.tracker.ui.components.cards.GroupedColumn
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.RowLabels
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.icons.iconax.Edit2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.screens.profile.EditProfileSheet
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.amber_light
import com.pennywiseai.tracker.ui.theme.amber_dark
import com.pennywiseai.tracker.ui.theme.orange_light
import com.pennywiseai.tracker.ui.theme.orange_dark
import com.pennywiseai.tracker.ui.theme.green_light
import com.pennywiseai.tracker.ui.theme.green_dark
import com.pennywiseai.tracker.ui.theme.teal_light
import com.pennywiseai.tracker.ui.theme.teal_dark
import com.pennywiseai.tracker.ui.theme.blue_light
import com.pennywiseai.tracker.ui.theme.blue_dark
import com.pennywiseai.tracker.ui.theme.indigo_light
import com.pennywiseai.tracker.ui.theme.indigo_dark
import com.pennywiseai.tracker.ui.theme.red_light
import com.pennywiseai.tracker.ui.theme.red_dark
import com.pennywiseai.tracker.ui.theme.pink_light
import com.pennywiseai.tracker.ui.theme.pink_dark
import com.pennywiseai.tracker.ui.theme.purple_light
import com.pennywiseai.tracker.ui.theme.purple_dark
import com.pennywiseai.tracker.ui.theme.cyan_light
import com.pennywiseai.tracker.ui.theme.cyan_dark
import com.pennywiseai.tracker.ui.theme.yellow_light
import com.pennywiseai.tracker.ui.theme.yellow_dark
import com.pennywiseai.tracker.ui.theme.grey_light
import com.pennywiseai.tracker.ui.theme.grey_dark
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.pennywiseai.tracker.ui.viewmodel.ThemeViewModel
import com.pennywiseai.tracker.data.preferences.NumberFormatStyle
import com.pennywiseai.tracker.utils.CurrencyFormatter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToManageAccounts: () -> Unit = {},
    onNavigateToFaq: () -> Unit = {},
    onNavigateToRules: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToLoans: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    onNavigateToTransactionGroups: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToProfiles: () -> Unit = {},
    onNavigateToPersonalDashboard: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToCurrencyFormats: () -> Unit = {},
    onNavigateToBackupImport: () -> Unit = {},
    onNavigateToPrivacySecurity: () -> Unit = {},
    onNavigateToAdvanced: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val isProEntitled by settingsViewModel.isProEntitled.collectAsStateWithLifecycle()
    val userPreferences by settingsViewModel.userPreferences.collectAsStateWithLifecycle()
    val transactionCount by settingsViewModel.transactionCount.collectAsStateWithLifecycle(initialValue = 0)
    var showUpgradeSheet by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showEditProfile by rememberSaveable { mutableStateOf(false) }
    // F-Droid builds have no Play billing, so they show a "Support development"
    // tip jar instead of the (un-buyable) Pro upsell. Play builds keep Pro.
    val isFdroidBuild = com.pennywiseai.tracker.BuildConfig.IS_FDROID_BUILD
    val context = LocalContext.current
    // Per-app language is a system screen on Android 13+; older versions
    // follow the device language.
    val hasLanguageRow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    // Scroll behaviors for collapsible TopAppBar
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    // Large left title collapsing on scroll, matching the other screens.
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.settings_title),
                hasBackButton = true,
                navigationContent = { SettingsNavigationContent(onNavigateBack) },
                hazeState = hazeState
            )
        }
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        // Cashiro-style root: the profile card, then short groups of
        // destinations separated by space alone. Every toggle and picker
        // lives one level down in a named sub-screen, so the root stays a
        // scannable index; each row gets its own hue so neighbours differ.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                // Top bar inset only: the bottom clearance is the explicit
                // navigation-bar-aware spacer after the version footer.
                .padding(
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    top = paddingValues.calculateTopPadding(),
                    end = paddingValues.calculateEndPadding(layoutDirection),
                )
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.sectionGap)
        ) {
            SettingsProfileHeaderCard(
                userName = userPreferences?.userName ?: stringResource(R.string.greeting_default_user_name),
                profileImageUri = userPreferences?.profileImageUri,
                profileBackgroundColor = userPreferences?.profileBackgroundColor ?: 0,
                transactionCount = transactionCount,
                onClick = onNavigateToPersonalDashboard,
                onEditClick = { showEditProfile = true }
            )

            // ── PennyWise Pro / Support development ──
            // Top of Settings on purpose: highest-discoverability slot.
            // F-Droid builds have no Play billing (everything is already
            // unlocked), so instead of an un-buyable Pro upsell they get a
            // "Support development" tip jar. Play builds keep the Pro upgrade.
            SettingsGroup {
                if (isFdroidBuild) {
                    SettingsNavItem(
                        icon = Icons.Default.Favorite,
                        iconBgColor = yellow_light,
                        iconTint = yellow_dark,
                        title = stringResource(R.string.support_title),
                        subtitle = stringResource(R.string.support_subtitle),
                        onClick = { showSupportDialog = true },
                        position = ListItemPosition.Single,
                    )
                } else {
                    // Paid users see "Active" so the row reads as status,
                    // free users see "Upgrade" so it reads as a call-to-action.
                    SettingsNavItem(
                        icon = Icons.Default.AutoAwesome,
                        iconBgColor = yellow_light,
                        iconTint = yellow_dark,
                        title = if (isProEntitled) stringResource(R.string.settings_pro_title_active) else stringResource(R.string.settings_pro_title_upgrade),
                        subtitle = if (isProEntitled) {
                            stringResource(R.string.settings_pro_subtitle_active)
                        } else {
                            stringResource(R.string.settings_pro_subtitle_upgrade)
                        },
                        onClick = { showUpgradeSheet = true },
                        position = ListItemPosition.Single,
                    )
                }
            }

            // ── Personalization ──
            SettingsGroup {
                SettingsNavItem(
                    icon = Icons.Default.Palette,
                    iconBgColor = orange_light,
                    iconTint = orange_dark,
                    title = stringResource(R.string.settings_appearance_title),
                    subtitle = stringResource(R.string.settings_appearance_subtitle),
                    onClick = onNavigateToAppearance,
                    position = ListItemPosition.Top
                )
                if (hasLanguageRow) {
                    SettingsNavItem(
                        icon = Icons.Default.Language,
                        iconBgColor = blue_light,
                        iconTint = blue_dark,
                        title = stringResource(R.string.settings_language_title),
                        subtitle = stringResource(R.string.settings_language_subtitle),
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                                    .setData(Uri.fromParts("package", context.packageName, null))
                            )
                        },
                        position = ListItemPosition.Middle
                    )
                }
                SettingsNavItem(
                    icon = Icons.Default.AttachMoney,
                    iconBgColor = cyan_light,
                    iconTint = cyan_dark,
                    title = stringResource(R.string.settings_currency_formats_title),
                    subtitle = stringResource(R.string.settings_currency_formats_subtitle),
                    onClick = onNavigateToCurrencyFormats,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.People,
                    iconBgColor = purple_light,
                    iconTint = purple_dark,
                    title = stringResource(R.string.profile_settings_title),
                    subtitle = stringResource(R.string.profile_settings_subtitle),
                    onClick = onNavigateToProfiles,
                    position = ListItemPosition.Bottom,
                )
            }

            // ── Manage data ──
            SettingsManageDataGroup(
                onNavigateToManageAccounts = onNavigateToManageAccounts,
                onNavigateToCategories = onNavigateToCategories,
                onNavigateToRules = onNavigateToRules,
                onNavigateToBudgets = onNavigateToBudgets,
                onNavigateToLoans = onNavigateToLoans,
                onNavigateToRecurring = onNavigateToRecurring,
                onNavigateToTransactionGroups = onNavigateToTransactionGroups,
            )

            // ── Data, security & advanced ──
            SettingsGroup {
                SettingsNavItem(
                    icon = Icons.Default.Backup,
                    iconBgColor = blue_light,
                    iconTint = blue_dark,
                    title = stringResource(R.string.settings_backup_import_title),
                    subtitle = stringResource(R.string.settings_backup_import_subtitle),
                    onClick = onNavigateToBackupImport,
                    position = ListItemPosition.Top
                )
                SettingsNavItem(
                    icon = Icons.Default.Lock,
                    iconBgColor = red_light,
                    iconTint = red_dark,
                    title = stringResource(R.string.settings_privacy_security_title),
                    subtitle = stringResource(R.string.settings_privacy_security_subtitle),
                    onClick = onNavigateToPrivacySecurity,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.Tune,
                    iconBgColor = grey_light,
                    iconTint = grey_dark,
                    title = stringResource(R.string.settings_advanced_title),
                    subtitle = stringResource(R.string.settings_advanced_subtitle),
                    onClick = onNavigateToAdvanced,
                    position = ListItemPosition.Bottom
                )
            }

            // ── Support & Community ──
            SettingsGroup {
                SettingsNavItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    iconBgColor = pink_light,
                    iconTint = pink_dark,
                    title = stringResource(R.string.settings_help_faq_title),
                    subtitle = stringResource(R.string.settings_help_faq_subtitle),
                    onClick = onNavigateToFaq,
                    position = ListItemPosition.Top
                )
                SettingsNavItem(
                    icon = Icons.Default.BugReport,
                    iconBgColor = teal_light,
                    iconTint = teal_dark,
                    title = stringResource(R.string.settings_report_issue_title),
                    subtitle = stringResource(R.string.settings_report_issue_subtitle),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sarim2000/pennywiseai-tracker/issues/new/choose"))
                        context.startActivity(intent)
                    },
                    position = ListItemPosition.Middle,
                    trailingIcon = Icons.AutoMirrored.Filled.OpenInNew
                )
                SettingsNavItem(
                    icon = Icons.Default.Info,
                    iconBgColor = indigo_light,
                    iconTint = indigo_dark,
                    title = stringResource(R.string.about_title),
                    subtitle = stringResource(R.string.about_settings_subtitle),
                    onClick = onNavigateToAbout,
                    position = ListItemPosition.Bottom,
                )
            }

            // App Version
            Text(
                text = stringResource(R.string.settings_app_version, com.pennywiseai.tracker.BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            // Keeps the footer clear of the gesture / navigation bar at the end
            // of the scroll.
            Spacer(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(Spacing.Layout.scrollBottomPadding)
            )
        }
    }

    if (showUpgradeSheet) {
        com.pennywiseai.tracker.presentation.paywall.UpgradeSheet(
            onDismiss = { showUpgradeSheet = false },
        )
    }

    if (showSupportDialog) {
        SupportDevelopmentDialog(onDismiss = { showSupportDialog = false })
    }

    if (showEditProfile) {
        EditProfileSheet(onDismiss = { showEditProfile = false })
    }
}

/** Root-level destinations for the user's own data (Cashiro keeps these on the root). */
@Composable
internal fun SettingsManageDataGroup(
    onNavigateToManageAccounts: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToRules: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToTransactionGroups: () -> Unit,
) {
    SettingsGroup {
        SettingsNavItem(
            icon = Icons.Default.AccountBalance,
            iconBgColor = pink_light,
            iconTint = pink_dark,
            title = stringResource(R.string.settings_manage_accounts_title),
            subtitle = stringResource(R.string.settings_manage_accounts_subtitle),
            onClick = onNavigateToManageAccounts,
            position = ListItemPosition.Top,
        )
        SettingsNavItem(
            icon = Icons.Default.Category,
            iconBgColor = purple_light,
            iconTint = purple_dark,
            title = stringResource(R.string.settings_categories_title),
            subtitle = stringResource(R.string.settings_categories_subtitle),
            onClick = onNavigateToCategories,
            position = ListItemPosition.Middle,
        )
        SettingsNavItem(
            icon = Icons.Default.AutoAwesome,
            iconBgColor = orange_light,
            iconTint = orange_dark,
            title = stringResource(R.string.settings_smart_rules_title),
            subtitle = stringResource(R.string.settings_smart_rules_subtitle),
            onClick = onNavigateToRules,
            position = ListItemPosition.Middle,
        )
        SettingsNavItem(
            icon = Icons.Default.AccountBalanceWallet,
            iconBgColor = green_light,
            iconTint = green_dark,
            title = stringResource(R.string.settings_budgets_title),
            subtitle = stringResource(R.string.settings_budgets_subtitle),
            onClick = onNavigateToBudgets,
            position = ListItemPosition.Middle,
        )
        SettingsNavItem(
            icon = Icons.Default.SwapHoriz,
            iconBgColor = amber_light,
            iconTint = amber_dark,
            title = stringResource(R.string.settings_loans_title),
            subtitle = stringResource(R.string.settings_loans_subtitle),
            onClick = onNavigateToLoans,
            position = ListItemPosition.Middle,
        )
        SettingsNavItem(
            icon = Icons.Default.EventRepeat,
            iconBgColor = teal_light,
            iconTint = teal_dark,
            title = stringResource(R.string.settings_recurring_title),
            subtitle = stringResource(R.string.settings_recurring_subtitle),
            onClick = onNavigateToRecurring,
            position = ListItemPosition.Middle,
        )
        SettingsNavItem(
            icon = Icons.Default.Folder,
            iconBgColor = indigo_light,
            iconTint = indigo_dark,
            title = stringResource(R.string.settings_transaction_groups_title),
            subtitle = stringResource(R.string.settings_transaction_groups_subtitle),
            onClick = onNavigateToTransactionGroups,
            position = ListItemPosition.Bottom,
        )
    }
}

/** The Backup & import sub-screen's sections: backup/restore, then imports & SMS. */
@Composable
internal fun SettingsBackupSections(
    scheduledFolderBackupEnabled: Boolean,
    scheduledFolderBackupLastTimestamp: Long?,
    isProEntitled: Boolean,
    smsScanAllTime: Boolean,
    smsScanUseCustomDate: Boolean,
    smsScanCustomDate: Long?,
    smsScanMonths: Int,
    onExportData: () -> Unit,
    onScheduledFolderBackupChange: (Boolean) -> Unit,
    onBackupNow: () -> Unit,
    onChangeBackupFolder: () -> Unit,
    onImportBackup: () -> Unit,
    onImportCsv: () -> Unit,
    onNavigateToImportStatement: () -> Unit,
    onNavigateToUnrecognizedSms: () -> Unit,
    onSmsScanPeriod: () -> Unit,
    onDeleteAllTransactions: () -> Unit = {},
) {
    SettingsSection(title = stringResource(R.string.settings_data_backup_section)) {
        SettingsGroup {
            SettingsNavItem(
                icon = Icons.Default.Upload,
                iconBgColor = blue_light,
                iconTint = blue_dark,
                title = stringResource(R.string.settings_export_data_title),
                subtitle = stringResource(R.string.settings_export_data_subtitle),
                onClick = onExportData,
                position = ListItemPosition.Top,
            )
            SettingsSwitchRow(
                icon = Icons.Default.Backup,
                iconBgColor = purple_light,
                iconTint = purple_dark,
                title = stringResource(R.string.settings_folder_backup_title),
                subtitle = if (scheduledFolderBackupEnabled) {
                    stringResource(R.string.settings_folder_backup_subtitle_enabled)
                } else if (!isProEntitled) {
                    stringResource(R.string.settings_folder_backup_subtitle_pro)
                } else {
                    stringResource(R.string.settings_folder_backup_subtitle_disabled)
                },
                checked = scheduledFolderBackupEnabled,
                onCheckedChange = onScheduledFolderBackupChange,
                position = ListItemPosition.Middle,
            )
            if (scheduledFolderBackupEnabled) {
                SettingsNavItem(
                    icon = Icons.Default.SaveAlt,
                    iconBgColor = green_light,
                    iconTint = green_dark,
                    title = stringResource(R.string.settings_backup_now_title),
                    subtitle = scheduledFolderBackupLastTimestamp?.let { timestamp ->
                        val formatted = java.time.Instant.ofEpochMilli(timestamp)
                            .atZone(java.time.ZoneId.systemDefault())
                            .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
                        stringResource(R.string.settings_backup_now_last_backup, formatted)
                    } ?: stringResource(R.string.settings_backup_now_subtitle),
                    onClick = onBackupNow,
                    position = ListItemPosition.Middle,
                )
                SettingsNavItem(
                    icon = Icons.Default.FolderOpen,
                    iconBgColor = amber_light,
                    iconTint = amber_dark,
                    title = stringResource(R.string.settings_change_backup_folder_title),
                    subtitle = stringResource(R.string.settings_change_backup_folder_subtitle),
                    onClick = onChangeBackupFolder,
                    position = ListItemPosition.Middle,
                )
            }
            SettingsNavItem(
                icon = Icons.Default.Download,
                iconBgColor = cyan_light,
                iconTint = cyan_dark,
                title = stringResource(R.string.settings_import_data_title),
                subtitle = stringResource(R.string.settings_import_data_subtitle),
                onClick = onImportBackup,
                position = ListItemPosition.Bottom,
            )
        }
    }

    SettingsSection(title = stringResource(R.string.settings_data_imports_section)) {
        SettingsGroup {
            SettingsNavItem(
                icon = Icons.Default.Download,
                iconBgColor = cyan_light,
                iconTint = cyan_dark,
                title = stringResource(R.string.settings_import_csv_title),
                subtitle = stringResource(R.string.settings_import_csv_subtitle),
                onClick = onImportCsv,
                position = ListItemPosition.Top,
            )
            SettingsNavItem(
                icon = Icons.Default.Description,
                iconBgColor = indigo_light,
                iconTint = indigo_dark,
                title = stringResource(R.string.settings_import_statement_title),
                subtitle = stringResource(R.string.settings_import_statement_subtitle),
                onClick = onNavigateToImportStatement,
                position = ListItemPosition.Middle,
            )
            SettingsNavItem(
                icon = Icons.Default.Sms,
                iconBgColor = orange_light,
                iconTint = orange_dark,
                title = stringResource(R.string.settings_unrecognized_sms_title),
                subtitle = stringResource(R.string.settings_unrecognized_sms_subtitle),
                onClick = onNavigateToUnrecognizedSms,
                position = ListItemPosition.Middle,
            )
            SettingsNavItem(
                icon = Icons.Default.CalendarMonth,
                iconBgColor = teal_light,
                iconTint = teal_dark,
                title = stringResource(R.string.settings_sms_scan_title),
                subtitle = when {
                    smsScanAllTime -> stringResource(R.string.settings_sms_scan_subtitle_all_time)
                    smsScanUseCustomDate -> {
                        val formattedDate = smsScanCustomDate?.let { formatSmsScanCustomDate(it) }
                        if (formattedDate != null) {
                            stringResource(R.string.settings_sms_scan_subtitle_from_date, formattedDate)
                        } else {
                            stringResource(R.string.settings_sms_scan_subtitle_custom)
                        }
                    }
                    else -> pluralStringResource(R.plurals.settings_sms_scan_subtitle_months, smsScanMonths, smsScanMonths)
                },
                onClick = onSmsScanPeriod,
                position = ListItemPosition.Middle,
                trailingText = when {
                    smsScanAllTime -> stringResource(R.string.settings_sms_scan_all_time)
                    smsScanUseCustomDate -> {
                        smsScanCustomDate?.let { formatSmsScanCustomDateShort(it) }
                            ?: stringResource(R.string.settings_sms_scan_custom_short)
                    }
                    else -> pluralStringResource(R.plurals.settings_sms_scan_months_short, smsScanMonths, smsScanMonths)
                },
            )
            SettingsNavItem(
                icon = Icons.Default.DeleteForever,
                iconBgColor = red_light,
                iconTint = red_dark,
                title = stringResource(R.string.settings_delete_all_title),
                subtitle = stringResource(R.string.settings_delete_all_subtitle),
                onClick = onDeleteAllTransactions,
                position = ListItemPosition.Bottom,
            )
        }
    }
}

// ── Reusable Settings Components ──
//
// Row chrome — tonal surface, grouped-corner shape, padding, minimum height,
// the tinted icon circle, title/subtitle typography — lives in the shared
// `GroupedList` / `GroupedRow` / `IconTile` / `RowLabels` primitives, so a
// settings row and a grouped row on any other screen are literally the same
// object. These wrappers only add the settings-specific trailing affordance.

@Composable
private fun SettingsProfileHeaderCard(
    userName: String,
    profileImageUri: String?,
    profileBackgroundColor: Int,
    transactionCount: Int,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profileBgColor = if (profileBackgroundColor != 0) {
        Color(profileBackgroundColor)
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(Spacing.md),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(Dimensions.Component.minTouchTarget)
                    .clip(CircleShape)
                    .background(profileBgColor),
                contentAlignment = Alignment.Center
            ) {
                val avatarResId = profileImageUri?.let { AvatarHelper.resolveAvatarDrawable(it) }
                if (avatarResId != null) {
                    Image(
                        painter = painterResource(id = avatarResId),
                        contentDescription = userName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (profileImageUri != null) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = userName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val initials = remember(userName) {
                        val parts = userName.trim().split("\\s+".toRegex())
                        if (parts.size >= 2) {
                            "${parts.first().first()}${parts.last().first()}".uppercase()
                        } else {
                            userName.trim().take(2).uppercase()
                        }
                    }
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Text Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = pluralStringResource(R.plurals.settings_profile_header_subtitle, transactionCount, transactionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f)
                )
            }

            // Edit: opens the profile sheet; the rest of the card still opens the dashboard.
            IconButton(onClick = onEditClick) {
                Icon(
                    Iconax.Edit2,
                    contentDescription = stringResource(R.string.edit_profile_title),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(Dimensions.Icon.inline)
                )
            }

            // Chevron
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(Dimensions.Icon.inline)
            )
        }
    }
}

@Composable
internal fun SettingsGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    GroupedList(content = content)
}

@Composable
internal fun SettingsNavItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    position: ListItemPosition,
    trailingText: String? = null,
    trailingIcon: ImageVector = Icons.Default.ChevronRight
) {
    GroupedRow(position = position, onClick = onClick) {
        IconTile(icon = icon, containerColor = iconBgColor, contentColor = iconTint)
        RowLabels(title = title, subtitle = subtitle)
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        // The chevron is a hint, not a control — at 20dp it stops competing
        // with the leading icon for attention the way a 24dp one did.
        Icon(
            trailingIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimensions.Icon.inline)
        )
    }
}

@Composable
internal fun SettingsSwitchRow(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    position: ListItemPosition,
    enabled: Boolean = true
) {
    GroupedRow(
        position = position,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) }
    ) {
        IconTile(icon = icon, containerColor = iconBgColor, contentColor = iconTint)
        RowLabels(
            title = title,
            subtitle = subtitle,
            subtitleColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsDropdownItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    fieldLabel: String,
    currentValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    position: ListItemPosition,
    dropdownContent: @Composable ColumnScope.() -> Unit
) {
    GroupedColumn(position = position) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(icon = icon, containerColor = iconBgColor, contentColor = iconTint)
            RowLabels(title = title, subtitle = subtitle)
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            TextField(
                value = currentValue,
                onValueChange = {},
                readOnly = true,
                label = { Text(fieldLabel) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = MaterialTheme.shapes.large,
                colors = TextFieldDefaults.colors(
                    // A field nested inside an already-tonal row needs a step
                    // of contrast against it, otherwise the input boundary
                    // disappears.
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                content = dropdownContent
            )
        }
    }
}

@Composable
internal fun AiChatSettingsItem(
    downloadState: DownloadState,
    downloadProgress: Int,
    downloadedMB: Long,
    totalMB: Long,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    GroupedColumn(
        position = ListItemPosition.Single,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(
                icon = Icons.Default.AutoAwesome,
                containerColor = yellow_light,
                contentColor = yellow_dark
            )
            RowLabels(
                title = stringResource(R.string.settings_ai_chat_title),
                subtitle = when (downloadState) {
                    DownloadState.NOT_DOWNLOADED -> stringResource(R.string.settings_ai_status_not_downloaded, Constants.ModelDownload.MODEL_SIZE_MB)
                    DownloadState.DOWNLOADING -> stringResource(R.string.settings_ai_status_downloading)
                    DownloadState.PAUSED -> stringResource(R.string.settings_ai_status_paused)
                    DownloadState.COMPLETED -> stringResource(R.string.settings_ai_status_completed)
                    DownloadState.FAILED -> stringResource(R.string.settings_ai_status_failed)
                    DownloadState.ERROR_INSUFFICIENT_SPACE -> stringResource(R.string.settings_ai_status_insufficient_space)
                }
            )

            when (downloadState) {
                DownloadState.NOT_DOWNLOADED -> {
                    Button(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.settings_ai_download))
                    }
                }
                DownloadState.DOWNLOADING -> {
                    Text(
                        text = "$downloadProgress%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                DownloadState.PAUSED -> {
                    Button(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.settings_ai_retry))
                    }
                }
                DownloadState.COMPLETED -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.settings_ai_downloaded),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimensions.Icon.medium)
                        )
                        TextButton(onClick = onDelete) {
                            Text(stringResource(R.string.settings_ai_delete))
                        }
                    }
                }
                DownloadState.FAILED -> {
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.settings_ai_retry))
                    }
                }
                DownloadState.ERROR_INSUFFICIENT_SPACE -> {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = stringResource(R.string.settings_ai_error),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Dimensions.Icon.medium)
                    )
                }
            }
        }

        // Progress details during download
        AnimatedVisibility(
            visible = downloadState == DownloadState.DOWNLOADING,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.settings_ai_download_progress, downloadedMB, totalMB),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.settings_ai_cancel_download))
                }
            }
        }

        // Info about AI features
        if (downloadState == DownloadState.NOT_DOWNLOADED ||
            downloadState == DownloadState.ERROR_INSUFFICIENT_SPACE
        ) {
            HorizontalDivider()
            Text(
                text = stringResource(R.string.settings_ai_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsNavigationContent(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .animateContentSize()
            .padding(start = Spacing.md)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onNavigateBack,
            ),
    ) {
        IconButton(
            onClick = onNavigateBack,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.settings_back),
                modifier = Modifier.size(Dimensions.Icon.inline)
            )
        }
    }
}

@Composable
internal fun numberFormatStyleLabel(style: NumberFormatStyle): String = stringResource(
    when (style) {
        NumberFormatStyle.AUTO -> R.string.settings_number_format_auto
        NumberFormatStyle.INDIAN -> R.string.settings_number_format_indian
        NumberFormatStyle.INTERNATIONAL -> R.string.settings_number_format_international
    }
)

@Composable
internal fun numberFormatStyleExample(style: NumberFormatStyle): String = stringResource(
    when (style) {
        NumberFormatStyle.AUTO -> R.string.settings_number_format_auto_example
        NumberFormatStyle.INDIAN -> R.string.settings_number_format_indian_example
        NumberFormatStyle.INTERNATIONAL -> R.string.settings_number_format_international_example
    }
)

/**
 * English ordinal suffix for the budget cycle start day — "1st", "2nd", "3rd",
 * "4th"… "11th", "12th", "13th" follow the standard rule that the last two
 * digits decide the suffix (the 11/12/13 teens are always "th").
 */
internal fun ordinalSuffix(day: Int): String {
    val safe = day.coerceIn(1, 31)
    val suffix = when {
        safe in 11..13 -> "th"
        safe % 10 == 1 -> "st"
        safe % 10 == 2 -> "nd"
        safe % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$safe$suffix"
}

internal fun formatSmsScanCustomDate(dateMillis: Long): String {
    return java.time.Instant.ofEpochMilli(dateMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
}

internal fun formatSmsScanCustomDateShort(dateMillis: Long): String {
    return java.time.Instant.ofEpochMilli(dateMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))
}
