package com.pennywiseai.tracker.ui.screens.analytics

import androidx.compose.ui.res.pluralStringResource
import com.pennywiseai.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.pennywiseai.tracker.ui.components.CategoryIcon
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Receipt1
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.util.Locale

/** How strongly the top category's colour washes its chip. */
private const val CATEGORY_CHIP_ALPHA = 0.2f

/**
 * The Analytics headline card, laid out like Cashiro's: a small "TOTAL" label
 * over the big figure with the transaction count as a tonal pill beside it,
 * a divider, then the daily average on the left and the top category (with its
 * share of the total) on the right.
 *
 * [totalAmount], [averageAmount] and [currency] describe one currency — the
 * caller has already filtered or converted to it.
 */
@Composable
fun AnalyticsSummaryCard(
    totalAmount: BigDecimal,
    transactionCount: Int,
    averageAmount: BigDecimal,
    topCategory: String?,
    topCategoryPercentage: Float,
    currency: String,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Animate alpha on load: 0 → 1
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val loadAlpha by animateFloatAsState(
        targetValue = if (visible && !isLoading) 1f else if (isLoading) 0.5f else 0f,
        animationSpec = tween(500),
        label = "summary_alpha"
    )

    PennyWiseCardV2(
        modifier = modifier.fillMaxWidth(),
        contentPadding = Dimensions.Padding.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(loadAlpha)
        ) {
            // Top row: total on the left, transaction-count pill on the right,
            // sitting level with the figure rather than with the label.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.analytics_summary_total),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    val formattedTotal = CurrencyFormatter.formatCurrency(totalAmount, currency)
                    // The pill takes a fixed share of the row, so a long figure
                    // steps down a size rather than being cut off.
                    val totalStyle = when {
                        formattedTotal.length > 13 -> MaterialTheme.typography.headlineSmall
                        formattedTotal.length > 10 -> MaterialTheme.typography.headlineMedium
                        else -> MaterialTheme.typography.headlineLarge
                    }
                    Text(
                        text = formattedTotal,
                        style = totalStyle,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(start = Spacing.sm)
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(horizontal = Spacing.smd, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        imageVector = Iconax.Receipt1,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.medium),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = pluralStringResource(R.plurals.analytics_summary_txns, transactionCount, transactionCount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            HorizontalDivider(
                thickness = Dimensions.Component.dividerThickness,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

            // Bottom row: average per day on the left, top category on the right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = stringResource(R.string.analytics_summary_average),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (transactionCount > 0) {
                                CurrencyFormatter.formatCurrency(averageAmount, currency)
                            } else {
                                CurrencyFormatter.formatCurrency(BigDecimal.ZERO, currency)
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = stringResource(R.string.analytics_summary_per_day),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(bottom = Spacing.xs)
                        )
                    }
                }

                if (topCategory != null && topCategoryPercentage > 0) {
                    val categoryColor = CategoryMapping.colorFor(topCategory)

                    Column(
                        modifier = Modifier
                            .padding(start = Spacing.md)
                            .weight(1f, fill = false),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = stringResource(
                                R.string.analytics_summary_percent_of_total,
                                topCategoryPercentage.toInt()
                            ).uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(
                                end = Spacing.sm,
                                bottom = Spacing.xs
                            )
                        )
                        Row(
                            modifier = Modifier
                                .background(
                                    color = categoryColor.copy(alpha = CATEGORY_CHIP_ALPHA),
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            CategoryIcon(
                                category = topCategory,
                                size = Dimensions.Icon.small,
                                tint = categoryColor
                            )
                            Text(
                                text = topCategory,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }
            }
        }
    }
}
