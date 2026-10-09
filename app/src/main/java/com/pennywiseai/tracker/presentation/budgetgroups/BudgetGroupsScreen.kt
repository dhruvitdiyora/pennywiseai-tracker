package com.pennywiseai.tracker.presentation.budgetgroups

import com.pennywiseai.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywiseai.tracker.data.database.entity.BudgetPeriodType
import com.pennywiseai.tracker.data.repository.BudgetGroupSpending
import com.pennywiseai.tracker.data.repository.BudgetOverallSummary
import com.pennywiseai.tracker.ui.components.CategoryIcon
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.toColorOr
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.BudgetCardCaption
import com.pennywiseai.tracker.ui.components.cards.BudgetCardTitle
import com.pennywiseai.tracker.ui.components.cards.BudgetHeroFigures
import com.pennywiseai.tracker.ui.components.cards.BudgetProgressTrack
import com.pennywiseai.tracker.ui.components.cards.budgetBarColor
import com.pennywiseai.tracker.ui.components.cards.budgetColorWash
import com.pennywiseai.tracker.ui.components.cards.budgetRenewalText
import com.pennywiseai.tracker.ui.components.cards.budgetRim
import com.pennywiseai.tracker.ui.components.cards.budgetStatusColor
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.icons.iconax.History as IconaxHistory
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.graphics.SolidColor
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.*
import kotlinx.coroutines.delay
import com.pennywiseai.tracker.data.database.entity.BudgetGroupType
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetGroupsScreen(
    viewModel: BudgetGroupsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToGroupEdit: (Long) -> Unit = {},
    onNavigateToHistory: (Long, Int, Int) -> Unit = { _, _, _ -> },
    onNavigateToDetail: (Long, Int, Int) -> Unit = { _, _, _ -> },
    onNavigateToCategory: (category: String, yearMonth: String, currency: String) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.budgets_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.budgets_back)
                    )
                },
                hazeState = hazeState
            )
        },
        floatingActionButton = {
            // Always present, as in Cashiro: the empty state offers the
            // smart-defaults shortcut, the FAB is the one place to add a budget.
            // The label is the action ("New Budget"), so the icon is decorative.
            if (!uiState.isLoading) {
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToGroupEdit(-1L) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.budget_edit_title_new)) },
                    shape = MaterialTheme.shapes.extraLarge,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (!uiState.hasGroups) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = paddingValues.calculateTopPadding() + Dimensions.Padding.content)
            ) {
                MonthSelector(
                    year = uiState.selectedYear,
                    month = uiState.selectedMonth,
                    isCurrentMonth = uiState.selectedYear == java.time.LocalDate.now().year &&
                        uiState.selectedMonth == java.time.LocalDate.now().monthValue,
                    onPrevious = { viewModel.selectPreviousMonth() },
                    onNext = { viewModel.selectNextMonth() }
                )
                EmptyBudgetState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    onSmartDefaults = { viewModel.runSmartDefaults() },
                    onCreateNew = { onNavigateToGroupEdit(-1L) }
                )
            }
        } else {
            BudgetGroupsContent(
                modifier = Modifier.hazeSource(hazeState).background(MaterialTheme.colorScheme.background),
                topPadding = paddingValues.calculateTopPadding(),
                uiState = uiState,
                onPreviousMonth = { viewModel.selectPreviousMonth() },
                onNextMonth = { viewModel.selectNextMonth() },
                onGroupClick = { groupId -> onNavigateToGroupEdit(groupId) },
                onDeleteGroup = { groupId -> viewModel.deleteGroup(groupId) },
                onMoveGroupUp = { groupId -> viewModel.moveGroupUp(groupId) },
                onMoveGroupDown = { groupId -> viewModel.moveGroupDown(groupId) },
                onNavigateToHistory = { groupId ->
                    onNavigateToHistory(groupId, uiState.selectedYear, uiState.selectedMonth)
                },
                onNavigateToDetail = { groupId ->
                    onNavigateToDetail(groupId, uiState.selectedYear, uiState.selectedMonth)
                },
                onCategoryClick = { category ->
                    val yearMonth = "%04d-%02d".format(uiState.selectedYear, uiState.selectedMonth)
                    onNavigateToCategory(category, yearMonth, uiState.currency)
                }
            )
        }
    }
}

/**
 * Cashiro's empty state: no card, just a centred icon, headline, one line of
 * explanation and the actions, sitting in the middle of the free space. The
 * block is lifted by the FAB's clearance so it centres on the visible area
 * rather than hiding a dead band above it, and it scrolls when a large font
 * makes it taller than the screen.
 */
@Composable
private fun EmptyBudgetState(
    modifier: Modifier = Modifier,
    onSmartDefaults: () -> Unit,
    onCreateNew: () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val minHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight)
                .padding(
                    start = Dimensions.Padding.empty,
                    end = Dimensions.Padding.empty,
                    top = Spacing.lg,
                    bottom = Dimensions.Component.fabScrollClearance
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(Dimensions.Icon.emptyStateContainer)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TrackChanges,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.emptyStateGlyph),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = stringResource(R.string.budgets_setup_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = stringResource(R.string.budgets_setup_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = EMPTY_STATE_TEXT_MAX_WIDTH)
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            Button(
                onClick = onSmartDefaults,
                modifier = Modifier.defaultMinSize(minHeight = Dimensions.Component.minTouchTarget)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.small)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(stringResource(R.string.budgets_setup_smart_defaults))
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            TextButton(
                onClick = onCreateNew,
                modifier = Modifier.defaultMinSize(minHeight = Dimensions.Component.minTouchTarget)
            ) {
                Text(stringResource(R.string.budgets_setup_custom))
            }
        }
    }
}

/** Keeps the centred explanation from wrapping into an awkward wide block. */
private val EMPTY_STATE_TEXT_MAX_WIDTH = 280.dp

@Composable
private fun BudgetGroupsContent(
    modifier: Modifier = Modifier,
    topPadding: Dp = Spacing.none,
    uiState: BudgetGroupsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGroupClick: (Long) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    onMoveGroupUp: (Long) -> Unit,
    onMoveGroupDown: (Long) -> Unit,
    onNavigateToHistory: (Long) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onCategoryClick: (String) -> Unit
) {
    val summary = uiState.summary ?: return
    val isCurrentMonth = YearMonth.of(uiState.selectedYear, uiState.selectedMonth) == YearMonth.now()
    val groupCount = summary.groups.size
    var deleteGroupId by remember { mutableStateOf<Long?>(null) }
    var deleteGroupName by remember { mutableStateOf("") }

    var hasAnimated by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    val slideOffsetPx = with(density) { Spacing.xl.roundToPx() }

    LaunchedEffect(Unit) {
        if (!hasAnimated) {
            delay(350)
            hasAnimated = true
        }
    }

    val lazyListState = rememberLazyListState()
    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize().overScrollVertical(),
        contentPadding = PaddingValues(
            start = Dimensions.Padding.content,
            end = Dimensions.Padding.content,
            top = Dimensions.Padding.content + topPadding,
            // Lets the last card scroll clear of the New Budget FAB.
            bottom = Dimensions.Component.fabScrollClearance + Spacing.md
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
    ) {
        // Month Selector
        item {
            val visible = remember { mutableStateOf(hasAnimated) }
            LaunchedEffect(Unit) {
                if (!hasAnimated) { delay(0); visible.value = true }
            }
            AnimatedVisibility(
                visible = visible.value,
                enter = fadeIn(tween(300)) + slideInVertically(
                    initialOffsetY = { slideOffsetPx },
                    animationSpec = tween(300)
                )
            ) {
                MonthSelector(
                    year = uiState.selectedYear,
                    month = uiState.selectedMonth,
                    isCurrentMonth = isCurrentMonth,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth
                )
            }
        }

        // Budget Cards
        itemsIndexed(
            items = summary.groups,
            key = { _, group -> group.group.budget.id }
        ) { index, groupSpending ->
            val visible = remember { mutableStateOf(hasAnimated) }
            LaunchedEffect(Unit) {
                if (!hasAnimated) { delay((index + 1) * 50L); visible.value = true }
            }
            AnimatedVisibility(
                visible = visible.value,
                enter = fadeIn(tween(300)) + slideInVertically(
                    initialOffsetY = { slideOffsetPx },
                    animationSpec = tween(300)
                )
            ) {
                BudgetOverviewCard(
                    groupSpending = groupSpending,
                    currency = uiState.currency,
                    isFirst = index == 0,
                    isLast = index == groupCount - 1,
                    onEdit = { onGroupClick(groupSpending.group.budget.id) },
                    onDelete = {
                        deleteGroupId = groupSpending.group.budget.id
                        deleteGroupName = groupSpending.group.budget.name
                    },
                    onMoveUp = { onMoveGroupUp(groupSpending.group.budget.id) },
                    onMoveDown = { onMoveGroupDown(groupSpending.group.budget.id) },
                    onViewHistory = { onNavigateToHistory(groupSpending.group.budget.id) },
                    onViewDetails = { onNavigateToDetail(groupSpending.group.budget.id) },
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    // Delete confirmation dialog
    if (deleteGroupId != null) {
        AlertDialog(
            onDismissRequest = { deleteGroupId = null },
            title = { Text(stringResource(R.string.budgets_delete_title)) },
            text = { Text(stringResource(R.string.budgets_delete_message, deleteGroupName)) },
            confirmButton = {
                Button(
                    onClick = {
                        deleteGroupId?.let { onDeleteGroup(it) }
                        deleteGroupId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.budgets_action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteGroupId = null }) {
                    Text(stringResource(R.string.budgets_action_cancel))
                }
            }
        )
    }
}

@Composable
private fun MonthSelector(
    year: Int,
    month: Int,
    isCurrentMonth: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val yearMonth = YearMonth.of(year, month)
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(
            onClick = onPrevious,
            modifier = Modifier.size(Dimensions.Component.minTouchTarget)
        ) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = stringResource(R.string.budgets_previous_month),
                modifier = Modifier.size(Dimensions.Icon.medium)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = yearMonth.format(formatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        FilledTonalIconButton(
            onClick = onNext,
            enabled = !isCurrentMonth,
            modifier = Modifier.size(Dimensions.Component.minTouchTarget)
        ) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.budgets_next_month),
                modifier = Modifier.size(Dimensions.Icon.medium)
            )
        }
    }
}

@Composable
internal fun BudgetOverviewCard(
    groupSpending: BudgetGroupSpending,
    currency: String,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onViewHistory: () -> Unit,
    onViewDetails: () -> Unit,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val budget = groupSpending.group.budget
    var expanded by remember { mutableStateOf(false) }
    val hasBreakdown = groupSpending.categorySpending.isNotEmpty() ||
        (groupSpending.dailyCumulativeSpending.size >= 2 && groupSpending.dailyBudgetPace.isNotEmpty())
    val expansionState = stringResource(
        if (expanded) R.string.budget_hide_breakdown else R.string.budget_show_breakdown
    )

    val pctUsed = groupSpending.percentageUsed

    var animatedProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgressState by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 800),
        label = "progressAnimation"
    )

    LaunchedEffect(pctUsed) {
        animatedProgress = (pctUsed / 100f).coerceIn(0f, 1f)
    }

    val budgetColor = budget.color.toColorOr(MaterialTheme.colorScheme.primary)
    val statusColor = budgetStatusColor(pctUsed)
    val barColor = budgetBarColor(pctUsed, budgetColor)

    PennyWiseCardV2(
        onClick = if (hasBreakdown) ({ expanded = !expanded }) else null,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (hasBreakdown) stateDescription = expansionState
            },
        shape = MaterialTheme.shapes.extraLarge,
        // A faint rim in the budget's own colour, over the standard card surface.
        border = budgetRim(budgetColor),
        // The wash is painted by the column below so it covers the whole card.
        contentPadding = Dimensions.Padding.none
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .budgetColorWash(budgetColor)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                .padding(horizontal = Spacing.md + Spacing.xs, vertical = Spacing.smd)
        ) {
            // Header: marker + UPPERCASE name, then history and the overflow menu.
            BudgetCardTitle(name = budget.name, markerColor = barColor) {
                BudgetHeroIconButton(
                    onClick = onViewHistory,
                    icon = Iconax.IconaxHistory,
                    contentDescription = stringResource(R.string.budgets_view_history)
                )
                Box {
                    var showMenu by remember { mutableStateOf(false) }
                    BudgetHeroIconButton(
                        onClick = { showMenu = true },
                        icon = Icons.Default.MoreVert,
                        contentDescription = stringResource(
                            R.string.budget_more_actions,
                            budget.name
                        )
                    )
                    BudgetOverflowMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        isFirst = isFirst,
                        isLast = isLast,
                        onEdit = onEdit,
                        onViewHistory = onViewHistory,
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        onDelete = onDelete
                    )
                }
            }

            if (groupSpending.totalBudget > BigDecimal.ZERO) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                BudgetHeroFigures(groupSpending = groupSpending, currency = currency)
                Spacer(modifier = Modifier.height(Spacing.md))
                BudgetProgressTrack(progress = animatedProgressState, color = barColor)
                Spacer(modifier = Modifier.height(Spacing.sm))

                // Footer: how much of the budget is used, then when the
                // budget's window renews (the overage is in the hero already).
                val percentText = stringResource(R.string.budgets_percent, pctUsed.toInt())
                val renewalText = budgetRenewalText(groupSpending)
                val footerText = buildAnnotatedString {
                    withStyle(SpanStyle(color = statusColor, fontWeight = FontWeight.SemiBold)) {
                        append(percentText)
                    }
                    append(" · ")
                    append(renewalText)
                }
                Text(
                    text = footerText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (groupSpending.isTrackingAllExpenses) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                BudgetCardCaption(text = stringResource(R.string.budgets_card_spent_label))
                Text(
                    text = CurrencyFormatter.formatCurrency(groupSpending.totalActual, currency),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.budgets_tracking_all),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Actions: the breakdown affordance (the whole card toggles it) on
            // the left, "View details" on the right.
            Spacer(modifier = Modifier.height(Spacing.xs))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (hasBreakdown) Arrangement.SpaceBetween else Arrangement.End,
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                if (hasBreakdown) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = expansionState,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(Dimensions.Icon.inline),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = onViewDetails,
                    modifier = Modifier.defaultMinSize(minHeight = Dimensions.Component.minTouchTarget),
                ) {
                    Text(stringResource(R.string.budget_view_details))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.inline),
                    )
                }
            }

            // Expandable category list + pace chart
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    // Per-budget spending pace chart
                    if (groupSpending.dailyCumulativeSpending.size >= 2 && groupSpending.dailyBudgetPace.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        SpendingPaceChart(
                            cumulativeSpending = groupSpending.dailyCumulativeSpending,
                            budgetPace = groupSpending.dailyBudgetPace,
                            currency = currency
                        )
                    }

                    if (groupSpending.categorySpending.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))

                    groupSpending.categorySpending.forEach { catSpending ->
                        val catPctUsed = catSpending.percentageUsed
                        val catStatusColor: Color = when {
                            catPctUsed >= 90f -> MaterialTheme.colorScheme.error
                            catPctUsed >= 70f -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Spacing.sm))
                                .clickable { onCategoryClick(catSpending.categoryName) }
                                .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            // Category icon in colored circle
                            Box(
                                modifier = Modifier
                                    .size(Dimensions.Icon.list)
                                    .clip(CircleShape)
                                    .background(CategoryMapping.colorFor(catSpending.categoryName).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CategoryIcon(
                                    category = catSpending.categoryName,
                                    size = Dimensions.Icon.inline,
                                    tint = CategoryMapping.colorFor(catSpending.categoryName)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = catSpending.categoryName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (catSpending.budgetAmount > BigDecimal.ZERO) {
                                        Text(
                                            text = stringResource(R.string.budgets_percent, catPctUsed.toInt()),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = catStatusColor
                                        )
                                    }
                                }

                                if (catSpending.budgetAmount > BigDecimal.ZERO) {
                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    val catBarShape = RoundedCornerShape(50)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(Dimensions.Component.progressBarHeight)
                                            .clip(catBarShape)
                                            .background(catStatusColor.copy(alpha = 0.15f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = (catPctUsed / 100f).coerceIn(0f, 1f))
                                                .fillMaxHeight()
                                                .clip(catBarShape)
                                                .background(catStatusColor)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    Text(
                                        text = stringResource(
                                            R.string.budgets_amount_of,
                                            CurrencyFormatter.formatCurrency(catSpending.actualAmount, currency),
                                            CurrencyFormatter.formatCurrency(catSpending.budgetAmount, currency)
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = CurrencyFormatter.formatCurrency(catSpending.actualAmount, currency),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingPaceChart(
    cumulativeSpending: List<Double>,
    budgetPace: List<Double>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val themeColors = MaterialTheme.colorScheme
    val isOverPace = cumulativeSpending.lastOrNull()?.let { actual ->
        budgetPace.lastOrNull()?.let { pace -> actual > pace }
    } ?: false
    val spendingColor = if (isOverPace) themeColors.error else themeColors.primary
    val actualLabel = stringResource(R.string.budgets_chart_actual)
    val budgetPaceLabel = stringResource(R.string.budgets_chart_pace)

    Column(modifier = modifier.fillMaxWidth()) {
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.Component.chartCompactHeight),
                data = listOf(
                    Line(
                        label = actualLabel,
                        values = cumulativeSpending,
                        color = SolidColor(spendingColor),
                        firstGradientFillColor = spendingColor.copy(alpha = 0.2f),
                        secondGradientFillColor = Color.Transparent,
                        strokeAnimationSpec = tween(1200),
                        gradientAnimationDelay = 600,
                        drawStyle = DrawStyle.Stroke(width = Dimensions.Component.chartStroke),
                        curvedEdges = true,
                        dotProperties = DotProperties(
                            enabled = false
                        )
                    ),
                    Line(
                        label = budgetPaceLabel,
                        values = budgetPace,
                        color = SolidColor(themeColors.onSurfaceVariant.copy(alpha = 0.4f)),
                        drawStyle = DrawStyle.Stroke(width = Dimensions.Component.chartReferenceStroke),
                        strokeAnimationSpec = tween(1200),
                        curvedEdges = false,
                        dotProperties = DotProperties(enabled = false)
                    )
                ),
                dividerProperties = DividerProperties(enabled = false),
                indicatorProperties = HorizontalIndicatorProperties(
                    enabled = true,
                    textStyle = PennyWiseText.chartLabel.copy(
                        color = themeColors.onSurfaceVariant
                    ),
                    contentBuilder = { value ->
                        CurrencyFormatter.formatAbbreviated(value, currency)
                    }
                ),
                labelHelperProperties = LabelHelperProperties(enabled = false),
                labelProperties = LabelProperties(enabled = false),
                gridProperties = GridProperties(
                    enabled = true,
                    xAxisProperties = GridProperties.AxisProperties(
                        enabled = false
                    ),
                    yAxisProperties = GridProperties.AxisProperties(
                        enabled = true,
                        style = StrokeStyle.Dashed(),
                        color = SolidColor(themeColors.onSurface.copy(alpha = 0.08f))
                    )
                ),
                animationMode = AnimationMode.Together(delayBuilder = { it * 100L }),
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimensions.Component.legendDot)
                        .clip(CircleShape)
                        .background(spendingColor)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = actualLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = themeColors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Box(
                    modifier = Modifier
                        .size(Dimensions.Component.legendDot)
                        .clip(CircleShape)
                        .background(themeColors.onSurfaceVariant.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = budgetPaceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = themeColors.onSurfaceVariant
                )
            }
        }
}

/** The overview card's overflow actions; Delete still goes through the confirmation dialog. */
@Composable
private fun BudgetOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onViewHistory: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.budget_edit)) },
            onClick = {
                onDismiss()
                onEdit()
            },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.budgets_view_history)) },
            onClick = {
                onDismiss()
                onViewHistory()
            },
            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.budgets_move_up)) },
            onClick = {
                onDismiss()
                onMoveUp()
            },
            leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) },
            enabled = !isFirst
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.budgets_move_down)) },
            onClick = {
                onDismiss()
                onMoveDown()
            },
            leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
            enabled = !isLast
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.budget_delete),
                    color = MaterialTheme.colorScheme.error
                )
            },
            onClick = {
                onDismiss()
                onDelete()
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}
