package com.pennywiseai.tracker.ui.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pennywiseai.tracker.ui.components.cards.glassRim
import com.pennywiseai.tracker.ui.theme.Dimensions
import androidx.compose.ui.text.style.TextOverflow

/**
 * A single choice in a horizontally scrolling period row ("This Month",
 * "Last 7 Days"...). The selected chip is filled with `primaryContainer`; the
 * others are small glass pills (translucent tonal fill + glass rim) so the row
 * reads as one set with a single clear selection, and still separates from a
 * cover/banner behind it.
 */
@Composable
fun PeriodFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        shape = MaterialTheme.shapes.medium,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(
                alpha = Dimensions.Glass.fillAlphaControl
            ),
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = if (selected) null else glassRim(),
    )
}
