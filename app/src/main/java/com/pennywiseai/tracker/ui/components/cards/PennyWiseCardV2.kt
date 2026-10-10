package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.pennywiseai.tracker.ui.theme.Dimensions

/**
 * The app's standard card container.
 *
 * One card style, everywhere: `shapes.large` corners, `surfaceContainerLow`
 * fill, no elevation — finished in the frosted-glass material by default
 * ([glass] = true): the fill goes slightly translucent
 * ([Dimensions.Glass.fillAlphaSolid]), a soft top sheen sits under the
 * content, and the diagonal glass rim ([glassRim]) replaces the old dark-only
 * hairline, so the card still separates from AMOLED black. With [glass] off it
 * is the flat tonal card (hairline in dark mode only).
 *
 * The card has no Haze blur of its own — for a card over a banner/cover that
 * should frost what's behind it, use [GlassCard] with a `hazeState`.
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
    colors: CardColors = CardDefaults.cardColors(
        containerColor = containerColor ?: MaterialTheme.colorScheme.surfaceContainerLow
    ),
    elevation: CardElevation = CardDefaults.cardElevation(
        defaultElevation = Dimensions.Elevation.card
    ),
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
    /** Frosted-glass finish (translucent fill, sheen, rim). On by default. */
    glass: Boolean = true,
    /** Glass fill alpha; modal hosts (dialogs) want [Dimensions.Glass.fillAlphaSheet]. */
    glassFillAlpha: Float = Dimensions.Glass.fillAlphaSolid,
    content: @Composable ColumnScope.() -> Unit
) {
    if (glass) {
        GlassCardV2(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            onClick = onClick,
            onLongClick = onLongClick,
            contentPadding = contentPadding,
            fillAlpha = glassFillAlpha,
            content = content
        )
        return
    }
    val effectiveBorder = border ?: if (isSystemInDarkTheme()) {
        BorderStroke(
            width = Dimensions.Component.hairline,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
        )
    } else {
        null
    }

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
                border = effectiveBorder
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
                border = effectiveBorder
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
                border = effectiveBorder
            ) {
                Column(modifier = Modifier.padding(contentPadding)) { content() }
            }
        }
    }
}

/** [PennyWiseCardV2]'s glass branch: same click semantics, glass fill/sheen/rim. */
@Composable
private fun GlassCardV2(
    modifier: Modifier,
    shape: CornerBasedShape,
    colors: CardColors,
    elevation: CardElevation,
    border: BorderStroke?,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    contentPadding: Dp,
    fillAlpha: Float,
    content: @Composable ColumnScope.() -> Unit
) {
    // Keep an explicitly transparent container transparent (e.g. a card used
    // purely as a ripple surface); otherwise frost the requested colour.
    val base = colors.containerColor
    val glassColors = colors.copy(
        containerColor = if (base.alpha == 0f) base else glassFill(base, blurLive = false, solidFillAlpha = fillAlpha * base.alpha)
    )
    val effectiveBorder = border ?: glassRim()
    val sheen = glassSheen()
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(sheen)
                .padding(contentPadding)
        ) { content() }
    }
    when {
        onLongClick != null -> {
            val tap = requireNotNull(onClick) {
                "PennyWiseCardV2: onLongClick requires onClick to also be non-null."
            }
            Card(
                modifier = modifier.clip(shape).combinedClickable(onClick = tap, onLongClick = onLongClick),
                colors = glassColors,
                shape = shape,
                elevation = elevation,
                border = effectiveBorder,
                content = inner
            )
        }
        onClick != null -> Card(
            modifier = modifier,
            onClick = onClick,
            colors = glassColors,
            shape = shape,
            elevation = elevation,
            border = effectiveBorder,
            content = inner
        )
        else -> Card(
            modifier = modifier,
            colors = glassColors,
            shape = shape,
            elevation = elevation,
            border = effectiveBorder,
            content = inner
        )
    }
}
