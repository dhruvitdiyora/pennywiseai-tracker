package com.pennywiseai.tracker.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.cards.glassFill
import com.pennywiseai.tracker.ui.components.cards.glassRim
import com.pennywiseai.tracker.ui.components.cards.glassSheen
import com.pennywiseai.tracker.ui.components.cards.glassSurface
import com.pennywiseai.tracker.ui.components.cards.isGlassDark
import com.pennywiseai.tracker.ui.theme.Dimensions
import kotlinx.coroutines.launch

/*
 * Shared glass chrome — the Cashiro look for modal surfaces and small controls.
 * Everything here is built from the GlassCard recipe (fill + sheen + rim) so a
 * sheet, a dialog and a card read as the same material.
 */

/**
 * The app's bottom sheet: a [ModalBottomSheet] whose container is frosted glass
 * (near-opaque [tint] fill, top sheen, rim catching the rounded top edge) with
 * the standard drag handle.
 *
 * A modal sheet lives in its own window, so Haze can't blur the page behind
 * it; the fill uses [Dimensions.Glass.fillAlphaSheet] instead of a live blur.
 * The rim and sheen are drawn inside the sheet's surface, so they follow it
 * through drag and predictive-back transforms.
 *
 * Parameters mirror [ModalBottomSheet]; prefer this over calling it directly.
 *
 * @param tint the fill's theme role (Cashiro sheets sit on `surfaceContainerLow`).
 * @param showDragHandle whether to draw the handle. The handle keeps the
 *   accessibility actions (dismiss / expand / collapse) Material's handle has.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PennyWiseBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    showDragHandle: Boolean = true,
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val fill = glassFill(tint, blurLive = false, solidFillAlpha = Dimensions.Glass.fillAlphaSheet)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        sheetGesturesEnabled = sheetGesturesEnabled,
        shape = shape,
        containerColor = fill,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        scrimColor = scrimColor,
        // The handle is drawn below, inside the glass layer, so the sheen runs
        // under it rather than starting beneath it.
        dragHandle = null,
        // Insets are applied inside the glass layer so the rim stays on the
        // sheet's real top edge whatever the insets are.
        contentWindowInsets = { WindowInsets(0) },
        properties = properties,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(glassSheen(), shape)
                .border(glassRim(), shape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(contentWindowInsets())
            ) {
                if (showDragHandle) {
                    GlassSheetHandle(
                        sheetState = sheetState,
                        onDismissRequest = onDismissRequest,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                content()
            }
        }
    }
}

/** Material's drag handle, with the accessibility actions Material attaches to its own slot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassSheetHandle(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val dismissLabel = stringResource(R.string.glass_sheet_dismiss)
    val expandLabel = stringResource(R.string.glass_sheet_expand)
    val collapseLabel = stringResource(R.string.glass_sheet_collapse)
    Box(
        modifier = modifier.semantics(mergeDescendants = true) {
            dismiss(dismissLabel) {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) onDismissRequest()
                }
                true
            }
            if (sheetState.currentValue == SheetValue.PartiallyExpanded) {
                expand(expandLabel) {
                    scope.launch { sheetState.expand() }
                    true
                }
            } else if (sheetState.hasPartiallyExpandedState) {
                collapse(collapseLabel) {
                    scope.launch { sheetState.partialExpand() }
                    true
                }
            }
        }
    ) {
        BottomSheetDefaults.DragHandle()
    }
}

/**
 * The app's alert dialog: Material's [AlertDialog] on a glass surface
 * (Cashiro's look). Same slots as [AlertDialog]. For Cashiro's split
 * Cancel/Confirm buttons, put a [ConnectedButtonPair] in [confirmButton] and
 * leave [dismissButton] null.
 *
 * Like sheets, dialogs get their own window, so the glass is near-opaque
 * ([Dimensions.Glass.fillAlphaSheet]) rather than blurred.
 */
@Composable
fun PennyWiseAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    properties: DialogProperties = DialogProperties(),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.glassSurface(
            shape = shape,
            blurEffects = false,
            tint = tint,
            solidFillAlpha = Dimensions.Glass.fillAlphaSheet,
        ),
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        properties = properties,
    )
}

/**
 * Cashiro's dialog footer: two full-width buttons joined in the middle — pill
 * outer corners, near-square inner corners, a hairline gap. The dismiss half
 * is a quiet glass tint; the confirm half is `primary`, or `errorContainer`
 * when [destructive].
 */
@Composable
fun ConnectedButtonPair(
    dismissLabel: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
) {
    val outer = Dimensions.Glass.connectedButtonOuterRadius
    val inner = Dimensions.CornerRadius.small
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Glass.connectedButtonGap),
    ) {
        Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(
                topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(
                    alpha = Dimensions.Glass.fillAlphaControl
                ),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = dismissLabel,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            shape = RoundedCornerShape(
                topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer
            ),
            colors = if (destructive) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = confirmLabel,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Cashiro's segmented switcher (its `GenericTypeSwitcher`): a glass track with
 * a sliding raised indicator under the selected option. Use for 2–4 mutually
 * exclusive views (Expense / Income, Monthly / Yearly).
 *
 * Each option is a selectable tab for accessibility; the row mirrors in RTL.
 */
@Composable
fun PennyWiseSegmentedSwitcher(
    options: List<String>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    val trackShape = MaterialTheme.shapes.large
    val indicatorShape = MaterialTheme.shapes.medium
    val dark = isGlassDark()
    val indicatorColor = if (dark) MaterialTheme.colorScheme.surfaceContainerHighest
    else MaterialTheme.colorScheme.surfaceContainerLowest

    BoxWithConstraints(
        modifier = modifier
            .height(Dimensions.Glass.switcherHeight)
            .glassSurface(
                shape = trackShape,
                blurEffects = false,
                tint = MaterialTheme.colorScheme.surfaceVariant,
                solidFillAlpha = Dimensions.Glass.fillAlphaControl,
            )
            .padding(Dimensions.Glass.switcherInset)
    ) {
        val segmentWidth = maxWidth / options.size
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex.coerceIn(0, options.lastIndex),
            animationSpec = tween(durationMillis = Dimensions.Animation.medium),
            label = "switcherIndicator",
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .glassSurface(
                    shape = indicatorShape,
                    blurEffects = false,
                    tint = indicatorColor,
                    solidFillAlpha = 1f,
                )
        )
        Row(modifier = Modifier.fillMaxSize().selectableGroup()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onIndexChange(index) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
