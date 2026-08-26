package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPersonSheet(
    initialName: String = "",
    mergeTargets: List<PersonSummary> = emptyList(),
    onDismiss: () -> Unit = {},
    onSave: (String) -> Unit = {},
    onMerge: (PersonSummary) -> Unit = {}
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var showMergeTargets by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { sheetState.expand() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (initialName.isBlank()) stringResource(R.string.person_add_title)
                else stringResource(R.string.person_edit_title),
                style = MaterialTheme.typography.titleLarge
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.person_name_label)) }
            )
            Button(
                onClick = { onSave(name.trim()) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.person_save)) }
            if (mergeTargets.isNotEmpty()) {
                OutlinedButton(
                    onClick = { showMergeTargets = !showMergeTargets },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.person_merge)) }
            }
            if (showMergeTargets) {
                mergeTargets.forEach { target ->
                    OutlinedButton(
                        onClick = { onMerge(target) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.person_merge_into, target.personName)) }
                }
            }
        }
    }
}
