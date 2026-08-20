package com.pennywiseai.tracker.ui.components

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.ui.theme.PennyWiseText
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

data class BalancePoint(
    val timestamp: LocalDateTime,
    val balance: BigDecimal,
    val currency: String = "INR"
)

/**
 * How many x-axis ticks a phone-width chart can show at -45° without crowding.
 */
private const val MAX_X_AXIS_LABELS = 6

/**
 * Samples at most [max] evenly-spaced labels, always including the first and last.
 *
 * `HorizontalLabels` lays the list out in a `Row` with `SpaceBetween` — labels are spread
 * evenly across the axis rather than pinned to their data point's x position. So handing it
 * one label per point doesn't produce one tick per point; it produces a row of labels crushed
 * together, and the rotated ones at the right get pushed past the edge. That's why a 16-day
 * month appeared to stop at "10 Aug". Sampling evenly keeps the positions honest, because
 * evenly-sampled labels land at evenly-spaced x positions.
 *
 * This must **filter**, never blank. The library derives every label's width from
 * `labelWidths.min()`, so a single empty string measures 0 and collapses the whole row to
 * zero width — which renders no axis at all.
 */
private fun thinLabels(labels: List<String>, max: Int): List<String> {
    if (labels.size <= max) return labels
    val lastIndex = labels.lastIndex
    return (0 until max).map { labels[it * lastIndex / (max - 1)] }
}

@Composable
fun BalanceChart(
    primaryCurrency: String,
    balanceHistory: List<BalancePoint>,
    modifier: Modifier = Modifier,
    height: Int = 200,
    /**
     * Legend label for the plotted series. Defaults to the account-balance case this chart
     * was written for; Analytics plots spending through the same component, where a legend
     * reading "Balance Trend" under an Expense filter contradicts the screen.
     */
    seriesLabel: String = "Balance Trend"
) {
    if (balanceHistory.isEmpty()) return

    val sortedHistory = remember(balanceHistory) {
        balanceHistory.sortedBy { it.timestamp }
    }

    val smoothedHistory = remember(sortedHistory) {
        smoothBalanceData(sortedHistory)
    }

    val themeColors = MaterialTheme.colorScheme
    // One style for every label this chart draws — axis ticks, the value
    // indicator and the legend — so they can't drift apart.
    val chartLabel = PennyWiseText.chartLabel

    val chartValues = remember(smoothedHistory) {
        smoothedHistory.map { it.balance.toDouble() }
    }

    val labels = remember(smoothedHistory) {
        val isYearly = smoothedHistory.size > 1 && smoothedHistory.all { it.timestamp.dayOfYear == 1 }
        val isMonthly = !isYearly && smoothedHistory.all { it.timestamp.dayOfMonth == 1 }
        val spansMultipleYears = if (smoothedHistory.isNotEmpty()) {
            smoothedHistory.first().timestamp.year != smoothedHistory.last().timestamp.year
        } else false

        val formatted = smoothedHistory.map {
            val date = it.timestamp
            when {
                isYearly -> date.format(DateTimeFormatter.ofPattern("yyyy"))
                isMonthly && spansMultipleYears -> date.format(DateTimeFormatter.ofPattern("MMM yy"))
                isMonthly -> date.format(DateTimeFormatter.ofPattern("MMM"))
                else -> date.format(DateTimeFormatter.ofPattern("dd MMM"))
            }
        }
        thinLabels(formatted, MAX_X_AXIS_LABELS)
    }

    LineChart(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .padding(horizontal = Spacing.sm, vertical = Spacing.md),
        data = listOf(
            Line(
                label = seriesLabel,
                values = chartValues,
                color = SolidColor(themeColors.primary),
                firstGradientFillColor = themeColors.primary.copy(alpha = 0.3f),
                secondGradientFillColor = Color.Transparent,
                strokeAnimationSpec = tween(1500, easing = EaseInOutCubic),
                gradientAnimationDelay = 750,
                drawStyle = DrawStyle.Stroke(width = 2.dp),
                curvedEdges = true,
                dotProperties = DotProperties(
                    enabled = true,
                    color = SolidColor(themeColors.primary),
                    strokeWidth = 3.dp,
                    radius = 4.dp,
                    strokeColor = SolidColor(themeColors.surface)
                )
            )
        ),
        dividerProperties = DividerProperties(
            enabled = true,
            xAxisProperties = LineProperties(
                color = SolidColor(themeColors.onSurface.copy(alpha = 0f)),
                thickness = 0.dp
            ),
            yAxisProperties = LineProperties(
                color = SolidColor(themeColors.onSurface.copy(alpha = 0f)),
                thickness = 0.dp
            )
        ),
        indicatorProperties = HorizontalIndicatorProperties(
            enabled = true,
            textStyle = chartLabel.copy(
                color = themeColors.onSurfaceVariant,
                textAlign = TextAlign.Center
            ),
            contentBuilder = { value ->
                CurrencyFormatter.formatAbbreviated(abs(value), primaryCurrency)
            }
        ),
        labelHelperProperties = LabelHelperProperties(
            enabled = true,
            textStyle = chartLabel.copy(
                color = themeColors.onSurface,
                textAlign = TextAlign.End
            ),
        ),
        labelProperties = LabelProperties(
            enabled = true,
            textStyle = chartLabel.copy(
                color = themeColors.onSurfaceVariant,
                textAlign = TextAlign.End
            ),
            labels = labels,
            padding = 16.dp,
            rotation = LabelProperties.Rotation(
                mode = LabelProperties.Rotation.Mode.Force,
                degree = -45f
            )
        ),
        zeroLineProperties = ZeroLineProperties(
            enabled = true,
            style = StrokeStyle.Dashed(),
            color = SolidColor(themeColors.onSurface.copy(alpha = 0.1f)),
        ),
        gridProperties = GridProperties(
            enabled = true,
            xAxisProperties = GridProperties.AxisProperties(
                enabled = true,
                style = StrokeStyle.Dashed(),
                color = SolidColor(themeColors.onSurface.copy(alpha = 0.1f))
            ),
            yAxisProperties = GridProperties.AxisProperties(
                enabled = true,
                style = StrokeStyle.Dashed(),
                color = SolidColor(themeColors.onSurface.copy(alpha = 0.1f))
            )
        ),
        animationMode = AnimationMode.Together(delayBuilder = { it * 200L }),
    )
}

/**
 * Smooth balance data to reduce noise in the chart
 * Applies time-based aggregation and moving average smoothing
 */
private fun smoothBalanceData(
    balanceHistory: List<BalancePoint>,
    maxPoints: Int = 50
): List<BalancePoint> {
    if (balanceHistory.size <= maxPoints) {
        return balanceHistory
    }

    val timeSpan = balanceHistory.last().timestamp.toLocalDate()
        .toEpochDay() - balanceHistory.first().timestamp.toLocalDate().toEpochDay()

    val intervalDays = maxOf(1, (timeSpan / maxPoints).toInt())

    val groupedData = balanceHistory.groupBy { point ->
        val dayIndex = (point.timestamp.toLocalDate().toEpochDay() -
            balanceHistory.first().timestamp.toLocalDate().toEpochDay()) / intervalDays
        dayIndex
    }

    return groupedData.map { (_, group) ->
        val avgBalance = group.map { it.balance }.reduce { acc, balance -> acc + balance } /
            BigDecimal(group.size.toLong())
        val middleIndex = group.size / 2
        val representativePoint = group[middleIndex]

        BalancePoint(
            timestamp = representativePoint.timestamp,
            balance = avgBalance,
            currency = representativePoint.currency
        )
    }.sortedBy { it.timestamp }
}
