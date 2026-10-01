package com.pennywiseai.tracker.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.preferences.CoverStyle
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import com.pennywiseai.tracker.data.profile.ProfileImageStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Preset avatars use `avatar://INDEX`; anything else is a private photo file URI. */
internal const val DEFAULT_AVATAR_URI = "avatar://0"

internal fun isPresetAvatar(uri: String): Boolean = uri.startsWith("avatar://")

/** The editable fields of the profile sheet. */
internal data class ProfileDraft(
    val name: String = "",
    val avatarUri: String = DEFAULT_AVATAR_URI,
    /** ARGB int; 0 means "no explicit colour" (the theme's container colour is used). */
    val avatarColor: Int = 0,
    val bannerUri: String? = null,
)

internal data class EditProfileUiState(
    val isLoaded: Boolean = false,
    val baseline: ProfileDraft = ProfileDraft(),
    val draft: ProfileDraft = ProfileDraft(),
    /** Used to draw the banner preview when the user has no custom banner. */
    val coverStyle: CoverStyle = CoverStyle.AURORA,
    val isSaving: Boolean = false,
    val imageError: Boolean = false,
) {
    val hasChanges: Boolean get() = draft != baseline
    val canSave: Boolean get() = isLoaded && !isSaving && hasChanges && draft.name.isNotBlank()
}

/**
 * Drives the "Edit profile" sheet. Edits live in a draft; nothing touches DataStore
 * until [save]. Images picked during a session are copied into private storage right
 * away (so previews survive the picker's temporary grant) and tracked in [staged] so
 * they can be deleted again if the user cancels or replaces them.
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val imageStore: ProfileImageStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    internal val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    /** Image files created during this edit session that aren't saved yet. */
    private val staged = mutableSetOf<String>()

    /** Loads the current profile into the draft. A no-op while a session is already open. */
    fun beginEditing() {
        if (_uiState.value.isLoaded) return
        viewModelScope.launch {
            val prefs = userPreferencesRepository.userPreferences.first()
            if (_uiState.value.isLoaded) return@launch
            val loaded = ProfileDraft(
                name = prefs.userName,
                avatarUri = prefs.profileImageUri ?: DEFAULT_AVATAR_URI,
                avatarColor = prefs.profileBackgroundColor,
                bannerUri = prefs.homeBannerUri,
            )
            _uiState.value = EditProfileUiState(
                isLoaded = true,
                baseline = loaded,
                draft = loaded,
                coverStyle = prefs.coverStyle,
            )
        }
    }

    fun onNameChange(name: String) = updateDraft { it.copy(name = name) }

    fun onAvatarColorChange(argb: Int) = updateDraft { it.copy(avatarColor = argb) }

    fun onPresetAvatarSelected(index: Int) = setAvatar("avatar://$index")

    /** "Clear": drops the custom photo and falls back to the default preset avatar. */
    fun onClearAvatarPhoto() = setAvatar(DEFAULT_AVATAR_URI)

    fun onAvatarPhotoPicked(source: Uri) {
        viewModelScope.launch {
            val saved = imageStore.import(source, AVATAR_PREFIX)
            if (saved == null) {
                _uiState.update { it.copy(imageError = true) }
            } else {
                staged += saved
                setAvatar(saved)
            }
        }
    }

    fun onBannerPicked(source: Uri) {
        viewModelScope.launch {
            val saved = imageStore.import(source, BANNER_PREFIX)
            if (saved == null) {
                _uiState.update { it.copy(imageError = true) }
            } else {
                staged += saved
                setBanner(saved)
            }
        }
    }

    fun onBannerRemoved() = setBanner(null)

    /** Writes the draft to DataStore in one edit, then deletes image files it replaced. */
    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val draft = state.draft
            userPreferencesRepository.saveProfileIdentity(
                name = draft.name.trim(),
                profileImageUri = draft.avatarUri,
                profileBackgroundColor = draft.avatarColor,
                homeBannerUri = draft.bannerUri,
            )
            // Old saved images that the draft no longer points at, plus staged leftovers.
            val kept = setOfNotNull(draft.avatarUri, draft.bannerUri)
            (staged + listOfNotNull(state.baseline.avatarUri, state.baseline.bannerUri))
                .filter { it !in kept }
                .forEach(imageStore::deleteIfOwned)
            staged.clear()
            // State is reset by discard() once the sheet has finished hiding.
            onSaved()
        }
    }

    /**
     * Ends the edit session: throws the draft away and removes any images still staged
     * for it. Called whenever the sheet closes, after a save too (staged is then empty).
     */
    fun discard() {
        staged.forEach(imageStore::deleteIfOwned)
        staged.clear()
        _uiState.value = EditProfileUiState()
    }

    override fun onCleared() {
        staged.forEach(imageStore::deleteIfOwned)
        staged.clear()
        super.onCleared()
    }

    private fun setAvatar(uri: String) {
        releaseStaged(_uiState.value.draft.avatarUri, replacedBy = uri)
        updateDraft { it.copy(avatarUri = uri) }
    }

    private fun setBanner(uri: String?) {
        releaseStaged(_uiState.value.draft.bannerUri, replacedBy = uri)
        updateDraft { it.copy(bannerUri = uri) }
    }

    /** A staged image that the draft is about to stop using can be deleted straight away. */
    private fun releaseStaged(old: String?, replacedBy: String?) {
        if (old != null && old != replacedBy && staged.remove(old)) {
            imageStore.deleteIfOwned(old)
        }
    }

    private fun updateDraft(transform: (ProfileDraft) -> ProfileDraft) {
        _uiState.update { it.copy(draft = transform(it.draft), imageError = false) }
    }

    private companion object {
        const val AVATAR_PREFIX = "avatar"
        const val BANNER_PREFIX = "banner"
    }
}
