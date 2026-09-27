package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

internal data class LoanEntryDraft(
    val personName: String = "",
    val direction: LoanDirection = LoanDirection.LENT,
    val amountText: String = "",
    val currency: String = "INR",
    val note: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendBorrowEntrySheet(
    initialCurrency: String,
    initialPersonName: String = "",
    isPersonEditable: Boolean = true,
    error: LoanEntryError?,
    isSaving: Boolean,
    onInputChanged: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (
        personName: String,
        direction: LoanDirection,
        amount: BigDecimal?,
        currency: String,
        note: String?,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
    ) {
        LendBorrowEntryForm(
            initialDraft = LoanEntryDraft(
                personName = initialPersonName,
                currency = initialCurrency,
            ),
            isPersonEditable = isPersonEditable,
            error = error,
            isSaving = isSaving,
            onInputChanged = onInputChanged,
            onSubmit = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(
                    start = Dimensions.Padding.dialog,
                    end = Dimensions.Padding.dialog,
                    bottom = Spacing.lg,
                ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LendBorrowEntryForm(
    initialDraft: LoanEntryDraft,
    isPersonEditable: Boolean = true,
    error: LoanEntryError?,
    isSaving: Boolean,
    onInputChanged: () -> Unit,
    onSubmit: (
        personName: String,
        direction: LoanDirection,
        amount: BigDecimal?,
        currency: String,
        note: String?,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    var personName by remember(initialDraft.personName) {
        mutableStateOf(initialDraft.personName)
    }
    var direction by remember(initialDraft.direction) {
        mutableStateOf(initialDraft.direction)
    }
    var amountText by remember(initialDraft.amountText) {
        mutableStateOf(initialDraft.amountText)
    }
    var currency by remember(initialDraft.currency) {
        mutableStateOf(initialDraft.currency)
    }
    var note by remember(initialDraft.note) { mutableStateOf(initialDraft.note) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    val amount = amountText.trim().toBigDecimalOrNull()
    val currencies = remember { CurrencyFormatter.getSupportedCurrencies().sorted() }

    fun changed() {
        onInputChanged()
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = stringResource(R.string.lend_borrow_entry_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.lend_borrow_entry_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = personName,
            onValueChange = {
                personName = it
                changed()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving && isPersonEditable,
            singleLine = true,
            label = { Text(stringResource(R.string.lend_borrow_entry_person)) },
            placeholder = {
                Text(stringResource(R.string.lend_borrow_entry_person_placeholder))
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            isError = error == LoanEntryError.PERSON_REQUIRED,
        )

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = stringResource(R.string.lend_borrow_entry_direction),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                FilterChip(
                    selected = direction == LoanDirection.LENT,
                    onClick = {
                        direction = LoanDirection.LENT
                        changed()
                    },
                    enabled = !isSaving,
                    label = { Text(stringResource(R.string.lend_borrow_entry_lent)) },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = direction == LoanDirection.BORROWED,
                    onClick = {
                        direction = LoanDirection.BORROWED
                        changed()
                    },
                    enabled = !isSaving,
                    label = { Text(stringResource(R.string.lend_borrow_entry_borrowed)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    changed()
                },
                modifier = Modifier.weight(1f),
                enabled = !isSaving,
                singleLine = true,
                label = { Text(stringResource(R.string.lend_borrow_entry_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = error == LoanEntryError.AMOUNT_REQUIRED ||
                    (amountText.isNotBlank() && (amount == null || amount <= BigDecimal.ZERO)),
            )

            ExposedDropdownMenuBox(
                expanded = currencyMenuExpanded,
                onExpandedChange = { if (!isSaving) currencyMenuExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = currency,
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isSaving,
                    singleLine = true,
                    label = { Text(stringResource(R.string.lend_borrow_entry_currency)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(currencyMenuExpanded)
                    },
                    isError = error == LoanEntryError.CURRENCY_REQUIRED,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = currencyMenuExpanded,
                    onDismissRequest = { currencyMenuExpanded = false },
                ) {
                    currencies.forEach { code ->
                        DropdownMenuItem(
                            text = { Text(code) },
                            onClick = {
                                currency = code
                                currencyMenuExpanded = false
                                changed()
                            },
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = {
                note = it
                changed()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            label = { Text(stringResource(R.string.lend_borrow_entry_note)) },
            minLines = 2,
            maxLines = 3,
        )

        error?.let {
            Text(
                text = stringResource(it.messageResource),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = {
                onSubmit(
                    personName.trim(),
                    direction,
                    amount,
                    currency,
                    note.trim().ifBlank { null },
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving && validateLoanEntry(personName, amount, currency) == null,
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Dimensions.Icon.inline),
                    strokeWidth = Dimensions.Component.progressRingStroke,
                )
            } else {
                Text(stringResource(R.string.lend_borrow_entry_save))
            }
        }
    }
}

private val LoanEntryError.messageResource: Int
    get() = when (this) {
        LoanEntryError.PERSON_REQUIRED -> R.string.lend_borrow_entry_person_required
        LoanEntryError.AMOUNT_REQUIRED -> R.string.lend_borrow_entry_amount_required
        LoanEntryError.CURRENCY_REQUIRED -> R.string.lend_borrow_entry_currency_required
        LoanEntryError.SAVE_FAILED -> R.string.lend_borrow_entry_save_failed
    }
