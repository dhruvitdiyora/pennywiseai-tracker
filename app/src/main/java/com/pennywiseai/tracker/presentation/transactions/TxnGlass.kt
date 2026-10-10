package com.pennywiseai.tracker.presentation.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.core.graphics.toColorInt
import com.pennywiseai.tracker.ui.components.cards.glassSurface
import com.pennywiseai.tracker.ui.effects.LocalBlurEffects
import com.pennywiseai.tracker.ui.icons.BrandIcons
import com.pennywiseai.tracker.ui.theme.Dimensions

/*
 * Glass helpers for the transaction / add / share / statement screens.
 *
 * Every surface here takes the app-wide frosted-glass recipe from
 * `GlassCard.kt` (Cashiro-matching solid/blur fill, no rim); these wrappers only pin the arguments the
 * screens in this area share, so a sheet, a field and a chip can't drift apart.
 * Surfaces that live inside a screen's `hazeSource` can't blur themselves, so
 * they are solid glass (no `hazeState`) — the recipe still follows the user's
 * blur setting through [LocalBlurEffects].
 */

/** The glass recipe on any surface in this area, with the screen's blur setting. */
@Composable
internal fun Modifier.txnGlass(
    shape: Shape,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    rimColor: Color? = null,
): Modifier = glassSurface(
    shape = shape,
    blurEffects = LocalBlurEffects.current,
    tint = tint,
    rimColor = rimColor,
)

/**
 * A modal bottom sheet in the glass material. The sheet's own container is
 * transparent; the solid fill is drawn on a column inside it
 * that also hosts the drag handle, so the glass moves with the sheet while it
 * is dragged (a modifier on the sheet itself would sit outside its drag
 * offset). The column runs under the navigation bar and pads its content
 * above it, so no strip of scrim shows below the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TxnGlassSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    showDragHandle: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = BottomSheetDefaults.ExpandedShape
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        shape = shape,
        // Cashiro's sheets are solid `surface` — nothing ghosts through.
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = Dimensions.Elevation.none,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            if (showDragHandle) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BottomSheetDefaults.DragHandle()
                }
            }
            content()
        }
    }
}

/**
 * The bank's brand colour, when we know it — used to tint an account chip or
 * card. `null` for an unknown bank, so callers fall back to a neutral role.
 */
internal fun bankBrandColor(bankName: String?): Color? {
    if (bankName.isNullOrBlank()) return null
    val hex = BrandIcons.getBrandColor(bankName) ?: return null
    return runCatching { Color(hex.toColorInt()) }.getOrNull()
}
