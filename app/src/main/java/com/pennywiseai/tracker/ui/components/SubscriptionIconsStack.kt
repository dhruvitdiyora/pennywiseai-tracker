package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/** How far each mark steps right of the previous one, as a fraction of its diameter. */
private const val STACK_STEP_FRACTION = 0.55f

/**
 * Overlapping brand marks for a set of subscriptions — the "logo stack" on
 * Cashiro's subscriptions card. Later marks sit on top of earlier ones, each
 * ringed in [borderColor] (pass the colour of the surface it sits on) so the
 * overlap reads as a clean cut-out.
 *
 * Up to [maxIcons] brands are shown; anything beyond is folded into a "+N"
 * bubble at the end of the stack. Renders nothing for an empty list.
 *
 * The stack reserves its own full width, since the offset marks don't
 * contribute to a plain `Box`'s measured size.
 */
@Composable
fun SubscriptionIconsStack(
    subscriptions: List<SubscriptionEntity>,
    modifier: Modifier = Modifier,
    iconSize: Dp = Dimensions.Icon.list,
    maxIcons: Int = 4,
    borderColor: Color = MaterialTheme.colorScheme.surfaceContainerLow
) {
    if (subscriptions.isEmpty()) return

    val shown = subscriptions.take(maxIcons)
    val extraCount = subscriptions.size - shown.size
    val slots = shown.size + if (extraCount > 0) 1 else 0

    // Each mark gets a ring of surface colour, so its outer circle is a little
    // larger than the brand mark itself.
    val outerSize = iconSize + Spacing.xs
    val step = iconSize * STACK_STEP_FRACTION
    val totalWidth = outerSize + step * (slots - 1)

    Box(modifier = modifier.width(totalWidth)) {
        shown.forEachIndexed { index, subscription ->
            Box(
                modifier = Modifier
                    .offset(x = step * index)
                    .zIndex(index.toFloat())
                    .size(outerSize)
                    .background(borderColor, CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                BrandIcon(
                    merchantName = subscription.merchantName,
                    size = iconSize,
                    showBackground = true
                )
            }
        }
        if (extraCount > 0) {
            Box(
                modifier = Modifier
                    .offset(x = step * shown.size)
                    .zIndex(shown.size.toFloat())
                    .size(outerSize)
                    .background(borderColor, CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+$extraCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
