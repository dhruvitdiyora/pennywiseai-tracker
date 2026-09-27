package com.pennywiseai.tracker.presentation.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.presentation.common.AccountOption
import com.pennywiseai.tracker.presentation.common.AmountRangeError
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.presentation.common.label
import com.pennywiseai.tracker.ui.components.CategoryIcon
import com.pennywiseai.tracker.ui.components.CustomDateRangePickerDialog
import com.pennywiseai.tracker.ui.components.profileIcon
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionFiltersSheet(
    initialDraft: TransactionFilterDraft,
    availableCategories: List<String>,
    profiles: List<ProfileEntity>,
    accountOptions: List<AccountOption>,
    availableTags: List<String>,
    availableOriginalCurrencies: List<String>,
    unifiedMode: Boolean,
    displayCurrency: String,
    onApply: (TransactionFilterDraft) -> TransactionFilterDraftValidation,
    onDismiss: () -> Unit,
) {
    var draft by remember(initialDraft) { mutableStateOf(initialDraft) }
    var validation by remember { mutableStateOf<TransactionFilterDraftValidation?>(null) }
    var showCustomDatePicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val amountError = validation?.amount?.error

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimensions.Padding.dialog)
                    .padding(bottom = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = stringResource(R.string.transactions_filters_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = stringResource(R.string.transactions_filters_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                FilterSection(title = stringResource(R.string.transactions_filters_period)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        TimePeriod.entries.forEach { period ->
                            val selected = draft.period == period
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (period == TimePeriod.CUSTOM) {
                                        showCustomDatePicker = true
                                    } else {
                                        draft = draft.copy(period = period, customDateRange = null)
                                        validation = null
                                    }
                                },
                                label = { Text(period.label) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else if (period == TimePeriod.CUSTOM) {
                                    { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
                                } else null,
                            )
                        }
                    }
                    if (draft.period == TimePeriod.CUSTOM && draft.customDateRange != null) {
                        Text(
                            text = com.pennywiseai.tracker.utils.DateRangeUtils
                                .formatDateRange(draft.customDateRange)
                                .orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (validation?.customDateRequired == true) {
                        FilterError(stringResource(R.string.transactions_filters_custom_date_required))
                    }
                }

                FilterSection(title = stringResource(R.string.transactions_filters_type)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        TransactionTypeFilter.entries.forEach { type ->
                            val selected = draft.transactionType == type
                            FilterChip(
                                selected = selected,
                                onClick = { draft = draft.copy(transactionType = type) },
                                label = { Text(type.label) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null,
                            )
                        }
                    }
                }

                FilterSection(title = stringResource(R.string.transactions_filters_category)) {
                    val selectedCategories = draft.selectedCategories(availableCategories)
                    draft.navigationCategories
                        ?.takeIf { draft.categoriesFromBudget && it.isNotEmpty() }
                        ?.let { categories ->
                        InputChip(
                            selected = true,
                            onClick = {
                                draft = draft.copy(navigationCategories = null, categoriesFromBudget = false)
                            },
                            label = {
                                Text(
                                    stringResource(
                                        R.string.transactions_filters_budget_categories,
                                        categories.size,
                                    )
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(
                                        R.string.transactions_filters_clear_budget_categories
                                    ),
                                )
                            },
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        contentPadding = PaddingValues(vertical = Spacing.xs),
                    ) {
                        item {
                            ChoiceChip(
                                label = stringResource(R.string.transactions_filters_all_categories),
                                selected = draft.category == null && draft.navigationCategories == null,
                                onClick = {
                                    draft = draft.copy(
                                        category = null,
                                        navigationCategories = null,
                                        categoriesFromBudget = false,
                                    )
                                },
                                icon = { Icon(Icons.Default.Category, contentDescription = null) },
                            )
                        }
                        // Every category starts ticked; untick to exclude it (#786).
                        items(availableCategories, key = { it }) { category ->
                            ChoiceChip(
                                label = category,
                                selected = category in selectedCategories,
                                onClick = {
                                    draft = draft.toggleCategory(category, availableCategories)
                                },
                                icon = {
                                    CategoryIcon(
                                        category = category,
                                        size = Dimensions.Icon.small,
                                    )
                                },
                            )
                        }
                    }
                }

                FilterSection(
                    title = stringResource(R.string.transactions_filters_profile),
                    description = stringResource(R.string.transactions_filters_profile_description),
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        contentPadding = PaddingValues(vertical = Spacing.xs),
                    ) {
                        item {
                            ChoiceChip(
                                label = stringResource(R.string.transactions_filters_all_profiles),
                                selected = draft.profileId == null,
                                onClick = { draft = draft.copy(profileId = null) },
                                icon = {
                                    Icon(Icons.Outlined.AccountBalance, contentDescription = null)
                                },
                            )
                        }
                        items(profiles, key = { it.id }) { profile ->
                            ChoiceChip(
                                label = profile.name,
                                selected = draft.profileId == profile.id,
                                onClick = { draft = draft.copy(profileId = profile.id) },
                                icon = { Icon(profileIcon(profile), contentDescription = null) },
                            )
                        }
                    }
                }

                if (accountOptions.isNotEmpty() || draft.accountKey != null) {
                    FilterSection(title = stringResource(R.string.transactions_filters_account)) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            contentPadding = PaddingValues(vertical = Spacing.xs),
                        ) {
                            item {
                                ChoiceChip(
                                    label = stringResource(R.string.transactions_filters_all_accounts),
                                    selected = draft.accountKey == null,
                                    onClick = { draft = draft.copy(accountKey = null) },
                                    icon = {
                                        Icon(
                                            Icons.Outlined.AccountBalanceWallet,
                                            contentDescription = null,
                                        )
                                    },
                                )
                            }
                            items(accountOptions, key = { it.key }) { account ->
                                ChoiceChip(
                                    label = account.label,
                                    selected = draft.accountKey == account.key,
                                    onClick = { draft = draft.copy(accountKey = account.key) },
                                    icon = {
                                        Icon(
                                            Icons.Outlined.AccountBalanceWallet,
                                            contentDescription = null,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                if (availableTags.isNotEmpty() || draft.tag != null) {
                    FilterSection(title = stringResource(R.string.transactions_filters_tag)) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            contentPadding = PaddingValues(vertical = Spacing.xs),
                        ) {
                            item {
                                ChoiceChip(
                                    label = stringResource(R.string.transactions_filters_all_tags),
                                    selected = draft.tag == null,
                                    onClick = { draft = draft.copy(tag = null) },
                                    icon = { Icon(Icons.Default.Sell, contentDescription = null) },
                                )
                            }
                            items(availableTags, key = { it }) { tag ->
                                ChoiceChip(
                                    label = tag,
                                    selected = draft.tag == tag,
                                    onClick = { draft = draft.copy(tag = tag) },
                                    icon = { Icon(Icons.Default.Sell, contentDescription = null) },
                                )
                            }
                        }
                    }
                }

                FilterSection(
                    title = stringResource(R.string.transactions_filter_amount_section),
                    description = if (unifiedMode) {
                        stringResource(R.string.transactions_filter_unified_description, displayCurrency)
                    } else {
                        stringResource(R.string.transactions_filter_native_description, displayCurrency)
                    },
                ) {
                    OutlinedTextField(
                        value = draft.minimumText,
                        onValueChange = {
                            draft = draft.copy(minimumText = it)
                            validation = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.transactions_filter_minimum_amount)) },
                        placeholder = { Text(stringResource(R.string.transactions_filter_amount_placeholder)) },
                        isError = amountError != null,
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                        ),
                    )
                    OutlinedTextField(
                        value = draft.maximumText,
                        onValueChange = {
                            draft = draft.copy(maximumText = it)
                            validation = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.transactions_filter_maximum_amount)) },
                        placeholder = { Text(stringResource(R.string.transactions_filter_amount_placeholder)) },
                        isError = amountError != null,
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                        ),
                    )
                    amountError?.let { FilterError(amountErrorMessage(it)) }
                }

                FilterSection(title = stringResource(R.string.transactions_filter_original_currency)) {
                    if (unifiedMode) {
                        Text(
                            text = stringResource(R.string.transactions_filter_original_currency_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val currencies = remember(
                            availableOriginalCurrencies,
                            draft.originalCurrencies,
                        ) {
                            (availableOriginalCurrencies + draft.originalCurrencies)
                                .distinct()
                                .sorted()
                        }
                        if (currencies.isEmpty()) {
                            Text(
                                text = stringResource(R.string.transactions_filter_no_currencies),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                contentPadding = PaddingValues(vertical = Spacing.xs),
                            ) {
                                items(currencies, key = { it }) { currency ->
                                    val selected = currency in draft.originalCurrencies
                                    ChoiceChip(
                                        label = currency,
                                        selected = selected,
                                        onClick = {
                                            draft = draft.copy(
                                                originalCurrencies = if (selected) {
                                                    draft.originalCurrencies - currency
                                                } else {
                                                    draft.originalCurrencies + currency
                                                }
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.transactions_filter_native_currency_note),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.Padding.dialog)
                    .padding(top = Spacing.smd, bottom = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        draft = draft.reset()
                        validation = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.defaultMinSize(
                        minHeight = Dimensions.Component.minTouchTarget
                    ),
                ) {
                    Text(stringResource(R.string.transactions_filter_reset))
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(
                        minHeight = Dimensions.Component.minTouchTarget
                    ),
                ) {
                    Text(stringResource(R.string.transactions_filter_cancel))
                }
                Button(
                    onClick = { validation = onApply(draft) },
                    modifier = Modifier.defaultMinSize(
                        minHeight = Dimensions.Component.minTouchTarget
                    ),
                ) {
                    Text(stringResource(R.string.transactions_filter_apply))
                }
            }
        }
    }

    if (showCustomDatePicker) {
        CustomDateRangePickerDialog(
            onDismiss = { showCustomDatePicker = false },
            onConfirm = { start, end ->
                draft = draft.copy(
                    period = TimePeriod.CUSTOM,
                    customDateRange = start to end,
                )
                validation = null
                showCustomDatePicker = false
            },
            initialStartDate = draft.customDateRange?.first,
            initialEndDate = draft.customDateRange?.second,
        )
    }
}

@Composable
private fun FilterSection(
    title: String,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null) }
        } else {
            icon
        },
        modifier = Modifier.defaultMinSize(minHeight = Dimensions.Component.minTouchTarget),
    )
}

@Composable
private fun FilterError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun amountErrorMessage(error: AmountRangeError): String = when (error) {
    AmountRangeError.INVALID_NUMBER ->
        stringResource(R.string.transactions_filter_invalid_amount)
    AmountRangeError.NEGATIVE_AMOUNT ->
        stringResource(R.string.transactions_filter_negative_amount)
    AmountRangeError.MINIMUM_GREATER_THAN_MAXIMUM ->
        stringResource(R.string.transactions_filter_minimum_greater_than_maximum)
}
