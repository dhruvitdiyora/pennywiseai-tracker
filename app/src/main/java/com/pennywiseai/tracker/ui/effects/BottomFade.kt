package com.pennywiseai.tracker.ui.effects

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

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
 * Fades the trailing edge of a horizontally scrolling row, so a chip that runs past the
 * screen edge reads as "there is more this way" instead of as a clipped, broken chip.
 *
 * [atStart] fades the leading edge too — pass `true` once the row has been scrolled, so the
 * affordance points both ways.
 */
fun Modifier.horizontalScrollFade(
    fadeWidthPercentage: Float = 0.06f,
    atStart: Boolean = false
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.horizontalGradient(
                0f to if (atStart) Color.Transparent else Color.Black,
                fadeWidthPercentage to Color.Black,
                (1f - fadeWidthPercentage) to Color.Black,
                1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }
