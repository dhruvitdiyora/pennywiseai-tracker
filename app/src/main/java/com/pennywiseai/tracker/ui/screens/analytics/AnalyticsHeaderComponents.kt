package com.pennywiseai.tracker.ui.screens.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.chipLabel
import com.pennywiseai.tracker.presentation.common.label
import com.pennywiseai.tracker.ui.components.PeriodFilterChip
import com.pennywiseai.tracker.ui.icons.iconax.Chart2
import com.pennywiseai.tracker.ui.icons.iconax.Grid2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.StatusUp
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/*
 * Header pieces for the Analytics screen, in Cashiro's layout: scrolling period
 * chips, a "More Filters" row that folds the remaining filters away, and accent
 * section headings. The filtering itself lives in AnalyticsScreen and the
 * ViewModel; nothing here changes what is filtered.
 */

/**
 * The period row ("This Month", "Last Month"...). Same chips and selection rule
 * as the Transactions screen: "Custom" only reads as selected once a range
 * exists. The row bleeds to the screen edge so the chips scroll under the
 * gutter, but the first one lines up with the content.
 */
@Composable
internal fun AnalyticsPeriodChips(
    selectedPeriod: TimePeriod,
    customDateRangeSelected: Boolean,
    customRangeLabel: String?,
    budgetCycleStartDay: Int,
    onPeriodSelected: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(TimePeriod.entries.toList(), key = { it.name }) { period ->
            PeriodFilterChip(
                selected = if (period == TimePeriod.CUSTOM) {
                    selectedPeriod == period && customDateRangeSelected
                } else {
                    selectedPeriod == period
                },
                // Budget-cycle-aware: a cycle that does not start on the 1st
                // shows its resolved dates, not "This Month".
                label = period.chipLabel(
                    budgetCycleStartDay,
                    customRangeLabel,
                    period.label,
                ),
                onClick = { onPeriodSelected(period) },
            )
        }
    }
}

/**
 * "More Filters" row with a filter glyph and a chevron. Tapping it folds the
 * [content] (the profile, type, currency, category and account chips) in and
 * out. [activeCount] is how many of those are narrowed, so a hidden filter
 * still shows.
 */
@Composable
internal fun AnalyticsMoreFilters(
    expanded: Boolean,
    activeCount: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AnalyticsMoreFiltersRow(
            expanded = expanded,
            activeCount = activeCount,
            onClick = onToggle,
            modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column(modifier = Modifier.padding(top = Spacing.xs)) { content() }
        }
    }
}

@Composable
private fun AnalyticsMoreFiltersRow(
    expanded: Boolean,
    activeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = activeCount > 0
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "analytics_more_filters_chevron",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.inline),
            tint = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        Text(
            text = if (active) {
                pluralStringResource(R.plurals.filter_row_more_active, activeCount, activeCount)
            } else {
                stringResource(R.string.filter_row_more)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = stringResource(
                if (expanded) R.string.filter_row_collapse else R.string.filter_row_expand
            ),
            modifier = Modifier
                .size(Dimensions.Icon.medium)
                .rotate(chevronRotation),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A section heading in the accent colour, like Cashiro's. The accent marks the
 * heading as the anchor for the block under it; [action] sits on the right and
 * may be a pill or an icon button. The title is inset a little so it lines up
 * with the text inside the cards below rather than with their edge.
 */
@Composable
internal fun AnalyticsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    topSpacing: Dp = Spacing.sm,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topSpacing)
            .defaultMinSize(minHeight = Dimensions.Component.iconButton)
            .semantics { heading() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.sm),
        )
        if (action != null) {
            Row(
                modifier = Modifier.padding(start = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) { action() }
        }
    }
}

/**
 * The small pill on the right of the "Trends" heading showing the current
 * chart type ("Line Chart"); tapping it opens the chart-type list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnalyticsChartTypePill(
    chartType: ChartType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        // Keeps the drawn pill small while the touch target reaches 48dp.
        modifier = modifier.minimumInteractiveComponentSize(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.smd, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(
                imageVector = chartType.icon(),
                contentDescription = null,
                modifier = Modifier.size(Dimensions.Icon.small),
            )
            Text(
                text = chartType.fullLabel(),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

/** The Iconsax glyph for a chart mode, used by the pill and the type list. */
internal fun ChartType.icon(): ImageVector = when (this) {
    ChartType.LINE -> Iconax.StatusUp
    ChartType.BAR -> Iconax.Chart2
    ChartType.HEATMAP -> Iconax.Grid2
}

/** "Line Chart" / "Bar Chart" / "Heatmap". */
@Composable
internal fun ChartType.fullLabel(): String = when (this) {
    ChartType.LINE -> stringResource(R.string.analytics_chart_line_full)
    ChartType.BAR -> stringResource(R.string.analytics_chart_bar_full)
    ChartType.HEATMAP -> stringResource(R.string.analytics_chart_heatmap)
}
