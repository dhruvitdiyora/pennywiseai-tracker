package com.pennywiseai.tracker.ui.effects

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.pennywiseai.tracker.ui.theme.Spacing

fun Modifier.bottomFade(
    fadeHeightPercentage: Float = 0.3f
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Black,
                (1f - fadeHeightPercentage) to Color.Black,
                1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }

/**
 * Fades the trailing edge of a horizontally-clipped row instead of cutting it
 * hard. Mirrors [bottomFade] on the X axis — pair with `Modifier.clipToBounds()`
 * on the same row so overflowing content doesn't just fade, it's also clipped.
 */
fun Modifier.horizontalScrollFade(
    width: Dp = Spacing.lg
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fadeWidthPx = width.toPx()
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startX = size.width - fadeWidthPx,
                endX = size.width
            ),
            blendMode = BlendMode.DstIn
        )
    }
