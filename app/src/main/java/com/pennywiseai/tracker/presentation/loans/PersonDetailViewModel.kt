package com.pennywiseai.tracker.presentation.loans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.LoanStatus
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.repository.LoanRepository
import com.pennywiseai.tracker.utils.Money
import com.pennywiseai.tracker.utils.sumByCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class PersonDetailUiState(
    val personId: String = "",
    val personName: String = "",
    val loans: List<LoanEntity> = emptyList(),
    val lentByCurrency: Map<String, Money> = emptyMap(),
    val borrowedByCurrency: Map<String, Money> = emptyMap(),
    val transactions: List<TransactionEntity> = emptyList(),
    val transactionForAction: TransactionEntity? = null,
    val transactionToEdit: TransactionEntity? = null,
    val selectedRecordIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val showEditPersonSheet: Boolean = false,
    val showAddTransactionSheet: Boolean = false,
    val showSettleSheet: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val showTransactionActionDialog: Boolean = false,
    val showEditTransactionSheet: Boolean = false,
    val showDeleteTransactionDialog: Boolean = false,
    val errorMessage: String? = null,
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
                val transactions = loans.flatMap { loan ->
                    loanRepository.getTransactionsForLoan(loan.id).first()
                }.distinctBy { it.id }.sortedByDescending { it.dateTime }
                val current = _uiState.value
                _uiState.value = PersonDetailUiState(
                    personId = personId,
                    personName = loans.lastOrNull()?.personName.orEmpty(),
                    loans = loans,
                    lentByCurrency = loans.filter { it.direction == LoanDirection.LENT && it.status == LoanStatus.ACTIVE }
                        .sumByCurrency({ it.currency }, { it.remainingAmount }),
                    borrowedByCurrency = loans.filter { it.direction == LoanDirection.BORROWED && it.status == LoanStatus.ACTIVE }
                        .sumByCurrency({ it.currency }, { it.remainingAmount }),
                    transactions = transactions,
                    transactionForAction = current.transactionForAction,
                    transactionToEdit = current.transactionToEdit,
                    selectedRecordIds = current.selectedRecordIds,
                    isSelectionMode = current.isSelectionMode,
                    showEditPersonSheet = current.showEditPersonSheet,
                    showAddTransactionSheet = current.showAddTransactionSheet,
                    showSettleSheet = current.showSettleSheet,
                    showDeleteConfirmDialog = current.showDeleteConfirmDialog,
                    showTransactionActionDialog = current.showTransactionActionDialog,
                    showEditTransactionSheet = current.showEditTransactionSheet,
                    showDeleteTransactionDialog = current.showDeleteTransactionDialog,
                    errorMessage = current.errorMessage,
                    isLoading = false
                )
            }
        }
    }

    fun showEditPersonSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showEditPersonSheet = show)
    }

    fun showAddTransactionSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddTransactionSheet = show)
    }

    fun showSettleSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSettleSheet = show)
    }

    fun showDeleteConfirmDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = show)
    }

    fun showTransactionActions(transaction: TransactionEntity) {
        _uiState.value = _uiState.value.copy(
            transactionForAction = transaction,
            showTransactionActionDialog = true
        )
    }

    fun dismissTransactionActions() {
        _uiState.value = _uiState.value.copy(showTransactionActionDialog = false)
    }

    fun openTransactionEdit(transaction: TransactionEntity) {
        _uiState.value = _uiState.value.copy(
            transactionToEdit = transaction,
            showEditTransactionSheet = true,
            showTransactionActionDialog = false
        )
    }

    fun hideEditTransactionSheet() {
        _uiState.value = _uiState.value.copy(showEditTransactionSheet = false, transactionToEdit = null)
    }

    fun deleteTransactionRequested(transaction: TransactionEntity) {
        _uiState.value = _uiState.value.copy(
            transactionForAction = transaction,
            showDeleteTransactionDialog = true,
            showTransactionActionDialog = false
        )
    }

    fun dismissTransactionDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteTransactionDialog = false)
    }

    /** Adds a new manual ledger entry for this person. */
    fun addTransaction(
        direction: LoanDirection,
        amount: BigDecimal,
        currency: String,
        note: String? = null
    ) {
        val name = _uiState.value.personName
        if (name.isBlank() || amount <= BigDecimal.ZERO || currency.isBlank()) return
        viewModelScope.launch {
            val loanId = loanRepository.createManualLoan(
                personName = name,
                personId = personId.takeUnless { it.startsWith("name:") },
                direction = direction,
                amount = amount,
                currency = currency,
                note = note
            )
            if (loanId != -1L) showAddTransactionSheet(false)
        }
    }

    /**
     * Settles only loans in [currency]. A person with multiple currencies must
     * choose one explicitly; no conversion or cross-currency sum is attempted.
     */
    fun settle(
        amount: BigDecimal,
        note: String,
        isLentSettlement: Boolean,
        currency: String
    ) {
        if (amount <= BigDecimal.ZERO || currency.isBlank()) return
        viewModelScope.launch {
            val direction = if (isLentSettlement) LoanDirection.LENT else LoanDirection.BORROWED
            val matchingLoans = _uiState.value.loans
                .filter { it.direction == direction && it.status == LoanStatus.ACTIVE && it.currency == currency }
                .sortedBy { it.createdAt }
            if (amount > matchingLoans.fold(BigDecimal.ZERO) { total, loan -> total + loan.remainingAmount }) {
                return@launch
            }
            var remaining = amount
            matchingLoans.forEach { loan ->
                    if (remaining <= BigDecimal.ZERO) return@forEach
                    val payment = remaining.min(loan.remainingAmount)
                    loanRepository.recordManualRepayment(
                        loanId = loan.id,
                        amount = payment,
                        personName = loan.personName,
                        currency = currency,
                        note = note
                    )
                    remaining -= payment
                }
            if (remaining < amount) showSettleSheet(false)
        }
    }

    /** Reassigns this person's loans; no loan or linked transaction is deleted. */
    fun deletePerson(
        reassignToPersonId: String? = null,
        reassignToPersonName: String? = null,
        onDeleted: () -> Unit = {}
    ) {
        val targetId = reassignToPersonId?.takeIf { it.isNotBlank() } ?: return
        val targetName = reassignToPersonName?.trim().orEmpty()
        if (targetName.isBlank() || targetId == personId) return
        viewModelScope.launch {
            loanRepository.mergePerson(personId, targetId, targetName)
            _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false)
            onDeleted()
        }
    }

    fun deleteTransaction(transactionId: Long) {
        val loan = _uiState.value.loans.firstOrNull { loan ->
            _uiState.value.transactions.any { it.id == transactionId && it.loanId == loan.id }
        } ?: return
        viewModelScope.launch {
            loanRepository.unlinkTransaction(transactionId, loan.id)
            dismissTransactionDeleteDialog()
        }
    }

    fun enterSelectionMode(recordId: Long) {
        _uiState.value = _uiState.value.copy(isSelectionMode = true, selectedRecordIds = setOf(recordId))
    }

    fun toggleSelectionMode() {
        _uiState.value = _uiState.value.let { state ->
            if (state.isSelectionMode) state.copy(isSelectionMode = false, selectedRecordIds = emptySet())
            else state.copy(isSelectionMode = true)
        }
    }

    fun toggleRecordSelection(recordId: Long) {
        _uiState.value = _uiState.value.copy(
            selectedRecordIds = if (recordId in _uiState.value.selectedRecordIds) {
                _uiState.value.selectedRecordIds - recordId
            } else {
                _uiState.value.selectedRecordIds + recordId
            }
        )
    }

    fun selectAllRecords() {
        _uiState.value = _uiState.value.copy(selectedRecordIds = _uiState.value.transactions.map { it.id }.toSet())
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedRecordIds = emptySet())
    }

    fun deleteSelectedRecords() {
        val selected = _uiState.value.selectedRecordIds
        if (selected.isEmpty()) return
        viewModelScope.launch {
            selected.mapNotNull { transactionId ->
                _uiState.value.transactions.firstOrNull { it.id == transactionId }?.let { transactionId to it.loanId }
            }.forEach { (transactionId, loanId) ->
                if (loanId != null) loanRepository.unlinkTransaction(transactionId, loanId)
            }
            _uiState.value = _uiState.value.copy(isSelectionMode = false, selectedRecordIds = emptySet())
        }
    }

    fun addPerson(name: String) {
        if (name.isNotBlank()) showAddTransactionSheet(true)
    }
}
