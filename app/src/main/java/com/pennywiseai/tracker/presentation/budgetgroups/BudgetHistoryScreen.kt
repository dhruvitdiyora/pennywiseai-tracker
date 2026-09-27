package com.pennywiseai.tracker.presentation.budgetgroups

import com.pennywiseai.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.data.database.entity.BudgetPeriodType
import com.pennywiseai.tracker.data.repository.PastWindowSpending
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.components.cards.CadencePill
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.success
import com.pennywiseai.tracker.ui.theme.warning
import com.pennywiseai.tracker.utils.CurrencyFormatter
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
import ir.ehsannarmani.compose_charts.models.StrokeStyle
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.math.BigDecimal

/**
 * Per-budget history drill-down. Opened from the Budgets page via the
 * 3-dots menu "View this period history" item. Lists every window for
 * the selected (year, month) — for a Weekly budget that's the per-week
 * sub-list; for Monthly / One-time it's a single entry.
 *
 * Each row carries a "Live" or "Frozen as of …" badge so the user can
 * see which weeks are still accumulating spend (current week in the
 * current month) vs which are frozen snapshots.
 */
@Composable
fun BudgetHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PennyWiseScaffold(
        title = state.budget?.name ?: stringResource(R.string.budget_history_title),
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.budgets_back)
                )
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.padding(padding).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.budget_history_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@PennyWiseScaffold
        }
        val budget = state.budget
        if (budget == null) {
            Box(modifier = Modifier.padding(padding).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.budget_history_not_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@PennyWiseScaffold
        }
        val trendWindows = remember(state.windowHistory) {
            eligibleTrendWindows(state.windowHistory, LocalDate.now())
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Dimensions.Padding.content,
                bottom = Dimensions.Component.bottomBarHeight + Spacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Summary card — period, total spent, displayed window range
            item {
                PennyWiseCardV2(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
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
                        val longFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
                        Text(
                            text = stringResource(
                                R.string.budget_history_window,
                                state.displayedWindowStart.format(longFormatter),
                                state.displayedWindowEnd.format(longFormatter)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.displayedIsLive) {
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            LiveBadge()
                        } else {
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            FrozenBadge(state.displayedCapDate)
                        }
                    }
                }
            }

            // A trend becomes informative once at least two comparable windows exist.
            if (trendWindows.size >= 2) {
                item {
                    SpendingTrendChart(
                        windows = trendWindows,
                        currency = state.currency,
                    )
                }
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
            items(state.windowHistory) { window ->
                HistoryRow(
                    window = window,
                    currency = state.currency,
                    budgetAmount = state.windowBudgetAmount,
                    isDisplayed = window.window.start == state.displayedWindowStart &&
                        window.window.end == state.displayedWindowEnd,
                    isCurrentPeriod = state.yearMonth == YearMonth.now(),
                    onClick = { viewModel.loadBreakdown(window.window) }
                )
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

@Composable
internal fun HistoryRow(
    window: PastWindowSpending,
    currency: String,
    budgetAmount: BigDecimal,
    isDisplayed: Boolean,
    isCurrentPeriod: Boolean,
    onClick: () -> Unit
) {
    val shortFormatter = remember { DateTimeFormatter.ofPattern("d MMM") }
    val percentageUsed = if (budgetAmount > BigDecimal.ZERO) {
        window.spent
            .divide(budgetAmount, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .coerceAtLeast(BigDecimal.ZERO)
    } else {
        BigDecimal.ZERO
    }
    val progressColor = when {
        percentageUsed >= BigDecimal(90) -> MaterialTheme.colorScheme.error
        percentageUsed >= BigDecimal(70) -> MaterialTheme.colorScheme.warning
        else -> MaterialTheme.colorScheme.success
    }
    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.budgets_date_range,
                        window.window.start.format(shortFormatter),
                        window.window.end.format(shortFormatter)
                    ),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (isDisplayed) {
                    Text(
                        text = stringResource(R.string.budget_history_current),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = CurrencyFormatter.formatCurrency(window.spent, currency),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    if (budgetAmount > BigDecimal.ZERO) {
                        Text(
                            text = "of ${CurrencyFormatter.formatCurrency(budgetAmount, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.size(Spacing.sm))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(Dimensions.Component.progressRingCompact),
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
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = progressColor,
                    )
                }
            }
            if (window.isLive && isCurrentPeriod) {
                LiveBadge()
            } else {
                FrozenBadge(window.capDate)
            }
        }
    }
}

@Composable
internal fun SpendingTrendChart(
    windows: List<PastWindowSpending>,
    currency: String,
) {
    if (windows.size < 2) return

    val colors = MaterialTheme.colorScheme
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM") }
    val chronologicalWindows = remember(windows) { windows.sortedBy { it.window.start } }
    val values = remember(chronologicalWindows) { chronologicalWindows.map { it.spent.toDouble() } }
    val labels = remember(chronologicalWindows) {
        listOf(chronologicalWindows.first(), chronologicalWindows.last())
            .distinctBy { it.window.start }
            .map { it.window.start.format(formatter) }
    }

    PennyWiseCardV2(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Spending trend",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        LineChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimensions.Component.chartCompactHeight),
            data = listOf(
                Line(
                    label = "Spending",
                    values = values,
                    color = SolidColor(colors.primary),
                    firstGradientFillColor = colors.primary.copy(alpha = 0.2f),
                    secondGradientFillColor = Color.Transparent,
                    strokeAnimationSpec = androidx.compose.animation.core.tween(1200),
                    gradientAnimationDelay = 600,
                    drawStyle = DrawStyle.Stroke(width = Dimensions.Component.chartStroke),
                    curvedEdges = true,
                    dotProperties = DotProperties(enabled = false),
                ),
            ),
            dividerProperties = DividerProperties(enabled = false),
            indicatorProperties = HorizontalIndicatorProperties(
                enabled = true,
                textStyle = PennyWiseText.chartLabel.copy(color = colors.onSurfaceVariant),
                contentBuilder = { value -> CurrencyFormatter.formatAbbreviated(value, currency) },
            ),
            labelHelperProperties = LabelHelperProperties(enabled = false),
            labelProperties = LabelProperties(enabled = false),
            gridProperties = GridProperties(
                enabled = true,
                xAxisProperties = GridProperties.AxisProperties(enabled = false),
                yAxisProperties = GridProperties.AxisProperties(
                    enabled = true,
                    style = StrokeStyle.Dashed(),
                    color = SolidColor(colors.onSurface.copy(alpha = 0.08f)),
                ),
            ),
            animationMode = AnimationMode.Together(delayBuilder = { it * 100L }),
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

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
