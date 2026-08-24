package com.pennywiseai.tracker.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.preferences.NumberFormatStyle
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.icons.iconsax.Convertshape2
import com.pennywiseai.tracker.ui.icons.iconsax.DollarCircle
import com.pennywiseai.tracker.ui.icons.iconsax.Iconsax
import com.pennywiseai.tracker.ui.icons.iconsax.Wallet3
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.blue_dark
import com.pennywiseai.tracker.ui.theme.blue_light
import com.pennywiseai.tracker.ui.theme.green_dark
import com.pennywiseai.tracker.ui.theme.green_light
import com.pennywiseai.tracker.ui.theme.indigo_dark
import com.pennywiseai.tracker.ui.theme.indigo_light
import com.pennywiseai.tracker.ui.theme.purple_dark
import com.pennywiseai.tracker.ui.theme.purple_light
import com.pennywiseai.tracker.ui.theme.teal_dark
import com.pennywiseai.tracker.ui.theme.teal_light
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExchangeRates: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val unifiedCurrencyMode by viewModel.unifiedCurrencyMode.collectAsStateWithLifecycle(initialValue = false)
    val displayCurrency by viewModel.displayCurrency.collectAsStateWithLifecycle(initialValue = "")
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle(initialValue = "")
    val availableCurrencies by viewModel.availableCurrencies.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val mainAccountKey by viewModel.mainAccountKey.collectAsStateWithLifecycle()
    val numberFormatStyle by viewModel.numberFormatStyle.collectAsStateWithLifecycle(initialValue = NumberFormatStyle.AUTO)
    var showDisplayCurrencyDialog by remember { mutableStateOf(false) }
    var showNumberFormatDialog by remember { mutableStateOf(false) }
    var showCurrencyDropdown by remember { mutableStateOf(false) }
    var showMainAccountDropdown by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        topBar = {
            CustomTitleTopAppBar(
                title = "Currency",
                scrollBehaviorSmall = scrollBehavior,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background).overScrollVertical()
                .verticalScroll(rememberScrollState()).padding(paddingValues)
                .padding(Dimensions.Padding.content),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.sm)
        ) {
            SectionHeaderV2(title = "Currency display")
            SettingsGroup {
                SettingsSwitchRow(Iconsax.Convertshape2, green_light, green_dark,
                    "Unified Currency Mode", "Convert all transactions to display currency",
                    unifiedCurrencyMode, viewModel::setUnifiedCurrencyMode, ListItemPosition.Top)
                AnimatedVisibility(unifiedCurrencyMode) {
                    SettingsNavItem(Iconsax.DollarCircle, teal_light, teal_dark, "Display Currency",
                        "All amounts shown in this currency", { showDisplayCurrencyDialog = true },
                        ListItemPosition.Middle,
                        trailingText = "${CurrencyFormatter.getCurrencySymbol(displayCurrency)} $displayCurrency")
                }
                SettingsNavItem(Icons.Default.SwapHoriz, blue_light, blue_dark, "Exchange Rates",
                    "View and customize rates", onNavigateToExchangeRates, ListItemPosition.Middle)
                SettingsDropdownItem(Icons.Default.SwapHoriz, indigo_light, indigo_dark,
                    "Default Currency", "Currency used for conversions",
                    "${CurrencyFormatter.getCurrencySymbol(baseCurrency)} $baseCurrency",
                    showCurrencyDropdown, { showCurrencyDropdown = it }, ListItemPosition.Middle) {
                    availableCurrencies.forEach { currency ->
                        DropdownMenuItem(text = { Text("${CurrencyFormatter.getCurrencySymbol(currency)} $currency") },
                            onClick = { viewModel.updateBaseCurrency(currency); showCurrencyDropdown = false },
                            leadingIcon = if (currency == baseCurrency) {{ Icon(Icons.Default.Check, null) }} else null)
                    }
                }
                if (accounts.isNotEmpty()) {
                    val mainAccount = accounts.firstOrNull { "${it.bankName}_${it.accountLast4}" == mainAccountKey }
                    SettingsDropdownItem(Iconsax.Wallet3, purple_light, purple_dark, "Main Account",
                        "Sets your default currency", mainAccount?.let {
                            AccountBalanceEntity.accountLabel(it.alias?.takeIf(String::isNotBlank) ?: it.bankName, it.accountLast4)
                        } ?: "Not set", showMainAccountDropdown, { showMainAccountDropdown = it }, ListItemPosition.Middle) {
                        accounts.forEach { account ->
                            val key = "${account.bankName}_${account.accountLast4}"
                            val label = AccountBalanceEntity.accountLabel(account.alias?.takeIf(String::isNotBlank) ?: account.bankName, account.accountLast4)
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                viewModel.setMainAccount(account); showMainAccountDropdown = false
                            }, leadingIcon = if (key == mainAccountKey) {{ Icon(Icons.Default.Check, null) }} else null)
                        }
                    }
                }
                SettingsNavItem(Icons.Default.Numbers, green_light, green_dark, "Number Format",
                    "How large amounts are grouped", { showNumberFormatDialog = true }, ListItemPosition.Bottom,
                    trailingText = numberFormatLabel(numberFormatStyle))
            }
            Spacer(Modifier.height(Spacing.md))
        }
    }

    if (showDisplayCurrencyDialog) CurrencyChoiceDialog("Display Currency", availableCurrencies, displayCurrency,
        onSelect = { viewModel.setDisplayCurrency(it); showDisplayCurrencyDialog = false },
        onDismiss = { showDisplayCurrencyDialog = false })
    if (showNumberFormatDialog) AlertDialog(onDismissRequest = { showNumberFormatDialog = false },
        title = { Text("Number Format") }, text = {
            Column {
                NumberFormatStyle.entries.forEach { style ->
                    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().selectable(style == numberFormatStyle) {
                        viewModel.updateNumberFormatStyle(style); showNumberFormatDialog = false
                    }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(style == numberFormatStyle, onClick = { viewModel.updateNumberFormatStyle(style); showNumberFormatDialog = false })
                        Column { Text(numberFormatLabel(style)); Text(numberFormatExample(style), style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { showNumberFormatDialog = false }) { Text("Cancel") } })
}

@Composable
private fun CurrencyChoiceDialog(title: String, currencies: List<String>, selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            currencies.forEach { currency ->
                androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().selectable(currency == selected) { onSelect(currency) }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(currency == selected, onClick = { onSelect(currency) })
                    Text("${CurrencyFormatter.getCurrencySymbol(currency)} $currency")
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

private fun numberFormatLabel(style: NumberFormatStyle) = when (style) {
    NumberFormatStyle.AUTO -> "Auto"
    NumberFormatStyle.INDIAN -> "Indian"
    NumberFormatStyle.INTERNATIONAL -> "International"
}

private fun numberFormatExample(style: NumberFormatStyle) = when (style) {
    NumberFormatStyle.AUTO -> "Matches each currency"
    NumberFormatStyle.INDIAN -> "1,50,000 (lakh / crore)"
    NumberFormatStyle.INTERNATIONAL -> "150,000 (thousand / million)"
}
