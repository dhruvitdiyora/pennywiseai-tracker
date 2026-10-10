package com.pennywiseai.tracker.ui.screens.profile

import com.pennywiseai.tracker.ui.screens.settings.glassSheet
import com.pennywiseai.tracker.ui.screens.settings.glassPanel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.preferences.CoverStyle
import com.pennywiseai.tracker.ui.components.AvatarHelper
import com.pennywiseai.tracker.ui.components.ColorPickerContent
import com.pennywiseai.tracker.ui.components.getCoverGradientColors
import com.pennywiseai.tracker.ui.components.parseProfileColor
import com.pennywiseai.tracker.ui.icons.iconax.Camera
import com.pennywiseai.tracker.ui.icons.iconax.CloseCircle
import com.pennywiseai.tracker.ui.icons.iconax.Edit2
import com.pennywiseai.tracker.ui.icons.iconax.GalleryExport
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import java.util.Locale
import kotlinx.coroutines.launch

private val BannerPreviewHeight = 168.dp
private val AvatarPreviewSize = 88.dp
private val PresetAvatarSize = 80.dp
private val SaveButtonHeight = 56.dp
private val SaveBarClearance = 104.dp

/**
 * Modal sheet for editing the user's name, avatar (preset or gallery photo), avatar
 * colour and Home banner. Edits stay in a draft until Save; swiping the sheet away,
 * tapping outside or pressing back discards them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    onDismiss: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.beginEditing() }

    // Photo Picker: no storage permission needed; the result is copied to private storage.
    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) viewModel.onAvatarPhotoPicked(uri) }
    val bannerPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) viewModel.onBannerPicked(uri) }
    val imageOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.discard()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        if (!state.isLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BannerPreviewHeight)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            EditProfileSheetContent(
                state = state,
                onNameChange = viewModel::onNameChange,
                onPresetAvatarSelected = viewModel::onPresetAvatarSelected,
                onPickAvatarPhoto = { avatarPicker.launch(imageOnly) },
                onClearAvatarPhoto = viewModel::onClearAvatarPhoto,
                onPickBanner = { bannerPicker.launch(imageOnly) },
                onRemoveBanner = viewModel::onBannerRemoved,
                onAvatarColorChange = viewModel::onAvatarColorChange,
                onSave = {
                    viewModel.save {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            viewModel.discard()
                            onDismiss()
                        }
                    }
                },
            )
        }
    }
}

@Composable
internal fun EditProfileSheetContent(
    state: EditProfileUiState,
    onNameChange: (String) -> Unit,
    onPresetAvatarSelected: (Int) -> Unit,
    onPickAvatarPhoto: () -> Unit,
    onClearAvatarPhoto: () -> Unit,
    onPickBanner: () -> Unit,
    onRemoveBanner: () -> Unit,
    onAvatarColorChange: (Int) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = state.draft
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = Spacing.xs, bottom = SaveBarClearance),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.edit_profile_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            ProfilePreview(
                draft = draft,
                coverStyle = state.coverStyle,
                modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
            )

            TextField(
                value = draft.name,
                onValueChange = onNameChange,
                label = {
                    Text(
                        text = stringResource(R.string.edit_profile_name_label),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                leadingIcon = { Icon(Iconax.Edit2, contentDescription = null) },
                // Save is disabled for a blank name; say why rather than leave it greyed out.
                isError = draft.name.isBlank(),
                supportingText = if (draft.name.isBlank()) {
                    { Text(stringResource(R.string.edit_profile_name_required)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = MaterialTheme.shapes.large,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.Padding.content),
            )

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SheetSectionLabel(stringResource(R.string.edit_profile_section_avatar))
                PresetAvatarRow(
                    selectedUri = draft.avatarUri,
                    avatarColor = draft.avatarColor,
                    onSelect = onPresetAvatarSelected,
                )
                PhotoButtons(
                    canClear = !isPresetAvatar(draft.avatarUri),
                    onGallery = onPickAvatarPhoto,
                    onClear = onClearAvatarPhoto,
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                )
                SheetSectionLabel(
                    text = stringResource(R.string.edit_profile_section_banner),
                    modifier = Modifier.padding(top = Spacing.sm),
                )
                BannerButtons(
                    hasBanner = draft.bannerUri != null,
                    onChange = onPickBanner,
                    onRemove = onRemoveBanner,
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                )
                if (state.imageError) {
                    Text(
                        text = stringResource(R.string.edit_profile_image_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                    )
                }
            }

            ColorsCard(
                avatarColor = draft.avatarColor,
                onColorChange = onAvatarColorChange,
                modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
            )
        }

        // Pinned Save bar: the content scrolls underneath and fades out behind it.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = Spacing.lg,
                    bottom = Spacing.md,
                ),
        ) {
            Button(
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SaveButtonHeight),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = stringResource(R.string.profile_save),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/** Banner (custom image, else the Home cover style) with the avatar over its centre. */
@Composable
private fun ProfilePreview(
    draft: ProfileDraft,
    coverStyle: CoverStyle,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            // A floor, not a fixed height: at large font scales the name pushes it taller.
            .heightIn(min = BannerPreviewHeight)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (draft.bannerUri != null) {
            AsyncImage(
                model = draft.bannerUri,
                contentDescription = stringResource(R.string.edit_profile_banner_description),
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (coverStyle != CoverStyle.NONE) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            getCoverGradientColors(
                                style = coverStyle,
                                isDark = isSystemInDarkTheme(),
                                forPreview = true,
                            ),
                        ),
                    ),
            )
        }
        // Dissolve the bottom edge into the sheet, like the Home header does.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to surface)),
        )
        // The avatar and name as the profile page will show them, so a rename is
        // previewed live alongside the picture and colour.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.smd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            AvatarBubble(
                avatarUri = draft.avatarUri,
                avatarColor = draft.avatarColor,
                size = AvatarPreviewSize,
                ringColor = surface,
            )
            Text(
                text = draft.name.trim().ifEmpty { stringResource(R.string.greeting_default_user_name) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A small heading that groups the sheet's controls (Avatar, Banner). */
@Composable
private fun SheetSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .padding(horizontal = Dimensions.Padding.content)
            .semantics { heading() },
    )
}

@Composable
private fun AvatarBubble(
    avatarUri: String,
    avatarColor: Int,
    size: Dp,
    ringColor: Color,
    modifier: Modifier = Modifier,
) {
    val background = if (avatarColor != 0) Color(avatarColor) else MaterialTheme.colorScheme.primaryContainer
    val ring = Dimensions.Component.selectionStroke
    Box(
        modifier = modifier
            .size(size)
            .border(ring, ringColor, CircleShape)
            .padding(ring)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        val presetRes = AvatarHelper.resolveAvatarDrawable(avatarUri)
        if (presetRes != null) {
            Image(
                painter = painterResource(presetRes),
                contentDescription = stringResource(R.string.edit_profile_photo_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            AsyncImage(
                model = avatarUri,
                contentDescription = stringResource(R.string.edit_profile_photo_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun PresetAvatarRow(
    selectedUri: String,
    avatarColor: Int,
    onSelect: (Int) -> Unit,
) {
    val tileColor = if (avatarColor != 0) Color(avatarColor) else MaterialTheme.colorScheme.primaryContainer
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
        contentPadding = PaddingValues(horizontal = Dimensions.Padding.content, vertical = Spacing.xs),
    ) {
        itemsIndexed(AvatarHelper.avatarDrawables) { index, drawable ->
            val isSelected = selectedUri == "avatar://$index"
            val description = stringResource(R.string.onboarding_avatar_description, index + 1)
            Box(
                modifier = Modifier
                    .size(PresetAvatarSize)
                    .clip(CircleShape)
                    .background(tileColor)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                Dimensions.Component.selectionStroke,
                                MaterialTheme.colorScheme.primary,
                                CircleShape,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .semantics {
                        contentDescription = description
                        selected = isSelected
                        role = Role.RadioButton
                    }
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(drawable),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun PhotoButtons(
    canClear: Boolean,
    onGallery: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outer = Dimensions.CornerRadius.large
    val inner = Dimensions.CornerRadius.small
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
    ) {
        FilledTonalButton(
            onClick = onGallery,
            modifier = Modifier
                .weight(1f)
                .height(Dimensions.Component.buttonHeight),
            shape = RoundedCornerShape(topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner),
        ) {
            Icon(Iconax.GalleryExport, contentDescription = null)
            Spacer(Modifier.width(Spacing.sm))
            Text(stringResource(R.string.add_gallery))
        }
        FilledTonalButton(
            onClick = onClear,
            enabled = canClear,
            modifier = Modifier
                .weight(1f)
                .height(Dimensions.Component.buttonHeight),
            shape = RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer),
        ) {
            Icon(Iconax.CloseCircle, contentDescription = null)
            Spacer(Modifier.width(Spacing.sm))
            Text(stringResource(R.string.edit_profile_clear_photo))
        }
    }
}

@Composable
private fun BannerButtons(
    hasBanner: Boolean,
    onChange: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FilledTonalButton(
            onClick = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimensions.Component.buttonHeight),
        ) {
            Icon(Iconax.Camera, contentDescription = null)
            Spacer(Modifier.width(Spacing.sm))
            Text(stringResource(R.string.edit_profile_change_banner))
        }
        if (hasBanner) {
            TextButton(
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Iconax.CloseCircle, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.edit_profile_remove_banner))
            }
        }
    }
}

@Composable
private fun ColorsCard(
    avatarColor: Int,
    onColorChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fallback = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .glassPanel(shape = MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = stringResource(R.string.edit_profile_colors),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(Spacing.smd))
            ColorPickerContent(
                selectedColor = avatarColor.toHexOrEmpty(),
                onColorChanged = { hex -> onColorChange(parseProfileColor(hex, fallback).toArgb()) },
            )
        }
    }
}

/** "#RRGGBB" for the picker; empty (nothing selected) while no colour has been chosen. */
private fun Int.toHexOrEmpty(): String =
    if (this == 0) "" else String.format(Locale.US, "#%06X", 0xFFFFFF and this)
