package com.pennywiseai.tracker.presentation.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.domain.usecase.BatchEditRequest
import com.pennywiseai.tracker.domain.usecase.BatchEditTransactionsUseCase
import com.pennywiseai.tracker.domain.usecase.FieldEdit
import com.pennywiseai.tracker.domain.usecase.TagEdit
import com.pennywiseai.tracker.domain.usecase.TagEditMode
import com.pennywiseai.tracker.ui.components.QuickCategoryPickerSheet
import com.pennywiseai.tracker.ui.components.TagInputField
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Cashiro-style batch edit sheet for the current selection. Every field is a
 * switch-gated card: off = unchanged. The draft lives only in this sheet, so
 * Cancel, back and swipe-dismiss write nothing; Apply hands one
 * [BatchEditRequest] to the caller, which runs it atomically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BatchEditTransactionsSheet(
    selectedCount: Int,
    categories: List<CategoryEntity>,
    allTags: List<String>,
    onApply: (BatchEditRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryOn by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf<String?>(null) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    var dateOn by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var timeOn by remember { mutableStateOf(false) }
    var time by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var showTimePicker by remember { mutableStateOf(false) }

    var noteOn by remember { mutableStateOf(false) }
    var noteClear by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    var tagsOn by remember { mutableStateOf(false) }
    var tagMode by remember { mutableStateOf(TagEditMode.ADD) }
    var tags by remember { mutableStateOf(emptyList<String>()) }

    val categoryValid = !categoryOn || category != null
    val noteValid = !noteOn || noteClear || note.isNotBlank()
    val tagsValid = !tagsOn || tagMode == TagEditMode.REPLACE ||
        BatchEditTransactionsUseCase.normalizeTags(tags).isNotEmpty()
    val anyOn = categoryOn || dateOn || timeOn || noteOn || tagsOn
    val canApply = anyOn && categoryValid && noteValid && tagsValid

    TxnGlassSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = Dimensions.Padding.content)
                .padding(bottom = Dimensions.Padding.content)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .txnGlass(
                                shape = RoundedCornerShape(14.dp),
                                tint = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.size(Spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.batch_edit_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.batch_edit_subtitle, selectedCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.batch_edit_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                BatchEditOptionCard(
                    title = stringResource(R.string.batch_edit_category),
                    icon = Icons.Outlined.Category,
                    checked = categoryOn,
                    onCheckedChange = {
                        categoryOn = it
                        if (it && category == null) showCategoryPicker = true
                    },
                ) {
                    BatchEditValueRow(
                        value = category ?: stringResource(R.string.batch_edit_select_category),
                        action = stringResource(R.string.batch_edit_choose),
                        onClick = { showCategoryPicker = true },
                    )
                }

                BatchEditOptionCard(
                    title = stringResource(R.string.batch_edit_date),
                    icon = Icons.Outlined.CalendarMonth,
                    checked = dateOn,
                    onCheckedChange = {
                        dateOn = it
                        if (it) showDatePicker = true
                    },
                ) {
                    BatchEditValueRow(
                        value = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                        supporting = stringResource(R.string.batch_edit_date_hint),
                        action = stringResource(R.string.batch_edit_pick),
                        onClick = { showDatePicker = true },
                    )
                }

                BatchEditOptionCard(
                    title = stringResource(R.string.batch_edit_time),
                    icon = Icons.Outlined.AccessTime,
                    checked = timeOn,
                    onCheckedChange = {
                        timeOn = it
                        if (it) showTimePicker = true
                    },
                ) {
                    BatchEditValueRow(
                        value = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                        supporting = stringResource(R.string.batch_edit_time_hint),
                        action = stringResource(R.string.batch_edit_pick),
                        onClick = { showTimePicker = true },
                    )
                }

                BatchEditOptionCard(
                    title = stringResource(R.string.batch_edit_note),
                    icon = Icons.AutoMirrored.Outlined.Notes,
                    checked = noteOn,
                    onCheckedChange = { noteOn = it },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = !noteClear,
                                onClick = { noteClear = false },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                            ) { Text(stringResource(R.string.batch_edit_note_set)) }
                            SegmentedButton(
                                selected = noteClear,
                                onClick = { noteClear = true },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                            ) { Text(stringResource(R.string.batch_edit_note_clear)) }
                        }
                        if (noteClear) {
                            Text(
                                text = stringResource(R.string.batch_edit_note_clear_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                placeholder = { Text(stringResource(R.string.batch_edit_note_placeholder)) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                            )
                        }
                    }
                }

                BatchEditOptionCard(
                    title = stringResource(R.string.batch_edit_tags),
                    icon = Icons.AutoMirrored.Outlined.Label,
                    checked = tagsOn,
                    onCheckedChange = { tagsOn = it },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        val modes = listOf(
                            TagEditMode.ADD to R.string.batch_edit_tags_add,
                            TagEditMode.REMOVE to R.string.batch_edit_tags_remove,
                            TagEditMode.REPLACE to R.string.batch_edit_tags_replace,
                        )
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            modes.forEachIndexed { index, (mode, label) ->
                                SegmentedButton(
                                    selected = tagMode == mode,
                                    onClick = { tagMode = mode },
                                    shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                                ) { Text(stringResource(label)) }
                            }
                        }
                        TagInputField(
                            selectedTags = tags,
                            allTags = allTags,
                            onAddTag = { name -> tags = tags + name },
                            onRemoveTag = { name -> tags = tags - name },
                        )
                        if (tagMode == TagEditMode.REPLACE && tags.isEmpty()) {
                            Text(
                                text = stringResource(R.string.batch_edit_tags_replace_empty_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.size(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.batch_edit_cancel))
                }
                Button(
                    onClick = {
                        onApply(
                            BatchEditRequest(
                                category = if (categoryOn) category else null,
                                date = if (dateOn) date else null,
                                time = if (timeOn) time else null,
                                note = when {
                                    !noteOn -> FieldEdit.Unchanged
                                    noteClear -> FieldEdit.Clear
                                    else -> FieldEdit.Set(note.trim())
                                },
                                tags = if (tagsOn) TagEdit(tagMode, tags) else null,
                            )
                        )
                    },
                    enabled = canApply,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.batch_edit_apply))
                }
            }
        }
    }

    if (showCategoryPicker) {
        QuickCategoryPickerSheet(
            currentCategory = category ?: "",
            categories = categories,
            onCategorySelected = { name ->
                category = name
                categoryOn = true
                showCategoryPicker = false
            },
            onDismiss = {
                showCategoryPicker = false
                if (category == null) categoryOn = false
            },
        )
    }

    if (showDatePicker) {
        BatchDatePickerDialog(
            initial = date,
            onConfirm = { date = it; showDatePicker = false },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showTimePicker) {
        BatchTimePickerDialog(
            initial = time,
            onConfirm = { time = it; showTimePicker = false },
            onDismiss = { showTimePicker = false },
        )
    }
}

/** Cashiro's switch-gated field card, in PennyWise's glass material. */
@Composable
private fun BatchEditOptionCard(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .txnGlass(shape = shape)
            .border(
                BorderStroke(
                    if (checked) 1.5.dp else 1.dp,
                    if (checked) colors.primary else colors.outlineVariant.copy(alpha = 0.5f),
                ),
                shape,
            )
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Switch) { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(Dimensions.Icon.small),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                color = if (checked) colors.primary else colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            // The row owns the toggle, so the switch itself isn't a second target.
            Switch(checked = checked, onCheckedChange = null)
        }
        AnimatedVisibility(visible = checked) {
            Box(Modifier.padding(top = Spacing.xs)) { content() }
        }
    }
}

@Composable
private fun BatchEditValueRow(
    value: String,
    action: String,
    onClick: () -> Unit,
    supporting: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .txnGlass(
                shape = RoundedCornerShape(12.dp),
                tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            )
            .clickable(onClick = onClick)
            .padding(start = Spacing.md, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onClick) { Text(action) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchDatePickerDialog(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toEpochDay() * 24 * 60 * 60 * 1000
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                // The picker reports UTC midnight of the chosen day.
                state.selectedDateMillis?.let { millis ->
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                } ?: onDismiss()
            }) { Text(stringResource(R.string.txn_detail_action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.txn_detail_action_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchTimePickerDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.txn_detail_select_time)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.txn_detail_action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.txn_detail_action_cancel)) }
        },
    )
}
