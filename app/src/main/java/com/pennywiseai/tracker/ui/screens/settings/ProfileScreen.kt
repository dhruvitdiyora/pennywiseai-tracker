package com.pennywiseai.tracker.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.pennywiseai.tracker.presentation.people.PeopleExtendedFab
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

/**
 * Profiles, in the Settings sub-screen chrome: a large title that collapses
 * into the blurred bar, the tonal back button, and the tonal "+" FAB used by
 * People and Loans.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileScreenContent(
    profiles: List<ProfileEntity>,
    onNavigateBack: () -> Unit,
    onAddProfile: () -> Unit,
    onEditProfile: (ProfileEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    PennyWiseScaffold(
        modifier = modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        customTopBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.profile_settings_title),
                hasBackButton = true,
                navigationContent = {
                    SettingsBackButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.profile_navigate_back),
                    )
                },
                hazeState = hazeState,
            )
        },
        floatingActionButton = {
            PeopleExtendedFab(
                label = stringResource(R.string.profile_add),
                onClick = onAddProfile,
                expanded = fabExpanded,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
                .overScrollVertical(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + Dimensions.Component.fabScrollClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.sectionGap),
            flingBehavior = rememberOverscrollFlingBehavior { listState },
        ) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
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
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.profile_settings_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)) {
                    SectionHeaderV2(title = stringResource(R.string.profile_your_profiles), topSpacing = Spacing.none)
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
                TextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onInputChanged()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                    label = { Text(stringResource(R.string.profile_name), fontWeight = FontWeight.SemiBold) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    // The dialog is surfaceContainerHigh; the base surface sets the
                    // field off from it in both light and dark.
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        errorContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                    ),
                    isError = state.error != null,
                    supportingText = state.error?.let { error ->
                        { Text(stringResource(error.messageResource)) }
                    },
                )
                FilledTonalButton(
                    onClick = { showColorPicker = true },
                    enabled = !state.isSaving,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimensions.Component.minTouchTarget),
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
