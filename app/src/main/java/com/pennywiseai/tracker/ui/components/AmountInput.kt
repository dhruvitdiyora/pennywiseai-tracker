package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing

/** Readable, tappable amount display used when entry is handled by NumberPad. */
@Composable
fun AmountInput(
    amount: String,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.CenterEnd,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(Dimensions.Padding.card),
        contentAlignment = contentAlignment
    ) {
        Text(
            text = "$currencySymbol ${amount.ifBlank { "0" }}",
            style = PennyWiseText.amountLarge,
            color = if (amount.isBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
