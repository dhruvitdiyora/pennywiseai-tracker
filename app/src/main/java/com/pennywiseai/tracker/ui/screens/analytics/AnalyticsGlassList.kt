package com.pennywiseai.tracker.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.expense
import com.pennywiseai.tracker.ui.theme.income
import com.pennywiseai.tracker.ui.theme.investment

/**
 * Cashiro's grouped glass list for the Analytics breakdowns (merchants,
 * accounts): sibling rows are separate [GlassCard]s joined by the grouped-list
 * gutter, the block's outer corners rounded via [ListItemPosition.toShape].
 * Shows [visibleItemCount] rows with a "View all" toggle beneath.
 */
@Composable
internal fun <T> AnalyticsGlassList(
    items: List<T>,
    modifier: Modifier = Modifier,
    visibleItemCount: Int = 3,
    itemContent: @Composable (item: T, position: ListItemPosition) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shown = if (expanded) items else items.take(visibleItemCount)

    Column(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)) {
            shown.forEachIndexed { index, item ->
                itemContent(item, ListItemPosition.from(index, shown.size))
            }
        }
        if (items.size > visibleItemCount) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (expanded) {
                        stringResource(R.string.expandable_list_view_less)
                    } else {
                        stringResource(R.string.expandable_list_view_all_more, items.size - visibleItemCount)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * One glass row of an [AnalyticsGlassList]: leading avatar, title over a
 * metadata line, and the (single-currency, already formatted) [amount] on the
 * right in [amountColor], with an optional [supporting] line (e.g. a share)
 * beneath it.
 */
@Composable
internal fun AnalyticsGlassRow(
    position: ListItemPosition,
    title: String,
    subtitle: String,
    amount: String,
    amountColor: Color,
    leadingContent: @Composable () -> Unit,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = position.toShape(),
        onClick = onClick,
        contentPadding = Dimensions.Padding.cardCompact,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Dimensions.Component.minTouchTarget),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingContent()
            Spacer(modifier = Modifier.width(Spacing.smd))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
            ) {
                Text(
                    text = title,
                    style = PennyWiseText.rowTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = PennyWiseText.metadata,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(Spacing.sm))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amount,
                    style = PennyWiseText.amountRow,
                    color = amountColor,
                    maxLines = 1
                )
                if (supporting != null) {
                    Text(
                        text = supporting,
                        style = PennyWiseText.metadata,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * The amount colour for the analytics type filter, matching the transaction
 * list: spending reads in the expense colour, income in the income colour.
 * Mixed or neutral views (all, transfers) stay on `onSurface`.
 */
@Composable
internal fun analyticsAmountColor(filter: TransactionTypeFilter): Color {
    val scheme = MaterialTheme.colorScheme
    return when (filter) {
        TransactionTypeFilter.EXPENSE, TransactionTypeFilter.CREDIT -> scheme.expense
        TransactionTypeFilter.INCOME -> scheme.income
        TransactionTypeFilter.INVESTMENT -> scheme.investment
        TransactionTypeFilter.ALL, TransactionTypeFilter.TRANSFER -> scheme.onSurface
    }
}
