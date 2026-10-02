package com.pennywiseai.tracker.presentation.add

import com.pennywiseai.tracker.presentation.transactions.transactionTypeLabel
import com.pennywiseai.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.pennywiseai.tracker.data.database.entity.BudgetImpactType
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.ui.components.AccountSelectionSheet
import com.pennywiseai.tracker.ui.components.CategoryIcon
import com.pennywiseai.tracker.ui.components.QuickCategoryPickerSheet
import com.pennywiseai.tracker.ui.components.TagInputField
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.iconax.DocumentText2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Shop
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Which account picker the shared account sheet is currently assigning to. */
private enum class AccountPickerTarget { FROM, TO }

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

    val isTransfer = uiState.transactionType == TransactionType.TRANSFER
    val topShape = ListItemPosition.Top.toShape()
    val bottomShape = ListItemPosition.Bottom.toShape()
    val fullShape = ListItemPosition.Single.toShape()
    val scrollState = rememberScrollState()

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
            // ── Amount hero ──
            AddAmountHero(
                amount = uiState.amount,
                currency = uiState.currency,
                onAmountChange = viewModel::updateTransactionAmount,
                onCurrencyChange = viewModel::updateTransactionCurrency,
                onOpenCalculator = { showAmountCalculator = true },
                error = uiState.amountError?.asString()
            )

            // ── Type ──
            // Type follows the amount so the transaction's meaning is set
            // before the form asks for merchant/account-specific details.
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
                            selected = uiState.transactionType == type,
                            onClick = { viewModel.updateTransactionType(type) },
                            label = stringResource(transactionTypeLabel(type))
                        )
                    }
                }
            }

            // ── Date + Time row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                AddDateCard(
                    dayLabel = uiState.date.format(
                        DateTimeFormatter.ofPattern("dd MMMM", Locale.getDefault())
                    ),
                    yearLabel = uiState.date.format(
                        DateTimeFormatter.ofPattern("yyyy", Locale.getDefault())
                    ),
                    onClickLabel = stringResource(R.string.add_pick_date),
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                )
                AddTimeCard(
                    dateTime = uiState.date,
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Account(s) + Category (connected cards) ──
            if (isTransfer) {
                // Two account pickers: From (money out) and To (money in), with a
                // swap badge sitting on the seam between them.
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
                    ) {
                        AddAccountSelectorCard(
                            account = uiState.selectedAccount,
                            placeholder = stringResource(R.string.add_txn_from_account),
                            shape = topShape,
                            onClick = { accountPickerTarget = AccountPickerTarget.FROM },
                            onClear = { viewModel.updateSelectedAccount(null) }
                        )
                        AddAccountSelectorCard(
                            account = uiState.toAccount,
                            placeholder = stringResource(R.string.add_txn_to_account),
                            shape = bottomShape,
                            onClick = { accountPickerTarget = AccountPickerTarget.TO },
                            onClear = { viewModel.updateToAccount(null) }
                        )
                    }
                    AddTransferBadge(modifier = Modifier.align(Alignment.Center))
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
                ) {
                    AddAccountSelectorCard(
                        account = uiState.selectedAccount,
                        placeholder = stringResource(R.string.add_txn_select_account),
                        shape = topShape,
                        onClick = { accountPickerTarget = AccountPickerTarget.FROM },
                        onClear = { viewModel.updateSelectedAccount(null) }
                    )

                    AddCategorySelector(
                        category = uiState.category,
                        error = uiState.categoryError?.asString(),
                        onClick = { showCategoryPicker = true },
                        minHeight = Dimensions.Component.listItemMinHeightTwoLine
                    )
                }
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
                        leadingIcon = { Icon(Iconax.Shop, contentDescription = null) },
                        isError = uiState.merchantError != null,
                        supportingText = uiState.merchantError?.let { { Text(it.asString()) } },
                        colors = addFieldColors()
                    )
                }

                TextField(
                    value = uiState.notes,
                    onValueChange = viewModel::updateTransactionNotes,
                    label = { Text(stringResource(R.string.add_field_notes), fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = if (isTransfer) fullShape else bottomShape,
                    leadingIcon = { Icon(Iconax.DocumentText2, contentDescription = null) },
                    colors = addFieldColors()
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
                AddErrorBanner(message = errorMessage.asString())
            }

            // ── Receipt ──
            ReceiptPickerSection(
                receiptUri = uiState.receiptUri,
                onReceiptSelected = { uri -> viewModel.updateReceiptUri(uri) },
                onReceiptRemoved = { viewModel.updateReceiptUri(null) },
                onCreateCameraUri = { viewModel.createCameraUri() },
                tonal = true
            )

            // Bottom padding for save button overlay
            Spacer(modifier = Modifier.height(Dimensions.Component.bottomBarHeight))
        }

        // ── Sticky Save Button ──
        AddSaveBar(
            enabled = uiState.isValid && !uiState.isLoading,
            isLoading = uiState.isLoading,
            onClick = { viewModel.saveTransaction(onSuccess = onSave) }
        )
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

    if (showAmountCalculator) {
        AmountCalculatorSheet(
            initialAmount = uiState.amount,
            onDismiss = { showAmountCalculator = false },
            onApply = { amount ->
                viewModel.updateTransactionAmount(amount)
                showAmountCalculator = false
            },
            currencyCode = uiState.currency
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

@Composable
internal fun AddCategorySelector(
    category: String,
    error: String?,
    onClick: () -> Unit,
    position: ListItemPosition = ListItemPosition.Bottom,
    modifier: Modifier = Modifier,
    minHeight: Dp = Dimensions.Component.listItemMinHeight
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
                    .heightIn(min = minHeight)
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

/**
 * Receipt attachment: a thumbnail with a remove button, or Gallery / Camera
 * buttons. Also used by the transaction detail edit form. Both screens pass
 * [tonal] = true so the buttons read as part of their tonal field cards; the
 * default keeps the outlined buttons.
 */
@Composable
fun ReceiptPickerSection(
    receiptUri: android.net.Uri?,
    onReceiptSelected: (android.net.Uri) -> Unit,
    onReceiptRemoved: () -> Unit,
    onCreateCameraUri: () -> android.net.Uri,
    tonal: Boolean = false
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
                    .clip(if (tonal) MaterialTheme.shapes.large else MaterialTheme.shapes.medium)
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
                ReceiptSourceButton(
                    tonal = tonal,
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
                ReceiptSourceButton(
                    tonal = tonal,
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

@Composable
private fun ReceiptSourceButton(
    tonal: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    if (tonal) {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = Dimensions.Component.minTouchTarget),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            content = content
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            content = content
        )
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
                onClick = { onImpactTypeChange(null) },
                label = stringResource(R.string.txn_detail_budget_impact_none)
            )
            AddChoiceChip(
                selected = budgetImpactType == BudgetImpactType.DEDUCT_SPENT,
                onClick = { onImpactTypeChange(BudgetImpactType.DEDUCT_SPENT) },
                label = stringResource(R.string.txn_detail_budget_impact_refund)
            )
            AddChoiceChip(
                selected = budgetImpactType == BudgetImpactType.ADD_TO_LIMIT,
                onClick = { onImpactTypeChange(BudgetImpactType.ADD_TO_LIMIT) },
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
                    shape = MaterialTheme.shapes.large,
                    colors = addFieldColors()
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
