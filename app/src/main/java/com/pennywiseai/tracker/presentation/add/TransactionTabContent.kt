package com.pennywiseai.tracker.presentation.add

import com.pennywiseai.tracker.presentation.transactions.transactionTypeLabel
import com.pennywiseai.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.BudgetImpactType
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.domain.model.getAccountType
import com.pennywiseai.tracker.presentation.accounts.AccountType
import com.pennywiseai.tracker.ui.components.AccountSelectionSheet
import com.pennywiseai.tracker.ui.components.CategoryIcon
import com.pennywiseai.tracker.ui.components.NumberPad
import com.pennywiseai.tracker.ui.components.NumberPadInputState
import com.pennywiseai.tracker.ui.components.QuickCategoryPickerSheet
import com.pennywiseai.tracker.ui.components.TagInputField
import com.pennywiseai.tracker.ui.components.evaluateNumberExpression
import com.pennywiseai.tracker.ui.components.formatNumberPadResult
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.ui.theme.Spacing
import java.time.format.DateTimeFormatter
import java.util.Locale

// Reusable filled text field colors with no indicator
@Composable
private fun filledFieldColors() = TextFieldDefaults.colors(
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

/** Which account picker the shared account dropdown is currently assigning to. */
private enum class AccountPickerTarget { FROM, TO }

/**
 * Tappable account card used by the single-account picker and by both legs of a
 * TRANSFER. Shows the selected account (alias when set, else bank name, with a
 * ••last4 subtitle) or [placeholder], with a clear button when an account is
 * set. The alias line matches the dropdown items so the account doesn't change
 * name the moment the menu closes (#637).
 */
@Composable
private fun AccountSelectorCard(
    account: AccountBalanceEntity?,
    placeholder: String,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
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
                when (account?.getAccountType()) {
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
                    text = account?.let { it.alias?.takeIf { a -> a.isNotBlank() } ?: it.bankName }
                        ?: placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (account != null)
                        MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (account != null &&
                    account.accountLast4 != AccountBalanceEntity.WALLET_ACCOUNT_MARKER) {
                    Text(
                        text = "••${account.accountLast4}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (account != null) {
                IconButton(
                    onClick = onClear,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionTabContent(
    viewModel: AddViewModel,
    onSave: () -> Unit
) {
    val uiState by viewModel.transactionUiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAmountCalculator by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    // Which account picker is open (null = closed). For a TRANSFER this routes the
    // chosen account to either the FROM or TO card; for other types only FROM is used.
    var accountPickerTarget by remember { mutableStateOf<AccountPickerTarget?>(null) }
    var showCurrencyMenu by remember { mutableStateOf(false) }

    val isTransfer = uiState.transactionType == TransactionType.TRANSFER
    val topShape = ListItemPosition.Top.toShape()
    val bottomShape = ListItemPosition.Bottom.toShape()
    val fullShape = ListItemPosition.Single.toShape()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimensions.Padding.content, vertical = Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // ── Amount ──
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
                        colors = filledFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = showCurrencyMenu,
                        onDismissRequest = { showCurrencyMenu = false }
                    ) {
                        CurrencyFormatter.getSupportedCurrencies().forEach { currency ->
                            DropdownMenuItem(
                                text = { Text("${CurrencyFormatter.getCurrencySymbol(currency)} $currency") },
                                onClick = {
                                    viewModel.updateTransactionCurrency(currency)
                                    showCurrencyMenu = false
                                }
                            )
                        }
                    }
                }

                TextField(
                    value = uiState.amount,
                    onValueChange = viewModel::updateTransactionAmount,
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
                    colors = filledFieldColors()
                )
            }

            // Type follows the amount so the transaction's meaning is set
            // before the form asks for merchant/account-specific details.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                TransactionType.values().forEach { type ->
                    FilterChip(
                        selected = uiState.transactionType == type,
                        onClick = { viewModel.updateTransactionType(type) },
                        label = {
                            Text(stringResource(transactionTypeLabel(type)))
                        },
                        leadingIcon = if (uiState.transactionType == type) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(Dimensions.Icon.small)
                                )
                            }
                        } else {
                            null
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderWidth = Dimensions.Padding.none,
                            selected = uiState.transactionType == type,
                            enabled = true
                        )
                    )
                }
            }

            // ── Date + Time row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date button
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
                                text = uiState.date.format(
                                    DateTimeFormatter.ofPattern("yyyy", Locale.getDefault())
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = uiState.date.format(
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

                // Time display
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.sm, vertical = Spacing.sm)
                        .clickable { showTimePicker = true },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        val hour = if (uiState.date.hour % 12 == 0) 12 else uiState.date.hour % 12
                        val minute = uiState.date.minute
                        val amPm = uiState.date.format(
                            DateTimeFormatter.ofPattern("a", Locale.getDefault())
                        )

                        Box(
                            modifier = Modifier
                                .padding(Spacing.xs)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(0.2f),
                                    shape = MaterialTheme.shapes.small
                                )
                        ) {
                            Text(
                                text = String.format("%02d", hour),
                                color = MaterialTheme.colorScheme.primary,
                                style = PennyWiseText.amountMedium,
                                modifier = Modifier.padding(Spacing.xs)
                            )
                        }
                        Text(
                            text = ":",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = PennyWiseText.amountMedium
                        )
                        Box(
                            modifier = Modifier
                                .padding(Spacing.xs)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small
                                )
                        ) {
                            Text(
                                text = String.format("%02d", minute),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = PennyWiseText.amountMedium,
                                modifier = Modifier.padding(Spacing.xs)
                            )
                        }
                        Box(modifier = Modifier.padding(Spacing.xs)) {
                            Text(
                                text = amPm,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Account(s) + Category (connected cards) ──
            if (isTransfer) {
                // Two account pickers: From (money out) and To (money in).
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
                ) {
                    AccountSelectorCard(
                        account = uiState.selectedAccount,
                        placeholder = stringResource(R.string.add_txn_from_account),
                        shape = topShape,
                        onClick = { accountPickerTarget = AccountPickerTarget.FROM },
                        onClear = { viewModel.updateSelectedAccount(null) }
                    )
                    AccountSelectorCard(
                        account = uiState.toAccount,
                        placeholder = stringResource(R.string.add_txn_to_account),
                        shape = bottomShape,
                        onClick = { accountPickerTarget = AccountPickerTarget.TO },
                        onClear = { viewModel.updateToAccount(null) }
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
                ) {
                    // Account card
                    AccountSelectorCard(
                        account = uiState.selectedAccount,
                        placeholder = stringResource(R.string.add_txn_select_account),
                        shape = topShape,
                        onClick = { accountPickerTarget = AccountPickerTarget.FROM },
                        onClear = { viewModel.updateSelectedAccount(null) }
                    )

                    AddCategorySelector(
                        category = uiState.category,
                        error = uiState.categoryError?.asString(),
                        onClick = { showCategoryPicker = true }
                    )
                }
            }

            accountPickerTarget?.let { target ->
                val selectedAccount = when (target) {
                    AccountPickerTarget.TO -> uiState.toAccount
                    AccountPickerTarget.FROM -> uiState.selectedAccount
                }
                AccountSelectionSheet(
                    accounts = accounts,
                    selectedAccount = selectedAccount,
                    allowManualEntry = !isTransfer,
                    title = stringResource(
                        if (isTransfer && target == AccountPickerTarget.TO) {
                            R.string.account_selection_title_to
                        } else if (isTransfer) {
                            R.string.account_selection_title_from
                        } else {
                            R.string.account_selection_title
                        }
                    ),
                    onAccountSelected = { account ->
                        if (target == AccountPickerTarget.TO) {
                            viewModel.updateToAccount(account)
                        } else {
                            viewModel.updateSelectedAccount(account)
                        }
                        accountPickerTarget = null
                    },
                    onDismissRequest = { accountPickerTarget = null }
                )
            }

            if (showCategoryPicker) {
                QuickCategoryPickerSheet(
                    currentCategory = uiState.category,
                    categories = categories,
                    title = stringResource(R.string.category_picker_select_title),
                    searchPlaceholder = stringResource(R.string.category_picker_search_placeholder),
                    onCategorySelected = { category ->
                        viewModel.updateTransactionCategory(category)
                        showCategoryPicker = false
                    },
                    onDismiss = { showCategoryPicker = false }
                )
            }

            // ── Merchant + Notes (connected cards) ──
            // Merchant is meaningless for a TRANSFER (it moves money between own
            // accounts), so hide it and show Notes on its own.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
            ) {
                if (!isTransfer) {
                    TextField(
                        value = uiState.merchant,
                        onValueChange = viewModel::updateTransactionMerchant,
                        label = { Text(stringResource(R.string.add_field_merchant), fontWeight = FontWeight.SemiBold) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = topShape,
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                        isError = uiState.merchantError != null,
                        supportingText = uiState.merchantError?.let { { Text(it.asString()) } },
                        colors = filledFieldColors()
                    )
                }

                TextField(
                    value = uiState.notes,
                    onValueChange = viewModel::updateTransactionNotes,
                    label = { Text(stringResource(R.string.add_field_notes), fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = if (isTransfer) fullShape else bottomShape,
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    colors = filledFieldColors()
                )
            }

            // ── Tags (create or select existing) ──
            val allTagNames by viewModel.allTagNames.collectAsState()
            TagInputField(
                selectedTags = uiState.tags,
                allTags = allTagNames,
                onAddTag = viewModel::addTransactionTag,
                onRemoveTag = viewModel::removeTransactionTag
            )

            // ── Budget Impact (INCOME only) ──
            if (uiState.transactionType == TransactionType.INCOME) {
                val activeBudgetCategories by viewModel.activeBudgetCategories.collectAsState()
                AddBudgetImpactSection(
                    budgetImpactType = uiState.budgetImpactType,
                    budgetCategory = uiState.budgetCategory,
                    activeBudgetCategories = activeBudgetCategories,
                    onImpactTypeChange = viewModel::updateBudgetImpactType,
                    onCategoryChange = viewModel::updateBudgetCategory
                )
            }

            uiState.error?.let { errorMessage ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(Dimensions.Padding.cardCompact),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = errorMessage.asString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // ── Receipt ──
            ReceiptPickerSection(
                receiptUri = uiState.receiptUri,
                onReceiptSelected = { uri -> viewModel.updateReceiptUri(uri) },
                onReceiptRemoved = { viewModel.updateReceiptUri(null) },
                onCreateCameraUri = { viewModel.createCameraUri() }
            )

            // Bottom padding for save button overlay
            Spacer(modifier = Modifier.height(Dimensions.Component.bottomBarHeight))
        }

        // ── Sticky Save Button ──
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
                onClick = { viewModel.saveTransaction(onSuccess = onSave) },
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
                viewModel.updateTransactionAmount(amount)
                showAmountCalculator = false
            }
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.date
                .toLocalDate()
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
                            viewModel.updateTransactionDate(millis)
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

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.date.hour,
            initialMinute = uiState.date.minute
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.add_select_time)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateTransactionTime(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) { Text(stringResource(R.string.add_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.add_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AmountCalculatorSheet(
    initialAmount: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
) {
    val initialExpression = remember(initialAmount) {
        initialAmount.takeIf { evaluateNumberExpression(it) != null }.orEmpty()
    }
    var inputState by remember(initialExpression) {
        mutableStateOf(
            NumberPadInputState(
                expression = initialExpression,
                replaceOnNextNumber = initialExpression.isNotBlank()
            )
        )
    }
    val result = evaluateNumberExpression(inputState.expression)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(
                    start = Dimensions.Padding.dialog,
                    end = Dimensions.Padding.dialog,
                    bottom = Dimensions.Padding.dialog
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.add_calculator_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.add_close_calculator)
                    )
                }
            }
            Text(
                text = stringResource(R.string.add_calculator_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NumberPad(
                state = inputState,
                onStateChange = { inputState = it }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.add_cancel))
                }
                Button(
                    onClick = { result?.let { onApply(formatNumberPadResult(it)) } },
                    enabled = result != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.add_use_amount))
                }
            }
        }
    }
}

@Composable
internal fun AddCategorySelector(
    category: String,
    error: String?,
    onClick: () -> Unit,
    position: ListItemPosition = ListItemPosition.Bottom,
    modifier: Modifier = Modifier
) {
    val shape = position.toShape()
    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = if (error == null) {
                    MaterialTheme.colorScheme.surfaceContainerLow
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimensions.Component.listItemMinHeight)
                    .padding(horizontal = Dimensions.Padding.cardCompact),
                horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIcon(
                    category = category,
                    size = Dimensions.Icon.medium,
                    tint = if (error == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
                ) {
                    Text(
                        text = stringResource(R.string.add_category_label),
                        style = PennyWiseText.fieldLabel,
                        color = if (error == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (error == null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (error == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(
                    start = Dimensions.Padding.cardCompact,
                    top = Spacing.xs
                )
            )
        }
    }
}

@Composable
fun ReceiptPickerSection(
    receiptUri: android.net.Uri?,
    onReceiptSelected: (android.net.Uri) -> Unit,
    onReceiptRemoved: () -> Unit,
    onCreateCameraUri: () -> android.net.Uri
) {
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { onReceiptSelected(it) } }

    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraUri?.let { onReceiptSelected(it) } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = stringResource(R.string.add_receipt),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (receiptUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
            ) {
                AsyncImage(
                    model = receiptUri,
                    contentDescription = stringResource(R.string.add_receipt_image),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = Dimensions.Component.receiptPreviewHeight),
                    contentScale = ContentScale.Crop
                )
                FilledIconButton(
                    onClick = onReceiptRemoved,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.xs)
                        .size(Dimensions.Component.minTouchTarget),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.add_receipt_remove),
                        modifier = Modifier.size(Dimensions.Icon.inline),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.small))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.add_receipt_gallery))
                }
                OutlinedButton(
                    onClick = {
                        val uri = onCreateCameraUri()
                        cameraUri = uri
                        cameraLauncher.launch(uri)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.small))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.add_receipt_camera))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetImpactSection(
    budgetImpactType: BudgetImpactType?,
    budgetCategory: String?,
    activeBudgetCategories: List<String>,
    onImpactTypeChange: (BudgetImpactType?) -> Unit,
    onCategoryChange: (String?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(
            text = stringResource(R.string.txn_detail_budget_impact),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = budgetImpactType == null,
                onClick = { onImpactTypeChange(null) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                label = { Text(stringResource(R.string.txn_detail_budget_impact_none), style = MaterialTheme.typography.labelSmall) }
            )
            SegmentedButton(
                selected = budgetImpactType == BudgetImpactType.DEDUCT_SPENT,
                onClick = { onImpactTypeChange(BudgetImpactType.DEDUCT_SPENT) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                label = { Text(stringResource(R.string.txn_detail_budget_impact_refund), style = MaterialTheme.typography.labelSmall) }
            )
            SegmentedButton(
                selected = budgetImpactType == BudgetImpactType.ADD_TO_LIMIT,
                onClick = { onImpactTypeChange(BudgetImpactType.ADD_TO_LIMIT) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                label = { Text(stringResource(R.string.txn_detail_budget_impact_extra), style = MaterialTheme.typography.labelSmall) }
            )
        }

        if (budgetImpactType != null) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = budgetCategory ?: stringResource(R.string.txn_detail_budget_select_category),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.txn_detail_budget_category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (activeBudgetCategories.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.txn_detail_budget_no_categories)) },
                            onClick = { expanded = false },
                            enabled = false
                        )
                    } else {
                        activeBudgetCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    onCategoryChange(category)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
