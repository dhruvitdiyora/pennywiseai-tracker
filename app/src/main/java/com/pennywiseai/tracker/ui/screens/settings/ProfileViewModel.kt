package com.pennywiseai.tracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.repository.AccountBalanceRepository
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.data.repository.TransactionRepository
import com.pennywiseai.tracker.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProfileError {
    BLANK_NAME,
    DUPLICATE_NAME,
    BUILT_IN_DELETE,
    REASSIGNMENT_REQUIRED
}

data class ProfileUiState(
    val isSaving: Boolean = false,
    val error: ProfileError? = null,
    val profilePendingDeletion: ProfileEntity? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    val profiles: StateFlow<List<ProfileEntity>> = repository.observeAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun save(profile: ProfileEntity) {
        viewModelScope.launch {
            val name = profile.name.trim()
            if (name.isBlank()) {
                _uiState.update { it.copy(error = ProfileError.BLANK_NAME) }
                return@launch
            }
            if (hasDuplicateName(name, profile.id)) {
                _uiState.update { it.copy(error = ProfileError.DUPLICATE_NAME) }
                return@launch
            }
            _uiState.update { it.copy(isSaving = true, error = null) }
            repository.insert(profile.copy(name = name))
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun createProfile(name: String, colorHex: String) {
        val nextId = (profiles.value.maxOfOrNull { it.id } ?: ProfileEntity.BUSINESS_ID) + 1L
        val nextSortOrder = (profiles.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
        save(ProfileEntity(nextId, name, colorHex, nextSortOrder))
    }

    fun updateProfile(profile: ProfileEntity, name: String, colorHex: String) {
        save(profile.copy(name = name, colorHex = colorHex))
    }

    fun requestDelete(profile: ProfileEntity) {
        if (profile.id == ProfileEntity.PERSONAL_ID || profile.id == ProfileEntity.BUSINESS_ID) {
            _uiState.update { it.copy(error = ProfileError.BUILT_IN_DELETE) }
            return
        }
        _uiState.update { it.copy(profilePendingDeletion = profile, error = null) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(profilePendingDeletion = null) }
    }

    fun deleteProfile(profile: ProfileEntity, reassignTo: ProfileEntity?) {
        if (reassignTo == null || reassignTo.id == profile.id) {
            _uiState.update { it.copy(error = ProfileError.REASSIGNMENT_REQUIRED) }
            return
        }
        if (profile.id == ProfileEntity.PERSONAL_ID || profile.id == ProfileEntity.BUSINESS_ID) {
            _uiState.update { it.copy(error = ProfileError.BUILT_IN_DELETE) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            accountBalanceRepository.getAllLatestBalances().first()
                .filter { it.profileId == profile.id }
                .distinctBy { it.bankName to it.accountLast4 }
                .forEach { account ->
                    accountBalanceRepository.setAccountProfile(
                        account.bankName,
                        account.accountLast4,
                        reassignTo.id
                    )
                }

            transactionRepository.getAllTransactions().first()
                .filter { it.profileId == profile.id }
                .forEach { transaction ->
                    transactionRepository.updateTransaction(transaction.copy(profileId = reassignTo.id))
                }

            repository.deleteById(profile.id)
            _uiState.update { it.copy(isSaving = false, profilePendingDeletion = null) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun hasDuplicateName(name: String, excludedId: Long): Boolean =
        profiles.value.any { it.id != excludedId && it.name.trim().equals(name, ignoreCase = true) }
}
