package com.pennywiseai.tracker.presentation.add

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
import com.pennywiseai.tracker.ui.components.evaluateNumberExpression
import com.pennywiseai.tracker.ui.components.formatNumberPadResult
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/**
 * Bottom sheet calculator for entering an exact amount expression. Shared by
 * the transaction and subscription tabs; applies the evaluated result back to
 * whichever amount field opened it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AmountCalculatorSheet(
    initialAmount: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
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
                onStateChange = { inputState = it }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.add_cancel))
                }
                Button(
                    onClick = { result?.let { onApply(formatNumberPadResult(it)) } },
                    enabled = result != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.add_use_amount))
                }
            }
        }
    }
}
