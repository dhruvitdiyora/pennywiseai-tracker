package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import com.pennywiseai.tracker.ui.icons.IconProvider
import com.pennywiseai.tracker.ui.icons.IconResource
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/** Diameter of one tiled mark — big enough to read as a pattern, not as noise. */
private val TILE_ICON_SIZE = Dimensions.Icon.avatarLarge + Spacing.sm

/** The pattern is tilted so it reads as a texture rather than a spreadsheet grid. */
private const val TILE_ROTATION_DEGREES = -20f

/** Gap between marks, as a fraction of the mark size. */
private const val TILE_GAP_FRACTION = 0.4f

/**
 * Faint but visible — twice the quiet `decorativeWatermark` so the brand is
 * recognisable on a card, while foreground text still owns contrast (callers
 * lay a surface gradient over the lower half).
 */
private const val TILE_ALPHA = Dimensions.Alpha.decorativeWatermark * 2f

/**
 * A deliberately quiet, brand-derived watermark for identity cards: the
 * merchant / bank mark repeated on a tilted, brick-staggered grid, in the
 * manner of Cashiro's account cards.
 * Foreground content owns contrast and semantics; this layer is decorative.
 *
 * Static on purpose — Cashiro scrolls the pattern, but a perpetual animation
 * behind a screen that is always on display costs battery for no information.
 */
@Composable
fun TiledIconBackground(
    merchantName: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = TILE_ICON_SIZE,
    alpha: Float = TILE_ALPHA,
) {
    val icon = remember(merchantName) { IconProvider.getTransactionIcon(merchantName, null) }
    val painter: Painter? = when (icon) {
        is IconResource.DrawableResource -> painterResource(id = icon.resId)
        is IconResource.VectorIcon -> rememberVectorPainter(image = icon.icon)
        // A custom-category emoji has no vector form to tile; the card simply
        // goes without the watermark.
        is IconResource.Emoji -> null
    }
    val colorFilter = (icon as? IconResource.VectorIcon)?.let { ColorFilter.tint(it.tint) }

    if (painter != null) {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds()
                .clearAndSetSemantics { }
        ) {
            val sizePx = iconSize.toPx()
            val step = sizePx * (1f + TILE_GAP_FRACTION)

            // Fit the mark inside its square cell without distorting non-square logos.
            val intrinsic = painter.intrinsicSize
            val markSize = if (intrinsic.isSpecified && intrinsic.width > 0f && intrinsic.height > 0f) {
                val scale = minOf(sizePx / intrinsic.width, sizePx / intrinsic.height)
                Size(intrinsic.width * scale, intrinsic.height * scale)
            } else {
                Size(sizePx, sizePx)
            }
            val insetX = (sizePx - markSize.width) / 2f
            val insetY = (sizePx - markSize.height) / 2f

            // Over-draw by a couple of cells on every side so no corner is left
            // bare once the grid is rotated about the centre.
            val columns = (size.width / step).toInt() + 4
            val rows = (size.height / step).toInt() + 4

            rotate(TILE_ROTATION_DEGREES) {
                for (row in -2..rows) {
                    val stagger = if (row % 2 != 0) step / 2f else 0f
                    for (column in -2..columns) {
                        translate(
                            left = column * step + stagger + insetX,
                            top = row * step + insetY
                        ) {
                            with(painter) {
                                draw(size = markSize, alpha = alpha, colorFilter = colorFilter)
                            }
                        }
                    }
                }
            }
        }
    }
}
