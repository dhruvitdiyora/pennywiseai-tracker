package com.pennywiseai.tracker.presentation.add

import com.pennywiseai.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.SubscriptionDirection
import com.pennywiseai.tracker.domain.model.getAccountType
import com.pennywiseai.tracker.presentation.accounts.AccountType
import com.pennywiseai.tracker.ui.components.AccountSelectionSheet
import com.pennywiseai.tracker.ui.components.QuickCategoryPickerSheet
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.presentation.subscriptions.CustomBillingCycleEditor
import com.pennywiseai.tracker.presentation.subscriptions.subscriptionBillingCycleLabel
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
private fun subFilledColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    disabledIndicatorColor = Color.Transparent,
    disabledLabelColor = MaterialTheme.colorScheme.primary,
    disabledTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
)

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
    var showCurrencyMenu by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showAmountCalculator by remember { mutableStateOf(false) }

    val topShape = ListItemPosition.Top.toShape()
    val bottomShape = ListItemPosition.Bottom.toShape()
    val fullShape = ListItemPosition.Single.toShape()

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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimensions.Padding.content, vertical = Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Error Card
            uiState.error?.let { errorMessage ->
                PennyWiseCardV2(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    contentPadding = Dimensions.Padding.cardCompact
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(Dimensions.Icon.medium)
                        )
                        Text(
                            text = errorMessage.asString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Income / Expense direction toggle (#371). Income subscriptions
            // get phantom-created on schedule (for accounts that don't send
            // SMS — wallets, allowances). Expense subscriptions match
            // incoming bank-debit SMS like today.
            SubscriptionDirectionSection(
                direction = uiState.direction,
                onDirectionChange = viewModel::updateSubscriptionDirection
            )

            // Amount row: currency + amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.Top
            ) {
                ExposedDropdownMenuBox(
                    expanded = showCurrencyMenu,
                    onExpandedChange = { showCurrencyMenu = it },
                    modifier = Modifier.width(Dimensions.Component.currencySelectorWidth)
                ) {
                    TextField(
                        value = "${CurrencyFormatter.getCurrencySymbol(uiState.currency)} ${uiState.currency}",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCurrencyMenu) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        singleLine = true,
                        shape = fullShape,
                        colors = subFilledColors()
                    )
                    ExposedDropdownMenu(
                        expanded = showCurrencyMenu,
                        onDismissRequest = { showCurrencyMenu = false }
                    ) {
                        CurrencyFormatter.getSupportedCurrencies().forEach { currency ->
                            DropdownMenuItem(
                                text = { Text("${CurrencyFormatter.getCurrencySymbol(currency)} $currency") },
                                onClick = {
                                    viewModel.updateSubscriptionCurrency(currency)
                                    showCurrencyMenu = false
                                }
                            )
                        }
                    }
                }

                TextField(
                    value = uiState.amount,
                    onValueChange = viewModel::updateSubscriptionAmount,
                    label = { Text(stringResource(R.string.add_field_amount), fontWeight = FontWeight.SemiBold) },
                    textStyle = MaterialTheme.typography.headlineSmall,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.amountError != null,
                    supportingText = uiState.amountError?.let { { Text(it.asString()) } },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = fullShape,
                    trailingIcon = {
                        IconButton(onClick = { showAmountCalculator = true }) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = stringResource(R.string.add_open_calculator)
                            )
                        }
                    },
                    colors = subFilledColors()
                )
            }

            // Billing cycle + Next payment date row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
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
                        leadingIcon = { Icon(Icons.Default.EventRepeat, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        singleLine = true,
                        shape = fullShape,
                        colors = subFilledColors()
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

                Spacer(modifier = Modifier.width(Spacing.sm))

                // Next payment date
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(Spacing.sm)
                        .clickable(
                            onClick = { showDatePicker = true },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.size(Spacing.sm))
                        Column {
                            Text(
                                text = uiState.nextPaymentDate.format(
                                    DateTimeFormatter.ofPattern("yyyy", Locale.getDefault())
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = uiState.nextPaymentDate.format(
                                    DateTimeFormatter.ofPattern("dd MMMM", Locale.getDefault())
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

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
                Card(
                    onClick = { showAccountSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = topShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = Dimensions.Padding.cardCompact,
                                vertical = Dimensions.Padding.content
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.smd)
                    ) {
                        Icon(
                            when (uiState.selectedAccount?.getAccountType()) {
                                AccountType.CASH -> Icons.Default.Money
                                AccountType.CREDIT -> Icons.Default.CreditCard
                                AccountType.SAVINGS, AccountType.CURRENT -> Icons.Default.AccountBalance
                                null -> Icons.Default.AccountBalance
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                // Alias-aware like the dropdown items, so the account
                                // doesn't change name when the menu closes (#637).
                                text = uiState.selectedAccount
                                    ?.let { it.alias?.takeIf { a -> a.isNotBlank() } ?: it.bankName }
                                    ?: stringResource(R.string.add_sub_paid_from),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (uiState.selectedAccount != null)
                                    MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.selectedAccount != null &&
                                uiState.selectedAccount?.accountLast4 != AccountBalanceEntity.WALLET_ACCOUNT_MARKER) {
                                Text(
                                    text = "••${uiState.selectedAccount?.accountLast4}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (uiState.selectedAccount != null) {
                            IconButton(
                                onClick = { viewModel.updateSubscriptionAccount(null) },
                                modifier = Modifier.size(Dimensions.Component.minTouchTarget)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.add_clear_account),
                                    modifier = Modifier.size(Dimensions.Icon.inline),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AddCategorySelector(
                    category = uiState.category,
                    error = uiState.categoryError?.asString(),
                    position = ListItemPosition.Bottom,
                    onClick = { showCategoryPicker = true }
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
                    leadingIcon = { Icon(Icons.Default.Subscriptions, contentDescription = null) },
                    isError = uiState.serviceError != null,
                    supportingText = uiState.serviceError?.let { { Text(it.asString()) } },
                    colors = subFilledColors()
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
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    colors = subFilledColors()
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

            // Bottom padding for save button overlay
            Spacer(modifier = Modifier.height(Dimensions.Component.bottomBarHeight))
        }

        // Sticky Save Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = { viewModel.saveSubscription(onSuccess = onSave) },
                enabled = uiState.isValid && !uiState.isLoading,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = Dimensions.Padding.content)
                    .fillMaxWidth()
                    .height(Dimensions.Component.listItemMinHeight)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimensions.Icon.small),
                        strokeWidth = Spacing.xxs
                    )
                } else {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.add_save), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
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
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !isIncome,
                onClick = { onDirectionChange(SubscriptionDirection.EXPENSE) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                modifier = Modifier.heightIn(min = Dimensions.Component.minTouchTarget),
                label = { Text(stringResource(R.string.add_sub_direction_expense)) }
            )
            SegmentedButton(
                selected = isIncome,
                onClick = { onDirectionChange(SubscriptionDirection.INCOME) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                modifier = Modifier.heightIn(min = Dimensions.Component.minTouchTarget),
                label = { Text(stringResource(R.string.add_sub_direction_income)) }
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
                    Icons.Default.Info,
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
