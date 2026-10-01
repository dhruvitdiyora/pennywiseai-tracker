package com.pennywiseai.tracker.presentation.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanStatus
import com.pennywiseai.tracker.data.preferences.CoverStyle
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
    /** The user's own Home banner (a private file URI); null falls back to [coverStyle]. */
    val homeBannerUri: String? = null,
    val coverStyle: CoverStyle = CoverStyle.AURORA,
    /** Open records where the user is owed money. */
    val lentRecordCount: Int = 0,
    /** Open records where the user owes money. */
    val borrowedRecordCount: Int = 0,
    val settledRecordCount: Int = 0,
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
        val openLoans = loans.filter { it.status == LoanStatus.ACTIVE }
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
            homeBannerUri = preferences.homeBannerUri,
            coverStyle = preferences.coverStyle,
            lentRecordCount = openLoans.count { it.direction == LoanDirection.LENT },
            borrowedRecordCount = openLoans.count { it.direction == LoanDirection.BORROWED },
            settledRecordCount = loans.count { it.status == LoanStatus.SETTLED },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PersonalDashboardUiState(),
    )
}
