package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.ui.theme.Spacing

private const val TAG_CONTAINER_ALPHA = 0.14f

/** A compact tonal label for structured metadata inside dense financial rows. */
@Composable
fun SubtitleTag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = TAG_CONTAINER_ALPHA),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = Spacing.sm,
                vertical = Spacing.xxs,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
