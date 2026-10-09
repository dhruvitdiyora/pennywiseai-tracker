package com.pennywiseai.tracker.presentation.budgetgroups

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
import androidx.compose.ui.graphics.vector.ImageVector
import com.pennywiseai.tracker.ui.theme.Dimensions

/*
 * Budget-card chrome shared by the overview, detail and history screens. The
 * colour wash, rim and figures live with the card family in
 * `ui/components/cards/BudgetCard.kt`.
 */

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
