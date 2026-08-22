package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.theme.Spacing

/**
 * A small tinted pill used for the transaction row's metadata (date, category,
 * recurring, ...). Ported from Cashiro's `SubtitleTag` (ui-revamp doc 20), with
 * one deliberate change: the label colour is derived from the chip's own
 * effective luminance instead of a fixed `onSurfaceVariant`, so it stays
 * readable on saturated category colours in dark mode too.
 */
@Composable
fun SubtitleTag(
    text: String,
    color: Color,
    alpha: Float = 0.2f,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
) {
    // The chip's *effective* colour is `color` blended at `alpha` over the
    // surface it sits on — that's what a viewer's eye actually contrasts
    // against, not the raw (fully opaque) tint. A near-black category colour
    // blended at 20% over a dark-theme surface is still dark (wants a light
    // label); the same colour blended over a light-theme surface reads as a
    // light grey (wants a dark label) — a fixed contrast rule gets one of
    // those two wrong.
    val surface = MaterialTheme.colorScheme.surface
    val labelColor = remember(color, alpha, surface) {
        val effective = lerp(surface, color, alpha)
        if (isLightColor(effective)) Color.Black.copy(alpha = 0.87f) else Color.White
    }

    Surface(
        color = color.copy(alpha = alpha),
        shape = RoundedCornerShape(Spacing.lg),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon?.invoke()
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
