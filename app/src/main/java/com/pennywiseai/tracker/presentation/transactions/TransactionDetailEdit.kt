package com.pennywiseai.tracker.presentation.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.BudgetImpactType
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.presentation.add.AddAmountHero
import com.pennywiseai.tracker.presentation.add.AddChoiceChip
import com.pennywiseai.tracker.presentation.add.AddDateCard
import com.pennywiseai.tracker.presentation.add.AddPillSwitcher
import com.pennywiseai.tracker.presentation.add.AddSectionLabel
import com.pennywiseai.tracker.presentation.add.AddTimeCard
import com.pennywiseai.tracker.presentation.add.AmountCalculatorSheet
import com.pennywiseai.tracker.presentation.add.ReceiptPickerSection
import com.pennywiseai.tracker.presentation.categories.CategoryEditDialog
import com.pennywiseai.tracker.presentation.people.tonalTextFieldColors
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.CategoryChip
import com.pennywiseai.tracker.ui.components.PreferenceSwitch
import com.pennywiseai.tracker.ui.components.SplitEditor
import com.pennywiseai.tracker.ui.components.SplitItem
import com.pennywiseai.tracker.ui.components.TagInputField
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.icons.iconax.Card
import com.pennywiseai.tracker.ui.icons.iconax.DocumentText2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Category2
import com.pennywiseai.tracker.ui.icons.iconax.Wallet3
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * The edit-mode form of the Transaction Detail screen. Same fields and the same
 * ViewModel calls as before; only the look changed, to match the Add screen
 * (itself a Cashiro-style form): a big amount hero, choice chips for the type,
 * tonal date / time tiles, and borderless tonal fields grouped into connected
 * blocks whose outer corners are round and whose seams are nearly square.
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TxnDetailEditForm(
    transaction: TransactionEntity,
    applyToAllFromMerchant: Boolean,
    updateExistingTransactions: Boolean,
    existingTransactionCount: Int,
    accountProfileId: Long?,
    viewModel: TransactionDetailViewModel,
    splits: List<SplitItem>,
    showSplitEditor: Boolean,
    modifier: Modifier = Modifier,
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val isTransfer = transaction.transactionType == TransactionType.TRANSFER
    val isIncome = transaction.transactionType == TransactionType.INCOME

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.smd)
    ) {
        // ── Amount hero ──
        TxnEditAmount(transaction = transaction, viewModel = viewModel)

        // ── Type ──
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            AddSectionLabel(text = stringResource(R.string.add_txn_type_label))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                TransactionType.entries.forEach { type ->
                    AddChoiceChip(
                        selected = transaction.transactionType == type,
                        onClick = { viewModel.updateTransactionType(type) },
                        label = stringResource(transactionTypeLabel(type))
                    )
                }
            }
        }

        // ── Date + time ──
        TxnEditDateTime(
            dateTime = transaction.dateTime,
            onDateTimeChange = { viewModel.updateDateTime(it) }
        )

        // ── Account(s) + category (connected) ──
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
        ) {
            if (isTransfer) {
                TxnEditAccountField(
                    accountNumber = transaction.fromAccount,
                    onAccountNumberChange = { viewModel.updateFromAccount(it) },
                    viewModel = viewModel,
                    position = ListItemPosition.Top,
                    label = stringResource(R.string.txn_detail_field_from_account),
                    placeholder = stringResource(R.string.txn_detail_field_from_account_placeholder),
                    excludeAccount = transaction.toAccount
                )
                TxnEditAccountField(
                    accountNumber = transaction.toAccount,
                    onAccountNumberChange = { viewModel.updateToAccount(it) },
                    viewModel = viewModel,
                    position = if (showSplitEditor) ListItemPosition.Bottom else ListItemPosition.Middle,
                    label = stringResource(R.string.txn_detail_field_to_account),
                    placeholder = stringResource(R.string.txn_detail_field_to_account_placeholder),
                    excludeAccount = transaction.fromAccount
                )
            } else {
                TxnEditAccountField(
                    accountNumber = transaction.accountNumber,
                    onAccountNumberChange = { viewModel.updateAccountNumber(it) },
                    onBankNameChange = { viewModel.updateBankName(it) },
                    viewModel = viewModel,
                    position = if (showSplitEditor) ListItemPosition.Single else ListItemPosition.Top,
                    label = stringResource(R.string.txn_detail_field_account),
                    placeholder = stringResource(R.string.txn_detail_field_account_placeholder)
                )
            }

            if (!showSplitEditor) {
                TxnEditCategoryField(
                    selectedCategory = transaction.category,
                    onCategorySelected = { viewModel.updateCategory(it) },
                    isIncomeTransaction = isIncome,
                    viewModel = viewModel,
                    position = ListItemPosition.Bottom
                )
            }
        }

        // ── Split into categories ──
        if (!showSplitEditor && transaction.transactionType in TransactionDetailViewModel.SPLITTABLE_TYPES) {
            FilledTonalButton(
                onClick = { viewModel.enableSplitMode() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimensions.Component.minTouchTarget),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CallSplit,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.small)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(stringResource(R.string.txn_detail_split_into_categories))
            }
        }

        if (showSplitEditor) {
            SplitEditor(
                totalAmount = transaction.amount,
                currency = transaction.currency,
                splits = splits,
                availableCategories = categories.map { it.name },
                onSplitsChanged = { viewModel.updateSplits(it) },
                onRemoveSplits = { viewModel.removeSplits() },
                // SplitEditor insets itself by a gutter, which on top of this
                // screen's own gutter left it narrower than every other block.
                modifier = Modifier.cancelHorizontalInset(Spacing.md)
            )
        }

        // ── Merchant + alias + notes (connected) ──
        TxnEditMerchantFields(transaction = transaction, viewModel = viewModel)

        // ── Tags (create or select existing) ──
        val editTags by viewModel.editableTags.collectAsStateWithLifecycle()
        val allTagNames by viewModel.allTagNames.collectAsStateWithLifecycle()
        TagInputField(
            selectedTags = editTags,
            allTags = allTagNames,
            onAddTag = { viewModel.addTag(it) },
            onRemoveTag = { viewModel.removeTag(it) }
        )
        // #752: mirrors the category checkbox below — saved as a Smart Rule.
        if (editTags.isNotEmpty()) {
            val applyTagsToAll by viewModel.applyTagsToAllFromMerchant.collectAsStateWithLifecycle()
            GroupedList {
                TxnEditCheckRow(
                    checked = applyTagsToAll,
                    onToggle = { viewModel.toggleApplyTagsToAllFromMerchant() },
                    text = stringResource(R.string.txn_detail_apply_tags_to_merchant, transaction.merchantName),
                    position = ListItemPosition.Single
                )
            }
        }

        // ── Apply to merchant / existing ──
        if (!showSplitEditor) {
            GroupedList {
                val hasExisting = existingTransactionCount > 0
                TxnEditCheckRow(
                    checked = applyToAllFromMerchant,
                    onToggle = { viewModel.toggleApplyToAllFromMerchant() },
                    text = stringResource(R.string.txn_detail_apply_category_to_merchant, transaction.merchantName),
                    position = if (hasExisting) ListItemPosition.Top else ListItemPosition.Single
                )
                if (hasExisting) {
                    TxnEditCheckRow(
                        checked = updateExistingTransactions,
                        onToggle = { viewModel.toggleUpdateExistingTransactions() },
                        text = pluralStringResource(
                            R.plurals.txn_detail_update_existing,
                            existingTransactionCount,
                            existingTransactionCount
                        ),
                        position = ListItemPosition.Bottom
                    )
                }
            }

            if (isIncome) {
                TxnEditBudgetImpact(viewModel = viewModel)
            }
        }

        // ── Classification ──
        val accountDefault = accountProfileId ?: ProfileEntity.PERSONAL_ID
        val effectiveProfileId = transaction.profileId ?: accountDefault
        val isEffectivelyBusiness = effectiveProfileId == ProfileEntity.BUSINESS_ID
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            AddSectionLabel(text = stringResource(R.string.txn_detail_label_classification))
            AddPillSwitcher(
                options = listOf(
                    stringResource(R.string.txn_detail_personal),
                    stringResource(R.string.txn_detail_business)
                ),
                selectedIndex = if (isEffectivelyBusiness) 1 else 0,
                onIndexChange = { index ->
                    val newId = if (index == 0) {
                        if (accountDefault == ProfileEntity.PERSONAL_ID) null else ProfileEntity.PERSONAL_ID
                    } else {
                        if (accountDefault == ProfileEntity.BUSINESS_ID) null else ProfileEntity.BUSINESS_ID
                    }
                    viewModel.updateProfileId(newId)
                }
            )
        }

        // ── Recurring ──
        GroupedList {
            PreferenceSwitch(
                title = stringResource(R.string.txn_detail_recurring_checkbox),
                subtitle = stringResource(R.string.txn_detail_ui_recurring_subtitle),
                checked = transaction.isRecurring,
                onCheckedChange = { viewModel.updateRecurringStatus(it) },
                position = ListItemPosition.Single
            )
        }

        // ── Receipt attachment ──
        val existingReceiptUri by viewModel.receiptUri.collectAsStateWithLifecycle()
        val pendingReceiptUri by viewModel.pendingReceiptUri.collectAsStateWithLifecycle()
        val receiptRemoved by viewModel.receiptRemoved.collectAsStateWithLifecycle()
        val displayReceiptUri = pendingReceiptUri ?: if (receiptRemoved) null else existingReceiptUri
        ReceiptPickerSection(
            receiptUri = displayReceiptUri,
            onReceiptSelected = { uri -> viewModel.updatePendingReceiptUri(uri) },
            onReceiptRemoved = { viewModel.removeReceipt() },
            onCreateCameraUri = { viewModel.createCameraUri() },
            tonal = true
        )

        // ── Bank (read-only) ──
        transaction.bankName?.let { bank ->
            Row(
                modifier = Modifier.padding(horizontal = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Icon(
                    imageVector = Iconax.Wallet3,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimensions.Icon.small)
                )
                Text(
                    text = stringResource(R.string.txn_detail_bank_read_only, bank),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Original SMS (never editable) ──
        if (!transaction.smsBody.isNullOrBlank()) {
            TxnDetailSmsSection(smsBody = transaction.smsBody)
        }

        // Room for the sticky Save button.
        Spacer(modifier = Modifier.height(Dimensions.Component.bottomBarHeight))
    }
}

/**
 * Cancels a horizontal inset the child applies to itself (SplitEditor insets
 * itself by one gutter): the child is measured [inset] wider on each side and
 * shifted back, so its card ends up exactly as wide as its siblings.
 */
private fun Modifier.cancelHorizontalInset(inset: Dp): Modifier = layout { measurable, constraints ->
    val insetPx = inset.roundToPx()
    val widened = if (constraints.hasBoundedWidth) {
        constraints.copy(
            minWidth = constraints.minWidth + 2 * insetPx,
            maxWidth = constraints.maxWidth + 2 * insetPx
        )
    } else {
        constraints
    }
    val placeable = measurable.measure(widened)
    val width = if (constraints.hasBoundedWidth) {
        (placeable.width - 2 * insetPx).coerceAtLeast(0)
    } else {
        placeable.width
    }
    layout(width, placeable.height) {
        placeable.placeRelative(if (constraints.hasBoundedWidth) -insetPx else 0, 0)
    }
}

// ── Amount ────────────────────────────────────────────────────────────────

@Composable
private fun TxnEditAmount(
    transaction: TransactionEntity,
    viewModel: TransactionDetailViewModel,
) {
    val primaryCurrency by viewModel.primaryCurrency.collectAsStateWithLifecycle()
    var showCalculator by remember { mutableStateOf(false) }
    val amountText = transaction.amount.stripTrailingZeros().toPlainString()

    AddAmountHero(
        amount = amountText,
        currency = transaction.currency.ifEmpty { primaryCurrency },
        onAmountChange = { viewModel.updateAmount(it) },
        onCurrencyChange = { viewModel.updateCurrency(it) },
        onOpenCalculator = { showCalculator = true }
    )

    if (showCalculator) {
        AmountCalculatorSheet(
            initialAmount = amountText,
            onDismiss = { showCalculator = false },
            onApply = { applied ->
                viewModel.updateAmount(applied)
                showCalculator = false
            }
        )
    }
}

// ── Date + time ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TxnEditDateTime(
    dateTime: LocalDateTime,
    onDateTimeChange: (LocalDateTime) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        AddDateCard(
            dayLabel = dateTime.format(DateTimeFormatter.ofPattern("dd MMMM", Locale.getDefault())),
            yearLabel = dateTime.format(DateTimeFormatter.ofPattern("yyyy", Locale.getDefault())),
            onClickLabel = stringResource(R.string.add_pick_date),
            onClick = { showDatePicker = true },
            modifier = Modifier.weight(1f)
        )
        AddTimeCard(
            dateTime = dateTime,
            onClick = { showTimePicker = true },
            modifier = Modifier.weight(1f)
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateTime.toLocalDate().toEpochDay() * 24 * 60 * 60 * 1000
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // The picker reports UTC midnight of the chosen day.
                        val newDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                        onDateTimeChange(
                            dateTime.withYear(newDate.year)
                                .withMonth(newDate.monthValue)
                                .withDayOfMonth(newDate.dayOfMonth)
                        )
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.txn_detail_action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.txn_detail_action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = dateTime.hour,
            initialMinute = dateTime.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(stringResource(R.string.txn_detail_select_time)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    onDateTimeChange(
                        dateTime.withHour(timePickerState.hour)
                            .withMinute(timePickerState.minute)
                    )
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.txn_detail_action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.txn_detail_action_cancel))
                }
            }
        )
    }
}

// ── Merchant, alias, notes ────────────────────────────────────────────────

@Composable
private fun TxnEditMerchantFields(
    transaction: TransactionEntity,
    viewModel: TransactionDetailViewModel,
) {
    val merchantAlias by viewModel.merchantAlias.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
    ) {
        TextField(
            value = transaction.merchantName,
            onValueChange = { viewModel.updateMerchantName(it) },
            label = {
                Text(stringResource(R.string.txn_detail_field_merchant), fontWeight = FontWeight.SemiBold)
            },
            leadingIcon = {
                BrandIcon(
                    merchantName = transaction.merchantName,
                    category = transaction.category,
                    size = Dimensions.Icon.medium,
                    showBackground = false
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = ListItemPosition.Top.toShape(),
            isError = transaction.merchantName.isBlank(),
            colors = tonalTextFieldColors()
        )

        // Display alias (#583): shown in place of the merchant everywhere,
        // for all transactions from it. The raw merchant above is unchanged.
        TextField(
            value = merchantAlias,
            onValueChange = { viewModel.updateMerchantAlias(it) },
            label = {
                Text(stringResource(R.string.txn_detail_field_alias), fontWeight = FontWeight.SemiBold)
            },
            placeholder = { Text(stringResource(R.string.txn_detail_field_alias_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = ListItemPosition.Middle.toShape(),
            colors = tonalTextFieldColors()
        )

        TextField(
            value = transaction.description ?: "",
            onValueChange = { viewModel.updateDescription(it) },
            label = {
                Text(stringResource(R.string.txn_detail_field_description), fontWeight = FontWeight.SemiBold)
            },
            leadingIcon = {
                Icon(
                    imageVector = Iconax.DocumentText2,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = ListItemPosition.Bottom.toShape(),
            colors = tonalTextFieldColors()
        )
    }
}

/** A checkbox on a tonal row; the whole row toggles, and the box carries its own semantics. */
@Composable
private fun TxnEditCheckRow(
    checked: Boolean,
    onToggle: () -> Unit,
    text: String,
    position: ListItemPosition,
) {
    GroupedRow(
        position = position,
        onClick = onToggle,
        contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Budget impact (INCOME only) ───────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TxnEditBudgetImpact(viewModel: TransactionDetailViewModel) {
    val budgetImpactType by viewModel.budgetImpactType.collectAsStateWithLifecycle()
    val budgetCategory by viewModel.budgetCategory.collectAsStateWithLifecycle()
    val activeBudgetCategories by viewModel.activeBudgetCategories.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        AddSectionLabel(text = stringResource(R.string.txn_detail_budget_impact))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            AddChoiceChip(
                selected = budgetImpactType == null,
                onClick = { viewModel.updateBudgetImpactType(null) },
                label = stringResource(R.string.txn_detail_budget_impact_none)
            )
            AddChoiceChip(
                selected = budgetImpactType == BudgetImpactType.DEDUCT_SPENT,
                onClick = { viewModel.updateBudgetImpactType(BudgetImpactType.DEDUCT_SPENT) },
                label = stringResource(R.string.txn_detail_budget_impact_refund)
            )
            AddChoiceChip(
                selected = budgetImpactType == BudgetImpactType.ADD_TO_LIMIT,
                onClick = { viewModel.updateBudgetImpactType(BudgetImpactType.ADD_TO_LIMIT) },
                label = stringResource(R.string.txn_detail_budget_impact_extra)
            )
        }

        if (budgetImpactType != null) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                TextField(
                    value = budgetCategory ?: stringResource(R.string.txn_detail_budget_select_category),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.txn_detail_budget_category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = ListItemPosition.Single.toShape(),
                    colors = tonalTextFieldColors()
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
                                    viewModel.updateBudgetCategory(category)
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

// ── Category ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TxnEditCategoryField(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    isIncomeTransaction: Boolean,
    viewModel: TransactionDetailViewModel,
    position: ListItemPosition,
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Find the selected category entity for displaying with color
    val selectedCategoryEntity = categories.find { it.name == selectedCategory }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selectedCategory,
            onValueChange = { },
            label = {
                Text(stringResource(R.string.txn_detail_label_category), fontWeight = FontWeight.SemiBold)
            },
            leadingIcon = {
                if (selectedCategoryEntity != null) {
                    CategoryChip(
                        category = selectedCategoryEntity,
                        showText = false,
                        modifier = Modifier.padding(start = Spacing.smd)
                    )
                } else {
                    Icon(
                        imageVector = Iconax.Category2,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.medium)
                    )
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            shape = position.toShape(),
            colors = tonalTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        CategoryChip(
                            category = category,
                            modifier = Modifier.padding(
                                start = if (category.parentId != null) Spacing.lg else Spacing.none
                            )
                        )
                    },
                    onClick = {
                        onCategorySelected(category.name)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }

            HorizontalDivider()

            // Create a new category without leaving the edit flow (#584)
            DropdownMenuItem(
                text = { Text(stringResource(R.string.txn_detail_add_category)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.medium)
                    )
                },
                onClick = {
                    expanded = false
                    showAddDialog = true
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
            )
        }
    }

    if (showAddDialog) {
        CategoryEditDialog(
            defaultIsIncome = isIncomeTransaction,
            lockType = true,
            onDismiss = { showAddDialog = false },
            onSave = { name, color, _, icon, _ ->
                // Dismiss only once the category is actually created/selected, so a
                // failure (e.g. same-name type conflict) keeps the dialog open with
                // the user's input intact.
                viewModel.createAndSelectCategory(name, color, icon) { success ->
                    if (success) showAddDialog = false
                }
            }
        )
    }
}

// ── Account ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TxnEditAccountField(
    accountNumber: String?,
    onAccountNumberChange: (String?) -> Unit,
    viewModel: TransactionDetailViewModel,
    position: ListItemPosition,
    label: String,
    placeholder: String,
    excludeAccount: String? = null,
    // Fired alongside onAccountNumberChange when a real account is picked from
    // the dropdown, so the transaction's bankName follows the selected account
    // (accounts are keyed by bankName+last4). Only the single-account field
    // wires this; transfer From/To fields leave it null. See #566 / #570.
    onBankNameChange: ((String?) -> Unit)? = null,
) {
    val availableAccounts by viewModel.availableAccounts.collectAsStateWithLifecycle()
    val filteredAccounts = availableAccounts.filter { it.accountLast4 != excludeAccount }
    var expanded by remember { mutableStateOf(false) }
    var selectedAccount by remember(accountNumber) {
        mutableStateOf(
            availableAccounts.find {
                accountNumber?.endsWith(it.accountLast4) == true
            }?.displayName ?: accountNumber ?: ""
        )
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selectedAccount,
            onValueChange = { newValue ->
                selectedAccount = newValue
                // If manually typing, update the account number directly
                if (!availableAccounts.any { it.displayName == newValue }) {
                    onAccountNumberChange(newValue.ifEmpty { null })
                }
            },
            label = { Text(label, fontWeight = FontWeight.SemiBold) },
            leadingIcon = {
                Icon(
                    imageVector = if (availableAccounts.any { it.displayName == selectedAccount && it.isCreditCard }) {
                        Iconax.Card
                    } else {
                        Iconax.Wallet3
                    },
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
            },
            shape = position.toShape(),
            colors = tonalTextFieldColors(),
            trailingIcon = {
                Row {
                    // Clear button if there's text
                    if (selectedAccount.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                selectedAccount = ""
                                onAccountNumberChange(null)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.txn_detail_clear),
                                modifier = Modifier.size(Dimensions.Icon.medium)
                            )
                        }
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
            singleLine = true,
            placeholder = { Text(placeholder) }
        )

        if (filteredAccounts.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredAccounts.forEach { account ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Icon(
                                    imageVector = if (account.isCreditCard) Iconax.Card else Iconax.Wallet3,
                                    contentDescription = null,
                                    modifier = Modifier.size(Dimensions.Icon.medium),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(account.displayName)
                            }
                        },
                        onClick = {
                            selectedAccount = account.displayName
                            onAccountNumberChange(account.accountLast4)
                            onBankNameChange?.invoke(account.bankName)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}
