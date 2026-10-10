package com.pennywiseai.tracker.presentation.add

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import com.pennywiseai.tracker.presentation.transactions.TxnGlassSheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.NumberPad
import com.pennywiseai.tracker.ui.components.NumberPadInputState
import com.pennywiseai.tracker.ui.components.containsNumberPadOperation
import com.pennywiseai.tracker.ui.components.evaluateNumberExpression
import com.pennywiseai.tracker.ui.components.formatNumberPadResult
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

/**
 * The number of decimal places [currencyCode] is written with (2 for INR/USD, 0 for
 * JPY, 3 for KWD). Falls back to 2 for a null, blank, unknown or pseudo currency.
 */
internal fun currencyFractionDigits(currencyCode: String?): Int =
    runCatching { Currency.getInstance(currencyCode.orEmpty()).defaultFractionDigits }
        .getOrNull()
        ?.takeIf { it >= 0 }
        ?: 2

/**
 * Rounds a calculated [amount] to what [currencyCode] can actually hold, so
 * "100 ÷ 3" lands as 33.33 rather than 34 significant digits.
 */
internal fun roundToCurrency(amount: BigDecimal, currencyCode: String?): BigDecimal =
    amount.setScale(currencyFractionDigits(currencyCode), RoundingMode.HALF_UP)

/**
 * Bottom sheet calculator for entering an exact amount expression. Shared by
 * the transaction and subscription tabs; applies the evaluated result back to
 * whichever amount field opened it.
 *
 * Only a positive result can be applied (the amount fields reject anything else),
 * and it is rounded to [currencyCode]'s decimal places first; an unknown or absent
 * code rounds to 2 places.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AmountCalculatorSheet(
    initialAmount: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
    currencyCode: String? = null,
) {
    val initialExpression = remember(initialAmount) {
        initialAmount.takeIf { evaluateNumberExpression(it) != null }.orEmpty()
    }
    var inputState by remember(initialExpression) {
        mutableStateOf(
            NumberPadInputState(
                expression = initialExpression,
                replaceOnNextNumber = initialExpression.isNotBlank()
            )
        )
    }
    val result = evaluateNumberExpression(inputState.expression)
    // What "Use amount" would hand back: the result at the currency's precision,
    // and only when it is a positive figure (10 - 15 = -5 must not be applied).
    val applicable = result
        ?.let { roundToCurrency(it, currencyCode) }
        ?.takeIf { it.signum() > 0 }
    val resultIsNotPositive = result != null && applicable == null
    // The preview under the expression shows the rounded figure that will be used.
    val previewText = if (containsNumberPadOperation(inputState.expression)) {
        result?.let { formatNumberPadResult(roundToCurrency(it, currencyCode)) }
    } else {
        null
    }

    TxnGlassSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = Dimensions.Padding.dialog,
                    end = Dimensions.Padding.dialog,
                    bottom = Dimensions.Padding.dialog
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.add_calculator_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.add_close_calculator)
                    )
                }
            }
            Text(
                text = stringResource(R.string.add_calculator_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NumberPad(
                state = inputState,
                onStateChange = { inputState = it },
                resultText = previewText
            )
            if (resultIsNotPositive) {
                Text(
                    text = stringResource(R.string.add_error_amount_positive),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimensions.Component.listItemMinHeight)
                ) {
                    Text(stringResource(R.string.add_cancel))
                }
                Button(
                    onClick = { applicable?.let { onApply(formatNumberPadResult(it)) } },
                    enabled = applicable != null,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimensions.Component.listItemMinHeight)
                ) {
                    Text(stringResource(R.string.add_use_amount))
                }
            }
        }
    }
}
