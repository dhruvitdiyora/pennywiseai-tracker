package com.pennywiseai.tracker.presentation.recurring

import com.pennywiseai.tracker.presentation.accounts.glassSheetContainerColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.RecurringFrequency
import com.pennywiseai.tracker.data.database.entity.RecurringTransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.presentation.add.AddChoiceChip
import com.pennywiseai.tracker.presentation.people.PeopleExtendedFab
import com.pennywiseai.tracker.presentation.people.TonalTextField
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.SubtitleTag
import com.pennywiseai.tracker.ui.components.TINTED_CONTAINER_ALPHA
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.components.legibleOn
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.icons.iconax.Calendar
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.expense
import com.pennywiseai.tracker.ui.theme.income
import com.pennywiseai.tracker.ui.theme.warning
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: RecurringTransactionsViewModel = hiltViewModel()
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()
    val canAddMore by viewModel.canAddMore.collectAsStateWithLifecycle()

    // Null = editor closed. Non-null = editing this form (id 0 for a fresh add).
    var editing by remember { mutableStateOf<RecurringFormState?>(null) }
    // Free tier allows a limited number of templates; adding beyond it opens the paywall (#706).
    var showUpgradeSheet by rememberSaveable { mutableStateOf(false) }

    val active = templates.filter { it.isActive }
    val paused = templates.filterNot { it.isActive }

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()
    // The add button carries its label only while the list is at the top.
    val fabExpanded by remember { derivedStateOf { lazyListState.firstVisibleItemIndex == 0 } }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.recurring_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.recurring_back)
                    )
                },
                hazeState = hazeState
            )
        },
        floatingActionButton = {
            PeopleExtendedFab(
                label = stringResource(R.string.recurring_fab_label),
                onClick = {
                    if (canAddMore) editing = RecurringFormState(currency = baseCurrency)
                    else showUpgradeSheet = true
                },
                expanded = fabExpanded
            )
        }
    ) { padding ->
        if (templates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                PennyWiseEmptyState(
                    icon = Icons.Default.EventRepeat,
                    headline = stringResource(R.string.recurring_empty_title),
                    description = stringResource(R.string.recurring_empty_hint)
                )
            }
            return@Scaffold
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
                .overScrollVertical(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Dimensions.Padding.content + padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + Dimensions.Component.fabScrollClearance
            ),
            // Rows of a section sit 2dp apart as one connected block; the summary
            // and headers carry their own spacing.
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
        ) {
            item(key = "summary") {
                RecurringSummaryCard(
                    active = active,
                    pausedCount = paused.size,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
            }
            if (active.isNotEmpty()) {
                item(key = "active-header") {
                    SectionHeaderV2(
                        title = stringResource(R.string.recurring_section_active),
                        modifier = Modifier.padding(bottom = Spacing.Layout.headerToContent)
                    )
                }
                itemsIndexed(active, key = { _, template -> template.id }) { index, template ->
                    RecurringItem(
                        template = template,
                        onEdit = { editing = RecurringFormState.from(template) },
                        onToggleActive = { viewModel.setActive(template, it) },
                        onDelete = { viewModel.delete(template) },
                        position = ListItemPosition.from(index, active.size)
                    )
                }
            }
            if (paused.isNotEmpty()) {
                item(key = "paused-header") {
                    SectionHeaderV2(
                        title = stringResource(R.string.recurring_section_paused),
                        modifier = Modifier.padding(bottom = Spacing.Layout.headerToContent),
                        topSpacing = Spacing.md
                    )
                }
                itemsIndexed(paused, key = { _, template -> template.id }) { index, template ->
                    RecurringItem(
                        template = template,
                        onEdit = { editing = RecurringFormState.from(template) },
                        onToggleActive = { viewModel.setActive(template, it) },
                        onDelete = { viewModel.delete(template) },
                        position = ListItemPosition.from(index, paused.size)
                    )
                }
            }
        }
    }

    editing?.let { form ->
        RecurringEditorDialog(
            form = form,
            categoryNames = categories.map { it.name },
            onDismiss = { editing = null },
            onSave = { viewModel.save(it); editing = null }
        )
    }

    if (showUpgradeSheet) {
        com.pennywiseai.tracker.presentation.paywall.UpgradeSheet(
            onDismiss = { showUpgradeSheet = false },
        )
    }
}

@Composable
private fun RecurringFrequency.label(): String = stringResource(
    when (this) {
        RecurringFrequency.DAILY -> R.string.recurring_frequency_daily
        RecurringFrequency.WEEKLY -> R.string.recurring_frequency_weekly
        RecurringFrequency.MONTHLY -> R.string.recurring_frequency_monthly
    }
)

private fun recurringDateFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())

private fun recurringDayOfWeekNames(): List<String> = DayOfWeek.values().map {
    it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
}

// ── Summary ───────────────────────────────────────────────────────────────

/**
 * The header card: how many templates are active (and paused), a round glyph,
 * and two tiles counting the active ones that move money out and money in.
 * The tiles are counts, not amounts, so there is nothing to total across
 * currencies.
 */
@Composable
private fun RecurringSummaryCard(
    active: List<RecurringTransactionEntity>,
    pausedCount: Int,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val incomeCount = active.count { it.transactionType == TransactionType.INCOME }
    val expenseCount = active.size - incomeCount
    val subtitle = if (pausedCount == 0) {
        stringResource(R.string.recurring_summary_active, active.size)
    } else {
        stringResource(R.string.recurring_summary_active_paused, active.size, pausedCount)
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = Dimensions.Padding.card
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.recurring_summary_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = PennyWiseText.metadata,
                    color = scheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .padding(start = Spacing.sm)
                    .size(Dimensions.Icon.list)
                    .background(
                        color = scheme.tertiaryContainer.copy(alpha = Dimensions.Alpha.medium),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EventRepeat,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.medium),
                    tint = scheme.onTertiaryContainer
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.md)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Spacing.smd)
        ) {
            RecurringCountTile(
                label = stringResource(R.string.recurring_expense),
                value = stringResource(R.string.recurring_tile_scheduled, expenseCount),
                color = scheme.expense,
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            RecurringCountTile(
                label = stringResource(R.string.recurring_income),
                value = stringResource(R.string.recurring_tile_scheduled, incomeCount),
                color = scheme.income,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun RecurringCountTile(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = color.copy(alpha = TINTED_CONTAINER_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.smd),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimensions.Icon.inline)
                        .clip(CircleShape)
                        .background(color.copy(alpha = TINTED_CONTAINER_ALPHA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.tiny),
                        tint = color
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = PennyWiseText.amountMedium,
                color = color,
                maxLines = 2
            )
        }
    }
}

// ── Row ───────────────────────────────────────────────────────────────────

/**
 * One recurring template as a tonal row of a connected block ([position] gives
 * it the grouped-list corners): brand/category avatar, name, next due date,
 * tinted chips for the cadence, category and paused state, the amount with its
 * Expense/Income label, and the overflow menu.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecurringItem(
    template: RecurringTransactionEntity,
    onEdit: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit,
    position: ListItemPosition = ListItemPosition.Single
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    val displayName = template.merchantName.ifBlank {
        stringResource(R.string.recurring_untitled)
    }
    val amountColor = when (template.transactionType) {
        TransactionType.INCOME -> scheme.income
        else -> scheme.expense
    }
    val dateFormatter = remember { recurringDateFormatter() }
    val rowBackground = scheme.surfaceContainerLow
    val categoryName = template.category.takeIf { it.isNotBlank() }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = position.toShape(),
        onClick = onEdit,
        contentPadding = Dimensions.Padding.cardCompact
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Dimensions.Component.minTouchTarget),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.smd)
        ) {
            BrandIcon(
                merchantName = displayName,
                category = template.category,
                size = Dimensions.Icon.avatarLarge
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
            ) {
                Text(
                    text = displayName,
                    style = PennyWiseText.rowTitle,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Icon(
                        imageVector = Iconax.Calendar,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.small),
                        tint = scheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(
                            R.string.recurring_next_date,
                            template.nextDueDate.format(dateFormatter)
                        ),
                        style = PennyWiseText.metadata,
                        color = scheme.onSurfaceVariant
                    )
                }
                // Chips wrap onto a second line rather than being cut off.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    SubtitleTag(
                        text = template.frequency.label(),
                        color = scheme.tertiary
                    )
                    if (categoryName != null) {
                        val categoryColor = CategoryMapping.colorFor(categoryName)
                        SubtitleTag(
                            text = categoryName,
                            color = categoryColor,
                            textColor = categoryColor.legibleOn(
                                background = rowBackground,
                                towards = scheme.onSurface
                            )
                        )
                    }
                    if (!template.isActive) {
                        SubtitleTag(
                            text = stringResource(R.string.recurring_paused),
                            color = scheme.warning,
                            textColor = scheme.warning.legibleOn(
                                background = rowBackground,
                                towards = scheme.onSurface
                            )
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatCurrency(template.amount, template.currency),
                    style = PennyWiseText.amountRow,
                    color = amountColor
                )
                Text(
                    text = stringResource(
                        if (template.transactionType == TransactionType.INCOME) {
                            R.string.recurring_income
                        } else {
                            R.string.recurring_expense
                        }
                    ),
                    style = PennyWiseText.metadata,
                    color = amountColor
                )
            }

            Box(modifier = Modifier.size(Dimensions.Component.minTouchTarget)) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(Dimensions.Component.minTouchTarget)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.recurring_more_options)
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (template.isActive) {
                                        R.string.recurring_pause
                                    } else {
                                        R.string.recurring_resume
                                    }
                                )
                            )
                        },
                        onClick = {
                            showMenu = false
                            onToggleActive(!template.isActive)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.recurring_edit)) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.recurring_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.recurring_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.recurring_delete_message,
                        displayName
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text(
                        stringResource(R.string.recurring_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.recurring_cancel))
                }
            }
        )
    }
}

// ── Editor ────────────────────────────────────────────────────────────────

/**
 * The add / edit form, as a rounded bottom sheet of tonal fields (it was a plain
 * dialog; the name and signature are kept so callers and tests are unchanged).
 * Type is a pair of choice chips, the schedule fields read as one connected
 * block, and Save stays disabled until the form is valid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecurringEditorDialog(
    form: RecurringFormState,
    categoryNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (RecurringFormState) -> Unit
) {
    var state by remember { mutableStateOf(form) }
    var freqExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var dowExpanded by remember { mutableStateOf(false) }

    val dayOfWeekNames = remember { recurringDayOfWeekNames() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme

    // Category and frequency always show; the day field joins them for monthly and
    // weekly schedules. Positions follow, so the block's corners always close up.
    val hasDayField = state.frequency != RecurringFrequency.DAILY
    val frequencyPosition = if (hasDayField) ListItemPosition.Middle else ListItemPosition.Bottom

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // The fields are tonal (surfaceContainerLow), so the sheet sits one step
        // lighter to let them read as raised fields.
        containerColor = glassSheetContainerColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(
                    start = Dimensions.Padding.dialog,
                    end = Dimensions.Padding.dialog,
                    bottom = Spacing.lg
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = stringResource(
                    if (form.id == 0L) R.string.recurring_editor_new else R.string.recurring_editor_edit
                ),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            TonalTextField(
                value = state.merchantName,
                onValueChange = { state = state.copy(merchantName = it) },
                label = stringResource(R.string.recurring_field_name)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                TonalTextField(
                    value = state.amount,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) state = state.copy(amount = filtered)
                    },
                    label = stringResource(R.string.recurring_field_amount),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                TonalTextField(
                    value = state.currency,
                    onValueChange = { state = state.copy(currency = it.uppercase().take(3)) },
                    label = stringResource(R.string.recurring_field_currency),
                    modifier = Modifier.width(Dimensions.Component.currencySelectorWidth)
                )
            }

            // Type: Expense / Income
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                AddChoiceChip(
                    selected = state.transactionType == TransactionType.EXPENSE,
                    onClick = { state = state.copy(transactionType = TransactionType.EXPENSE) },
                    label = stringResource(R.string.recurring_expense)
                )
                AddChoiceChip(
                    selected = state.transactionType == TransactionType.INCOME,
                    onClick = { state = state.copy(transactionType = TransactionType.INCOME) },
                    label = stringResource(R.string.recurring_income)
                )
            }

            // Schedule block: category, frequency and (when it applies) the day.
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)) {
                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    TonalTextField(
                        value = state.category,
                        onValueChange = {},
                        label = stringResource(R.string.recurring_field_category),
                        position = ListItemPosition.Top,
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categoryNames.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    state = state.copy(category = name)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Frequency dropdown
                ExposedDropdownMenuBox(
                    expanded = freqExpanded,
                    onExpandedChange = { freqExpanded = it }
                ) {
                    TonalTextField(
                        value = state.frequency.label(),
                        onValueChange = {},
                        label = stringResource(R.string.recurring_field_frequency),
                        position = frequencyPosition,
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(freqExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = freqExpanded,
                        onDismissRequest = { freqExpanded = false }
                    ) {
                        RecurringFrequency.entries.forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq.label()) },
                                onClick = {
                                    state = state.copy(frequency = freq)
                                    freqExpanded = false
                                }
                            )
                        }
                    }
                }

                // Day selector — depends on cadence
                when (state.frequency) {
                    RecurringFrequency.MONTHLY -> {
                        TonalTextField(
                            value = state.dayOfMonth?.toString() ?: "",
                            onValueChange = { input ->
                                val n = input.filter { it.isDigit() }.toIntOrNull()
                                state = state.copy(dayOfMonth = n?.coerceIn(1, 31))
                            },
                            label = stringResource(R.string.recurring_field_day_of_month),
                            position = ListItemPosition.Bottom,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    RecurringFrequency.WEEKLY -> {
                        ExposedDropdownMenuBox(
                            expanded = dowExpanded,
                            onExpandedChange = { dowExpanded = it }
                        ) {
                            TonalTextField(
                                value = state.dayOfWeek?.let { dayOfWeekNames[it - 1] }
                                    ?: stringResource(R.string.recurring_day_of_week_any),
                                onValueChange = {},
                                label = stringResource(R.string.recurring_field_day_of_week),
                                position = ListItemPosition.Bottom,
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dowExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = dowExpanded,
                                onDismissRequest = { dowExpanded = false }
                            ) {
                                dayOfWeekNames.forEachIndexed { index, name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            state = state.copy(dayOfWeek = index + 1)
                                            dowExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    RecurringFrequency.DAILY -> { /* no day selector */ }
                }
            }

            TonalTextField(
                value = state.note,
                onValueChange = { state = state.copy(note = it) },
                label = stringResource(R.string.recurring_field_note)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ListItemPosition.Single.toShape(),
                color = scheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimensions.Component.listItemMinHeight)
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.smd)
                ) {
                    Text(
                        stringResource(R.string.recurring_field_active),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface
                    )
                    Switch(
                        checked = state.isActive,
                        onCheckedChange = { state = state.copy(isActive = it) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.recurring_cancel)) }
                Button(
                    onClick = { onSave(state) },
                    enabled = state.isValid,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimensions.Component.fab)
                ) {
                    Text(
                        text = stringResource(R.string.recurring_save),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
