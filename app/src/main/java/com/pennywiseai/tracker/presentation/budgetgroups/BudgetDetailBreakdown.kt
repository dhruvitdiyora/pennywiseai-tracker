package com.pennywiseai.tracker.presentation.budgetgroups

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.repository.BudgetCategorySpending
import com.pennywiseai.tracker.ui.components.CategoryIcon
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import kotlin.math.roundToInt

private val DonutSize = 128.dp
private val DonutStroke = 12.dp
private val DonutSelectedExtra = 3.dp
private val DonutCenterSize = 52.dp

/** Degrees of empty space left between two donut slices. */
private const val DONUT_GAP_DEGREES = 5f

/**
 * Cashiro's category breakdown: a donut of how the budget's spending splits
 * across its categories, with a legend beside it. Tapping a legend row selects
 * that slice (its icon sits in the donut's centre). Unlike Cashiro, a legend
 * row also keeps PennyWise's per-category allowance: a thin bar and
 * "of limit - percent" for every category that has its own budget amount.
 *
 * All categories of one budget are expressed in the same [currency], so the
 * shares are a ratio within that single currency.
 */
@Composable
internal fun BudgetCategoryBreakdownCard(
    categories: List<BudgetCategorySpending>,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val fallbackPalette = listOf(
        scheme.primary,
        scheme.tertiary,
        scheme.secondary,
        scheme.error,
        scheme.inversePrimary,
    )
    val sorted = remember(categories) { categories.sortedByDescending { it.actualAmount } }
    // A category the app has no colour for would come back grey; give those
    // distinct theme hues so neighbouring slices stay tellable apart.
    val colors = sorted.mapIndexed { index, category ->
        CategoryMapping.colorFor(category.categoryName).takeUnless { it == Color.Gray }
            ?: fallbackPalette[index % fallbackPalette.size]
    }
    val total = sorted.fold(BigDecimal.ZERO) { sum, category ->
        sum + category.actualAmount.coerceAtLeast(BigDecimal.ZERO)
    }
    val fractions = sorted.map { category ->
        if (total.signum() > 0) {
            (category.actualAmount.coerceAtLeast(BigDecimal.ZERO).toDouble() / total.toDouble()).toFloat()
        } else {
            0f
        }
    }
    var selected by remember(categories) { mutableIntStateOf(0) }
    val selectedIndex = selected.coerceIn(0, (sorted.size - 1).coerceAtLeast(0))

    GlassCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(DonutSize),
                contentAlignment = Alignment.Center,
            ) {
                CategoryDonut(
                    fractions = fractions,
                    colors = colors,
                    selected = selectedIndex,
                    modifier = Modifier.size(DonutSize),
                )
                sorted.getOrNull(selectedIndex)?.let { category ->
                    val color = colors[selectedIndex]
                    Box(
                        modifier = Modifier
                            .size(DonutCenterSize)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CategoryIcon(
                            category = category.categoryName,
                            size = Dimensions.Icon.medium,
                            tint = color,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                sorted.forEachIndexed { index, category ->
                    CategoryLegendRow(
                        category = category,
                        color = colors[index],
                        sharePercent = if (total.signum() > 0 && category.actualAmount.signum() > 0) {
                            (fractions[index] * 100f).roundToInt()
                        } else {
                            null
                        },
                        isSelected = index == selectedIndex,
                        currency = currency,
                        onClick = { selected = index },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryDonut(
    fractions: List<Float>,
    colors: List<Color>,
    selected: Int,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    Canvas(modifier = modifier) {
        val strokePx = DonutStroke.toPx()
        val extraPx = DonutSelectedExtra.toPx()
        val inset = strokePx / 2f + extraPx
        val topLeft = Offset(inset, inset)
        val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)

        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx),
        )

        val drawn = fractions.count { it > 0f }
        val gap = if (drawn > 1) DONUT_GAP_DEGREES else 0f
        var start = -90f
        fractions.forEachIndexed { index, fraction ->
            if (fraction <= 0f) return@forEachIndexed
            val sweep = 360f * fraction
            drawArc(
                color = colors[index],
                startAngle = start + gap / 2f,
                sweepAngle = (sweep - gap).coerceAtLeast(1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = if (index == selected) strokePx + extraPx else strokePx,
                ),
            )
            start += sweep
        }
    }
}

@Composable
private fun CategoryLegendRow(
    category: BudgetCategorySpending,
    color: Color,
    sharePercent: Int?,
    isSelected: Boolean,
    currency: String,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val amount = CurrencyFormatter.formatCurrency(category.actualAmount, currency)
    val hasAllowance = category.budgetAmount > BigDecimal.ZERO
    val allowanceColor = when {
        category.percentageUsed >= 90f -> scheme.error
        category.percentageUsed >= 70f -> scheme.tertiary
        else -> color
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (isSelected) scheme.surfaceContainerHighest.copy(alpha = 0.6f) else Color.Transparent,
            )
            .selectable(selected = isSelected, onClick = onClick, role = Role.Button)
            .defaultMinSize(minHeight = Dimensions.Component.minTouchTarget)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(Dimensions.Component.legendDot)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            Text(
                text = category.categoryName,
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (sharePercent != null) {
                    stringResource(R.string.budget_detail_legend_amount, amount, sharePercent)
                } else {
                    amount
                },
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            if (hasAllowance) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Spacing.xs)
                        .clip(CircleShape)
                        .background(allowanceColor.copy(alpha = Dimensions.Alpha.divider)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((category.percentageUsed / 100f).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(allowanceColor),
                    )
                }
                Text(
                    text = stringResource(
                        R.string.budget_history_of_budget_percent,
                        CurrencyFormatter.formatCurrency(category.budgetAmount, currency),
                        category.percentageUsed.toInt(),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
