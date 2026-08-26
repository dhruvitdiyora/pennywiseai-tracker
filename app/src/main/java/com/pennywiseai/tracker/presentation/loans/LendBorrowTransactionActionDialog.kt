package com.pennywiseai.tracker.presentation.loans

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun LendBorrowTransactionActionDialog(
    isSettled: Boolean,
    onDismiss: () -> Unit,
    onEditAmount: () -> Unit,
    onSettle: () -> Unit,
    onReopen: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lend & Borrow actions") },
        text = {
            androidx.compose.foundation.layout.Column {
                if (isSettled) {
                    TextButton(onClick = onReopen) { Text("Reopen") }
                } else {
                    TextButton(onClick = onEditAmount) { Text("Set expected return") }
                    TextButton(onClick = onSettle) { Text("Settle") }
                }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
