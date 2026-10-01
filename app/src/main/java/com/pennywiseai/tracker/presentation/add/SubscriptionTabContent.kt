package com.pennywiseai.tracker.presentation.add

import com.pennywiseai.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.data.database.entity.SubscriptionDirection
import com.pennywiseai.tracker.presentation.subscriptions.CustomBillingCycleEditor
import com.pennywiseai.tracker.presentation.subscriptions.subscriptionBillingCycleLabel
import com.pennywiseai.tracker.ui.components.QuickCategoryPickerSheet
import com.pennywiseai.tracker.ui.components.AccountSelectionSheet
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.iconax.DocumentText2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Information
import com.pennywiseai.tracker.ui.icons.iconax.VideoPlay
import com.pennywiseai.tracker.ui.icons.iconax.VideoTime
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionTabContent(
    viewModel: AddViewModel,
    onSave: () -> Unit
) {
    val uiState by viewModel.subscriptionUiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showBillingCycleMenu by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showAmountCalculator by remember { mutableStateOf(false) }

    val topShape = ListItemPosition.Top.toShape()
    val bottomShape = ListItemPosition.Bottom.toShape()
    val fullShape = ListItemPosition.Single.toShape()
    val scrollState = rememberScrollState()

    val billingCycles = listOf(
        "Monthly" to R.string.subscription_cycle_monthly,
        "Quarterly" to R.string.subscription_cycle_quarterly,
        "Semi-Annual" to R.string.subscription_cycle_semi_annual,
        "Annual" to R.string.subscription_cycle_annual,
        "Weekly" to R.string.subscription_cycle_weekly,
        "Custom" to R.string.subscription_cycle_custom,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .imePadding()
                .verticalScroll(
                    state = scrollState,
                    flingBehavior = rememberOverscrollFlingBehavior { scrollState }
                )
                .padding(horizontal = Dimensions.Padding.content, vertical = Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.smd)
        ) {
            // Error banner
            uiState.error?.let { errorMessage ->
                AddErrorBanner(message = errorMessage.asString())
            }

            // ── Amount hero ──
            AddAmountHero(
                amount = uiState.amount,
                currency = uiState.currency,
                onAmountChange = viewModel::updateSubscriptionAmount,
                onCurrencyChange = viewModel::updateSubscriptionCurrency,
                onOpenCalculator = { showAmountCalculator = true },
                error = uiState.amountError?.asString()
            )

            // Income / Expense direction toggle (#371). Income subscriptions
            // get phantom-created on schedule (for accounts that don't send
            // SMS — wallets, allowances). Expense subscriptions match
            // incoming bank-debit SMS like today.
            SubscriptionDirectionSection(
                direction = uiState.direction,
                onDirectionChange = viewModel::updateSubscriptionDirection
            )

            // Billing cycle + Next payment date row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Billing cycle
                ExposedDropdownMenuBox(
                    expanded = showBillingCycleMenu,
                    onExpandedChange = { showBillingCycleMenu = it },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = subscriptionBillingCycleLabel(uiState.billingCycle),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.add_sub_billing_cycle), fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Iconax.VideoTime, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        singleLine = true,
                        shape = fullShape,
                        colors = addFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = showBillingCycleMenu,
                        onDismissRequest = { showBillingCycleMenu = false }
                    ) {
                        billingCycles.forEach { (cycle, labelResource) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(labelResource)) },
                                onClick = {
                                    viewModel.updateSubscriptionBillingCycle(cycle)
                                    showBillingCycleMenu = false
                                }
                            )
                        }
                    }
                }

                // Next payment date
                AddDateCard(
                    dayLabel = uiState.nextPaymentDate.format(
                        DateTimeFormatter.ofPattern("dd MMMM", Locale.getDefault())
                    ),
                    yearLabel = uiState.nextPaymentDate.format(
                        DateTimeFormatter.ofPattern("yyyy", Locale.getDefault())
                    ),
                    onClickLabel = stringResource(R.string.subscriptions_edit_next_date),
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                )
            }

            if (uiState.isCustomCycle) {
                PennyWiseCardV2(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    contentPadding = Dimensions.Padding.cardCompact
                ) {
                    CustomBillingCycleEditor(
                        countInput = uiState.customCycleCountInput,
                        unit = uiState.customCycleUnit,
                        onCountChanged = viewModel::updateSubscriptionCustomCycleCountInput,
                        onUnitChanged = viewModel::updateSubscriptionCustomCycleUnit,
                    )
                }
            }

            // ── Funding account (optional) ──
            // When set, marking this subscription paid (or an auto-created
            // scheduled income) moves the chosen account's balance. Leaving it
            // unset keeps the subscription unlinked, exactly like before. (#570)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
            ) {
                AddAccountSelectorCard(
                    account = uiState.selectedAccount,
                    placeholder = stringResource(R.string.add_sub_paid_from),
                    shape = topShape,
                    onClick = { showAccountSheet = true },
                    onClear = { viewModel.updateSubscriptionAccount(null) }
                )

                AddCategorySelector(
                    category = uiState.category,
                    error = uiState.categoryError?.asString(),
                    position = ListItemPosition.Bottom,
                    onClick = { showCategoryPicker = true },
                    minHeight = Dimensions.Component.listItemMinHeightTwoLine
                )
            }

            // Service name + notes remain a separate connected group after the
            // funding context has been chosen.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
            ) {
                TextField(
                    value = uiState.serviceName,
                    onValueChange = viewModel::updateSubscriptionService,
                    label = {
                        Text(
                            stringResource(R.string.add_sub_service_name),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = topShape,
                    leadingIcon = { Icon(Iconax.VideoPlay, contentDescription = null) },
                    isError = uiState.serviceError != null,
                    supportingText = uiState.serviceError?.let { { Text(it.asString()) } },
                    colors = addFieldColors()
                )

                TextField(
                    value = uiState.notes,
                    onValueChange = viewModel::updateSubscriptionNotes,
                    label = {
                        Text(
                            stringResource(R.string.add_field_notes),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = bottomShape,
                    leadingIcon = { Icon(Iconax.DocumentText2, contentDescription = null) },
                    colors = addFieldColors()
                )
            }

            // Bottom padding for save button overlay
            Spacer(modifier = Modifier.height(Dimensions.Component.bottomBarHeight))
        }

        // Sticky Save Button
        AddSaveBar(
            enabled = uiState.isValid && !uiState.isLoading,
            isLoading = uiState.isLoading,
            onClick = { viewModel.saveSubscription(onSuccess = onSave) }
        )
    }

    if (showAccountSheet) {
        AccountSelectionSheet(
            accounts = accounts,
            selectedAccount = uiState.selectedAccount,
            allowManualEntry = true,
            onAccountSelected = { account ->
                viewModel.updateSubscriptionAccount(account)
                showAccountSheet = false
            },
            onDismissRequest = { showAccountSheet = false }
        )
    }

    if (showCategoryPicker) {
        QuickCategoryPickerSheet(
            currentCategory = uiState.category,
            categories = categories,
            title = stringResource(R.string.category_picker_select_title),
            searchPlaceholder = stringResource(R.string.category_picker_search_placeholder),
            onCategorySelected = { category ->
                viewModel.updateSubscriptionCategory(category)
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false }
        )
    }

    if (showAmountCalculator) {
        AmountCalculatorSheet(
            initialAmount = uiState.amount,
            onDismiss = { showAmountCalculator = false },
            onApply = { amount ->
                viewModel.updateSubscriptionAmount(amount)
                showAmountCalculator = false
            }
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.nextPaymentDate
                .atStartOfDay()
                .toInstant(java.time.ZoneOffset.UTC)
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.updateSubscriptionNextPaymentDate(millis)
                        }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.add_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.add_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Expense / Income choice (as chips, matching the transaction-type row) plus the
 * explanation of what the chosen direction does.
 */
@Composable
internal fun SubscriptionDirectionSection(
    direction: SubscriptionDirection,
    onDirectionChange: (SubscriptionDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isIncome = direction == SubscriptionDirection.INCOME
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        AddSectionLabel(text = stringResource(R.string.add_sub_direction_label))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            AddChoiceChip(
                selected = !isIncome,
                onClick = { onDirectionChange(SubscriptionDirection.EXPENSE) },
                label = stringResource(R.string.add_sub_direction_expense)
            )
            AddChoiceChip(
                selected = isIncome,
                onClick = { onDirectionChange(SubscriptionDirection.INCOME) },
                label = stringResource(R.string.add_sub_direction_income)
            )
        }

        PennyWiseCardV2(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            contentPadding = Dimensions.Padding.cardCompact
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Iconax.Information,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
                Text(
                    text = stringResource(
                        if (isIncome) R.string.add_sub_info_income
                        else R.string.add_subscription_expense_info
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
