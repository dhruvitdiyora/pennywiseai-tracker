package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.pennywiseai.tracker.ui.effects.LocalBlurEffects
import com.pennywiseai.tracker.ui.theme.Dimensions
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/**
 * PennyWise's frosted-glass material — an exact port of Cashiro's (see its
 * `BalanceCard`): a plain Material 3 card whose container is
 * `surfaceContainerLow` at 50% alpha over a 20dp Haze blur of the
 * `hazeSource` behind it, or the same role fully **solid** when blur is off
 * or there is no backdrop. No rim, no sheen, no extra elevation — a card is
 * Cashiro's `Card` with default elevation.
 *
 * For a non-card surface (sheet, bar, pill) use [Modifier.glassSurface], which
 * applies the same recipe to any composable.
 *
 * @param shape corner shape; hero/summary cards are 24dp (the default),
 *   ordinary cards use `MaterialTheme.shapes.large`.
 * @param blurEffects the user's blur setting; defaults to [LocalBlurEffects].
 * @param hazeState the [HazeState] whose `hazeSource` sits *behind* this card
 *   (e.g. Home's banner). `null` = no blur, solid card.
 * @param tint the fill's theme role (e.g. `errorContainer` for a warning card).
 * @param solidFillAlpha fill alpha when the blur is off (1 = solid).
 * @param rimColor explicit border colour, drawn only when non-null. Cashiro
 *   draws none except its budget card (1dp, budget colour at 10%) and
 *   selection states; callers pass the final colour including alpha.
 * @param onClick makes the whole card clickable.
 * @param contentPadding inner padding.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(Dimensions.Glass.heroRadius),
    blurEffects: Boolean = LocalBlurEffects.current,
    hazeState: HazeState? = null,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    solidFillAlpha: Float = Dimensions.Glass.fillAlphaSolid,
    rimColor: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = Dimensions.Padding.card,
    content: @Composable ColumnScope.() -> Unit
) {
    val blurLive = blurEffects && hazeState != null
    val fill = glassFill(tint, blurLive, solidFillAlpha)
    // Explicit content colour: a translucent container can't be mapped back to
    // its on-role by contentColorFor, which left dark text on dark glass.
    val colors = CardDefaults.cardColors(
        containerColor = fill,
        contentColor = contentColorFor(tint).takeOrElse { MaterialTheme.colorScheme.onSurface },
    )
    val border = rimColor?.let { glassBorder(it) }
    val cardModifier = modifier.glassBlur(shape, if (blurLive) hazeState else null, tint)

    val inner: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            border = border,
            content = inner
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            border = border,
            content = inner
        )
    }
}

/** The glass fill colour for [tint]: 50% over a live blur, [solidFillAlpha] (solid) otherwise. */
fun glassFill(
    tint: Color,
    blurLive: Boolean,
    solidFillAlpha: Float = Dimensions.Glass.fillAlphaSolid
): Color = tint.copy(
    alpha = if (blurLive) Dimensions.Glass.fillAlphaBlurred else solidFillAlpha
)

/**
 * Clips to [shape] and frosts whatever [hazeState] captures behind it, tinted
 * with [tint] (the un-alpha'd role, as Cashiro passes `HazeDefaults.tint(surfaceContainerLow)`).
 * A no-op when [hazeState] is null (blur off).
 */
fun Modifier.glassBlur(shape: Shape, hazeState: HazeState?, tint: Color): Modifier =
    if (hazeState == null) this else this
        .clip(shape)
        .hazeEffect(
            state = hazeState,
            block = fun HazeEffectScope.() {
                style = HazeDefaults.style(
                    backgroundColor = Color.Transparent,
                    tint = HazeDefaults.tint(tint),
                    blurRadius = Dimensions.Glass.blurRadius,
                    noiseFactor = -1f,
                )
                blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
            }
        )

/**
 * The glass recipe as a modifier, for surfaces that aren't a [GlassCard] —
 * bottom sheets, top bars, pills: blur over [hazeState] (when [blurEffects] and
 * a state are present), the [tint] fill (50% over blur, solid otherwise), clipped
 * to [shape]. A border is drawn only when [rimColor] is given.
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape,
    blurEffects: Boolean,
    hazeState: HazeState? = null,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    solidFillAlpha: Float = Dimensions.Glass.fillAlphaSolid,
    rimColor: Color? = null,
): Modifier {
    val blurLive = blurEffects && hazeState != null
    val fill = glassFill(tint, blurLive, solidFillAlpha)
    return this
        .glassBlur(shape, if (blurLive) hazeState else null, tint)
        .clip(shape)
        .background(fill, shape)
        .then(
            if (rimColor != null) Modifier.border(glassBorder(rimColor), shape)
            else Modifier
        )
}

/**
 * The explicit border for a glass surface: Cashiro's translucent 1dp budget
 * border when [color] is translucent, its 2dp selection border (full-alpha
 * `primary`) when opaque.
 */
fun glassBorder(color: Color): BorderStroke = BorderStroke(
    if (color.alpha < 1f) Dimensions.Glass.rimWidth else Dimensions.Glass.selectedRimWidth,
    color
)
