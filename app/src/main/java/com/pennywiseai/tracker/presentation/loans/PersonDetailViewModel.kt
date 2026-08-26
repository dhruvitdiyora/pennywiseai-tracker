package com.pennywiseai.tracker.presentation.loans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.repository.LoanRepository
import com.pennywiseai.tracker.utils.Money
import com.pennywiseai.tracker.utils.sumByCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonDetailUiState(
    val personId: String = "",
    val personName: String = "",
    val loans: List<LoanEntity> = emptyList(),
    val lentByCurrency: Map<String, Money> = emptyMap(),
    val borrowedByCurrency: Map<String, Money> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val personId: String = savedStateHandle["personId"] ?: ""
    private val _uiState = MutableStateFlow(PersonDetailUiState(personId = personId))
    val uiState: StateFlow<PersonDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loanRepository.getAllLoans().collect { allLoans ->
                val loans = allLoans.filter { loan ->
                    if (personId.startsWith("name:")) loan.personName == personId.removePrefix("name:")
                    else loan.personId == personId
                }
                _uiState.value = PersonDetailUiState(
                    personId = personId,
                    personName = loans.lastOrNull()?.personName.orEmpty(),
                    loans = loans,
                    lentByCurrency = loans.filter { it.direction == LoanDirection.LENT && it.status.name == "ACTIVE" }
                        .sumByCurrency({ it.currency }, { it.remainingAmount }),
                    borrowedByCurrency = loans.filter { it.direction == LoanDirection.BORROWED && it.status.name == "ACTIVE" }
                        .sumByCurrency({ it.currency }, { it.remainingAmount }),
                    isLoading = false
                )
            }
        }
    }
}
