package com.pennywiseai.tracker.ui.screens.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.theme.Spacing

/*
 * Small pieces shared by the "utility" screens that sit below Settings (Rules,
 * Create Rule, Unrecognized SMS, Exchange Rates, Import Statement), so the five
 * read as one Cashiro-style family with the rest of the app instead of each
 * re-assembling its own info card.
 */

/**
 * The extra-large primary-container card that opens a utility screen: a primary
 * icon tile, a title and one explanation, with optional [content] (a filter chip,
 * a count) underneath. It is the same card the profile header and Data & privacy
 * use, so the accent reads the same way across the app.
 */
@Composable
internal fun UtilityHeroCard(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tint = scheme.primaryContainer,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.smd)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(
                    icon = icon,
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onPrimaryContainer,
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onPrimaryContainer,
                    )
                }
            }
            if (content != null) content()
        }
    }
}
