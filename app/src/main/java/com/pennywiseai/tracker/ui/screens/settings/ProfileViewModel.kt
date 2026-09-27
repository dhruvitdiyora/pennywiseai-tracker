package com.pennywiseai.tracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class ProfileSaveError {
    BLANK_NAME,
    DUPLICATE_NAME,
    SAVE_FAILED,
}

internal data class ProfileEditorState(
    val isSaving: Boolean = false,
    val error: ProfileSaveError? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
) : ViewModel() {
    val profiles: StateFlow<List<ProfileEntity>> = repository.observeAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _editorState = MutableStateFlow(ProfileEditorState())
    internal val editorState: StateFlow<ProfileEditorState> = _editorState.asStateFlow()

    fun createProfile(
        name: String,
        colorHex: String,
        onSaved: () -> Unit,
    ) {
        saveProfile(name = name, excludedId = null, onSaved = onSaved) { current ->
            repository.insert(newProfile(current, name, colorHex))
        }
    }

    fun updateProfile(
        profile: ProfileEntity,
        name: String,
        colorHex: String,
        onSaved: () -> Unit,
    ) {
        saveProfile(name = name, excludedId = profile.id, onSaved = onSaved) {
            repository.update(
                profile.copy(
                    name = name.trim(),
                    colorHex = colorHex,
                ),
            )
        }
    }

    fun clearEditorError() {
        _editorState.update { it.copy(error = null) }
    }

    private fun saveProfile(
        name: String,
        excludedId: Long?,
        onSaved: () -> Unit,
        persist: suspend (List<ProfileEntity>) -> Unit,
    ) {
        if (_editorState.value.isSaving) return
        viewModelScope.launch {
            val current = runCatching { repository.getAllProfiles() }.getOrElse {
                _editorState.value = ProfileEditorState(error = ProfileSaveError.SAVE_FAILED)
                return@launch
            }
            val error = validateProfileName(name, current, excludedId)
            if (error != null) {
                _editorState.value = ProfileEditorState(error = error)
                return@launch
            }

            _editorState.value = ProfileEditorState(isSaving = true)
            runCatching { persist(current) }
                .onSuccess {
                    _editorState.value = ProfileEditorState()
                    onSaved()
                }
                .onFailure {
                    _editorState.value = ProfileEditorState(error = ProfileSaveError.SAVE_FAILED)
                }
        }
    }
}

internal fun newProfile(
    profiles: List<ProfileEntity>,
    name: String,
    colorHex: String,
): ProfileEntity = ProfileEntity(
    id = (profiles.maxOfOrNull(ProfileEntity::id) ?: ProfileEntity.BUSINESS_ID) + 1L,
    name = name.trim(),
    colorHex = colorHex,
    sortOrder = (profiles.maxOfOrNull(ProfileEntity::sortOrder) ?: -1) + 1,
)

internal fun validateProfileName(
    name: String,
    profiles: List<ProfileEntity>,
    excludedId: Long?,
): ProfileSaveError? {
    val normalized = name.trim()
    return when {
        normalized.isBlank() -> ProfileSaveError.BLANK_NAME
        profiles.any {
            it.id != excludedId && it.name.trim().equals(normalized, ignoreCase = true)
        } -> ProfileSaveError.DUPLICATE_NAME
        else -> null
    }
}
