package com.pennywiseai.tracker.ui.screens.onboarding

import com.pennywiseai.tracker.ui.screens.settings.glassPanel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.presentation.people.contentColorOn
import com.pennywiseai.tracker.presentation.people.tonalTextFieldColors
import com.pennywiseai.tracker.ui.icons.iconax.Edit2
import com.pennywiseai.tracker.ui.icons.iconax.GalleryExport
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.screens.FirstRunColumn
import com.pennywiseai.tracker.ui.screens.FirstRunHeading
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/**
 * The profile step: a live identity card, the name field, the avatar strip and
 * the background colour palette. Everything it shows and changes is the
 * existing [OnBoardingViewModel] state (name, preset avatar or picked photo,
 * background colour index); only the presentation is new.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OnboardingProfileStep(uiState: OnBoardingUiState, viewModel: OnBoardingViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let(viewModel::selectProfileImage)
    }
    val avatarColor = viewModel.backgroundColors
        .getOrNull(uiState.selectedBackgroundColor)
        ?.let { Color(it) }
        ?: MaterialTheme.colorScheme.primaryContainer

    FirstRunColumn {
        FirstRunHeading(
            title = stringResource(R.string.onboarding_profile_title),
            body = stringResource(R.string.onboarding_profile_body)
        )
        Spacer(Modifier.height(Spacing.lg))
        ProfileIdentityCard(uiState = uiState, viewModel = viewModel, avatarColor = avatarColor)
        Spacer(Modifier.height(Spacing.lg))
        TextField(
            value = uiState.userName,
            onValueChange = viewModel::updateUserName,
            label = {
                Text(
                    text = stringResource(R.string.onboarding_profile_name_label),
                    fontWeight = FontWeight.SemiBold
                )
            },
            leadingIcon = { Icon(Iconax.Edit2, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = MaterialTheme.shapes.large,
            colors = tonalTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Spacing.lg))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            ProfileSectionTitle(stringResource(R.string.onboarding_profile_avatar))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.smd)
            ) {
                itemsIndexed(viewModel.avatarDrawables) { index, drawable ->
                    val selected = uiState.profileImageUri == null && uiState.selectedAvatarIndex == index
                    AvatarOptionTile(
                        selected = selected,
                        tileColor = avatarColor,
                        onClick = { viewModel.selectAvatar(index) }
                    ) {
                        Image(
                            painter = painterResource(drawable),
                            contentDescription = stringResource(R.string.onboarding_avatar_description, index + 1),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                item {
                    val hasPhoto = uiState.profileImageUri != null
                    AvatarOptionTile(
                        selected = hasPhoto,
                        tileColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        onClick = { picker.launch("image/*") }
                    ) {
                        if (hasPhoto) {
                            AsyncImage(
                                model = uiState.profileImageUri,
                                contentDescription = stringResource(R.string.onboarding_photo_selected),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Iconax.GalleryExport,
                                contentDescription = stringResource(R.string.onboarding_add_photo),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .glassPanel(shape = MaterialTheme.shapes.large),
            shape = MaterialTheme.shapes.large,
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier.padding(Dimensions.Padding.card),
                verticalArrangement = Arrangement.spacedBy(Spacing.smd)
            ) {
                ProfileSectionTitle(stringResource(R.string.onboarding_profile_background))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    viewModel.backgroundColors.forEachIndexed { index, colorInt ->
                        val swatch = Color(colorInt)
                        val selected = uiState.selectedBackgroundColor == index
                        val description = stringResource(R.string.onboarding_background_description, index + 1)
                        Box(
                            modifier = Modifier
                                .size(Dimensions.Component.minTouchTarget)
                                .clip(CircleShape)
                                .background(swatch)
                                .then(
                                    if (selected) {
                                        Modifier.border(
                                            Dimensions.Component.onboardingSelectionStroke,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .semantics { contentDescription = description }
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { viewModel.selectBackgroundColor(index) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.onboarding_selected),
                                    tint = contentColorOn(swatch),
                                    modifier = Modifier.size(Dimensions.Icon.medium)
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
private fun ProfileSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() }
    )
}

/**
 * A live preview of the identity being built: the avatar over the chosen
 * colour, and the name as typed. Decorative duplicate of the controls below it,
 * so it is hidden from accessibility services.
 */
@Composable
private fun ProfileIdentityCard(
    uiState: OnBoardingUiState,
    viewModel: OnBoardingViewModel,
    avatarColor: Color,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { }
            .glassPanel(shape = MaterialTheme.shapes.large)
    ) {
        // The chosen colour washes down from the top edge into the card.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(avatarColor.copy(alpha = Dimensions.Alpha.medium), Color.Transparent)
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.smd)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimensions.Icon.extraLarge)
                    .border(Dimensions.Component.selectionStroke, scheme.surfaceContainerLow, CircleShape)
                    .padding(Dimensions.Component.selectionStroke)
                    .clip(CircleShape)
                    .background(avatarColor)
            ) {
                if (uiState.profileImageUri != null) {
                    AsyncImage(
                        model = uiState.profileImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(
                            viewModel.avatarDrawables.getOrElse(uiState.selectedAvatarIndex) {
                                viewModel.avatarDrawables.first()
                            }
                        ),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            val named = uiState.userName.isNotBlank()
            Text(
                text = if (named) {
                    uiState.userName
                } else {
                    stringResource(R.string.onboarding_profile_preview_name)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (named) scheme.onSurface else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** One round, selectable tile in the avatar strip; the selected one gets a strong ring. */
@Composable
private fun AvatarOptionTile(
    selected: Boolean,
    tileColor: Color,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .size(Dimensions.Component.onboardingOptionTile)
            .clip(CircleShape)
            .background(tileColor)
            .then(
                if (selected) {
                    Modifier.border(
                        Dimensions.Component.onboardingSelectionStroke,
                        MaterialTheme.colorScheme.onSurface,
                        CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}
