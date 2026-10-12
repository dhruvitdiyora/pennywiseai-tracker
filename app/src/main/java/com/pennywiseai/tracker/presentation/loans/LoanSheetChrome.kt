package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Information
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/*
 * Small pieces the lend & borrow sheets share, so the entry, settle-up and
 * record-payment sheets read like Cashiro's AddEditLendBorrowTransactionSheet
 * and SettleUpSheet: a centred bold title, a primary-washed info card, and a
 * tall full-width save button.
 */

/** How strongly the info card's fill and rim are washed with `primary`. */
private const val INFO_FILL_ALPHA = 0.2f
private const val INFO_RIM_ALPHA = 0.1f

/** Cashiro's sheet title: centred, bold `titleMedium`, with an optional quieter line under it. */
@Composable
internal fun LoanSheetTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Cashiro's info card: an information glyph and a short explanation on a
 * faint `primaryContainer` wash with a hairline `primary` rim. [title] is the
 * optional bold `primary` lead-in Cashiro uses on its settle-up sheet.
 */
@Composable
internal fun LoanInfoCard(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimensions.CornerRadius.large),
        color = scheme.primaryContainer.copy(alpha = INFO_FILL_ALPHA),
        border = BorderStroke(Dimensions.Glass.rimWidth, scheme.primary.copy(alpha = INFO_RIM_ALPHA)),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.smd),
            horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Iconax.Information,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.Icon.inline),
                tint = scheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary,
                    )
                }
                Text(
                    text = text,
                    style = if (title != null) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelLarge,
                    color = scheme.onSurface,
                )
            }
        }
    }
}

/** Cashiro's sheet save button: full width, 56dp tall, pill-shaped. */
@Composable
internal fun LoanSheetButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimensions.Component.fab),
        shape = CircleShape,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(Dimensions.Icon.inline),
                strokeWidth = Dimensions.Component.progressRingStroke,
            )
        } else {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
