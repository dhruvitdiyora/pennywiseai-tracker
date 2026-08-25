package com.pennywiseai.tracker.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.ui.components.ColorPickerContent
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<ProfileEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_settings_title)) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_navigate_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.profile_add))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Spacing.md)
        ) {
            item {
                Text(
                    text = stringResource(R.string.profile_settings_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
            }
            items(profiles, key = { it.id }) { profile ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
                        .padding(Dimensions.Padding.cardCompact),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileColorDot(profile.colorHex)
                    Spacer(Modifier.width(Spacing.md))
                    Text(profile.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { editing = profile }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.profile_edit))
                    }
                }
            }
        }
    }

    if (creating) {
        ProfileEditorDialog(
            profile = null,
            onDismiss = { creating = false },
            onSave = { name, color ->
                viewModel.save(ProfileEntity((profiles.maxOfOrNull { it.id } ?: 2L) + 1L, name, color, profiles.size))
                creating = false
            }
        )
    }
    editing?.let { profile ->
        ProfileEditorDialog(
            profile = profile,
            onDismiss = { editing = null },
            onSave = { name, color ->
                viewModel.save(profile.copy(name = name, colorHex = color))
                editing = null
            }
        )
    }
}

@Composable
private fun ProfileColorDot(hex: String) {
    val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)
    Spacer(Modifier.size(Dimensions.Icon.medium).clip(CircleShape).background(color))
}

@Composable
private fun ProfileEditorDialog(
    profile: ProfileEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(profile) { mutableStateOf(profile?.name.orEmpty()) }
    var color by remember(profile) { mutableStateOf(profile?.colorHex ?: "#6750A4") }
    var showColorPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (profile == null) R.string.profile_add else R.string.profile_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.profile_name)) },
                    singleLine = true
                )
                Button(onClick = { showColorPicker = true }) {
                    Text(stringResource(R.string.profile_choose_color))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), color) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )

    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text(stringResource(R.string.profile_choose_color)) },
            text = {
                ColorPickerContent(
                    selectedColor = color,
                    onColorChanged = { color = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) { Text(stringResource(R.string.select)) }
            }
        )
    }
}
