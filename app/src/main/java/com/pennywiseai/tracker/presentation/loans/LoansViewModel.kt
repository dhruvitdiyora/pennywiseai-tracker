package com.pennywiseai.tracker.presentation.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.currency.CurrencyConversionService
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.LoanStatus
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import com.pennywiseai.tracker.data.repository.LoanRepository
import com.pennywiseai.tracker.data.repository.PersonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class LoansUiState(
    val activeLoans: List<LoanEntity> = emptyList(),
    val settledLoans: List<LoanEntity> = emptyList(),
    /** All loans grouped by person — what the list renders. */
    val people: List<LoanPerson> = emptyList(),
    val totalLentRemaining: BigDecimal = BigDecimal.ZERO,
    val totalBorrowedRemaining: BigDecimal = BigDecimal.ZERO,
    val summaryCurrency: String = "INR",
    val defaultEntryCurrency: String = "INR",
    val isLoading: Boolean = true,
    /**
     * How each linked contact looks (their colour and relationship label), by
     * person id. Purely presentational: loans without a contact keep the
     * direction-tinted initials.
     */
    val contacts: Map<Long, LoanContactStyle> = emptyMap(),
    val showAddEntrySheet: Boolean = false,
    val isSavingEntry: Boolean = false,
    val entryError: LoanEntryError? = null,
)

/** The look of a contact on the lend & borrow list: their own colour and relationship label. */
data class LoanContactStyle(
    val color: String,
    val category: String?,
)

enum class LoanEntryError {
    PERSON_REQUIRED,
    AMOUNT_REQUIRED,
    CURRENCY_REQUIRED,
    SAVE_FAILED,
}

@HiltViewModel
class LoansViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val currencyConversionService: CurrencyConversionService,
    private val personRepository: PersonRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoansUiState())
    val uiState: StateFlow<LoansUiState> = _uiState.asStateFlow()

    init {
        // Corrects active loans whose stored remainder predates #876.
        viewModelScope.launch { loanRepository.refreshActiveLoans() }
        loadLoans()
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            personRepository.observePeople(includeArchived = true).collect { rows ->
                val contacts = rows.associate { row ->
                    row.person.id to LoanContactStyle(
                        color = row.person.color,
                        category = row.person.category?.takeIf { it.isNotBlank() },
                    )
                }
                _uiState.update { it.copy(contacts = contacts) }
            }
        }
    }

    private fun loadLoans() {
        viewModelScope.launch {
            combine(
                loanRepository.getActiveLoans(),
                loanRepository.getAllLoans(),
                userPreferencesRepository.unifiedCurrencyMode,
                userPreferencesRepository.displayCurrency,
                userPreferencesRepository.baseCurrency
            ) { active, all, isUnified, displayCurrency, baseCurrency ->
                LoanInputs(
                    activeLoans = active,
                    allLoans = all,
                    isUnified = isUnified,
                    displayCurrency = displayCurrency,
                    baseCurrency = baseCurrency
                )
            }.collect { inputs ->
                val summaryCurrency = if (inputs.isUnified) inputs.displayCurrency else inputs.baseCurrency

                val loanCurrencies = inputs.activeLoans.map { it.currency }.distinct()
                if (inputs.isUnified && loanCurrencies.size > 1) {
                    currencyConversionService.refreshExchangeRatesForAccount((loanCurrencies + summaryCurrency).distinct())
                }

                val loansForTotals = if (inputs.isUnified) {
                    inputs.activeLoans
                } else {
                    inputs.activeLoans.filter { it.currency.equals(summaryCurrency, ignoreCase = true) }
                }

                val (lent, borrowed) = if (inputs.isUnified) {
                    computeUnifiedTotals(loansForTotals, summaryCurrency)
                } else {
                    computeNativeTotals(loansForTotals)
                }

                _uiState.update { state ->
                    state.copy(
                        activeLoans = inputs.activeLoans,
                        settledLoans = inputs.allLoans.filter { it.status == LoanStatus.SETTLED },
                        people = groupLoansByPerson(inputs.allLoans),
                        totalLentRemaining = lent,
                        totalBorrowedRemaining = borrowed,
                        summaryCurrency = summaryCurrency,
                        defaultEntryCurrency = inputs.baseCurrency.ifBlank { "INR" },
                        isLoading = false,
                    )
                }
            }
        }
    }

    private data class LoanInputs(
        val activeLoans: List<LoanEntity>,
        val allLoans: List<LoanEntity>,
        val isUnified: Boolean,
        val displayCurrency: String,
        val baseCurrency: String
    )

    private suspend fun computeUnifiedTotals(
        loans: List<LoanEntity>,
        targetCurrency: String
    ): Pair<BigDecimal, BigDecimal> {
        var lent = BigDecimal.ZERO
        var borrowed = BigDecimal.ZERO
        for (loan in loans) {
            // Skip unconvertible loans rather than counting face value (#670).
            val converted = currencyConversionService.convertAmountOrNull(
                amount = loan.remainingAmount,
                fromCurrency = loan.currency,
                toCurrency = targetCurrency
            ) ?: continue
            when (loan.direction) {
                LoanDirection.LENT -> lent += converted
                LoanDirection.BORROWED -> borrowed += converted
            }
        }
        return lent to borrowed
    }

    private fun computeNativeTotals(loans: List<LoanEntity>): Pair<BigDecimal, BigDecimal> {
        val lent = loans
            .filter { it.direction == LoanDirection.LENT }
            .fold(BigDecimal.ZERO) { acc, l -> acc + l.remainingAmount }
        val borrowed = loans
            .filter { it.direction == LoanDirection.BORROWED }
            .fold(BigDecimal.ZERO) { acc, l -> acc + l.remainingAmount }
        return lent to borrowed
    }

    /** Settles every open loan with [person]; unpaid balances are forgiven, as with a single loan. */
    fun settleUp(person: LoanPerson) {
        viewModelScope.launch {
            loanRepository.settleLoans(person.activeLoans.map { it.id })
        }
    }

    fun showAddEntrySheet(show: Boolean) {
        if (_uiState.value.isSavingEntry) return
        _uiState.value = _uiState.value.copy(
            showAddEntrySheet = show,
            entryError = null,
        )
    }

    fun addManualEntry(
        personName: String,
        direction: LoanDirection,
        amount: BigDecimal?,
        currency: String,
        note: String?,
    ) {
        val error = validateLoanEntry(personName, amount, currency)
        if (error != null) {
            _uiState.value = _uiState.value.copy(entryError = error)
            return
        }
        if (_uiState.value.isSavingEntry) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingEntry = true, entryError = null)
            runCatching {
                loanRepository.createManualLoan(
                    personName = personName,
                    direction = direction,
                    amount = requireNotNull(amount),
                    currency = currency,
                    note = note,
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    showAddEntrySheet = false,
                    isSavingEntry = false,
                    entryError = null,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isSavingEntry = false,
                    entryError = LoanEntryError.SAVE_FAILED,
                )
            }
        }
    }

    fun clearEntryError() {
        _uiState.value = _uiState.value.copy(entryError = null)
    }
}

internal fun validateLoanEntry(
    personName: String,
    amount: BigDecimal?,
    currency: String,
): LoanEntryError? = when {
    personName.isBlank() -> LoanEntryError.PERSON_REQUIRED
    amount == null || amount <= BigDecimal.ZERO -> LoanEntryError.AMOUNT_REQUIRED
    currency.isBlank() -> LoanEntryError.CURRENCY_REQUIRED
    else -> null
}
