package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/** How strongly a tint colour washes its container (chip fill, category avatar). */
internal const val TINTED_CONTAINER_ALPHA = 0.2f

/**
 * A compact tinted chip for structured metadata inside dense financial rows
 * (date, category, "Recurring", ...): [color] at low alpha as the fill, an
 * optional leading [icon], then a single line of [text].
 *
 * [textColor] (and the icon, which follows it) defaults to a quiet neutral;
 * pass a category colour — see [legibleOn] — to name a category in its own hue.
 */
@Composable
fun SubtitleTag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    icon: ImageVector? = null,
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = TINTED_CONTAINER_ALPHA),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Spacing.sm,
                vertical = Spacing.xxs,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            if (icon != null) {
                // Decorative: the chip's text already says everything.
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.tiny),
                    tint = textColor,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * This colour, moved toward [towards] only as far as needed to reach
 * [minContrast] against [background] (WCAG contrast ratio).
 *
 * Category colours are picked for a brand or a swatch, not for text: a black
 * "Transportation" would vanish on a dark card and a pale user colour on a
 * light one. A colour that already reads is returned untouched.
 */
internal fun Color.legibleOn(
    background: Color,
    towards: Color,
    minContrast: Float = 3f,
): Color {
    // Opaque colours only: contrast is measured against what is actually drawn.
    val base = copy(alpha = 1f)
    if (contrastRatio(base, background) >= minContrast) return base
    for (step in 1..STEPS) {
        val candidate = lerp(base, towards, step / STEPS.toFloat())
        if (contrastRatio(candidate, background) >= minContrast) return candidate
    }
    return towards
}

private const val STEPS = 10

private fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}
