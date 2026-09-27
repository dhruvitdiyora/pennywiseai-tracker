package com.pennywiseai.tracker.presentation.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.pennywiseai.tracker.data.database.entity.PersonEntity
import com.pennywiseai.tracker.ui.components.ColorPickerContent
import com.pennywiseai.tracker.ui.components.parseProfileColor
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonEditorSheet(
    person: PersonEntity?,
    state: PersonEditorState,
    onInputChanged: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        phoneNumber: String?,
        notes: String?,
        category: String?,
        color: String,
    ) -> Unit,
) {
    var name by remember(person) { mutableStateOf(person?.name.orEmpty()) }
    var phone by remember(person) { mutableStateOf(person?.phoneNumber.orEmpty()) }
    var notes by remember(person) { mutableStateOf(person?.notes.orEmpty()) }
    var category by remember(person) { mutableStateOf(person?.category) }
    var color by remember(person) { mutableStateOf(person?.color ?: "#5E35B1") }
    var showColorPicker by remember { mutableStateOf(false) }
    val categories = listOf(
        stringResource(R.string.people_category_friend),
        stringResource(R.string.people_category_family),
        stringResource(R.string.people_category_work),
        stringResource(R.string.people_category_other),
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun changed() = onInputChanged()

    ModalBottomSheet(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(
                    start = Dimensions.Padding.dialog,
                    end = Dimensions.Padding.dialog,
                    bottom = Spacing.lg,
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = stringResource(
                    if (person == null) R.string.people_add_person else R.string.people_edit_person,
                ),
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.people_editor_description),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; changed() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
                label = { Text(stringResource(R.string.people_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                isError = state.error == PersonSaveError.NAME_REQUIRED,
                supportingText = state.error?.let { error ->
                    { Text(stringResource(error.messageResource)) }
                },
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; changed() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
                label = { Text(stringResource(R.string.people_phone_optional)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = stringResource(R.string.people_category),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    categories.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = {
                                category = if (category == option) null else option
                                changed()
                            },
                            enabled = !state.isSaving,
                            label = { Text(option) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it; changed() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
                label = { Text(stringResource(R.string.people_notes_optional)) },
                minLines = 2,
                maxLines = 4,
            )

            OutlinedButton(
                onClick = { showColorPicker = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
            ) {
                Surface(
                    modifier = Modifier.size(Dimensions.Icon.medium),
                    shape = CircleShape,
                    color = parseProfileColor(
                        color,
                        androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    ),
                ) {}
                Spacer(modifier = Modifier.size(Spacing.sm))
                Icon(Icons.Default.Palette, contentDescription = null)
                Spacer(modifier = Modifier.size(Spacing.sm))
                Text(stringResource(R.string.people_choose_color))
            }

            Button(
                onClick = {
                    onSave(
                        name,
                        phone.trim().ifBlank { null },
                        notes.trim().ifBlank { null },
                        category,
                        color,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && !state.isSaving,
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimensions.Icon.inline),
                        strokeWidth = Dimensions.Component.progressRingStroke,
                    )
                } else {
                    Text(stringResource(R.string.people_save))
                }
            }
        }
    }

    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text(stringResource(R.string.people_choose_color)) },
            text = {
                ColorPickerContent(
                    selectedColor = color,
                    onColorChanged = { color = it; changed() },
                )
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) {
                    Text(stringResource(R.string.profile_color_done))
                }
            },
        )
    }
}

private val PersonSaveError.messageResource: Int
    get() = when (this) {
        PersonSaveError.NAME_REQUIRED -> R.string.people_name_required
        PersonSaveError.SAVE_FAILED -> R.string.people_save_failed
    }
