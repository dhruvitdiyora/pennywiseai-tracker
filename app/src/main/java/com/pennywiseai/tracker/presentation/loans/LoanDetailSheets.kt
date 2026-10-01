package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.presentation.people.TonalTextField
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.ListItemCardV2
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/** Confirms settling one loan: the unpaid remainder is forgiven. */
@Composable
internal fun SettleLoanDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
        title = { Text(stringResource(R.string.loan_detail_settle_title)) },
        text = { Text(stringResource(R.string.loan_detail_settle_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.loan_detail_settle))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.accounts_action_cancel))
            }
        }
    )
}

/** Sets how much the loan is expected to bring back (its original amount). */
@Composable
internal fun EditExpectedReturnDialog(
    loan: LoanEntity,
    onSave: (BigDecimal) -> Unit,
    onDismiss: () -> Unit,
) {
    var editAmount by remember(loan.id) { mutableStateOf(loan.originalAmount.toPlainString()) }
    val parsed = editAmount.toBigDecimalOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text(stringResource(R.string.loan_detail_expected_return_title)) },
        text = {
            TonalTextField(
                value = editAmount,
                onValueChange = { value ->
                    if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                        editAmount = value
                    }
                },
                label = stringResource(R.string.loan_detail_expected_return_label),
                prefix = { Text(CurrencyFormatter.getCurrencySymbol(loan.currency)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onSave) },
                enabled = parsed != null && parsed > BigDecimal.ZERO
            ) {
                Text(stringResource(R.string.accounts_action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.accounts_action_cancel))
            }
        }
    )
}

/** Confirms deleting a loan; linked transactions are unlinked, not deleted. */
@Composable
internal fun DeleteLoanDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        iconContentColor = MaterialTheme.colorScheme.error,
        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
        title = { Text(stringResource(R.string.loan_detail_delete_title)) },
        text = { Text(stringResource(R.string.loan_detail_delete_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.accounts_action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.accounts_action_cancel))
            }
        }
    )
}

/**
 * Records a repayment: link a recent transaction that already was one, or type
 * the amount in by hand.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordPaymentBottomSheet(
    personName: String,
    remainingAmount: BigDecimal,
    currency: String,
    recentUnlinkedTransactions: List<TransactionEntity>,
    onDismiss: () -> Unit,
    onLinkTransaction: (Long) -> Unit,
    onManualPayment: (BigDecimal) -> Unit
) {
    var manualAmount by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val manualParsed = manualAmount.toBigDecimalOrNull()
    val candidates = recentUnlinkedTransactions.take(MAX_LINK_CANDIDATES)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Tonal fields and rows sit on the card surface, one step above the sheet.
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(
                    start = Dimensions.Padding.dialog,
                    end = Dimensions.Padding.dialog,
                    bottom = Spacing.lg,
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.loan_detail_record_payment_from, personName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(
                        R.string.loan_detail_amount_remaining,
                        CurrencyFormatter.formatCurrency(remainingAmount, currency),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            // Link existing transactions
            if (candidates.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = stringResource(R.string.loan_detail_link_existing),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    GroupedList {
                        candidates.forEachIndexed { index, txn ->
                            ListItemCardV2(
                                title = txn.merchantName,
                                subtitle = txn.dateTime.format(DateTimeFormatter.ofPattern("d MMM")),
                                amount = CurrencyFormatter.formatCurrency(txn.amount, txn.currency),
                                shape = ListItemPosition.from(index, candidates.size).toShape(),
                                onClick = { onLinkTransaction(txn.id) },
                            )
                        }
                    }
                }
            }

            // Manual entry
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = if (candidates.isNotEmpty()) {
                        stringResource(R.string.loan_detail_or_enter_manually)
                    } else {
                        stringResource(R.string.loan_detail_enter_amount)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TonalTextField(
                        value = manualAmount,
                        onValueChange = { value ->
                            if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                                manualAmount = value
                            }
                        },
                        label = stringResource(R.string.loan_detail_amount_label),
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        prefix = {
                            Text(
                                text = CurrencyFormatter.getCurrencySymbol(currency),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    Button(
                        onClick = { manualParsed?.let(onManualPayment) },
                        enabled = manualParsed != null && manualParsed > BigDecimal.ZERO,
                        modifier = Modifier.heightIn(min = Dimensions.Component.fab),
                    ) {
                        Text(stringResource(R.string.loan_detail_add))
                    }
                }
            }
        }
    }
}

private const val MAX_LINK_CANDIDATES = 5
