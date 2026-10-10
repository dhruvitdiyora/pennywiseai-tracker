package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.BudgetGroupType
import com.pennywiseai.tracker.data.database.entity.BudgetPeriodType
import com.pennywiseai.tracker.data.repository.BudgetGroupSpending
import com.pennywiseai.tracker.ui.components.toColorOr
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/*
 * The Cashiro-style budget card family. The Home carousel card below, the
 * Budgets overview card and the Budget detail hero are all assembled from the
 * pieces in this file, so a budget reads as the same card wherever it shows:
 * the budget's colour washed over the surface with a faint rim, a marker and
 * an upper-case tracked name, the daily-left / spent-over-limit figures, a
 * progress track and a centred renewal footer.
 */

/** How strongly the budget's colour rims the card. */
private const val BUDGET_RIM_ALPHA = 0.12f

/** The 1dp rim drawn around a tinted budget card. */
fun budgetRim(color: Color): BorderStroke = BorderStroke(
    width = Dimensions.Component.dividerThickness,
    color = color.copy(alpha = BUDGET_RIM_ALPHA),
)

/**
 * Washes a few soft blobs of [color] over the card, like Cashiro's gradient
 * mesh but static: nothing animates, so it costs no frames and renders
 * identically in screenshots.
 */
fun Modifier.budgetColorWash(color: Color): Modifier = drawBehind {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.26f), Color.Transparent),
            center = Offset(size.width * 0.12f, size.height * 0.10f),
            radius = size.width * 0.70f,
        ),
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.18f), Color.Transparent),
            center = Offset(size.width * 0.95f, size.height * 0.95f),
            radius = size.width * 0.60f,
        ),
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(size.width * 0.75f, size.height * 0.15f),
            radius = size.width * 0.50f,
        ),
    )
}

/** Semantic colour for a percent-used figure: primary, tertiary from 70%, error from 90%. */
@Composable
fun budgetStatusColor(percentUsed: Float): Color = when {
    percentUsed >= 90f -> MaterialTheme.colorScheme.error
    percentUsed >= 70f -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

/**
 * The track's fill: the budget's own colour while it is healthy, the semantic
 * status colour once it nears or crosses the limit (so a red budget at 36%
 * does not read as danger).
 */
@Composable
fun budgetBarColor(percentUsed: Float, budgetColor: Color): Color =
    if (percentUsed >= 70f) budgetStatusColor(percentUsed) else budgetColor

/** Marker + upper-case, widely tracked budget name, with optional trailing content. */
@Composable
fun BudgetCardTitle(
    name: String,
    markerColor: Color,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Api,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.small),
            tint = markerColor,
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = name.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/** The small, upper-case, lightly tracked caption above a figure. */
@Composable
fun BudgetCardCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * The hero pair. On a spending limit the large figure is what is left to spend
 * per day ("per day" means nothing for a target or for expected bills, and
 * nothing is left once the limit is crossed), so those show what is left of
 * the whole budget - or how far over it is - instead. The right-hand pair is
 * spent over limit (or actual over target / expected).
 *
 * Every figure is in the one [currency] the summary was built for; nothing is
 * summed across currencies here.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetHeroFigures(
    groupSpending: BudgetGroupSpending,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val budget = groupSpending.group.budget
    val scheme = MaterialTheme.colorScheme
    val isOver = groupSpending.remaining < BigDecimal.ZERO
    val showDaily = budget.groupType == BudgetGroupType.LIMIT &&
        !isOver &&
        groupSpending.dailyAllowance > BigDecimal.ZERO
    val heroLabel = stringResource(
        when {
            showDaily -> R.string.budgets_card_daily_left
            isOver -> R.string.budgets_card_over_label
            else -> R.string.budgets_card_remaining_label
        },
    )
    val heroAmount = when {
        showDaily -> groupSpending.dailyAllowance
        isOver -> groupSpending.remaining.abs()
        else -> groupSpending.remaining.coerceAtLeast(BigDecimal.ZERO)
    }
    val pairLabel = stringResource(
        when (budget.groupType) {
            BudgetGroupType.LIMIT -> R.string.budgets_card_spent_limit
            BudgetGroupType.EXPECTED -> R.string.budgets_card_spent_expected
            BudgetGroupType.TARGET -> R.string.budgets_card_actual_target
        },
    )

    // FlowRow: when there is no room (narrow card, large font) the right-hand
    // figures wrap below instead of squeezing the hero.
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        Column {
            BudgetCardCaption(text = heroLabel)
            Text(
                text = CurrencyFormatter.formatCurrency(heroAmount, currency),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isOver) scheme.error else scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            BudgetCardCaption(text = pairLabel)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = CurrencyFormatter.formatCurrency(groupSpending.totalActual, currency),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = " / ",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = CurrencyFormatter.formatCurrency(groupSpending.totalBudget, currency),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
    }
}

/** The rounded progress track; [progress] is clamped to 0..1. */
@Composable
fun BudgetProgressTrack(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val barShape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimensions.Component.progressBarHeight)
            .clip(barShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = Dimensions.Alpha.divider)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(barShape)
                .background(color),
        )
    }
}

/**
 * When the budget's window ends, framed per cadence ("Resets in X days" /
 * "Runs 1 Jun – 30 Jun · N days left" / "Finished"). The overage is not
 * repeated here: the hero figure already says how far over the budget is.
 * The window is the budget's own current window, so the number is the same
 * on every month view.
 */
@Composable
fun budgetRenewalText(groupSpending: BudgetGroupSpending): String {
    val budget = groupSpending.group.budget
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
    return when {
        groupSpending.daysRemaining == 0 && groupSpending.daysElapsed >= groupSpending.windowDays ->
            stringResource(R.string.budgets_finished)
        groupSpending.periodType == BudgetPeriodType.WEEKLY -> {
            val renewalIn = (groupSpending.daysRemaining - 1).coerceAtLeast(0)
            val weekdayName = DayOfWeek.of((budget.weekStartDay ?: 1).coerceIn(1, 7))
                .getDisplayName(TextStyle.FULL, locale)
            if (renewalIn == 0) {
                stringResource(R.string.budgets_resets_today_weekly, weekdayName)
            } else {
                pluralStringResource(R.plurals.budgets_resets_in_weekly, renewalIn, renewalIn, weekdayName)
            }
        }
        groupSpending.periodType == BudgetPeriodType.MONTHLY -> {
            val startDay = budget.monthStartDay ?: groupSpending.windowStart.dayOfMonth
            val renewalIn = (groupSpending.daysRemaining - 1).coerceAtLeast(0)
            if (renewalIn == 0) {
                stringResource(R.string.budgets_resets_today_monthly, startDay)
            } else {
                pluralStringResource(R.plurals.budgets_resets_in_monthly, renewalIn, renewalIn, startDay)
            }
        }
        groupSpending.periodType == BudgetPeriodType.CUSTOM -> {
            val range = stringResource(
                R.string.budgets_date_range,
                groupSpending.windowStart.format(dateFormatter),
                groupSpending.windowEnd.format(dateFormatter),
            )
            if (groupSpending.daysRemaining >= 1) {
                // >1 counts the days after today; ==1 reads as "1 day".
                val left = (groupSpending.daysRemaining - 1).coerceAtLeast(1)
                pluralStringResource(R.plurals.budgets_runs_days_remaining, left, range, left)
            } else {
                stringResource(R.string.budgets_runs_finished, range)
            }
        }
        else -> pluralStringResource(
            R.plurals.budgets_days_remaining,
            groupSpending.daysRemaining,
            groupSpending.daysRemaining,
        )
    }
}

/**
 * The Home carousel's budget card: Cashiro's budget card as a single tap
 * target that opens the Budgets screen. It shows percent used beside the name
 * in place of the overview card's history and overflow actions.
 */
@Composable
fun BudgetCard(
    groupSpending: BudgetGroupSpending,
    currency: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    blurEffects: Boolean = false,
    hazeState: HazeState? = null
) {
    val budget = groupSpending.group.budget
    val pctUsed = groupSpending.percentageUsed
    val hasLimit = groupSpending.totalBudget > BigDecimal.ZERO

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

    // Home's frosted-glass card, with Cashiro's faint budget-colour border.
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        blurEffects = blurEffects,
        hazeState = hazeState,
        // Cashiro BudgetCard: 1dp border at 10% of the budget colour.
        rimColor = budgetColor.copy(alpha = 0.1f),
        // The wash is painted by the column below so it covers the whole card.
        contentPadding = Dimensions.Padding.none
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .budgetColorWash(budgetColor)
                .padding(horizontal = Spacing.md + Spacing.xs, vertical = Spacing.md)
        ) {
            BudgetCardTitle(
                name = budget.name,
                markerColor = barColor,
                modifier = Modifier.heightIn(min = Dimensions.Icon.large),
            ) {
                if (hasLimit) {
                    Text(
                        text = stringResource(R.string.budget_card_percent, pctUsed.toInt()),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(start = Spacing.sm)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            if (hasLimit) {
                BudgetHeroFigures(groupSpending = groupSpending, currency = currency)
                Spacer(modifier = Modifier.height(Spacing.md))
                BudgetProgressTrack(progress = animatedProgressState, color = barColor)
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = budgetRenewalText(groupSpending),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                BudgetCardCaption(text = stringResource(R.string.budgets_card_spent_label))
                Text(
                    text = CurrencyFormatter.formatCurrency(groupSpending.totalActual, currency),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.budget_card_tracking_all),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Small pill that names the budget's cadence at a glance: Weekly / Monthly /
 * One-time. Colour-coded so a user can spot which type of budget they're
 * looking at without reading the subtitle.
 */
@Composable
fun CadencePill(periodType: BudgetPeriodType) {
    val (label, bg, fg) = when (periodType) {
        BudgetPeriodType.WEEKLY -> Triple(
            stringResource(R.string.budget_cadence_weekly),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        BudgetPeriodType.MONTHLY -> Triple(
            stringResource(R.string.budget_cadence_monthly),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        BudgetPeriodType.CUSTOM -> Triple(
            stringResource(R.string.budget_cadence_one_time),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        modifier = Modifier
            .background(color = bg, shape = RoundedCornerShape(50))
            .padding(horizontal = Spacing.sm, vertical = 2.dp)
    )
}
