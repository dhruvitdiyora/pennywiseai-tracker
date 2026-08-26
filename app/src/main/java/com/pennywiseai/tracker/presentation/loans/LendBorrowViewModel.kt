package com.pennywiseai.tracker.presentation.loans

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

data class PersonSummary(
    val personId: String,
    val personName: String,
    val loans: List<LoanEntity>,
    val lentByCurrency: Map<String, Money>,
    val borrowedByCurrency: Map<String, Money>
)

data class LendBorrowUiState(
    val people: List<PersonSummary> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class LendBorrowViewModel @Inject constructor(
    private val loanRepository: LoanRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LendBorrowUiState())
    val uiState: StateFlow<LendBorrowUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loanRepository.getAllLoans().collect { loans ->
                _uiState.value = LendBorrowUiState(
                    people = loans.groupBy { it.personId.ifBlank { "name:${it.personName}" } }
                        .map { (personId, personLoans) ->
                            PersonSummary(
                                personId = personId,
                                personName = personLoans.last().personName,
                                loans = personLoans,
                                lentByCurrency = personLoans.filter { it.direction == LoanDirection.LENT && it.status.name == "ACTIVE" }
                                    .sumByCurrency({ it.currency }, { it.remainingAmount }),
                                borrowedByCurrency = personLoans.filter { it.direction == LoanDirection.BORROWED && it.status.name == "ACTIVE" }
                                    .sumByCurrency({ it.currency }, { it.remainingAmount })
                            )
                        }
                        .sortedBy { it.personName.lowercase() },
                    isLoading = false
                )
            }
        }
    }
}
