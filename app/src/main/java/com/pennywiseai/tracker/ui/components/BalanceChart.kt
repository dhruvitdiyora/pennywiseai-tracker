package com.pennywiseai.tracker.ui.components

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.R
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
import ir.ehsannarmani.compose_charts.models.PopupProperties
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

@Composable
fun BalanceChart(
    primaryCurrency: String,
    balanceHistory: List<BalancePoint>,
    modifier: Modifier = Modifier,
    height: Int = 200,
    seriesLabel: String? = null
) {
    if (balanceHistory.isEmpty()) return

    val resolvedSeriesLabel = seriesLabel ?: stringResource(R.string.balance_chart_label)

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

        smoothedHistory.map {
            val date = it.timestamp
            when {
                isYearly -> date.format(DateTimeFormatter.ofPattern("yyyy"))
                isMonthly && spansMultipleYears -> date.format(DateTimeFormatter.ofPattern("MMM yy"))
                isMonthly -> date.format(DateTimeFormatter.ofPattern("MMM"))
                else -> date.format(DateTimeFormatter.ofPattern("dd MMM"))
            }
        }.let(::thinAxisLabels)
    }

    LineChart(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .padding(horizontal = Spacing.sm, vertical = Spacing.md),
        data = listOf(
            Line(
                label = resolvedSeriesLabel,
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
            // Labels are thinned to fit flat (see [thinAxisLabels]), so they
            // stay level instead of being forced into a crowded 45° slant.
            rotation = LabelProperties.Rotation(
                mode = LabelProperties.Rotation.Mode.Force,
                degree = 0f
            )
        ),
        // The library's default popup prints the raw double ("1641.1") at any
        // interpolated touch point. Snap it to real data points and tag it with
        // the chart's currency, on an inverse-surface bubble for contrast.
        popupProperties = PopupProperties(
            enabled = true,
            mode = PopupProperties.Mode.PointMode(),
            textStyle = chartLabel.copy(color = themeColors.inverseOnSurface),
            containerColor = themeColors.inverseSurface,
            contentBuilder = { popup ->
                CurrencyFormatter.formatCurrency(popup.value, primaryCurrency)
            }
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

/** Most x-axis labels the trend chart draws, so a flat "dd MMM" row fits a phone width. */
private const val MAX_AXIS_LABELS = 5

/**
 * Blanks all but an evenly spaced subset of [labels] (at most
 * [MAX_AXIS_LABELS]), counted back from the newest point so the latest date is
 * always labelled. The chart still plots every point; only its captions thin.
 */
internal fun thinAxisLabels(labels: List<String>): List<String> {
    if (labels.size <= MAX_AXIS_LABELS) return labels
    val step = (labels.size + MAX_AXIS_LABELS - 1) / MAX_AXIS_LABELS
    val last = labels.lastIndex
    return labels.mapIndexed { index, label -> if ((last - index) % step == 0) label else "" }
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
