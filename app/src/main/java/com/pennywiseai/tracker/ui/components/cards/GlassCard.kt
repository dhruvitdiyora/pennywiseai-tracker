package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import com.pennywiseai.tracker.ui.effects.LocalBlurEffects
import com.pennywiseai.tracker.ui.theme.Dimensions
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/**
 * PennyWise's frosted-glass material (Cashiro's look) — the app-wide default
 * container for cards, sheets and bars. Home is the first adopter.
 *
 * The recipe (all numbers in [Dimensions.Glass], all colours theme roles, so it
 * holds in light, dark and AMOLED):
 * - **Fill** — [tint] at [Dimensions.Glass.fillAlphaBlurred] while the blur is
 *   live, otherwise [solidFillAlpha], so text never sits on an unblurred busy
 *   backdrop.
 * - **Blur** — a Haze effect over [hazeState], only when [blurEffects] is on
 *   *and* a state is supplied. With no state the card is simply solid glass.
 * - **Rim** — a 1dp diagonal gradient border ([glassRim]): it catches the
 *   top-start edge in dark/AMOLED, where a tonal fill barely separates from
 *   black, and grounds the bottom edge in light.
 * - **Sheen** — a soft top-down highlight ([glassSheen]) that makes the surface
 *   read as glass rather than flat plastic, with or without blur.
 *
 * For a non-card surface (bottom sheet, top bar, pill) use [Modifier.glassSurface],
 * which applies the same recipe to any composable.
 *
 * @param shape corner shape; Home hero-tier cards use `extraLarge`.
 * @param blurEffects the user's blur setting; defaults to [LocalBlurEffects].
 * @param hazeState the [HazeState] whose `hazeSource` sits *behind* this card
 *   (e.g. Home's banner). `null` = no blur.
 * @param tint the fill's theme role (e.g. `errorContainer` for a warning card).
 * @param solidFillAlpha fill alpha when the blur is off.
 * @param rimColor accent for the rim (e.g. a budget's colour); `null` = neutral.
 * @param onClick makes the whole card clickable (ripple covers the sheen).
 * @param contentPadding inner padding; pass it here rather than padding the
 *   content, so the sheen and ripple cover the whole surface.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = MaterialTheme.shapes.extraLarge,
    blurEffects: Boolean = LocalBlurEffects.current,
    hazeState: HazeState? = null,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    /** Solid-mode fill alpha; a semantic container may want a lighter wash. */
    solidFillAlpha: Float = Dimensions.Glass.fillAlphaSolid,
    /** Accent for the rim (e.g. a budget's colour); `null` uses the neutral rim. */
    rimColor: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = Dimensions.Padding.card,
    content: @Composable ColumnScope.() -> Unit
) {
    val blurLive = blurEffects && hazeState != null
    val fill = glassFill(tint, blurLive, solidFillAlpha)
    val colors = CardDefaults.cardColors(containerColor = fill)
    val elevation = CardDefaults.cardElevation(defaultElevation = Dimensions.Elevation.card)
    val border = glassRim(rimColor)
    val cardModifier = modifier.glassBlur(shape, if (blurLive) hazeState else null, fill)
    val sheen = glassSheen()

    val inner: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(sheen)
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
            elevation = elevation,
            border = border,
            content = inner
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = inner
        )
    }
}

/** The glass fill colour for [tint]: translucent over a live blur, near-solid otherwise. */
fun glassFill(
    tint: Color,
    blurLive: Boolean,
    solidFillAlpha: Float = Dimensions.Glass.fillAlphaSolid
): Color = tint.copy(
    alpha = if (blurLive) Dimensions.Glass.fillAlphaBlurred else solidFillAlpha
)

/**
 * Clips to [shape] and frosts whatever [hazeState] captures behind it, tinted
 * with [tint]. A no-op when [hazeState] is null (blur off).
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

/** True when the active colour scheme is a dark one (incl. AMOLED), whatever the system says. */
@Composable
@ReadOnlyComposable
internal fun isGlassDark(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/** The diagonal rim: highlight at the top-start edge, fading toward the bottom-end. */
@Composable
@ReadOnlyComposable
fun glassRim(accent: Color? = null): BorderStroke {
    val (from, to) = when {
        accent != null -> accent.copy(alpha = Dimensions.Glass.rimAccentHighlight) to
            accent.copy(alpha = Dimensions.Glass.rimAccentShade)
        isGlassDark() -> MaterialTheme.colorScheme.onSurface.let {
            it.copy(alpha = Dimensions.Glass.rimHighlightDark) to it.copy(alpha = Dimensions.Glass.rimShadeDark)
        }
        else -> MaterialTheme.colorScheme.onSurface.let {
            it.copy(alpha = Dimensions.Glass.rimHighlightLight) to it.copy(alpha = Dimensions.Glass.rimShadeLight)
        }
    }
    return BorderStroke(Dimensions.Glass.rimWidth, Brush.linearGradient(listOf(from, to)))
}

/** The soft top-down sheen laid over the fill, under the content. */
@Composable
@ReadOnlyComposable
fun glassSheen(): Brush {
    val color = if (isGlassDark()) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = Dimensions.Glass.sheenDark)
    } else {
        MaterialTheme.colorScheme.surfaceBright.copy(alpha = Dimensions.Glass.sheenLight)
    }
    // Fade to the same colour at zero alpha, not Color.Transparent, so the
    // gradient doesn't blend toward black midway.
    return Brush.verticalGradient(
        0f to color,
        Dimensions.Glass.sheenExtent to color.copy(alpha = 0f),
    )
}

/**
 * The glass recipe as a modifier, for surfaces that aren't a [GlassCard] —
 * bottom sheets, top bars, pills. Applies, in order: blur over [hazeState]
 * (when [blurEffects] and a state are present), the translucent [tint] fill,
 * the top sheen, and the rim. Clips to [shape].
 *
 * @param shape the surface's shape (also used for the rim).
 * @param blurEffects the user's blur setting; pass `LocalBlurEffects.current`.
 * @param hazeState the state whose source is behind this surface; `null` = no blur.
 * @param tint the fill's theme role.
 * @param solidFillAlpha fill alpha when the blur is off.
 * @param rimColor accent for the rim; `null` = neutral.
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
    val rim = glassRim(rimColor)
    val sheen = glassSheen()
    return this
        .glassBlur(shape, if (blurLive) hazeState else null, fill)
        .clip(shape)
        .background(fill, shape)
        .background(sheen, shape)
        .border(rim, shape)
}
