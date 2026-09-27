package com.pennywiseai.tracker.presentation.people

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.PersonEntity
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import com.pennywiseai.tracker.data.repository.LoanRepository
import com.pennywiseai.tracker.data.repository.PersonLoanSummary
import com.pennywiseai.tracker.data.repository.PersonRepository
import com.pennywiseai.tracker.data.repository.PersonWithSummary
import com.pennywiseai.tracker.data.repository.summarizePersonLoans
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PersonSaveError {
    NAME_REQUIRED,
    SAVE_FAILED,
}

data class PersonEditorState(
    val isSaving: Boolean = false,
    val error: PersonSaveError? = null,
)

data class PeopleUiState(
    val people: List<PersonWithSummary> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
)

@HiltViewModel
class PeopleViewModel @Inject constructor(
    private val repository: PersonRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val _editorState = MutableStateFlow(PersonEditorState())
    val editorState: StateFlow<PersonEditorState> = _editorState.asStateFlow()

    val uiState: StateFlow<PeopleUiState> = combine(
        repository.observePeople(),
        query,
    ) { people, search ->
        val normalizedSearch = search.trim()
        PeopleUiState(
            people = if (normalizedSearch.isBlank()) {
                people
            } else {
                people.filter { row ->
                    row.person.name.contains(normalizedSearch, ignoreCase = true) ||
                        row.person.category.orEmpty().contains(normalizedSearch, ignoreCase = true)
                }
            },
            query = search,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PeopleUiState(),
    )

    fun updateQuery(value: String) {
        query.value = value
    }

    fun clearEditorError() {
        _editorState.value = _editorState.value.copy(error = null)
    }

    fun savePerson(
        person: PersonEntity?,
        name: String,
        phoneNumber: String?,
        notes: String?,
        category: String?,
        color: String,
        onSaved: () -> Unit,
    ) {
        if (name.isBlank()) {
            _editorState.value = PersonEditorState(error = PersonSaveError.NAME_REQUIRED)
            return
        }
        if (_editorState.value.isSaving) return
        viewModelScope.launch {
            _editorState.value = PersonEditorState(isSaving = true)
            runCatching {
                if (person == null) {
                    repository.createPerson(name, phoneNumber, notes, category = category, color = color)
                } else {
                    repository.updatePerson(
                        personId = person.id,
                        name = name,
                        phoneNumber = phoneNumber,
                        notes = notes,
                        avatar = person.avatar,
                        category = category,
                        color = color,
                    )
                }
            }.onSuccess {
                _editorState.value = PersonEditorState()
                onSaved()
            }.onFailure {
                _editorState.value = PersonEditorState(error = PersonSaveError.SAVE_FAILED)
            }
        }
    }
}

data class PersonDetailUiState(
    val person: PersonEntity? = null,
    val loans: List<LoanEntity> = emptyList(),
    val summary: PersonLoanSummary? = null,
    val defaultEntryCurrency: String = "INR",
    val isLoading: Boolean = true,
    val isSavingEntry: Boolean = false,
    val entryError: com.pennywiseai.tracker.presentation.loans.LoanEntryError? = null,
    val editorState: PersonEditorState = PersonEditorState(),
)

@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    private val personRepository: PersonRepository,
    private val loanRepository: LoanRepository,
    userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val personId: Long = savedStateHandle.get<Long>("personId") ?: -1L
    private val actionState = MutableStateFlow(PersonDetailActionState())

    val uiState: StateFlow<PersonDetailUiState> = combine(
        personRepository.observePerson(personId),
        personRepository.observePersonLoans(personId),
        userPreferencesRepository.baseCurrency,
        actionState,
    ) { person, loans, baseCurrency, action ->
        PersonDetailUiState(
            person = person,
            loans = loans,
            summary = person?.let { summarizePersonLoans(it.id, it.name, loans) },
            defaultEntryCurrency = baseCurrency.ifBlank { "INR" },
            isLoading = false,
            isSavingEntry = action.isSavingEntry,
            entryError = action.entryError,
            editorState = action.editorState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PersonDetailUiState(),
    )

    fun clearEntryError() {
        actionState.value = actionState.value.copy(entryError = null)
    }

    fun clearEditorError() {
        actionState.value = actionState.value.copy(
            editorState = actionState.value.editorState.copy(error = null),
        )
    }

    fun addEntry(
        personName: String,
        direction: LoanDirection,
        amount: BigDecimal?,
        currency: String,
        note: String?,
        onSaved: () -> Unit,
    ) {
        val error = com.pennywiseai.tracker.presentation.loans.validateLoanEntry(
            personName,
            amount,
            currency,
        )
        if (error != null) {
            actionState.value = actionState.value.copy(entryError = error)
            return
        }
        if (actionState.value.isSavingEntry) return
        viewModelScope.launch {
            actionState.value = actionState.value.copy(isSavingEntry = true, entryError = null)
            runCatching {
                loanRepository.createManualLoan(
                    personName = personName,
                    personId = personId,
                    direction = direction,
                    amount = requireNotNull(amount),
                    currency = currency,
                    note = note,
                )
            }.onSuccess {
                actionState.value = actionState.value.copy(isSavingEntry = false)
                onSaved()
            }.onFailure {
                actionState.value = actionState.value.copy(
                    isSavingEntry = false,
                    entryError = com.pennywiseai.tracker.presentation.loans.LoanEntryError.SAVE_FAILED,
                )
            }
        }
    }

    fun savePerson(
        name: String,
        phoneNumber: String?,
        notes: String?,
        category: String?,
        color: String,
        onSaved: () -> Unit,
    ) {
        if (name.isBlank()) {
            actionState.value = actionState.value.copy(
                editorState = PersonEditorState(error = PersonSaveError.NAME_REQUIRED),
            )
            return
        }
        if (actionState.value.editorState.isSaving) return
        viewModelScope.launch {
            actionState.value = actionState.value.copy(editorState = PersonEditorState(isSaving = true))
            runCatching {
                val person = personRepository.getPerson(personId) ?: error("Person not found")
                personRepository.updatePerson(
                    personId = personId,
                    name = name,
                    phoneNumber = phoneNumber,
                    notes = notes,
                    avatar = person.avatar,
                    category = category,
                    color = color,
                )
            }.onSuccess {
                actionState.value = actionState.value.copy(editorState = PersonEditorState())
                onSaved()
            }.onFailure {
                actionState.value = actionState.value.copy(
                    editorState = PersonEditorState(error = PersonSaveError.SAVE_FAILED),
                )
            }
        }
    }

    fun deleteOrArchive(onComplete: () -> Unit) {
        viewModelScope.launch {
            personRepository.deleteOrArchivePerson(personId)
            onComplete()
        }
    }
}

private data class PersonDetailActionState(
    val isSavingEntry: Boolean = false,
    val entryError: com.pennywiseai.tracker.presentation.loans.LoanEntryError? = null,
    val editorState: PersonEditorState = PersonEditorState(),
)
