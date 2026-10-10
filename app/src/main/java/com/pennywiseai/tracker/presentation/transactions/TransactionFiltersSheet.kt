package com.pennywiseai.tracker.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.presentation.add.addFieldColors
import com.pennywiseai.tracker.presentation.common.AccountOption
import com.pennywiseai.tracker.presentation.common.AmountRangeError
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.presentation.common.label
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.CategoryIcon
import com.pennywiseai.tracker.ui.components.CustomDateRangePickerDialog
import com.pennywiseai.tracker.ui.components.TiledIconBackground
import com.pennywiseai.tracker.ui.components.profileIcon
import com.pennywiseai.tracker.ui.icons.BrandIcons
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToLong

/** Cashiro's category filter tile: a rounded icon square with the name below. */
private val CategoryTileSize = 64.dp
private val CategoryTileRadius = 18.dp

/** Bank-brand account card width in the account filter row. */
private val AccountCardWidth = 220.dp

/** Currency tile (code / symbol / name) width. */
private val CurrencyTileWidth = 92.dp

/** The selected check badge on a category tile / account card. */
private val CheckBadgeSize = 18.dp

/** Wash of a category's own colour behind its tile glyph. */
private const val CATEGORY_TILE_ALPHA = 0.2f

/** Unselected chip fill over the glass sheet. */
private const val CHIP_FILL_ALPHA = 0.6f

/**
 * The unified Transactions filter sheet, in Cashiro's layout and the glass
 * material: type chips, category icon tiles, an amount range slider (with exact
 * min/max fields), bank-brand account cards and currency tiles — plus the
 * PennyWise-only Period (with Custom Range), Profile and Tag filters.
 *
 * The sheet edits a [TransactionFilterDraft] copy; only Apply commits it.
 *
 * @param amountSliderMax upper bound for the amount slider, in [displayCurrency]
 *  (the largest single amount the list can show). `null` hides the slider and
 *  keeps the exact fields only.
 */
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
    amountSliderMax: BigDecimal? = null,
) {
    var draft by remember(initialDraft) { mutableStateOf(initialDraft) }
    var validation by remember { mutableStateOf<TransactionFilterDraftValidation?>(null) }
    var showCustomDatePicker by remember { mutableStateOf(false) }
    val amountError = validation?.amount?.error

    TxnGlassSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.Padding.content)
                    .padding(bottom = Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.transactions_filters_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.transactions_filters_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                FilterSection(title = stringResource(R.string.transactions_filters_period)) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        TimePeriod.entries.forEach { period ->
                            ChoiceChip(
                                label = period.label,
                                selected = draft.period == period,
                                onClick = {
                                    if (period == TimePeriod.CUSTOM) {
                                        showCustomDatePicker = true
                                    } else {
                                        draft = draft.copy(period = period, customDateRange = null)
                                        validation = null
                                    }
                                },
                                icon = if (period == TimePeriod.CUSTOM) {
                                    { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
                                } else null,
                            )
                        }
                    }
                    if (draft.period == TimePeriod.CUSTOM && draft.customDateRange != null) {
                        FilterDescription(
                            com.pennywiseai.tracker.utils.DateRangeUtils
                                .formatDateRange(draft.customDateRange)
                                .orEmpty()
                        )
                    }
                    if (validation?.customDateRequired == true) {
                        FilterError(stringResource(R.string.transactions_filters_custom_date_required))
                    }
                }

                FilterSection(title = stringResource(R.string.transactions_filters_type)) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
                    ) {
                        items(TransactionTypeFilter.entries, key = { it.name }) { type ->
                            ChoiceChip(
                                label = type.label,
                                selected = draft.transactionType == type,
                                onClick = { draft = draft.copy(transactionType = type) },
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
                                shape = CircleShape,
                                modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                            )
                        }
                    val allSelected = draft.category == null && draft.navigationCategories == null
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
                        contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
                    ) {
                        item(key = "__all__") {
                            CategoryTile(
                                name = stringResource(R.string.transactions_filters_all_categories),
                                tint = MaterialTheme.colorScheme.surfaceContainerHighest,
                                selected = allSelected,
                                onClick = {
                                    draft = draft.copy(
                                        category = null,
                                        navigationCategories = null,
                                        categoriesFromBudget = false,
                                    )
                                },
                            ) {
                                Icon(
                                    Icons.Default.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(Dimensions.Icon.large),
                                )
                            }
                        }
                        // Every category starts ticked; untick to exclude it (#786).
                        // With nothing filtered the "All" tile carries the check, so
                        // the row doesn't read as a wall of badges.
                        items(availableCategories, key = { it }) { category ->
                            CategoryTile(
                                name = category,
                                tint = CategoryMapping.colorFor(category).copy(alpha = CATEGORY_TILE_ALPHA),
                                selected = category in selectedCategories,
                                showMark = !allSelected,
                                onClick = {
                                    draft = draft.toggleCategory(category, availableCategories)
                                },
                            ) {
                                CategoryIcon(category = category, size = Dimensions.Icon.large)
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
                    AmountRangeCard(
                        minimumText = draft.minimumText,
                        maximumText = draft.maximumText,
                        currency = displayCurrency,
                        sliderMax = amountSliderMax,
                        isError = amountError != null,
                        onChange = { min, max ->
                            draft = draft.copy(minimumText = min, maximumText = max)
                            validation = null
                        },
                    )
                    amountError?.let { FilterError(amountErrorMessage(it)) }
                }

                if (accountOptions.isNotEmpty() || draft.accountKey != null) {
                    FilterSection(title = stringResource(R.string.transactions_filters_account)) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
                        ) {
                            item(key = "__all__") {
                                AccountFilterCard(
                                    title = stringResource(R.string.transactions_filters_all_accounts),
                                    bankName = null,
                                    selected = draft.accountKey == null,
                                    onClick = { draft = draft.copy(accountKey = null) },
                                )
                            }
                            items(accountOptions, key = { it.key }) { account ->
                                AccountFilterCard(
                                    title = account.label,
                                    // The option key is "bank_last4"; the bank part
                                    // picks the logo, colour and watermark.
                                    bankName = account.key.substringBeforeLast('_'),
                                    selected = draft.accountKey == account.key,
                                    onClick = { draft = draft.copy(accountKey = account.key) },
                                )
                            }
                        }
                    }
                }

                FilterSection(title = stringResource(R.string.transactions_filter_original_currency)) {
                    if (unifiedMode) {
                        FilterDescription(
                            stringResource(R.string.transactions_filter_original_currency_description)
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
                            FilterDescription(stringResource(R.string.transactions_filter_no_currencies))
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
                            ) {
                                items(currencies, key = { it }) { currency ->
                                    val selected = currency in draft.originalCurrencies
                                    CurrencyTile(
                                        code = currency,
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
                        FilterDescription(stringResource(R.string.transactions_filter_native_currency_note))
                    }
                }

                FilterSection(
                    title = stringResource(R.string.transactions_filters_profile),
                    description = stringResource(R.string.transactions_filters_profile_description),
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
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

                if (availableTags.isNotEmpty() || draft.tag != null) {
                    FilterSection(title = stringResource(R.string.transactions_filters_tag)) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
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
            }

            // Cashiro's action bar: a wide pill Apply, with Reset and Cancel
            // kept as quieter actions beside it.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.Padding.content)
                    .padding(top = Spacing.smd, bottom = Spacing.md),
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
                    modifier = Modifier.defaultMinSize(minHeight = Dimensions.Component.minTouchTarget),
                ) {
                    Text(stringResource(R.string.transactions_filter_reset))
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    modifier = Modifier.heightIn(min = Dimensions.Component.listItemMinHeight),
                ) {
                    Text(stringResource(R.string.transactions_filter_cancel))
                }
                Button(
                    onClick = { validation = onApply(draft) },
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimensions.Component.listItemMinHeight),
                ) {
                    Text(
                        text = stringResource(R.string.transactions_filter_apply),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
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
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
        )
        description?.let { FilterDescription(it) }
        content()
    }
}

@Composable
private fun FilterDescription(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
    )
}

/** A borderless pill chip; the selected one takes Cashiro's tertiary container. */
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
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.small),
                )
            }
        } else {
            icon
        },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = CHIP_FILL_ALPHA),
            labelColor = MaterialTheme.colorScheme.onSurface,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        border = null,
        modifier = Modifier.defaultMinSize(minHeight = Dimensions.Component.minTouchTarget),
    )
}

/**
 * Cashiro's category tile: the category glyph on a wash of its own colour (a
 * primary wash when marked), a check badge in the corner, the name below.
 *
 * @param showMark draw the selected look; off while every category is included,
 *  so the "All" tile alone carries the mark. Selection semantics are unchanged.
 */
@Composable
private fun CategoryTile(
    name: String,
    tint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    showMark: Boolean = true,
    glyph: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(CategoryTileRadius)
    val marked = selected && showMark
    Column(
        modifier = Modifier
            .width(CategoryTileSize + Spacing.sm)
            .clip(shape)
            .selectable(selected = selected, role = Role.Checkbox, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(CategoryTileSize)
                .clip(shape)
                .background(
                    if (marked) MaterialTheme.colorScheme.primary.copy(alpha = CATEGORY_TILE_ALPHA) else tint
                ),
            contentAlignment = Alignment.Center,
        ) {
            glyph()
            if (marked) {
                CheckBadge(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Spacing.xs + Spacing.xxs)
                )
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (marked) FontWeight.Bold else FontWeight.Normal,
            color = if (marked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
        )
    }
}

@Composable
private fun CheckBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(CheckBadgeSize)
            .background(MaterialTheme.colorScheme.tertiary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier
                .padding(Spacing.xxs)
                .fillMaxSize(),
        )
    }
}

/**
 * Amount range: Cashiro's range slider over [0, sliderMax] on a glass panel,
 * with exact min / max fields beneath it. Both edit the same draft text, so
 * typing moves the thumbs and dragging fills the fields; a thumb resting at
 * either end of the track leaves that bound open.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AmountRangeCard(
    minimumText: String,
    maximumText: String,
    currency: String,
    sliderMax: BigDecimal?,
    isError: Boolean,
    onChange: (min: String, max: String) -> Unit,
) {
    val panelShape = MaterialTheme.shapes.extraLarge
    Column(
        modifier = Modifier
            .padding(horizontal = Dimensions.Padding.content)
            .fillMaxWidth()
            .txnGlass(panelShape)
            .padding(Dimensions.Padding.cardCompact),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        val typedMax = maximumText.toBigDecimalOrNull()
        val bound = remember(sliderMax, typedMax) {
            listOfNotNull(sliderMax, typedMax)
                .maxOrNull()
                ?.takeIf { it.signum() > 0 }
                ?.setScale(0, RoundingMode.CEILING)
                ?.toFloat()
        }
        if (bound != null) {
            val start = (minimumText.toFloatOrNull() ?: 0f).coerceIn(0f, bound)
            val end = (maximumText.toFloatOrNull() ?: bound).coerceIn(start, bound)
            RangeSlider(
                value = start..end,
                onValueChange = { range ->
                    val min = range.start.roundToLong()
                    val max = range.endInclusive.roundToLong()
                    onChange(
                        if (min <= 0L) "" else min.toString(),
                        if (max >= bound.roundToLong()) "" else max.toString(),
                    )
                },
                valueRange = 0f..bound,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val symbol = CurrencyFormatter.getCurrencySymbol(currency)
            AmountField(
                value = minimumText,
                label = stringResource(R.string.transactions_filter_minimum_amount),
                prefix = symbol,
                isError = isError,
                onValueChange = { onChange(it, maximumText) },
                modifier = Modifier.weight(1f),
            )
            AmountField(
                value = maximumText,
                label = stringResource(R.string.transactions_filter_maximum_amount),
                prefix = symbol,
                isError = isError,
                onValueChange = { onChange(minimumText, it) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AmountField(
    value: String,
    label: String,
    prefix: String,
    isError: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        placeholder = { Text(stringResource(R.string.transactions_filter_amount_placeholder)) },
        prefix = { Text(prefix) },
        isError = isError,
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        colors = addFieldColors(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
        ),
    )
}

/**
 * Cashiro's account filter card: the bank's logo and faint tiled watermark on
 * glass, the account's label, and a check badge when selected (the rim takes
 * the primary colour then; otherwise the bank's own). [bankName] `null` is the
 * "All accounts" card.
 */
@Composable
private fun AccountFilterCard(
    title: String,
    bankName: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.extraLarge
    val brand = bankBrandColor(bankName)
    Box(
        modifier = Modifier
            .width(AccountCardWidth)
            .txnGlass(
                shape = shape,
                rimColor = if (selected) MaterialTheme.colorScheme.primary else brand,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        if (bankName != null) {
            TiledIconBackground(merchantName = bankName, modifier = Modifier.matchParentSize())
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimensions.Component.listItemMinHeightTwoLine)
                .padding(Dimensions.Padding.card),
            horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (bankName != null && BrandIcons.getIconResource(bankName) != null) {
                BrandIcon(merchantName = bankName, size = Dimensions.Icon.avatarLarge, showBackground = true)
            } else {
                Box(
                    modifier = Modifier
                        .size(Dimensions.Icon.avatarLarge)
                        .background(
                            (brand ?: MaterialTheme.colorScheme.primary)
                                .copy(alpha = Dimensions.Alpha.tonalIconContainer),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        if (selected) {
            CheckBadge(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.smd)
            )
        }
    }
}

/** Cashiro's currency tile: code, symbol and name, primary-tinted when selected. */
@Composable
private fun CurrencyTile(
    code: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val name = remember(code) {
        runCatching { java.util.Currency.getInstance(code).displayName }.getOrDefault(code)
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = Modifier
            .width(CurrencyTileWidth)
            .then(
                if (selected) {
                    Modifier
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                } else {
                    Modifier.txnGlass(shape)
                }
            )
            .selectable(selected = selected, role = Role.Checkbox, onClick = onClick)
            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Text(
            text = code.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = content,
        )
        Text(
            text = CurrencyFormatter.getCurrencySymbol(code),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 1,
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) content else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
        )
    }
}

@Composable
private fun FilterError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
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
