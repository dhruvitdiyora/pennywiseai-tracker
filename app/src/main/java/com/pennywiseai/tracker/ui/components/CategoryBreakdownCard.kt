package com.pennywiseai.tracker.ui.components

import com.pennywiseai.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.screens.analytics.CategoryData
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter

@Composable
fun CategoryBreakdownCard(
    categories: List<CategoryData>,
    currency: String,
    modifier: Modifier = Modifier,
    onCategoryClick: (CategoryData) -> Unit = {}
) {
    val maxAmount = categories.map { it.amount }.maxOrNull() ?: java.math.BigDecimal.ZERO

    // No inner "Spending by Category" title: the section header above the
    // card already names it, and Cashiro's list sits straight in the card.
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = Spacing.sm
    ) {
        ExpandableList(
            items = categories,
            visibleItemCount = 5
        ) { category ->
            CategoryBar(
                category = category,
                maxAmount = maxAmount,
                currency = currency,
                onClick = { onCategoryClick(category) }
            )
        }
    }
}

@Composable
private fun CategoryBar(
    category: CategoryData,
    maxAmount: java.math.BigDecimal,
    currency: String,
    onClick: () -> Unit = {}
) {
    val targetFraction = if (maxAmount > java.math.BigDecimal.ZERO) {
        (category.amount.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Animated progress bar fill
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val animatedFraction by animateFloatAsState(
        targetValue = if (visible) targetFraction else 0f,
        animationSpec = tween(800),
        label = "category_bar_${category.name}"
    )

    // Category color: user's assigned color, else built-in palette, else gray (#586)
    val categoryColor = CategoryMapping.colorFor(category.name, category.color)

    // Cashiro-style row: a rounded-square tonal icon, the name over its share,
    // the amount on the right, then a bar on a track tinted with the category's
    // own colour (so it reads in both themes without a neutral grey strip).
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(horizontal = Spacing.sm, vertical = Spacing.smd),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Dimensions.Icon.avatar)
                    .clip(MaterialTheme.shapes.medium)
                    .background(categoryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    category = category.name,
                    size = Dimensions.Icon.medium,
                    tint = categoryColor
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (category.amount.signum() > 0 && category.percentage < 1f) {
                        stringResource(R.string.analytics_percent_below_one)
                    } else {
                        stringResource(R.string.analytics_percent, category.percentage.toInt())
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = CurrencyFormatter.formatCurrency(category.amount, currency),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.sm)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(categoryColor)
            )
        }
    }
}
