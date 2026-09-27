package com.pennywiseai.tracker.data.repository

import androidx.room.withTransaction
import com.pennywiseai.tracker.data.database.PennyWiseDatabase
import com.pennywiseai.tracker.data.database.dao.LoanDao
import com.pennywiseai.tracker.data.database.dao.PersonDao
import com.pennywiseai.tracker.data.database.entity.PersonEntity
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class PersonWithSummary(
    val person: PersonEntity,
    val summary: PersonLoanSummary,
)

@Singleton
class PersonRepository @Inject constructor(
    private val personDao: PersonDao,
    private val loanDao: LoanDao,
    private val database: PennyWiseDatabase,
) {
    fun observePeople(includeArchived: Boolean = false): Flow<List<PersonWithSummary>> {
        val people = if (includeArchived) personDao.getAllPeople() else personDao.getActivePeople()
        return combine(people, loanDao.getAllLoans()) { personRows, loans ->
            personRows.map { person ->
                PersonWithSummary(
                    person = person,
                    summary = summarizePersonLoans(
                        personId = person.id,
                        personName = person.name,
                        loans = loans.filter { it.personId == person.id },
                    ),
                )
            }
        }
    }

    fun observePerson(personId: Long): Flow<PersonEntity?> = personDao.getPersonByIdFlow(personId)

    fun observePersonLoans(personId: Long) = loanDao.getLoansByPerson(personId)

    suspend fun getPerson(personId: Long): PersonEntity? = personDao.getPersonById(personId)

    suspend fun createPerson(
        name: String,
        phoneNumber: String? = null,
        notes: String? = null,
        avatar: String? = null,
        category: String? = null,
        color: String = "#4CAF50",
    ): Long {
        val displayName = name.trim()
        require(displayName.isNotBlank()) { "Person name cannot be blank" }
        return personDao.insertPerson(
            PersonEntity(
                name = displayName,
                normalizedName = normalizePersonName(displayName),
                phoneNumber = phoneNumber.normalizedOptional(),
                notes = notes.normalizedOptional(),
                avatar = avatar.normalizedOptional(),
                category = category.normalizedOptional(),
                color = color.trim().takeIf(String::isNotBlank) ?: "#4CAF50",
            )
        )
    }

    suspend fun updatePerson(
        personId: Long,
        name: String,
        phoneNumber: String?,
        notes: String?,
        avatar: String?,
        category: String?,
        color: String,
    ) {
        val displayName = name.trim()
        require(displayName.isNotBlank()) { "Person name cannot be blank" }
        val existing = personDao.getPersonById(personId) ?: error("Person not found")
        personDao.updatePerson(
            existing.copy(
                name = displayName,
                normalizedName = normalizePersonName(displayName),
                phoneNumber = phoneNumber.normalizedOptional(),
                notes = notes.normalizedOptional(),
                avatar = avatar.normalizedOptional(),
                category = category.normalizedOptional(),
                color = color.trim().takeIf(String::isNotBlank) ?: existing.color,
                updatedAt = LocalDateTime.now(),
            )
        )
    }

    suspend fun archivePerson(personId: Long) {
        val person = personDao.getPersonById(personId) ?: return
        personDao.updatePerson(
            person.copy(isArchived = true, updatedAt = LocalDateTime.now())
        )
    }

    suspend fun restorePerson(personId: Long) {
        val person = personDao.getPersonById(personId) ?: return
        personDao.updatePerson(
            person.copy(isArchived = false, updatedAt = LocalDateTime.now())
        )
    }

    suspend fun deleteOrArchivePerson(personId: Long): Boolean = database.withTransaction {
        val person = personDao.getPersonById(personId) ?: return@withTransaction true
        if (loanDao.getLoansByPersonOnce(personId).isEmpty()) {
            personDao.deletePerson(person)
            true
        } else {
            personDao.updatePerson(
                person.copy(isArchived = true, updatedAt = LocalDateTime.now())
            )
            false
        }
    }
}

private fun String?.normalizedOptional(): String? = this?.trim()?.takeIf(String::isNotBlank)
