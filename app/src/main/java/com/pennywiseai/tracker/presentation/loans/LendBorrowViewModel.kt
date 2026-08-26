package com.pennywiseai.tracker.presentation.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.LoanStatus
import com.pennywiseai.tracker.data.repository.LoanRepository
import com.pennywiseai.tracker.utils.Money
import com.pennywiseai.tracker.utils.sumByCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
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
    val filteredPeople: List<PersonSummary> = emptyList(),
    val entries: List<LoanEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: LendBorrowFilter = LendBorrowFilter.ALL,
    val selectedTab: Int = 0,
    val selectedPersonIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val showAddPersonSheet: Boolean = false,
    val showAddTransactionSheet: Boolean = false,
    val personForTransaction: PersonSummary? = null,
    val draftPersonName: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = true
)

enum class LendBorrowFilter {
    ALL,
    YOU_GET,
    YOU_OWE,
    SETTLED
}

@HiltViewModel
class LendBorrowViewModel @Inject constructor(
    private val loanRepository: LoanRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LendBorrowUiState())
    val uiState: StateFlow<LendBorrowUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loanRepository.getAllLoans().collect { loans ->
                val people = loans.toPeople()
                val current = _uiState.value
                _uiState.value = current.copy(
                    people = people,
                    filteredPeople = applyFilterAndSearch(people, current.selectedFilter, current.searchQuery),
                    entries = loans,
                    isLoading = false
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.withFilteredPeople(searchQuery = query)
    }

    fun onFilterSelected(filter: LendBorrowFilter) {
        _uiState.value = _uiState.value.withFilteredPeople(selectedFilter = filter)
    }

    fun onTabSelected(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex.coerceIn(0, 1))
    }

    fun showAddPersonSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddPersonSheet = show)
    }

    fun showAddTransactionSheet(show: Boolean, person: PersonSummary? = null) {
        _uiState.value = _uiState.value.copy(
            showAddTransactionSheet = show,
            personForTransaction = person,
            draftPersonName = if (show) person?.personName.orEmpty() else ""
        )
    }

    /**
     * A person has no standalone row in PennyWise's existing ledger. Keep the
     * name as the next-entry draft; the first lend/borrow entry persists the
     * person identity on its LoanEntity.
     */
    fun addPerson(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        _uiState.value = _uiState.value.copy(
            showAddPersonSheet = false,
            showAddTransactionSheet = true,
            personForTransaction = null,
            draftPersonName = trimmed
        )
    }

    fun addTransaction(
        personName: String,
        direction: LoanDirection,
        amount: BigDecimal,
        currency: String,
        note: String? = null,
        sourceTransactionId: Long? = null
    ) {
        val trimmedName = personName.trim()
        if (trimmedName.isBlank() || amount <= BigDecimal.ZERO || currency.isBlank()) return
        viewModelScope.launch {
            val personId = _uiState.value.people
                .firstOrNull { it.personName.equals(trimmedName, ignoreCase = true) }
                ?.personId
                ?.takeUnless { it.startsWith("name:") }
            val result = if (sourceTransactionId != null) {
                loanRepository.createLoan(
                    personName = trimmedName,
                    direction = direction,
                    amount = amount,
                    currency = currency,
                    note = note,
                    sourceTransactionId = sourceTransactionId,
                    personId = personId
                )
            } else {
                loanRepository.createManualLoan(
                    personName = trimmedName,
                    personId = personId,
                    direction = direction,
                    amount = amount,
                    currency = currency,
                    note = note
                )
            }
            if (result != -1L) showAddTransactionSheet(false)
        }
    }

    fun toggleSelectionMode() {
        _uiState.value = _uiState.value.let { state ->
            if (state.isSelectionMode) state.copy(isSelectionMode = false, selectedPersonIds = emptySet())
            else state.copy(isSelectionMode = true)
        }
    }

    fun togglePersonSelection(personId: String) {
        _uiState.value = _uiState.value.copy(
            selectedPersonIds = if (personId in _uiState.value.selectedPersonIds) {
                _uiState.value.selectedPersonIds - personId
            } else {
                _uiState.value.selectedPersonIds + personId
            }
        )
    }

    fun selectAllPersons() {
        _uiState.value = _uiState.value.copy(
            selectedPersonIds = _uiState.value.filteredPeople.map { it.personId }.toSet()
        )
    }

    fun selectPersonSet(ids: Set<String>) {
        _uiState.value = _uiState.value.copy(selectedPersonIds = ids)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedPersonIds = emptySet())
    }

    /** Reassigns selected people to an existing person; it never cascades loans. */
    fun deleteSelectedPersons(reassignToPersonId: String? = null) {
        val targetId = reassignToPersonId ?: return
        val target = _uiState.value.people.firstOrNull { it.personId == targetId } ?: return
        viewModelScope.launch {
            _uiState.value.selectedPersonIds
                .filter { it != targetId }
                .forEach { sourceId ->
                    loanRepository.mergePerson(sourceId, targetId, target.personName)
                }
            _uiState.value = _uiState.value.copy(isSelectionMode = false, selectedPersonIds = emptySet())
        }
    }

    private fun LendBorrowUiState.withFilteredPeople(
        searchQuery: String = this.searchQuery,
        selectedFilter: LendBorrowFilter = this.selectedFilter
    ): LendBorrowUiState = copy(
        searchQuery = searchQuery,
        selectedFilter = selectedFilter,
        filteredPeople = applyFilterAndSearch(people, selectedFilter, searchQuery),
        selectedPersonIds = selectedPersonIds.intersect(
            applyFilterAndSearch(people, selectedFilter, searchQuery).map { it.personId }.toSet()
        )
    )

    private fun applyFilterAndSearch(
        people: List<PersonSummary>,
        filter: LendBorrowFilter,
        query: String
    ): List<PersonSummary> = people.filter { person ->
        val matchesQuery = query.isBlank() || person.personName.contains(query, ignoreCase = true)
        val matchesFilter = when (filter) {
            LendBorrowFilter.ALL -> true
            LendBorrowFilter.YOU_GET -> person.lentByCurrency.isNotEmpty()
            LendBorrowFilter.YOU_OWE -> person.borrowedByCurrency.isNotEmpty()
            LendBorrowFilter.SETTLED -> person.lentByCurrency.isEmpty() && person.borrowedByCurrency.isEmpty()
        }
        matchesQuery && matchesFilter
    }

    private fun List<LoanEntity>.toPeople(): List<PersonSummary> = groupBy {
        it.personId.ifBlank { "name:${it.personName}" }
    }.map { (personId, personLoans) ->
        PersonSummary(
            personId = personId,
            personName = personLoans.last().personName,
            loans = personLoans,
            lentByCurrency = personLoans
                .filter { it.direction == LoanDirection.LENT && it.status == LoanStatus.ACTIVE }
                .sumByCurrency({ it.currency }, { it.remainingAmount }),
            borrowedByCurrency = personLoans
                .filter { it.direction == LoanDirection.BORROWED && it.status == LoanStatus.ACTIVE }
                .sumByCurrency({ it.currency }, { it.remainingAmount })
        )
    }.sortedBy { it.personName.lowercase() }
}
