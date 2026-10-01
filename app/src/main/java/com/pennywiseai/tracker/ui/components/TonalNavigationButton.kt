package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.pennywiseai.tracker.ui.icons.iconax.ArrowLeft02
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.Dimensions

/**
 * The round, tonal navigation button used in the leading slot of
 * [CustomTitleTopAppBar] (back arrow by default, close in selection modes).
 *
 * The tonal `surfaceContainer` disc keeps the control findable over a blurred
 * list without competing with the title. [contentDescription] is required
 * because the button has no visible text. The icon is mirrored in RTL (the
 * Iconax arrow has no auto-mirrored variant); pass [mirrorInRtl] = false for
 * an icon that must not flip.
 */
@Composable
fun TonalNavigationButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Iconax.ArrowLeft02,
    mirrorInRtl: Boolean = true,
) {
    val mirrored = mirrorInRtl && LocalLayoutDirection.current == LayoutDirection.Rtl
    IconButton(
        onClick = onClick,
        modifier = modifier.padding(start = Dimensions.Padding.content),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(Dimensions.Icon.inline)
                .then(if (mirrored) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier),
        )
    }
}
