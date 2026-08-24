package com.pennywiseai.tracker.presentation.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchEditSheet(
    selectedCount: Int,
    selectedCurrencies: Set<String>,
    onApply: (TransactionsViewModel.BulkEditPatch) -> Unit,
    onDismiss: () -> Unit
) {
    var changeDateTime by remember { mutableStateOf(false) }
    var changeAmount by remember { mutableStateOf(false) }
    var changeNote by remember { mutableStateOf(false) }
    var dateTimeText by remember { mutableStateOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var confirmAmount by remember { mutableStateOf(false) }
    val parsedDate = runCatching { LocalDateTime.parse(dateTimeText, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) }.getOrNull()
    val parsedAmount = amountText.toBigDecimalOrNull()
    val canApply = (!changeDateTime || parsedDate != null) && (!changeAmount || parsedAmount != null)
    val patch = TransactionsViewModel.BulkEditPatch(
        dateTime = parsedDate,
        amount = parsedAmount,
        note = noteText,
        updateDateTime = changeDateTime,
        updateAmount = changeAmount,
        updateNote = changeNote
    )
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            modifier = Modifier.padding(Dimensions.Padding.dialog),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text("Edit $selectedCount transactions", style = MaterialTheme.typography.titleLarge)
            EditToggle("Date & time", changeDateTime) { changeDateTime = it }
            if (changeDateTime) OutlinedTextField(dateTimeText, { dateTimeText = it }, Modifier.fillMaxWidth(), label = { Text("YYYY-MM-DD HH:MM") }, isError = parsedDate == null)
            EditToggle("Amount", changeAmount) { changeAmount = it }
            if (changeAmount) {
                OutlinedTextField(amountText, { amountText = it }, Modifier.fillMaxWidth(), label = { Text("New amount") }, isError = parsedAmount == null)
                if (selectedCurrencies.size > 1) Text("Amount changes require a single-currency selection.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            EditToggle("Note", changeNote) { changeNote = it }
            if (changeNote) OutlinedTextField(noteText, { noteText = it }, Modifier.fillMaxWidth(), label = { Text("Note") })
            Button(
                onClick = { if (changeAmount) confirmAmount = true else { onApply(patch); onDismiss() } },
                enabled = canApply && (changeDateTime || changeAmount || changeNote) && (!changeAmount || selectedCurrencies.size == 1),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Apply changes") }
        }
    }
    if (confirmAmount) AlertDialog(
        onDismissRequest = { confirmAmount = false },
        title = { Text("Confirm amount change") },
        text = { Text("Set the amount of $selectedCount transactions to $amountText ${selectedCurrencies.singleOrNull() ?: ""}?") },
        confirmButton = { TextButton(onClick = { confirmAmount = false; onApply(patch); onDismiss() }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = { confirmAmount = false }) { Text("Cancel") } }
    )
}

@Composable
private fun EditToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}
