package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendBorrowEntrySheet(
    initialPersonName: String = "",
    onDismiss: () -> Unit,
    onSubmit: (personName: String, direction: LoanDirection, amount: BigDecimal, currency: String, note: String?) -> Unit
) {
    var personName by remember(initialPersonName) { mutableStateOf(initialPersonName) }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }
    var note by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(LoanDirection.LENT) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    val amount = amountText.toBigDecimalOrNull()
    val currencies = remember { CurrencyFormatter.getSupportedCurrencies().sorted() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.lend_borrow_entry_title))
            OutlinedTextField(
                value = personName,
                onValueChange = { personName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.lend_borrow_entry_person)) }
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.lend_borrow_title))
                FilterChip(
                    selected = direction == LoanDirection.LENT,
                    onClick = { direction = LoanDirection.LENT },
                    label = { Text(stringResource(R.string.lend_borrow_entry_lent)) }
                )
                FilterChip(
                    selected = direction == LoanDirection.BORROWED,
                    onClick = { direction = LoanDirection.BORROWED },
                    label = { Text(stringResource(R.string.lend_borrow_entry_borrowed)) }
                )
            }
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.lend_borrow_entry_amount)) },
                isError = amountText.isNotBlank() && amount == null
            )
            ExposedDropdownMenuBox(
                expanded = currencyMenuExpanded,
                onExpandedChange = { currencyMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = currency,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    label = { Text(stringResource(R.string.lend_borrow_entry_currency)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyMenuExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = currencyMenuExpanded,
                    onDismissRequest = { currencyMenuExpanded = false }
                ) {
                    currencies.forEach { code ->
                        DropdownMenuItem(
                            text = { Text(code) },
                            onClick = {
                                currency = code
                                currencyMenuExpanded = false
                            }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.lend_borrow_entry_note)) }
            )
            Button(
                onClick = {
                    onSubmit(personName.trim(), direction, amount!!, currency, note.trim().ifBlank { null })
                },
                enabled = personName.isNotBlank() && amount != null && amount > BigDecimal.ZERO,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.save)) }
        }
    }
}
