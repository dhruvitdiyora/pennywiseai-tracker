package com.pennywiseai.tracker.ui.screens.settings

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.pennywiseai.tracker.ui.components.cards.GroupedColumn
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.RowLabels
import com.pennywiseai.tracker.ui.components.cards.glassSurface
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.effects.BlurredAnimatedVisibility
import com.pennywiseai.tracker.ui.effects.LocalBlurEffects
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/*
 * Glass adapters for the Settings / Profile / People / onboarding area.
 *
 * Cashiro's frosted-glass material is the default container, but the shared
 * GroupedRow / GroupedColumn primitives paint an opaque tonal Surface. These
 * thin wrappers keep those primitives (same layout, padding, tap target and
 * corner logic) and swap the opaque fill for the shared glass recipe from
 * GlassCard.kt: Cashiro's solid theme-role fill, no rim or sheen.
 *
 * Rows sit *inside* the screen's hazeSource, so there is nothing behind them to
 * blur; they render as solid glass (Dimensions.Glass.fillAlphaSolid). The
 * blurred surfaces in this area are the top bars (CustomTitleTopAppBar) and
 * anything floating over content.
 */

/** The glass recipe clipped to a grouped-list [position]'s corners. */
@Composable
internal fun Modifier.glassRow(
    position: ListItemPosition,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    rimColor: Color? = null,
): Modifier = glassSurface(
    shape = position.toShape(),
    blurEffects = LocalBlurEffects.current,
    tint = tint,
    rimColor = rimColor,
)

/** The glass recipe for an arbitrary [shape] (cards, tiles, pills) — solid glass. */
@Composable
internal fun Modifier.glassPanel(
    shape: Shape,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    solidFillAlpha: Float = Dimensions.Glass.fillAlphaSolid,
    rimColor: Color? = null,
): Modifier = glassSurface(
    shape = shape,
    blurEffects = LocalBlurEffects.current,
    tint = tint,
    solidFillAlpha = solidFillAlpha,
    rimColor = rimColor,
)

/**
 * Glass bottom-sheet surface: the sheet's top corners, solid. Pass `containerColor = Color.Transparent` to the ModalBottomSheet and
 * this as its modifier. The fill stays near-solid — a sheet floats over a
 * scrim, and text must never sit on a see-through backdrop there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Modifier.glassSheet(
    tint: Color = MaterialTheme.colorScheme.surfaceContainerLow,
): Modifier = glassSurface(
    shape = BottomSheetDefaults.ExpandedShape,
    blurEffects = LocalBlurEffects.current,
    tint = tint,
    solidFillAlpha = SheetFillAlpha,
)

/** Sheets and dialogs sit in their own window, so they are solid. */
private const val SheetFillAlpha = Dimensions.Glass.fillAlphaSheet

/** [GroupedRow] in the glass material. Same parameters; [tint] replaces `containerColor`. */
@Composable
internal fun GlassGroupedRow(
    position: ListItemPosition,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    rimColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = Spacing.md,
        vertical = Dimensions.Padding.listRowVertical
    ),
    minHeight: Dp = Dimensions.Component.minTouchTarget,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(Spacing.md),
    content: @Composable RowScope.() -> Unit
) {
    GroupedRow(
        position = position,
        modifier = modifier.glassRow(position, tint, rimColor),
        onClick = onClick,
        enabled = enabled,
        containerColor = Color.Transparent,
        contentPadding = contentPadding,
        minHeight = minHeight,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement,
        content = content,
    )
}

/** [GroupedColumn] in the glass material. Same parameters; [tint] replaces `containerColor`. */
@Composable
internal fun GlassGroupedColumn(
    position: ListItemPosition,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    rimColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = Spacing.md,
        vertical = Dimensions.Padding.listRowVertical
    ),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Spacing.smd),
    content: @Composable ColumnScope.() -> Unit
) {
    GroupedColumn(
        position = position,
        modifier = modifier.glassRow(position, tint, rimColor),
        containerColor = Color.Transparent,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/**
 * The shared PreferenceSwitch row, in glass. Mirrors
 * `ui/components/PreferenceSwitch.kt` (which paints an opaque row) so the
 * switch rows on Appearance match the glass nav rows around them.
 */
@Composable
internal fun GlassPreferenceSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    position: ListItemPosition,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    subtitle: String = "",
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    BlurredAnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        GlassGroupedRow(
            position = position,
            modifier = modifier,
            onClick = { onCheckedChange(!checked) },
        ) {
            if (leadingIcon != null) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.secondary
                ) { leadingIcon() }
            }
            RowLabels(title = title, subtitle = subtitle.ifBlank { null })
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = {
                    Icon(
                        if (checked) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                },
            )
        }
    }
}

/**
 * Glass dialog surface. Pass `containerColor = Color.Transparent` to the
 * AlertDialog and this as its modifier, with the dialog's [shape]. Near-solid,
 * like a sheet: a dialog floats over a scrim.
 */
@Composable
internal fun Modifier.glassDialog(
    shape: Shape = AlertDialogDefaults.shape,
): Modifier = glassSurface(
    shape = shape,
    blurEffects = LocalBlurEffects.current,
    tint = AlertDialogDefaults.containerColor,
    solidFillAlpha = SheetFillAlpha,
)
