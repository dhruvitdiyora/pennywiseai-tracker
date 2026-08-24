package com.pennywiseai.tracker.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
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
import com.pennywiseai.tracker.data.preferences.HomeWidget
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWidgetsSheet(
    layout: List<HomeWidget>,
    onSave: (List<HomeWidget>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var widgets by remember(layout) { mutableStateOf(layout) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.padding(Dimensions.Padding.dialog), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("Edit widgets", style = MaterialTheme.typography.titleLarge)
            Text("Choose what appears on Home and change its order.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            (widgets + HomeWidget.defaults.filterNot { it in widgets }).forEach { widget ->
                val index = widgets.indexOf(widget)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = index >= 0,
                        onCheckedChange = { enabled ->
                            widgets = if (enabled) (widgets + widget).distinct() else widgets - widget
                        }
                    )
                    Text(stringResource(widget.titleRes), Modifier.weight(1f).padding(start = Spacing.sm))
                    IconButton(enabled = index > 0, onClick = {
                        widgets = widgets.toMutableList().also { list ->
                            val item = list.removeAt(index); list.add(index - 1, item)
                        }
                    }) { Icon(Icons.Default.ArrowUpward, "Move up") }
                    IconButton(enabled = index >= 0 && index < widgets.lastIndex, onClick = {
                        widgets = widgets.toMutableList().also { list ->
                            val item = list.removeAt(index); list.add(index + 1, item)
                        }
                    }) { Icon(Icons.Default.ArrowDownward, "Move down") }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                TextButton(onClick = onReset) { Text("Reset") }
                Button(onClick = { onSave(widgets); onDismiss() }, modifier = Modifier.weight(1f)) { Text("Done") }
            }
        }
    }
}
