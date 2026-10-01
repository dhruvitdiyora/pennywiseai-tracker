package com.pennywiseai.tracker.presentation.budgetgroups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pennywiseai.tracker.ui.theme.Dimensions

/**
 * Look shared by the budget detail hero and the budget history summary: the
 * budget's own colour as a soft wash with a faint rim, the same treatment the
 * Budgets overview cards wear so the three screens read as one family.
 */

/** How strongly the budget's colour rims the card. */
internal const val BUDGET_DETAIL_RIM_ALPHA = 0.12f

/** The 1dp rim drawn around a tinted budget card. */
internal fun budgetRim(color: Color): BorderStroke = BorderStroke(
    width = Dimensions.Component.dividerThickness,
    color = color.copy(alpha = BUDGET_DETAIL_RIM_ALPHA),
)

/**
 * Washes a few soft blobs of [color] over the card, like Cashiro's gradient
 * mesh but static: nothing animates, so it costs no frames and renders
 * identically in screenshots.
 */
internal fun Modifier.budgetColorWash(color: Color): Modifier = drawBehind {
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

/**
 * A small round tonal button for a budget card header (history). The drawn
 * disc is [Dimensions.Icon.large]; the touch target is the full
 * [Dimensions.Component.minTouchTarget].
 */
@Composable
internal fun BudgetHeroIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(Dimensions.Component.minTouchTarget),
    ) {
        Box(
            modifier = Modifier
                .size(Dimensions.Icon.large)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = Dimensions.Alpha.divider),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(Dimensions.Icon.small),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The round tonal action button for the large top bar's trailing slot (the
 * counterpart of `TonalNavigationButton` on the leading side), inset from the
 * screen edge by the standard gutter.
 */
@Composable
internal fun BudgetTonalActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.padding(end = Dimensions.Padding.content),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(Dimensions.Icon.inline),
        )
    }
}
