package com.pennywiseai.tracker.presentation.people

import com.pennywiseai.tracker.ui.screens.settings.glassDialog
import com.pennywiseai.tracker.ui.screens.settings.glassSheet
import androidx.compose.ui.graphics.Color
import com.pennywiseai.tracker.ui.screens.settings.glassPanel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.PersonEntity
import com.pennywiseai.tracker.ui.components.ColorPickerContent
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
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
    val scheme = MaterialTheme.colorScheme
    val chosenColor = parseProfileColor(color, scheme.primary)

    fun changed() = onInputChanged()

    ModalBottomSheet(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        sheetState = sheetState,
        // The fields are tonal (surfaceContainerLow), so the sheet sits one step
        // lighter to let them read as raised fields.
        modifier = Modifier.glassSheet(tint = scheme.surface),
        containerColor = Color.Transparent,
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = stringResource(
                        if (person == null) R.string.people_add_person else R.string.people_edit_person,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.people_editor_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            // Live preview: the avatar updates with the name and the chosen colour.
            PersonAvatar(
                initials = initialsOf(name),
                color = chosenColor,
                size = Dimensions.Icon.extraLarge,
                textStyle = MaterialTheme.typography.headlineMedium,
            )

            // Name, phone and notes read as one connected block of fields.
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)) {
                TonalTextField(
                    value = name,
                    onValueChange = { name = it; changed() },
                    label = stringResource(R.string.people_name),
                    position = ListItemPosition.Top,
                    enabled = !state.isSaving,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    isError = state.error == PersonSaveError.NAME_REQUIRED,
                    supportingText = state.error?.let { error -> stringResource(error.messageResource) },
                )
                TonalTextField(
                    value = phone,
                    onValueChange = { phone = it; changed() },
                    label = stringResource(R.string.people_phone_optional),
                    position = ListItemPosition.Middle,
                    enabled = !state.isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                TonalTextField(
                    value = notes,
                    onValueChange = { notes = it; changed() },
                    label = stringResource(R.string.people_notes_optional),
                    position = ListItemPosition.Bottom,
                    enabled = !state.isSaving,
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.people_category),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
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
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = scheme.surfaceContainerLow,
                                selectedContainerColor = scheme.tertiaryContainer,
                                selectedLabelColor = scheme.onTertiaryContainer,
                            ),
                            // The tonal fill is the container; no outline.
                            border = null,
                        )
                    }
                }
            }

            Surface(
                onClick = { showColorPicker = true },
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .glassPanel(shape = MaterialTheme.shapes.large),
                shape = MaterialTheme.shapes.large,
                color = Color.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = Dimensions.Component.listItemMinHeight)
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimensions.Icon.medium)
                            .clip(CircleShape)
                            .background(chosenColor),
                    )
                    Text(
                        text = stringResource(R.string.people_choose_color),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface,
                    )
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                    )
                }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimensions.Component.fab),
                enabled = name.isNotBlank() && !state.isSaving,
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        color = scheme.onPrimary,
                        modifier = Modifier.size(Dimensions.Icon.inline),
                        strokeWidth = Dimensions.Component.progressRingStroke,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.people_save),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }

    if (showColorPicker) {
        AlertDialog(
            modifier = Modifier.glassDialog(MaterialTheme.shapes.extraLarge),
            containerColor = Color.Transparent,
            onDismissRequest = { showColorPicker = false },
            shape = MaterialTheme.shapes.extraLarge,
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
