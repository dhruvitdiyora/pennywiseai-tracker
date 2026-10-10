package com.pennywiseai.tracker.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.ui.components.ColorPickerContent
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.components.cards.RowLabels
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.parseProfileColor
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    var editedProfile by remember { mutableStateOf<ProfileEntity?>(null) }
    var isCreating by remember { mutableStateOf(false) }

    ProfileScreenContent(
        profiles = profiles,
        onNavigateBack = onNavigateBack,
        onAddProfile = {
            viewModel.clearEditorError()
            isCreating = true
        },
        onEditProfile = {
            viewModel.clearEditorError()
            editedProfile = it
        },
        modifier = modifier,
    )

    if (isCreating) {
        ProfileEditorDialog(
            profile = null,
            state = editorState,
            onInputChanged = viewModel::clearEditorError,
            onDismiss = {
                viewModel.clearEditorError()
                isCreating = false
            },
            onSave = { name, color ->
                viewModel.createProfile(name, color) { isCreating = false }
            },
        )
    }

    editedProfile?.let { profile ->
        ProfileEditorDialog(
            profile = profile,
            state = editorState,
            onInputChanged = viewModel::clearEditorError,
            onDismiss = {
                viewModel.clearEditorError()
                editedProfile = null
            },
            onSave = { name, color ->
                viewModel.updateProfile(profile, name, color) { editedProfile = null }
            },
        )
    }
}

@Composable
internal fun ProfileScreenContent(
    profiles: List<ProfileEntity>,
    onNavigateBack: () -> Unit,
    onAddProfile: () -> Unit,
    onEditProfile: (ProfileEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    PennyWiseScaffold(
        modifier = modifier,
        title = stringResource(R.string.profile_settings_title),
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.profile_navigate_back),
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProfile,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                    )
                },
                text = { Text(stringResource(R.string.profile_add)) },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Spacing.md,
                bottom = Dimensions.Component.fabScrollClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.sectionGap),
        ) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    tint = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(Dimensions.Icon.avatarLarge),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(Spacing.smd),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text(
                                text = stringResource(R.string.profile_settings_summary),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = stringResource(R.string.profile_settings_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)) {
                    SectionHeaderV2(title = stringResource(R.string.profile_your_profiles))
                    if (profiles.isEmpty()) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(Dimensions.Component.progressIndicatorSize),
                        )
                    } else {
                        GroupedList {
                            profiles.forEachIndexed { index, profile ->
                                ProfileRow(
                                    profile = profile,
                                    position = ListItemPosition.from(index, profiles.size),
                                    onClick = { onEditProfile(profile) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: ProfileEntity,
    position: ListItemPosition,
    onClick: () -> Unit,
) {
    val isBuiltIn = profile.id == ProfileEntity.PERSONAL_ID ||
        profile.id == ProfileEntity.BUSINESS_ID
    val fallback = MaterialTheme.colorScheme.primary
    GlassGroupedRow(
        position = position,
        onClick = onClick,
        minHeight = Dimensions.Component.listItemMinHeightTwoLine,
    ) {
        Surface(
            modifier = Modifier.size(Dimensions.Icon.avatar),
            shape = CircleShape,
            color = parseProfileColor(profile.colorHex, fallback),
        ) {}
        RowLabels(
            title = profile.name,
            subtitle = stringResource(
                if (isBuiltIn) R.string.profile_built_in else R.string.profile_custom,
            ),
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = stringResource(R.string.profile_edit_named, profile.name),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimensions.Icon.inline),
        )
    }
}

@Composable
internal fun ProfileEditorDialog(
    profile: ProfileEntity?,
    state: ProfileEditorState,
    onInputChanged: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember(profile) { mutableStateOf(profile?.name.orEmpty()) }
    var color by remember(profile) { mutableStateOf(profile?.colorHex ?: "#5E35B1") }
    var showColorPicker by remember(profile) { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.glassDialog(),
        containerColor = Color.Transparent,
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        title = {
            Text(
                stringResource(
                    if (profile == null) R.string.profile_add else R.string.profile_edit,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onInputChanged()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                    label = { Text(stringResource(R.string.profile_name)) },
                    singleLine = true,
                    isError = state.error != null,
                    supportingText = state.error?.let { error ->
                        { Text(stringResource(error.messageResource)) }
                    },
                )
                OutlinedButton(
                    onClick = { showColorPicker = true },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Surface(
                        modifier = Modifier.size(Dimensions.Icon.medium),
                        shape = CircleShape,
                        color = parseProfileColor(color, MaterialTheme.colorScheme.primary),
                    ) {}
                    Spacer(modifier = Modifier.size(Spacing.sm))
                    Text(stringResource(R.string.profile_choose_color))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, color) },
                enabled = name.isNotBlank() && !state.isSaving,
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimensions.Icon.inline),
                        strokeWidth = Dimensions.Component.progressRingStroke,
                    )
                } else {
                    Text(stringResource(R.string.profile_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) {
                Text(stringResource(R.string.profile_cancel))
            }
        },
    )

    if (showColorPicker) {
        AlertDialog(
            modifier = Modifier.glassDialog(),
            containerColor = Color.Transparent,
            onDismissRequest = { showColorPicker = false },
            title = { Text(stringResource(R.string.profile_choose_color)) },
            text = {
                ColorPickerContent(
                    selectedColor = color,
                    onColorChanged = { color = it },
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

private val ProfileSaveError.messageResource: Int
    get() = when (this) {
        ProfileSaveError.BLANK_NAME -> R.string.profile_error_blank_name
        ProfileSaveError.DUPLICATE_NAME -> R.string.profile_error_duplicate_name
        ProfileSaveError.SAVE_FAILED -> R.string.profile_error_save_failed
    }
