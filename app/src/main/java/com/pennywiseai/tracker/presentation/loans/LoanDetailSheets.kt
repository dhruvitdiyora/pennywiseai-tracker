package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.presentation.people.TonalTextField
import com.pennywiseai.tracker.ui.components.ConnectedButtonPair
import com.pennywiseai.tracker.ui.components.PennyWiseAlertDialog
import com.pennywiseai.tracker.ui.components.PennyWiseBottomSheet
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.ListItemCardV2
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.icons.iconax.Bag
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/*
 * The loan detail's dialogs and sheet, in Cashiro's modal chrome: dialogs are
 * PennyWiseAlertDialog with the joined Cancel / Confirm pair, the sheet is a
 * PennyWiseBottomSheet with the centred title and tall button.
 */

/** Confirms settling one loan: the unpaid remainder is forgiven. */
@Composable
internal fun SettleLoanDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    PennyWiseAlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
        title = { Text(stringResource(R.string.loan_detail_settle_title)) },
        text = { Text(stringResource(R.string.loan_detail_settle_message)) },
        confirmButton = {
            ConnectedButtonPair(
                dismissLabel = stringResource(R.string.accounts_action_cancel),
                onDismiss = onDismiss,
                confirmLabel = stringResource(R.string.loan_detail_settle),
                onConfirm = onConfirm,
            )
        },
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
    PennyWiseAlertDialog(
        onDismissRequest = onDismiss,
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
            ConnectedButtonPair(
                dismissLabel = stringResource(R.string.accounts_action_cancel),
                onDismiss = onDismiss,
                confirmLabel = stringResource(R.string.accounts_action_save),
                onConfirm = { parsed?.let(onSave) },
                confirmEnabled = parsed != null && parsed > BigDecimal.ZERO,
            )
        },
    )
}

/** Confirms deleting a loan; linked transactions are unlinked, not deleted. */
@Composable
internal fun DeleteLoanDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    PennyWiseAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Iconax.Bag, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        title = { Text(stringResource(R.string.loan_detail_delete_title)) },
        text = { Text(stringResource(R.string.loan_detail_delete_message)) },
        confirmButton = {
            ConnectedButtonPair(
                dismissLabel = stringResource(R.string.accounts_action_cancel),
                onDismiss = onDismiss,
                confirmLabel = stringResource(R.string.accounts_action_delete),
                onConfirm = onConfirm,
                destructive = true,
            )
        },
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

    PennyWiseBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
            LoanSheetTitle(
                title = stringResource(R.string.loan_detail_record_payment_from, personName),
                subtitle = stringResource(
                    R.string.loan_detail_amount_remaining,
                    CurrencyFormatter.formatCurrency(remainingAmount, currency),
                ),
            )

            // Link existing transactions
            if (candidates.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    LoanSheetSectionLabel(stringResource(R.string.loan_detail_link_existing))
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
                LoanSheetSectionLabel(
                    if (candidates.isNotEmpty()) {
                        stringResource(R.string.loan_detail_or_enter_manually)
                    } else {
                        stringResource(R.string.loan_detail_enter_amount)
                    },
                )
                TonalTextField(
                    value = manualAmount,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                            manualAmount = value
                        }
                    },
                    label = stringResource(R.string.loan_detail_amount_label),
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    prefix = {
                        Text(
                            text = CurrencyFormatter.getCurrencySymbol(currency),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                LoanSheetButton(
                    label = stringResource(R.string.loan_detail_add),
                    onClick = { manualParsed?.let(onManualPayment) },
                    enabled = manualParsed != null && manualParsed > BigDecimal.ZERO,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
        }
    }
}

/** A quiet label over a group in a loan sheet, inset to line up with the rows' text. */
@Composable
private fun LoanSheetSectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = Spacing.xs),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val MAX_LINK_CANDIDATES = 5
