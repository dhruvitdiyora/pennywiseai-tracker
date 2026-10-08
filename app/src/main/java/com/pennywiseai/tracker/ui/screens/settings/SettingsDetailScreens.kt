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


/*
 * Settings sub-screens split out of the Settings root so the root reads as a
 * short list of destinations (Cashiro's layout). Each hosts the same controls,
 * dialogs and launchers that used to sit inline on the root, unchanged.
 */

@Composable
fun CurrencyFormatsSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExchangeRates: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val baseCurrency by settingsViewModel.baseCurrency.collectAsStateWithLifecycle(initialValue = "")
    val numberFormatStyle by settingsViewModel.numberFormatStyle.collectAsStateWithLifecycle(initialValue = NumberFormatStyle.AUTO)
    val budgetCycleStartDay by settingsViewModel.budgetCycleStartDay.collectAsStateWithLifecycle(initialValue = 1)
    val unifiedCurrencyMode by settingsViewModel.unifiedCurrencyMode.collectAsStateWithLifecycle(initialValue = false)
    val countCreditCardAsExpense by settingsViewModel.countCreditCardAsExpense.collectAsStateWithLifecycle(initialValue = false)
    val displayCurrency by settingsViewModel.displayCurrency.collectAsStateWithLifecycle(initialValue = "")
    val availableCurrencies by settingsViewModel.availableCurrencies.collectAsStateWithLifecycle()
    val accounts by settingsViewModel.accounts.collectAsStateWithLifecycle()
    val mainAccountKey by settingsViewModel.mainAccountKey.collectAsStateWithLifecycle()
    var showDisplayCurrencyDialog by remember { mutableStateOf(false) }
    var showNumberFormatDialog by remember { mutableStateOf(false) }
    var showBudgetCycleDialog by remember { mutableStateOf(false) }
    var showCurrencyDropdown by remember { mutableStateOf(false) }
    var showMainAccountDropdown by remember { mutableStateOf(false) }

    SettingsSubScreen(
        title = stringResource(R.string.settings_currency_formats_title),
        backContentDescription = stringResource(R.string.settings_back),
        onNavigateBack = onNavigateBack,
    ) {
        SettingsSection(title = stringResource(R.string.settings_currency_section)) {
            SettingsGroup {
                SettingsSwitchRow(
                    icon = Icons.Default.CurrencyExchange,
                    iconBgColor = green_light,
                    iconTint = green_dark,
                    title = stringResource(R.string.settings_unified_currency_title),
                    subtitle = stringResource(R.string.settings_unified_currency_subtitle),
                    checked = unifiedCurrencyMode,
                    onCheckedChange = { settingsViewModel.setUnifiedCurrencyMode(it) },
                    position = ListItemPosition.Top
                )
                AnimatedVisibility(visible = unifiedCurrencyMode) {
                    SettingsNavItem(
                        icon = Icons.Default.AttachMoney,
                        iconBgColor = teal_light,
                        iconTint = teal_dark,
                        title = stringResource(R.string.settings_display_currency_title),
                        subtitle = stringResource(R.string.settings_display_currency_subtitle),
                        onClick = { showDisplayCurrencyDialog = true },
                        position = ListItemPosition.Middle,
                        trailingText = "${CurrencyFormatter.getCurrencySymbol(displayCurrency)} $displayCurrency"
                    )
                }
                SettingsNavItem(
                    icon = Icons.Default.SwapHoriz,
                    iconBgColor = blue_light,
                    iconTint = blue_dark,
                    title = stringResource(R.string.settings_exchange_rates_title),
                    subtitle = stringResource(R.string.settings_exchange_rates_subtitle),
                    onClick = onNavigateToExchangeRates,
                    position = ListItemPosition.Middle
                )
                SettingsSwitchRow(
                    icon = Icons.Default.CreditCard,
                    iconBgColor = indigo_light,
                    iconTint = indigo_dark,
                    title = stringResource(R.string.settings_card_spend_as_expense_title),
                    subtitle = stringResource(R.string.settings_card_spend_as_expense_subtitle),
                    checked = countCreditCardAsExpense,
                    onCheckedChange = { settingsViewModel.setCountCreditCardAsExpense(it) },
                    position = ListItemPosition.Middle
                )
                SettingsDropdownItem(
                    icon = Icons.Default.Flag,
                    iconBgColor = indigo_light,
                    iconTint = indigo_dark,
                    title = stringResource(R.string.settings_default_currency_title),
                    subtitle = stringResource(R.string.settings_default_currency_subtitle),
                    fieldLabel = stringResource(R.string.settings_currency_field_label),
                    currentValue = "${CurrencyFormatter.getCurrencySymbol(baseCurrency)} $baseCurrency",
                    expanded = showCurrencyDropdown,
                    onExpandedChange = { showCurrencyDropdown = it },
                    position = ListItemPosition.Middle
                ) {
                    availableCurrencies.forEach { currency ->
                        DropdownMenuItem(
                            text = {
                                Text("${CurrencyFormatter.getCurrencySymbol(currency)} $currency")
                            },
                            onClick = {
                                settingsViewModel.updateBaseCurrency(currency)
                                showCurrencyDropdown = false
                            },
                            leadingIcon = if (currency == baseCurrency) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null
                        )
                    }
                }

                // Main account → sets the default currency (unless explicitly chosen above).
                if (accounts.isNotEmpty()) {
                    val mainAccount = accounts.firstOrNull {
                        "${it.bankName}_${it.accountLast4}" == mainAccountKey
                    }
                    SettingsDropdownItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconBgColor = purple_light,
                        iconTint = purple_dark,
                        title = stringResource(R.string.settings_main_account_title),
                        subtitle = stringResource(R.string.settings_main_account_subtitle),
                        fieldLabel = stringResource(R.string.settings_account_field_label),
                        currentValue = mainAccount?.let { acc ->
                            val name = acc.alias?.takeIf { it.isNotBlank() } ?: acc.bankName
                            AccountBalanceEntity.accountLabel(name, acc.accountLast4)
                        } ?: stringResource(R.string.settings_main_account_not_set),
                        expanded = showMainAccountDropdown,
                        onExpandedChange = { showMainAccountDropdown = it },
                        position = ListItemPosition.Middle
                    ) {
                        accounts.forEach { account ->
                            val name = account.alias?.takeIf { it.isNotBlank() } ?: account.bankName
                            val label = AccountBalanceEntity.accountLabel(name, account.accountLast4)
                            val key = "${account.bankName}_${account.accountLast4}"
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    settingsViewModel.setMainAccount(account)
                                    showMainAccountDropdown = false
                                },
                                leadingIcon = if (key == mainAccountKey) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }

                SettingsNavItem(
                    icon = Icons.Default.Numbers,
                    iconBgColor = green_light,
                    iconTint = green_dark,
                    title = stringResource(R.string.settings_number_format_title),
                    subtitle = stringResource(R.string.settings_number_format_subtitle),
                    onClick = { showNumberFormatDialog = true },
                    position = ListItemPosition.Bottom,
                    trailingText = numberFormatStyleLabel(numberFormatStyle)
                )
            }
        }

        // The cycle start day changes how Home / Analytics bucket
        // transactions, so it sits with the other display knobs.
        SettingsSection(title = stringResource(R.string.settings_budget_section)) {
            SettingsGroup {
                SettingsNavItem(
                    icon = Icons.Default.DateRange,
                    iconBgColor = teal_light,
                    iconTint = teal_dark,
                    title = stringResource(R.string.settings_budget_cycle_title),
                    subtitle = stringResource(R.string.settings_budget_cycle_subtitle),
                    onClick = { showBudgetCycleDialog = true },
                    position = ListItemPosition.Single,
                    trailingText = ordinalSuffix(budgetCycleStartDay)
                )
            }
        }
    }

    // Display Currency Dialog
    if (showDisplayCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showDisplayCurrencyDialog = false },
            title = { Text(stringResource(R.string.settings_display_currency_title)) },
            text = {
                // Scrollable: the full currency list overflows the dialog's max
                // height, so without this the entries below the fold (e.g. MXN)
                // are unreachable. (#615)
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    availableCurrencies.forEach { currency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = currency == displayCurrency,
                                    onClick = {
                                        settingsViewModel.setDisplayCurrency(currency)
                                        showDisplayCurrencyDialog = false
                                    }
                                )
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currency == displayCurrency,
                                onClick = {
                                    settingsViewModel.setDisplayCurrency(currency)
                                    showDisplayCurrencyDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = "${CurrencyFormatter.getCurrencySymbol(currency)} $currency",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisplayCurrencyDialog = false }) {
                    Text(stringResource(R.string.settings_action_cancel))
                }
            }
        )
    }

    // Number Format Dialog
    if (showNumberFormatDialog) {
        AlertDialog(
            onDismissRequest = { showNumberFormatDialog = false },
            title = { Text(stringResource(R.string.settings_number_format_title)) },
            text = {
                Column {
                    NumberFormatStyle.entries.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = style == numberFormatStyle,
                                    onClick = {
                                        settingsViewModel.updateNumberFormatStyle(style)
                                        showNumberFormatDialog = false
                                    }
                                )
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = style == numberFormatStyle,
                                onClick = {
                                    settingsViewModel.updateNumberFormatStyle(style)
                                    showNumberFormatDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Column {
                                Text(
                                    text = numberFormatStyleLabel(style),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = numberFormatStyleExample(style),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNumberFormatDialog = false }) {
                    Text(stringResource(R.string.settings_action_cancel))
                }
            }
        )
    }

    // Budget Cycle Start Day Dialog
    if (showBudgetCycleDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetCycleDialog = false },
            title = { Text(stringResource(R.string.settings_budget_cycle_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.settings_budget_cycle_dialog_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))

                    (1..31).forEach { day ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = day == budgetCycleStartDay,
                                    onClick = {
                                        settingsViewModel.updateBudgetCycleStartDay(day)
                                        showBudgetCycleDialog = false
                                    }
                                )
                                .padding(vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = day == budgetCycleStartDay,
                                onClick = {
                                    settingsViewModel.updateBudgetCycleStartDay(day)
                                    showBudgetCycleDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = ordinalSuffix(day),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBudgetCycleDialog = false }) {
                    Text(stringResource(R.string.settings_action_cancel))
                }
            }
        )
    }
}

@Composable
fun PrivacySecuritySettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDataPrivacy: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    appLockViewModel: com.pennywiseai.tracker.ui.viewmodel.AppLockViewModel = hiltViewModel(),
) {
    val appLockUiState by appLockViewModel.uiState.collectAsStateWithLifecycle()
    val useContactsForVpa by settingsViewModel.useContactsForVpa.collectAsStateWithLifecycle(initialValue = false)
    var showTimeoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // Launches the runtime permission request. If granted, we flip the
    // preference on; if denied, leave the switch off so the user can try
    // again without us silently turning the feature on later.
    val readContactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) settingsViewModel.setUseContactsForVpa(true)
    }

    SettingsSubScreen(
        title = stringResource(R.string.settings_privacy_security_title),
        backContentDescription = stringResource(R.string.settings_back),
        onNavigateBack = onNavigateBack,
    ) {
        SettingsSection(title = stringResource(R.string.settings_security_section)) {
            SettingsGroup {
                SettingsSwitchRow(
                    icon = Icons.Default.Lock,
                    iconBgColor = red_light,
                    iconTint = red_dark,
                    title = stringResource(R.string.settings_app_lock_title),
                    subtitle = if (appLockUiState.canUseBiometric) {
                        stringResource(R.string.settings_app_lock_subtitle)
                    } else {
                        appLockUiState.biometricCapability.errorMessageRes?.let { stringResource(it) }.orEmpty()
                    },
                    checked = appLockUiState.isLockEnabled,
                    onCheckedChange = { appLockViewModel.setAppLockEnabled(it) },
                    enabled = appLockUiState.canUseBiometric,
                    position = if (appLockUiState.isLockEnabled) ListItemPosition.Top else ListItemPosition.Single
                )
                AnimatedVisibility(visible = appLockUiState.isLockEnabled) {
                    SettingsNavItem(
                        icon = Icons.Default.Timer,
                        iconBgColor = pink_light,
                        iconTint = pink_dark,
                        title = stringResource(R.string.settings_lock_timeout_title),
                        subtitle = when (appLockUiState.timeoutMinutes) {
                            0 -> stringResource(R.string.settings_lock_timeout_immediately_subtitle)
                            else -> pluralStringResource(
                                R.plurals.settings_lock_timeout_after_minutes,
                                appLockUiState.timeoutMinutes,
                                appLockUiState.timeoutMinutes
                            )
                        },
                        onClick = { showTimeoutDialog = true },
                        position = ListItemPosition.Bottom
                    )
                }
            }
        }

        SettingsSection(title = stringResource(R.string.settings_contacts_section)) {
            SettingsGroup {
                SettingsSwitchRow(
                    icon = Icons.Default.Contacts,
                    iconBgColor = teal_light,
                    iconTint = teal_dark,
                    title = stringResource(R.string.settings_contacts_vpa_title),
                    subtitle = stringResource(R.string.settings_contacts_vpa_subtitle),
                    checked = useContactsForVpa,
                    onCheckedChange = { wantsOn ->
                        if (wantsOn) {
                            val alreadyGranted = androidx.core.content.ContextCompat
                                .checkSelfPermission(
                                    context,
                                    android.Manifest.permission.READ_CONTACTS
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (alreadyGranted) {
                                settingsViewModel.setUseContactsForVpa(true)
                            } else {
                                readContactsLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                            }
                        } else {
                            settingsViewModel.setUseContactsForVpa(false)
                        }
                    },
                    position = ListItemPosition.Single
                )
            }
        }

        SettingsSection(title = stringResource(R.string.data_privacy_title)) {
            SettingsGroup {
                SettingsNavItem(
                    icon = Icons.Default.PrivacyTip,
                    iconBgColor = purple_light,
                    iconTint = purple_dark,
                    title = stringResource(R.string.data_privacy_title),
                    subtitle = stringResource(R.string.data_privacy_settings_subtitle),
                    onClick = onNavigateToDataPrivacy,
                    position = ListItemPosition.Single,
                )
            }
        }
    }

    // Lock Timeout Dialog
    if (showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text(stringResource(R.string.settings_lock_timeout_title)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.settings_lock_timeout_dialog_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))

                    val timeoutOptions = listOf(
                        0 to stringResource(R.string.settings_lock_timeout_option_immediately),
                        1 to pluralStringResource(R.plurals.settings_lock_timeout_option_minutes, 1, 1),
                        5 to pluralStringResource(R.plurals.settings_lock_timeout_option_minutes, 5, 5),
                        15 to pluralStringResource(R.plurals.settings_lock_timeout_option_minutes, 15, 15)
                    )

                    timeoutOptions.forEach { (minutes, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    appLockViewModel.setTimeoutMinutes(minutes)
                                    showTimeoutDialog = false
                                }
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appLockUiState.timeoutMinutes == minutes,
                                onClick = {
                                    appLockViewModel.setTimeoutMinutes(minutes)
                                    showTimeoutDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeoutDialog = false }) {
                    Text(stringResource(R.string.settings_action_done))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupImportSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToImportStatement: () -> Unit = {},
    onNavigateToUnrecognizedSms: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val smsScanMonths by settingsViewModel.smsScanMonths.collectAsStateWithLifecycle(initialValue = 3)
    val smsScanAllTime by settingsViewModel.smsScanAllTime.collectAsStateWithLifecycle(initialValue = false)
    val smsScanUseCustomDate by settingsViewModel.smsScanUseCustomDate.collectAsStateWithLifecycle(initialValue = false)
    val smsScanCustomDate by settingsViewModel.smsScanCustomDate.collectAsStateWithLifecycle(initialValue = null)
    val importExportMessage by settingsViewModel.importExportMessage.collectAsStateWithLifecycle()
    val exportedBackupFile by settingsViewModel.exportedBackupFile.collectAsStateWithLifecycle()
    val deleteAllTransactionsCount by settingsViewModel.deleteAllTransactionsCount.collectAsStateWithLifecycle()
    val isDeletingAllTransactions by settingsViewModel.isDeletingAllTransactions.collectAsStateWithLifecycle()
    val deleteAllTransactionsResult by settingsViewModel.deleteAllTransactionsResult.collectAsStateWithLifecycle()
    val isProEntitled by settingsViewModel.isProEntitled.collectAsStateWithLifecycle()
    val scheduledFolderBackupEnabled by settingsViewModel.scheduledFolderBackupEnabled.collectAsStateWithLifecycle(initialValue = false)
    val scheduledFolderBackupLastTimestamp by settingsViewModel.scheduledFolderBackupLastTimestamp.collectAsStateWithLifecycle(initialValue = null)
    val requestFolderPicker by settingsViewModel.requestFolderPicker.collectAsStateWithLifecycle()
    var showUpgradeSheet by remember { mutableStateOf(false) }
    var showSmsScanDialog by remember { mutableStateOf(false) }
    var showSmsScanDatePicker by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                settingsViewModel.importBackup(it)
            }
        }
    )

    // File picker for CSV transaction import
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                settingsViewModel.importCsv(it)
            }
        }
    )

    // File saver for export
    val exportSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri ->
            uri?.let {
                settingsViewModel.saveBackupToFile(it)
            }
        }
    )

    val backupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri?.let { settingsViewModel.onBackupFolderSelected(it) }
        }
    )

    LaunchedEffect(requestFolderPicker) {
        if (requestFolderPicker) {
            backupFolderLauncher.launch(null)
            settingsViewModel.onFolderPickerLaunched()
        }
    }

    SettingsSubScreen(
        title = stringResource(R.string.settings_backup_import_title),
        backContentDescription = stringResource(R.string.settings_back),
        onNavigateBack = onNavigateBack,
    ) {
        SettingsBackupSections(
            scheduledFolderBackupEnabled = scheduledFolderBackupEnabled,
            scheduledFolderBackupLastTimestamp = scheduledFolderBackupLastTimestamp,
            isProEntitled = isProEntitled,
            smsScanAllTime = smsScanAllTime,
            smsScanUseCustomDate = smsScanUseCustomDate,
            smsScanCustomDate = smsScanCustomDate,
            smsScanMonths = smsScanMonths,
            onExportData = { settingsViewModel.exportBackup() },
            onScheduledFolderBackupChange = { enabled ->
                if (enabled && !isProEntitled) {
                    showUpgradeSheet = true
                } else {
                    settingsViewModel.setScheduledFolderBackupEnabled(enabled)
                }
            },
            onBackupNow = { settingsViewModel.backupToFolderNow() },
            onChangeBackupFolder = { settingsViewModel.requestChangeBackupFolder() },
            onImportBackup = { importLauncher.launch("*/*") },
            onImportCsv = { csvImportLauncher.launch("*/*") },
            onNavigateToImportStatement = onNavigateToImportStatement,
            onNavigateToUnrecognizedSms = onNavigateToUnrecognizedSms,
            onSmsScanPeriod = { showSmsScanDialog = true },
            onDeleteAllTransactions = { settingsViewModel.requestDeleteAllTransactions() },
        )
    }

    // SMS Scan Period Dialog
    if (showSmsScanDialog) {
        AlertDialog(
            onDismissRequest = { showSmsScanDialog = false },
            title = { Text(stringResource(R.string.settings_sms_scan_title)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.settings_sms_scan_dialog_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))

                    val options = listOf(-1, -2) + listOf(1, 2, 3, 6, 12, 24)
                    options.forEach { months ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (months) {
                                        -1 -> {
                                            settingsViewModel.updateSmsScanAllTime(true)
                                            showSmsScanDialog = false
                                        }
                                        -2 -> {
                                            showSmsScanDialog = false
                                            showSmsScanDatePicker = true
                                        }
                                        else -> {
                                            settingsViewModel.updateSmsScanMonths(months)
                                            settingsViewModel.updateSmsScanAllTime(false)
                                            showSmsScanDialog = false
                                        }
                                    }
                                }
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isSelected = when (months) {
                                -1 -> smsScanAllTime
                                -2 -> smsScanUseCustomDate && !smsScanAllTime
                                else -> smsScanMonths == months && !smsScanAllTime && !smsScanUseCustomDate
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    when (months) {
                                        -1 -> {
                                            settingsViewModel.updateSmsScanAllTime(true)
                                            showSmsScanDialog = false
                                        }
                                        -2 -> {
                                            showSmsScanDialog = false
                                            showSmsScanDatePicker = true
                                        }
                                        else -> {
                                            settingsViewModel.updateSmsScanMonths(months)
                                            settingsViewModel.updateSmsScanAllTime(false)
                                            showSmsScanDialog = false
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(
                                text = when (months) {
                                    -1 -> stringResource(R.string.settings_sms_scan_all_time)
                                    -2 -> {
                                        val formattedDate = smsScanCustomDate?.let { formatSmsScanCustomDate(it) }
                                        if (formattedDate != null) {
                                            stringResource(R.string.settings_sms_scan_option_custom_with_date, formattedDate)
                                        } else {
                                            stringResource(R.string.settings_sms_scan_option_custom)
                                        }
                                    }
                                    24 -> pluralStringResource(R.plurals.settings_sms_scan_option_years, 2, 2)
                                    else -> pluralStringResource(R.plurals.settings_sms_scan_option_months, months, months)
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSmsScanDialog = false }) {
                    Text(stringResource(R.string.settings_action_cancel))
                }
            }
        )
    }

    if (showSmsScanDatePicker) {
        val todayMillis = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val initialSelectedDateMillis = smsScanCustomDate
            ?: java.time.LocalDate.now()
                .minusMonths(3)
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedDateMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayMillis
                }
            }
        )

        // Cancelling/dismissing the picker returns to the scan-period dialog rather than
        // silently dropping the user back to Settings (they came here to change the period).
        fun reopenScanDialog() {
            showSmsScanDatePicker = false
            showSmsScanDialog = true
        }

        DatePickerDialog(
            onDismissRequest = { reopenScanDialog() },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            settingsViewModel.updateSmsScanCustomDate(millis)
                        }
                        showSmsScanDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.settings_action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { reopenScanDialog() }) {
                    Text(stringResource(R.string.settings_action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Delete-all-transactions confirmation. Irreversible and unbatched, so it
    // asks for the word DELETE rather than a single tap, names the exact number
    // of rows, and points at Export Data first.
    deleteAllTransactionsCount?.let { count ->
        var confirmationText by rememberSaveable(count) { mutableStateOf("") }
        // The confirmation word is deliberately not translated.
        val confirmWord = "DELETE"
        val confirmed = confirmationText.trim().equals(confirmWord, ignoreCase = false)

        AlertDialog(
            onDismissRequest = {
                if (!isDeletingAllTransactions) settingsViewModel.cancelDeleteAllTransactions()
            },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.settings_delete_all_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(
                        pluralStringResource(R.plurals.settings_delete_all_dialog_body, count, count)
                    )
                    Text(
                        stringResource(R.string.settings_delete_all_dialog_kept),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = confirmationText,
                        onValueChange = { confirmationText = it },
                        singleLine = true,
                        enabled = !isDeletingAllTransactions,
                        label = { Text(stringResource(R.string.settings_delete_all_dialog_type_to_confirm, confirmWord)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { settingsViewModel.deleteAllTransactions(count) },
                    enabled = confirmed && !isDeletingAllTransactions,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (isDeletingAllTransactions) stringResource(R.string.settings_delete_all_dialog_deleting) else stringResource(R.string.settings_delete_all_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { settingsViewModel.cancelDeleteAllTransactions() },
                    enabled = !isDeletingAllTransactions
                ) {
                    Text(stringResource(R.string.settings_action_cancel))
                }
            }
        )
    }

    deleteAllTransactionsResult?.let { sentences ->
        AlertDialog(
            onDismissRequest = { settingsViewModel.clearDeleteAllTransactionsResult() },
            title = { Text(stringResource(R.string.settings_delete_all_result_title)) },
            text = { Text(sentences.map { it.asString() }.joinToString(" ")) },
            confirmButton = {
                TextButton(onClick = { settingsViewModel.clearDeleteAllTransactionsResult() }) {
                    Text(stringResource(R.string.settings_action_ok))
                }
            }
        )
    }

    // Show import/export message
    importExportMessage?.let { message ->
        if (exportedBackupFile != null && message == UiText.Res(R.string.settings_backup_created_choose)) {
            showExportOptionsDialog = true
        } else {
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(5000)
                settingsViewModel.clearImportExportMessage()
            }

            AlertDialog(
                onDismissRequest = { settingsViewModel.clearImportExportMessage() },
                title = { Text(stringResource(R.string.settings_backup_status_title)) },
                text = { Text(message.asString()) },
                confirmButton = {
                    TextButton(onClick = { settingsViewModel.clearImportExportMessage() }) {
                        Text(stringResource(R.string.settings_action_ok))
                    }
                }
            )
        }
    }

    // Export options dialog
    if (showExportOptionsDialog && exportedBackupFile != null) {
        val timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy_MM_dd_HHmmss")
        )
        val fileName = "PennyWise_Backup_$timestamp.pennywisebackup"

        AlertDialog(
            onDismissRequest = {
                showExportOptionsDialog = false
                settingsViewModel.clearImportExportMessage()
            },
            title = { Text(stringResource(R.string.settings_save_backup_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_save_backup_created))
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(stringResource(R.string.settings_save_backup_choose), style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            exportSaveLauncher.launch(fileName)
                            showExportOptionsDialog = false
                            settingsViewModel.clearImportExportMessage()
                        }
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.settings_save_backup_to_files))
                    }

                    TextButton(
                        onClick = {
                            settingsViewModel.shareBackup()
                            showExportOptionsDialog = false
                            settingsViewModel.clearImportExportMessage()
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.settings_save_backup_share))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportOptionsDialog = false
                        settingsViewModel.clearImportExportMessage()
                    }
                ) {
                    Text(stringResource(R.string.settings_action_cancel))
                }
            }
        )
    }

    if (showUpgradeSheet) {
        com.pennywiseai.tracker.presentation.paywall.UpgradeSheet(
            onDismiss = { showUpgradeSheet = false },
        )
    }
}

@Composable
fun AdvancedSettingsScreen(
    onNavigateBack: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    permissionViewModel: com.pennywiseai.tracker.ui.viewmodel.PermissionViewModel = hiltViewModel(),
) {
    val downloadState by settingsViewModel.downloadState.collectAsStateWithLifecycle()
    val downloadProgress by settingsViewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadedMB by settingsViewModel.downloadedMB.collectAsStateWithLifecycle()
    val totalMB by settingsViewModel.totalMB.collectAsStateWithLifecycle()
    val isDeveloperModeEnabled by settingsViewModel.isDeveloperModeEnabled.collectAsStateWithLifecycle(initialValue = false)
    val permissionUiState by permissionViewModel.uiState.collectAsStateWithLifecycle()
    val hasNotificationAccess = permissionUiState.hasNotificationAccess
    val notificationAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        permissionViewModel.refreshNotificationAccess()
    }

    SettingsSubScreen(
        title = stringResource(R.string.settings_advanced_title),
        backContentDescription = stringResource(R.string.settings_back),
        onNavigateBack = onNavigateBack,
    ) {
        SettingsSection(title = stringResource(R.string.settings_notifications_section)) {
            SettingsGroup {
                SettingsNavItem(
                    icon = Icons.Default.Notifications,
                    iconBgColor = indigo_light,
                    iconTint = indigo_dark,
                    title = stringResource(R.string.settings_notification_access_title),
                    subtitle = if (hasNotificationAccess) stringResource(R.string.settings_notification_access_enabled) else stringResource(R.string.settings_notification_access_disabled),
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        notificationAccessLauncher.launch(intent)
                    },
                    position = ListItemPosition.Single,
                    trailingText = if (hasNotificationAccess) stringResource(R.string.settings_notification_access_on) else stringResource(R.string.settings_notification_access_off)
                )
            }
        }

        SettingsSection(title = stringResource(R.string.settings_ai_section)) {
            SettingsGroup {
                AiChatSettingsItem(
                    downloadState = downloadState,
                    downloadProgress = downloadProgress,
                    downloadedMB = downloadedMB,
                    totalMB = totalMB,
                    onDownload = { settingsViewModel.startModelDownload() },
                    onCancel = { settingsViewModel.cancelDownload() },
                    onDelete = { settingsViewModel.deleteModel() }
                )
            }
        }

        SettingsSection(title = stringResource(R.string.settings_developer_section)) {
            SettingsGroup {
                SettingsSwitchRow(
                    icon = Icons.Default.Code,
                    iconBgColor = grey_light,
                    iconTint = grey_dark,
                    title = stringResource(R.string.settings_developer_mode_title),
                    subtitle = stringResource(R.string.settings_developer_mode_subtitle),
                    checked = isDeveloperModeEnabled,
                    onCheckedChange = { settingsViewModel.toggleDeveloperMode(it) },
                    position = ListItemPosition.Single
                )
            }
        }
    }
}
