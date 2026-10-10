package com.pennywiseai.tracker.presentation.budgetgroups

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.BudgetPeriodType
import com.pennywiseai.tracker.presentation.add.AddChoiceChip
import com.pennywiseai.tracker.presentation.add.AddErrorBanner
import com.pennywiseai.tracker.presentation.add.AddSectionLabel
import com.pennywiseai.tracker.presentation.add.addFieldColors
import com.pennywiseai.tracker.ui.components.ColorSwatchRow
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.components.toColorOr
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.iconax.Edit2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetGroupEditScreen(
    viewModel: BudgetGroupEditViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddCategoryDropdown by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveComplete) {
        if (uiState.saveComplete) {
            onNavigateBack()
        }
    }

    val isEditing = (uiState.groupId ?: -1L) > 0
    val overallAmount = uiState.overallAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val categoryTotal = uiState.categories.fold(BigDecimal.ZERO) { acc, c -> acc + c.amount }
    val canSave = uiState.name.isNotBlank() && overallAmount > BigDecimal.ZERO && !uiState.isSaving
    val budgetColor = uiState.color.toColorOr(MaterialTheme.colorScheme.primary)

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardVisible = imeBottom > 0

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(if (isEditing) R.string.budget_edit_title_edit else R.string.budget_edit_title_new),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.budgets_back)
                    )
                },
                actionContent = {
                    if (isEditing) {
                        BudgetDeleteActionButton(
                            onClick = { showDeleteDialog = true },
                            contentDescription = stringResource(R.string.budgets_delete)
                        )
                    }
                },
                hazeState = hazeState
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val lazyListState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .overScrollVertical(),
                contentPadding = PaddingValues(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                    // Clears the sticky save bar that floats over the form.
                    bottom = paddingValues.calculateBottomPadding() +
                        Dimensions.Component.bottomBarHeight + Dimensions.Padding.content
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.Layout.sectionGap),
                flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
            ) {
                // Amount: the big figure, on a card washed with the budget's colour.
                item(key = "amount") {
                    BudgetAmountHero(
                        amount = uiState.overallAmount,
                        currency = uiState.currency,
                        color = budgetColor,
                        onAmountChange = viewModel::updateOverallAmount
                    )
                }

                // Name — always editable (a real field, so it can't look static; #763).
                item(key = "name") {
                    TextField(
                        value = uiState.name,
                        onValueChange = viewModel::updateName,
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                text = stringResource(R.string.budget_edit_name_placeholder),
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = { Icon(Iconax.Edit2, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = ListItemPosition.Single.toShape(),
                        colors = addFieldColors()
                    )
                }

                // Color (#763) — shown as a dot next to the name wherever the budget appears.
                item(key = "color") {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)) {
                        AddSectionLabel(text = stringResource(R.string.budget_edit_color))
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            contentPadding = Dimensions.Padding.cardCompact
                        ) {
                            ColorSwatchRow(
                                selected = uiState.color,
                                onSelect = { viewModel.updateColor(it) }
                            )
                        }
                    }
                }

                // Budget Period — three cadences:
                //   Weekly  → pick the day-of-week the week starts on. Recurring.
                //   Monthly → pick the day-of-month the cycle starts on. Recurring.
                //   One-time → pick an exact start and end date. No rolling.
                //
                // The cadence chip row sits at the top; the group of rows below
                // rebuilds to match. The "current window" row at the bottom
                // shows the resolved window for the picked cadence (the row's
                // persisted [startDate, endDate] cache is refreshed on save so
                // the home card / widget stay in sync).
                item(key = "period") {
                    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }
                    val longDateFormatter = remember { DateTimeFormatter.ofPattern("d MMM") }

                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)) {
                        AddSectionLabel(text = stringResource(R.string.budget_edit_period))

                        // Cashiro's segmented cadence switch. Labels may wrap to
                        // two lines at large font sizes rather than truncate; the
                        // recurring/one-off detail is in the window caption below.
                        val periods = listOf(
                            BudgetPeriodType.WEEKLY to R.string.budget_cadence_weekly,
                            BudgetPeriodType.MONTHLY to R.string.budget_cadence_monthly,
                            BudgetPeriodType.CUSTOM to R.string.budget_cadence_one_time
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            periods.forEachIndexed { index, (period, label) ->
                                SegmentedButton(
                                    selected = uiState.periodType == period,
                                    onClick = { viewModel.updatePeriodType(period) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = periods.size
                                    ),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                                        activeBorderColor = Color.Transparent,
                                        inactiveBorderColor = Color.Transparent
                                    ),
                                    icon = {},
                                    label = {
                                        Text(
                                            text = stringResource(label),
                                            maxLines = 2,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                )
                            }
                        }

                        val anchorCaption = when (uiState.periodType) {
                            BudgetPeriodType.WEEKLY ->
                                stringResource(R.string.budget_edit_resets_every, dayOfWeekName(uiState.weekStartDay))
                            BudgetPeriodType.MONTHLY ->
                                stringResource(R.string.budget_edit_resets_on_day, uiState.monthStartDay)
                            BudgetPeriodType.CUSTOM ->
                                stringResource(R.string.budget_edit_runs_once)
                        }

                        // Read-only "current window" line: the window the budget will
                        // track *now* — same logic the home card and widget use.
                        val windowCaption = stringResource(
                            R.string.budget_edit_window_caption,
                            uiState.startDate.format(longDateFormatter),
                            uiState.endDate.format(longDateFormatter),
                            anchorCaption
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)) {
                            when (uiState.periodType) {
                                BudgetPeriodType.WEEKLY -> {
                                    BudgetWeekdayField(
                                        weekStartDay = uiState.weekStartDay,
                                        onWeekdaySelected = viewModel::updateWeekStartDay,
                                        shape = ListItemPosition.Top.toShape()
                                    )
                                }
                                BudgetPeriodType.MONTHLY -> {
                                    BudgetMonthDayRow(
                                        monthStartDay = uiState.monthStartDay,
                                        onMonthDaySelected = viewModel::updateMonthStartDay,
                                        shape = ListItemPosition.Top.toShape()
                                    )
                                }
                                BudgetPeriodType.CUSTOM -> {
                                    BudgetDateRow(
                                        label = stringResource(R.string.budget_edit_start_date),
                                        date = uiState.startDate,
                                        formatter = dateFormatter,
                                        shape = ListItemPosition.Top.toShape(),
                                        onClick = { showStartDatePicker = true }
                                    )
                                    BudgetDateRow(
                                        label = stringResource(R.string.budget_edit_end_date),
                                        date = uiState.endDate,
                                        formatter = dateFormatter,
                                        shape = ListItemPosition.Middle.toShape(),
                                        onClick = { showEndDatePicker = true }
                                    )
                                }
                            }
                            BudgetWindowRow(
                                caption = windowCaption,
                                shape = ListItemPosition.Bottom.toShape()
                            )
                        }
                    }
                }

                // Categories Section
                item(key = "categories") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)
                    ) {
                        AddSectionLabel(text = stringResource(R.string.budget_edit_category_limits))

                        if (uiState.categories.isEmpty()) {
                            BudgetHintCard(text = stringResource(R.string.budget_edit_no_categories))
                        } else {
                            GroupedList {
                                uiState.categories.forEachIndexed { index, cat ->
                                    // Keyed by name so each row's typed text stays with its
                                    // category when another row is removed.
                                    key(cat.categoryName) {
                                        BudgetCategoryLimitRow(
                                            categoryName = cat.categoryName,
                                            amount = cat.amount,
                                            currentSpending = cat.currentSpending,
                                            currency = uiState.currency,
                                            position = ListItemPosition.from(index, uiState.categories.size),
                                            onAmountChange = { viewModel.updateCategoryAmount(cat.categoryName, it) },
                                            onRemove = { viewModel.removeCategory(cat.categoryName) }
                                        )
                                    }
                                }
                            }
                        }

                        // Unallocated / Over-allocated info
                        if (uiState.categories.isNotEmpty() && overallAmount > BigDecimal.ZERO) {
                            val diff = overallAmount - categoryTotal
                            when {
                                diff > BigDecimal.ZERO -> {
                                    Text(
                                        text = stringResource(
                                            R.string.budget_edit_unallocated,
                                            CurrencyFormatter.formatCurrency(diff, uiState.currency)
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = Spacing.xs)
                                    )
                                }
                                diff < BigDecimal.ZERO -> {
                                    AddErrorBanner(
                                        message = stringResource(
                                            R.string.budget_edit_over_allocated,
                                            CurrencyFormatter.formatCurrency(diff.abs(), uiState.currency)
                                        )
                                    )
                                }
                            }
                        }

                        // Add Category button
                        Box {
                            FilledTonalButton(
                                onClick = { showAddCategoryDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = uiState.availableCategories.isNotEmpty() ||
                                    uiState.availableTypeBuckets.isNotEmpty(),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(Dimensions.Icon.small)
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(stringResource(R.string.budget_edit_add_category))
                            }

                            DropdownMenu(
                                expanded = showAddCategoryDropdown,
                                onDismissRequest = { showAddCategoryDropdown = false }
                            ) {
                                uiState.availableCategories.forEach { categoryName ->
                                    BudgetCategoryMenuItem(
                                        categoryName = categoryName,
                                        label = categoryName,
                                        onClick = {
                                            viewModel.addCategory(categoryName)
                                            showAddCategoryDropdown = false
                                        }
                                    )
                                }
                                // Transaction-type buckets (e.g. Investments) —
                                // track a whole transaction type, not a category.
                                uiState.availableTypeBuckets.forEach { option ->
                                    BudgetCategoryMenuItem(
                                        categoryName = option.displayName,
                                        label = stringResource(R.string.budget_edit_type_bucket_all, option.displayName),
                                        onClick = {
                                            viewModel.addTypeBucket(option)
                                            showAddCategoryDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            BudgetSaveBar(
                label = stringResource(if (isEditing) R.string.budget_edit_save else R.string.budget_edit_create),
                enabled = canSave,
                isLoading = uiState.isSaving,
                visible = !isKeyboardVisible,
                onClick = { viewModel.save() }
            )
        }
    }

    // Start-date picker — One-time (CUSTOM) mode only.
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.startDate.toEpochDay() * 86_400_000
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.updateStartDate(LocalDate.ofEpochDay(millis / 86_400_000))
                    }
                    showStartDatePicker = false
                }) { Text(stringResource(R.string.budgets_action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(R.string.budgets_action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // End-date picker — One-time (CUSTOM) mode only.
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.endDate.toEpochDay() * 86_400_000
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.updateEndDate(LocalDate.ofEpochDay(millis / 86_400_000))
                    }
                    showEndDatePicker = false
                }) { Text(stringResource(R.string.budgets_action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(stringResource(R.string.budgets_action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.budgets_delete_title)) },
            text = { Text(stringResource(R.string.budgets_delete_message, uiState.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteGroup()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.budgets_action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.budgets_action_cancel))
                }
            }
        )
    }
}
