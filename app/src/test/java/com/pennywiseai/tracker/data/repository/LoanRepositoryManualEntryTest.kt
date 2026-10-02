package com.pennywiseai.tracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywiseai.tracker.data.database.PennyWiseDatabase
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.PersonEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LoanRepositoryManualEntryTest {
    private lateinit var database: PennyWiseDatabase
    private lateinit var repository: LoanRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PennyWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LoanRepository(
            loanDao = database.loanDao(),
            personDao = database.personDao(),
            transactionDao = database.transactionDao(),
            database = database,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun manualEntryCreatesOneLoanWithOneLinkedTransaction() = runBlocking {
        val createdAt = LocalDateTime.of(2026, 9, 2, 10, 30)
        val loanId = repository.createManualLoan(
            personName = "  Aarav  ",
            direction = LoanDirection.LENT,
            amount = BigDecimal("1250.50"),
            currency = "inr",
            note = "  Train tickets  ",
            dateTime = createdAt,
        )

        val loan = repository.getLoanById(loanId)
        val transactions = repository.getTransactionsForLoan(loanId).first()

        assertNotNull(loan)
        assertEquals("Aarav", loan?.personName)
        assertEquals("INR", loan?.currency)
        assertEquals(BigDecimal("1250.50"), loan?.remainingAmount)
        assertNotNull(loan?.personId)
        assertEquals("Aarav", database.personDao().getPersonById(loan!!.personId!!)?.name)
        assertEquals(1, transactions.size)
        assertEquals(loanId, transactions.single().loanId)
        assertEquals(TransactionType.EXPENSE, transactions.single().transactionType)
        assertEquals("Train tickets", transactions.single().description)
    }

    @Test
    fun addingLoanUnderArchivedPersonsNameRevivesThemInsteadOfDuplicating() = runBlocking {
        val firstLoanId = repository.createManualLoan(
            personName = "Casey",
            direction = LoanDirection.LENT,
            amount = BigDecimal("100"),
            currency = "INR",
            note = null,
        )
        val personId = repository.getLoanById(firstLoanId)!!.personId!!
        val people = PersonRepository(database.personDao(), database.loanDao(), database)
        // Has loan history, so "delete" only archives.
        assertEquals(false, people.deleteOrArchivePerson(personId))
        assertEquals(true, people.getPerson(personId)?.isArchived)

        val secondLoanId = repository.createManualLoan(
            personName = "  CASEY ",
            direction = LoanDirection.BORROWED,
            amount = BigDecimal("40"),
            currency = "INR",
            note = null,
        )

        val second = repository.getLoanById(secondLoanId)!!
        assertEquals(personId, second.personId)
        assertEquals("Casey", second.personName)
        assertEquals(false, people.getPerson(personId)?.isArchived)
        assertEquals(1, database.personDao().getAllPeopleSync().size)
        assertEquals(2, database.loanDao().getLoansByPersonOnce(personId).size)
    }

    @Test
    fun activePersonIsPreferredOverAnArchivedNamesake() = runBlocking {
        val archivedId = database.personDao().insertPerson(
            PersonEntity(name = "Casey", normalizedName = "casey", isArchived = true)
        )
        val activeId = database.personDao().insertPerson(
            PersonEntity(name = "Casey", normalizedName = "casey")
        )

        val loanId = repository.createManualLoan(
            personName = "casey",
            direction = LoanDirection.LENT,
            amount = BigDecimal("10"),
            currency = "INR",
            note = null,
        )

        assertEquals(activeId, repository.getLoanById(loanId)?.personId)
        assertEquals(true, database.personDao().getPersonById(archivedId)?.isArchived)
    }
}
