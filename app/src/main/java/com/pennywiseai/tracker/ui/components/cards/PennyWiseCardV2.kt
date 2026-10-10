package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.pennywiseai.tracker.ui.theme.Dimensions

/**
 * The app's standard card — Cashiro's `CashiroCard`: a plain Material 3 card,
 * `shapes.large` corners, solid `surfaceContainerLow`, default elevation, no
 * border and no sheen. Haze glass belongs only on surfaces with a `hazeSource`
 * behind them; use [GlassCard] with a `hazeState` for those.
 *
 * Pass [contentPadding] rather than padding the content yourself, so the
 * ripple on a clickable card covers the whole surface.
 */
@Composable
fun PennyWiseCardV2(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = MaterialTheme.shapes.large,
    /**
     * Default-only convenience for callers that just want to swap the
     * container colour (e.g. a selected-state tint) without constructing a
     * full [CardColors]. Wired into the default value of [colors]; if a
     * caller passes [colors] explicitly, that wins and this value is unused.
     */
    containerColor: androidx.compose.ui.graphics.Color? = null,
    colors: CardColors = (containerColor ?: MaterialTheme.colorScheme.surfaceContainerLow).let { container ->
        CardDefaults.cardColors(
            containerColor = container,
            // Explicit so a transparent/translucent container still gets a
            // readable (theme-aware) content colour, never a stale dark one.
            contentColor = contentColorFor(container).takeOrElse { MaterialTheme.colorScheme.onSurface },
        )
    },
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    /**
     * Optional long-press handler. Routes the card through
     * [Modifier.combinedClickable] so tap and long-press resolve on the same
     * gesture surface — important when the card lives inside a scrolling
     * container or another drag-aware parent (e.g. SwipeToDismissBox) that
     * would otherwise race with a child pointerInput.
     *
     * **Requires [onClick]** to also be non-null. A long-press-only card
     * would still announce as a button to accessibility but no-op on tap;
     * we fail fast rather than ship that affordance.
     */
    onLongClick: (() -> Unit)? = null,
    contentPadding: Dp = Dimensions.Padding.card,
    /** Source-compat only: the card is always Cashiro's solid card now. */
    glass: Boolean = true,
    /** Source-compat only; the fill is solid. */
    glassFillAlpha: Float = Dimensions.Glass.fillAlphaSolid,
    content: @Composable ColumnScope.() -> Unit
) {
    when {
        onLongClick != null -> {
            // Combined click + long-click. Material's Card composable doesn't
            // accept onLongClick directly, so we wrap a non-clickable Card with
            // combinedClickable on the outer modifier. Require onClick here so
            // the card never advertises a button affordance whose tap is a
            // no-op (would mislead screen readers and touch users).
            val tap = requireNotNull(onClick) {
                "PennyWiseCardV2: onLongClick requires onClick to also be non-null."
            }
            Card(
                modifier = modifier.combinedClickable(
                    onClick = tap,
                    onLongClick = onLongClick
                ),
                colors = colors,
                shape = shape,
                elevation = elevation,
                border = border
            ) {
                Column(modifier = Modifier.padding(contentPadding)) { content() }
            }
        }
        onClick != null -> {
            Card(
                modifier = modifier,
                onClick = onClick,
                colors = colors,
                shape = shape,
                elevation = elevation,
                border = border
            ) {
                Column(modifier = Modifier.padding(contentPadding)) { content() }
            }
        }
        else -> {
            Card(
                modifier = modifier,
                colors = colors,
                shape = shape,
                elevation = elevation,
                border = border
            ) {
                Column(modifier = Modifier.padding(contentPadding)) { content() }
            }
        }
    }
}
