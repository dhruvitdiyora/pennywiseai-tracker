package com.pennywiseai.tracker.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.BottomSheetDefaults
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
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimensions.Padding.dialog),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = "Edit widgets",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "Choose what appears on Home and change its order.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimensions.Padding.dialog),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = Dimensions.Padding.content
                )
            ) {
                items(
                    items = widgets + HomeWidget.defaults.filterNot { it in widgets },
                    key = { it.name }
                ) { widget ->
                    val index = widgets.indexOf(widget)
                    PennyWiseCardV2(
                        modifier = Modifier.fillMaxWidth().defaultMinSize(
                            minHeight = Dimensions.Component.listItemMinHeight
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        contentPadding = Spacing.sm
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = index >= 0,
                                onCheckedChange = { enabled ->
                                    widgets = if (enabled) {
                                        (widgets + widget).distinct()
                                    } else {
                                        widgets - widget
                                    }
                                }
                            )
                            Text(
                                text = stringResource(widget.titleRes),
                                modifier = Modifier.weight(1f).padding(start = Spacing.sm),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            IconButton(
                                enabled = index > 0,
                                onClick = {
                                    widgets = widgets.toMutableList().also { list ->
                                        val item = list.removeAt(index)
                                        list.add(index - 1, item)
                                    }
                                }
                            ) { Icon(Icons.Default.ArrowUpward, "Move up") }
                            IconButton(
                                enabled = index >= 0 && index < widgets.lastIndex,
                                onClick = {
                                    widgets = widgets.toMutableList().also { list ->
                                        val item = list.removeAt(index)
                                        list.add(index + 1, item)
                                    }
                                }
                            ) { Icon(Icons.Default.ArrowDownward, "Move down") }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                TextButton(onClick = onReset) { Text("Reset") }
                Button(
                    onClick = { onSave(widgets); onDismiss() },
                    modifier = Modifier.weight(1f)
                ) { Text("Done") }
            }
        }
    }
}
