package com.pennywiseai.tracker.presentation.budgetgroups

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.BudgetEntity
import com.pennywiseai.tracker.data.database.entity.BudgetPeriodType
import com.pennywiseai.tracker.data.repository.PastWindowSpending
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.CadencePill
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.components.toColorOr
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DividerProperties
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.LineProperties
import ir.ehsannarmani.compose_charts.models.StrokeStyle
import ir.ehsannarmani.compose_charts.models.ZeroLineProperties
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Per-budget history drill-down. Opened from the Budgets page via the
 * 3-dots menu "View this period history" item. Lists every window for
 * the selected (year, month) — for a Weekly budget that's the per-week
 * sub-list; for Monthly / One-time it's a single entry.
 *
 * Styled after Cashiro's budget history: a large collapsing title, a spending
 * line chart, then one rounded row per window with a progress ring on the
 * right. Each row still carries a "Live" or "Frozen as of …" badge so the user
 * can see which weeks are still accumulating spend (current week in the
 * current month) vs which are frozen snapshots.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val budgetName = state.budget?.name

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = if (budgetName != null) {
                    stringResource(R.string.budget_history_title_format, budgetName)
                } else {
                    stringResource(R.string.budget_history_title)
                },
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.budgets_back),
                    )
                },
                hazeState = hazeState,
            )
        }
    ) { padding ->
        val topPadding = padding.calculateTopPadding()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val budget = state.budget
            when {
                state.isLoading -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.budget_history_loading),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                budget == null -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.budget_history_not_found),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    val trendWindows = remember(state.windowHistory) {
                        eligibleTrendWindows(state.windowHistory, LocalDate.now())
                    }
                    val listState = rememberLazyListState()

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Dimensions.Padding.content,
                            end = Dimensions.Padding.content,
                            top = Dimensions.Padding.content + topPadding,
                            bottom = Dimensions.Component.bottomBarHeight + Spacing.md
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        // A trend becomes informative once at least two comparable windows exist.
                        if (trendWindows.size >= 2) {
                            item {
                                SpendingTrendChart(
                                    windows = trendWindows,
                                    currency = state.currency,
                                )
                            }
                        }

                        // Summary card — period, total spent, displayed window range
                        item {
                            HistorySummaryCard(state = state, budget = budget)
                        }

                        // Per-window list
                        item {
                            SectionHeaderV2(
                                title = when (budget.periodType) {
                                    BudgetPeriodType.WEEKLY -> stringResource(R.string.budget_history_per_week)
                                    BudgetPeriodType.MONTHLY -> stringResource(R.string.budget_history_cycle)
                                    BudgetPeriodType.CUSTOM -> stringResource(R.string.budget_history_range)
                                },
                                subtitle = stringResource(R.string.budget_history_per_window_hint),
                            )
                        }
                        item {
                            // One connected block of rows, like Cashiro's period list.
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                                state.windowHistory.forEachIndexed { index, window ->
                                    HistoryRow(
                                        window = window,
                                        currency = state.currency,
                                        budgetAmount = state.windowBudgetAmount,
                                        isDisplayed = window.window.start == state.displayedWindowStart &&
                                            window.window.end == state.displayedWindowEnd,
                                        isCurrentPeriod = state.yearMonth == YearMonth.now(),
                                        onClick = { viewModel.loadBreakdown(window.window) },
                                        listItemPosition = ListItemPosition.from(
                                            index,
                                            state.windowHistory.size
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    // Per-window breakdown sheet — rendered when a row is tapped.
                    // ModalBottomSheet is hosted at the screen level (not inside
                    // the LazyColumn) so it doesn't get clipped by the scroll
                    // container and so the dismiss gesture covers the full screen.
                    if (state.breakdown != null || state.isLoadingBreakdown) {
                        BreakdownSheet(
                            breakdown = state.breakdown,
                            isLoading = state.isLoadingBreakdown,
                            currency = state.currency,
                            onDismiss = { viewModel.dismissBreakdown() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * The period summary: cadence, month, spent of budget and the displayed
 * window, washed in the budget's own colour like the detail hero.
 */
@Composable
private fun HistorySummaryCard(
    state: BudgetHistoryUiState,
    budget: BudgetEntity,
) {
    val budgetColor = budget.color.toColorOr(MaterialTheme.colorScheme.primary)
    val longFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }

    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        border = budgetRim(budgetColor),
        // The wash is painted by the column below so it covers the whole card.
        contentPadding = Dimensions.Padding.none,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .budgetColorWash(budgetColor)
                .padding(horizontal = Spacing.md + Spacing.xs, vertical = Spacing.smd),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                CadencePill(periodType = budget.periodType)
                Text(
                    text = monthLabel(state.yearMonth),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = stringResource(
                    R.string.budgets_amount_of,
                    CurrencyFormatter.formatCurrency(state.totalSpent, state.currency),
                    CurrencyFormatter.formatCurrency(state.budgetAmount, state.currency)
                ),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = stringResource(
                    R.string.budget_history_window,
                    state.displayedWindowStart.format(longFormatter),
                    state.displayedWindowEnd.format(longFormatter)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            if (state.displayedIsLive) {
                LiveBadge()
            } else {
                FrozenBadge(state.displayedCapDate)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HistoryRow(
    window: PastWindowSpending,
    currency: String,
    budgetAmount: BigDecimal,
    isDisplayed: Boolean,
    isCurrentPeriod: Boolean,
    onClick: () -> Unit,
    listItemPosition: ListItemPosition = ListItemPosition.Single,
) {
    val scheme = MaterialTheme.colorScheme
    val shortFormatter = remember { DateTimeFormatter.ofPattern("d MMM") }
    val hasBudget = budgetAmount > BigDecimal.ZERO
    val percentageUsed = if (hasBudget) {
        window.spent
            .divide(budgetAmount, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .coerceAtLeast(BigDecimal.ZERO)
    } else {
        BigDecimal.ZERO
    }
    val isOver = hasBudget && window.spent > budgetAmount
    val progressColor = when {
        percentageUsed >= BigDecimal(90) -> scheme.error
        percentageUsed >= BigDecimal(70) -> scheme.tertiary
        else -> scheme.primary
    }
    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth(),
        shape = listItemPosition.toShape(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = stringResource(
                            R.string.budgets_date_range,
                            window.window.start.format(shortFormatter),
                            window.window.end.format(shortFormatter)
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isDisplayed) {
                        Text(
                            text = stringResource(R.string.budget_history_current),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onPrimary,
                            modifier = Modifier
                                .background(
                                    color = scheme.primary,
                                    shape = CircleShape
                                )
                                .padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    itemVerticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = CurrencyFormatter.formatCurrency(window.spent, currency),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = scheme.onSurface,
                    )
                    if (hasBudget) {
                        Text(
                            text = stringResource(
                                R.string.budget_history_row_of,
                                CurrencyFormatter.formatCurrency(budgetAmount, currency)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasBudget) {
                        Text(
                            text = if (isOver) {
                                stringResource(
                                    R.string.budgets_over_by,
                                    CurrencyFormatter.formatCurrency(window.spent - budgetAmount, currency)
                                )
                            } else {
                                stringResource(
                                    R.string.budgets_remaining,
                                    CurrencyFormatter.formatCurrency(budgetAmount - window.spent, currency)
                                )
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (isOver) scheme.error else scheme.onSurfaceVariant,
                        )
                    }
                    if (window.isLive && isCurrentPeriod) {
                        LiveBadge()
                    } else {
                        FrozenBadge(window.capDate)
                    }
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Dimensions.Component.progressRingCompact)
                    .clip(CircleShape)
                    .background(progressColor.copy(alpha = 0.1f)),
            ) {
                CircularProgressIndicator(
                    progress = { percentageUsed.toFloat().div(100f).coerceIn(0f, 1f) },
                    modifier = Modifier.size(Dimensions.Component.progressRingCompact),
                    color = progressColor,
                    strokeWidth = Dimensions.Component.progressRingStroke,
                    trackColor = progressColor.copy(alpha = Dimensions.Alpha.divider),
                )
                Text(
                    text = if (percentageUsed >= BigDecimal(1000)) {
                        "999%+"
                    } else {
                        "${percentageUsed.toInt()}%"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = progressColor,
                )
            }
        }
    }
}

/**
 * Cashiro's spending line chart: a curved, gradient-filled line with a dot per
 * window, abbreviated amounts down the side, rotated window dates along the
 * bottom and a dashed grid, with the series legend on top.
 */
@Composable
internal fun SpendingTrendChart(
    windows: List<PastWindowSpending>,
    currency: String,
) {
    if (windows.size < 2) return

    val colors = MaterialTheme.colorScheme
    val chartLabel = PennyWiseText.chartLabel
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM") }
    val chronologicalWindows = remember(windows) { windows.sortedBy { it.window.start } }
    val values = remember(chronologicalWindows) { chronologicalWindows.map { it.spent.toDouble() } }
    val labels = remember(chronologicalWindows) {
        chronologicalWindows.map { it.window.start.format(formatter) }
    }
    val gridColor = SolidColor(colors.onSurface.copy(alpha = 0.1f))

    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        LineChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(TREND_CHART_HEIGHT)
                .padding(horizontal = Spacing.xs, vertical = Spacing.sm),
            data = listOf(
                Line(
                    label = stringResource(R.string.budget_history_trend_title),
                    values = values,
                    color = SolidColor(colors.primary),
                    firstGradientFillColor = colors.primary.copy(alpha = 0.3f),
                    secondGradientFillColor = Color.Transparent,
                    strokeAnimationSpec = tween(1200, easing = EaseInOutCubic),
                    gradientAnimationDelay = 600,
                    drawStyle = DrawStyle.Stroke(width = Dimensions.Component.chartStroke),
                    curvedEdges = true,
                    dotProperties = DotProperties(
                        enabled = true,
                        color = SolidColor(colors.primary),
                        strokeWidth = 3.dp,
                        radius = 4.dp,
                        strokeColor = SolidColor(colors.surfaceContainerLow),
                    ),
                ),
            ),
            dividerProperties = DividerProperties(
                enabled = true,
                xAxisProperties = LineProperties(
                    color = SolidColor(colors.onSurface.copy(alpha = 0f)),
                    thickness = 0.dp,
                ),
                yAxisProperties = LineProperties(
                    color = SolidColor(colors.onSurface.copy(alpha = 0f)),
                    thickness = 0.dp,
                ),
            ),
            indicatorProperties = HorizontalIndicatorProperties(
                enabled = true,
                textStyle = chartLabel.copy(
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                ),
                contentBuilder = { value -> CurrencyFormatter.formatAbbreviated(value, currency) },
            ),
            labelHelperProperties = LabelHelperProperties(
                enabled = true,
                textStyle = chartLabel.copy(
                    color = colors.onSurface,
                    textAlign = TextAlign.End,
                ),
            ),
            labelProperties = LabelProperties(
                enabled = true,
                textStyle = chartLabel.copy(
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.End,
                ),
                labels = labels,
                padding = Spacing.md,
                rotation = LabelProperties.Rotation(
                    mode = LabelProperties.Rotation.Mode.Force,
                    degree = -45f,
                ),
            ),
            zeroLineProperties = ZeroLineProperties(
                enabled = true,
                style = StrokeStyle.Dashed(),
                color = gridColor,
            ),
            gridProperties = GridProperties(
                enabled = true,
                xAxisProperties = GridProperties.AxisProperties(
                    enabled = true,
                    style = StrokeStyle.Dashed(),
                    color = gridColor,
                ),
                yAxisProperties = GridProperties.AxisProperties(
                    enabled = true,
                    style = StrokeStyle.Dashed(),
                    color = gridColor,
                ),
            ),
            animationMode = AnimationMode.Together(delayBuilder = { it * 100L }),
        )
    }
}

/** Tall enough for the legend, the plot and the rotated window dates. */
private val TREND_CHART_HEIGHT = 240.dp

internal fun eligibleTrendWindows(
    windows: List<PastWindowSpending>,
    asOf: LocalDate,
): List<PastWindowSpending> = windows
    .filter { !it.window.start.isAfter(asOf) }
    .sortedBy { it.window.start }

@Composable
private fun LiveBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(Spacing.sm)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary)
        )
        Text(
            text = stringResource(R.string.budget_history_live),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun FrozenBadge(capDate: LocalDate) {
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(Spacing.sm)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = stringResource(R.string.budget_history_frozen, capDate.format(formatter)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun monthLabel(yearMonth: YearMonth): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    return yearMonth.format(formatter)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreakdownSheet(
    breakdown: com.pennywiseai.tracker.data.repository.WindowBreakdown?,
    isLoading: Boolean,
    currency: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val longFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    bottom = Dimensions.Component.bottomBarHeight + Spacing.md
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            if (isLoading || breakdown == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val window = breakdown.window
            val totalActual = breakdown.totalActual
            val totalBudget = breakdown.totalBudget
            val pctUsed = if (totalBudget > BigDecimal.ZERO) {
                (totalActual.toFloat() / totalBudget.toFloat() * 100f).coerceAtLeast(0f)
            } else 0f
            val remaining = totalBudget - totalActual
            val isOver = totalActual > totalBudget

            Text(
                text = stringResource(
                    R.string.budgets_date_range,
                    window.start.format(longFormatter),
                    window.end.format(longFormatter)
                ),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = if (breakdown.isTrackingAll) stringResource(R.string.budgets_tracking_all)
                else stringResource(
                    R.string.budgets_amount_of,
                    CurrencyFormatter.formatCurrency(totalActual, currency),
                    CurrencyFormatter.formatCurrency(totalBudget, currency)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Per-category list. For Tracking-All budgets the list is
            // empty (no per-cat allocations); we show the total instead
            // and skip the list.
            if (!breakdown.isTrackingAll && breakdown.categorySpending.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.budget_history_categories),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                breakdown.categorySpending.forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cat.categoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyFormatter.formatCurrency(cat.actualAmount, currency),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            if (cat.budgetAmount > BigDecimal.ZERO) {
                                Text(
                                    text = stringResource(
                                        R.string.budget_history_of_budget_percent,
                                        CurrencyFormatter.formatCurrency(cat.budgetAmount, currency),
                                        cat.percentageUsed.toInt()
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = when {
                    isOver -> stringResource(R.string.budgets_over_by, CurrencyFormatter.formatCurrency(-remaining, currency))
                    remaining > BigDecimal.ZERO -> stringResource(R.string.budgets_remaining, CurrencyFormatter.formatCurrency(remaining, currency))
                    else -> stringResource(R.string.budget_history_zero_remaining)
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isOver) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
            if (totalBudget > BigDecimal.ZERO) {
                Text(
                    text = stringResource(R.string.budget_history_percent_used, pctUsed.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
