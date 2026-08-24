package com.pennywiseai.tracker.presentation.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreFiltersSheet(
    initial: TransactionsViewModel.MoreFilters,
    subcategories: List<String>,
    currencies: List<String>,
    onApply: (TransactionsViewModel.MoreFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSubcategories by remember { mutableStateOf(initial.subcategories) }
    var selectedCurrencies by remember { mutableStateOf(initial.currencies) }
    var minimum by remember { mutableStateOf(initial.minimumAmount?.toPlainString().orEmpty()) }
    var maximum by remember { mutableStateOf(initial.maximumAmount?.toPlainString().orEmpty()) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.padding(Dimensions.Padding.dialog), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text("More filters", style = MaterialTheme.typography.titleLarge)
            Text("Amount range", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(minimum, { minimum = it }, Modifier.weight(1f), label = { Text("Minimum") })
                OutlinedTextField(maximum, { maximum = it }, Modifier.weight(1f), label = { Text("Maximum") })
            }
            FilterChoices("Subcategory", subcategories, selectedSubcategories) { selectedSubcategories = it }
            FilterChoices("Currency", currencies, selectedCurrencies) { selectedCurrencies = it }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = {
                    onApply(TransactionsViewModel.MoreFilters())
                    onDismiss()
                }) { Text("Clear") }
                Button(
                    onClick = {
                        onApply(TransactionsViewModel.MoreFilters(
                            subcategories = selectedSubcategories,
                            currencies = selectedCurrencies,
                            minimumAmount = minimum.toBigDecimalOrNull(),
                            maximumAmount = maximum.toBigDecimalOrNull()
                        ))
                        onDismiss()
                    }, modifier = Modifier.weight(1f)
                ) { Text("Apply") }
            }
        }
    }
}

@Composable
private fun FilterChoices(
    title: String,
    choices: List<String>,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit
) {
    if (choices.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            items(choices, key = { it }) { choice ->
                AssistChip(
                    onClick = { onSelectedChange(if (choice in selected) selected - choice else selected + choice) },
                    label = { Text(choice) },
                    leadingIcon = if (choice in selected) ({ Text("✓") }) else null
                )
            }
        }
    }
}
