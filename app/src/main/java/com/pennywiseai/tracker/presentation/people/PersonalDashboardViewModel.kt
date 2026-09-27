package com.pennywiseai.tracker.presentation.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import com.pennywiseai.tracker.data.repository.LoanRepository
import com.pennywiseai.tracker.data.repository.PersonLoanSummary
import com.pennywiseai.tracker.data.repository.PersonRepository
import com.pennywiseai.tracker.data.repository.PersonWithSummary
import com.pennywiseai.tracker.data.repository.summarizePersonLoans
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PersonalDashboardUiState(
    val userName: String = "User",
    val profileImageUri: String? = null,
    val profileBackgroundColor: Int = 0,
    val people: List<PersonWithSummary> = emptyList(),
    val loanSummary: PersonLoanSummary = PersonLoanSummary(-1L, ""),
    val isLoading: Boolean = true,
)

@HiltViewModel
class PersonalDashboardViewModel @Inject constructor(
    personRepository: PersonRepository,
    loanRepository: LoanRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<PersonalDashboardUiState> = combine(
        personRepository.observePeople(),
        loanRepository.getAllLoans(),
        userPreferencesRepository.userPreferences,
    ) { people, loans, preferences ->
        PersonalDashboardUiState(
            userName = preferences.userName,
            profileImageUri = preferences.profileImageUri,
            profileBackgroundColor = preferences.profileBackgroundColor,
            people = people.sortedWith(
                compareByDescending<PersonWithSummary> { it.summary.activeLoanCount }
                    .thenBy { it.person.name.lowercase() },
            ),
            loanSummary = summarizePersonLoans(-1L, "", loans),
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PersonalDashboardUiState(),
    )
}
