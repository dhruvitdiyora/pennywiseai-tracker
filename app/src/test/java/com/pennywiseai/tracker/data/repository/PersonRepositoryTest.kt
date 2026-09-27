package com.pennywiseai.tracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywiseai.tracker.data.database.PennyWiseDatabase
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.LoanStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PersonRepositoryTest {
    private lateinit var database: PennyWiseDatabase
    private lateinit var repository: PersonRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PennyWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PersonRepository(database.personDao(), database.loanDao(), database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun duplicateDisplayNamesRemainSeparatePeople() = runBlocking {
        val firstId = repository.createPerson(name = "Sam", phoneNumber = "111")
        val secondId = repository.createPerson(name = " Sam ", phoneNumber = "222")

        assertNotEquals(firstId, secondId)
        assertEquals("sam", repository.getPerson(firstId)?.normalizedName)
        assertEquals("sam", repository.getPerson(secondId)?.normalizedName)
    }

    @Test
    fun deletingPersonWithLedgerHistoryArchivesInstead() = runBlocking {
        val personId = repository.createPerson("Taylor")
        database.loanDao().insertLoan(
            LoanEntity(
                personName = "Taylor",
                personId = personId,
                direction = LoanDirection.LENT,
                originalAmount = BigDecimal("50"),
                remainingAmount = BigDecimal("50"),
            )
        )

        assertFalse(repository.deleteOrArchivePerson(personId))
        assertTrue(repository.getPerson(personId)?.isArchived == true)
    }

    @Test
    fun deletingPersonWithoutLedgerHistoryRemovesIt() = runBlocking {
        val personId = repository.createPerson("Jordan")

        assertTrue(repository.deleteOrArchivePerson(personId))
        assertNull(repository.getPerson(personId))
    }
}

class PersonLoanSummaryTest {
    private val now = LocalDateTime.of(2026, 9, 6, 12, 0)

    @Test
    fun summaryKeepsCurrenciesSeparateAndExcludesSettledBalances() {
        val loans = listOf(
            loan(LoanDirection.LENT, "100", "INR"),
            loan(LoanDirection.BORROWED, "20", "inr"),
            loan(LoanDirection.LENT, "10", "USD"),
            loan(LoanDirection.LENT, "5", "EUR", LoanStatus.SETTLED),
        )

        val summary = summarizePersonLoans(7L, "Taylor", loans)

        assertEquals(mapOf("INR" to BigDecimal("100"), "USD" to BigDecimal("10")), summary.lentByCurrency)
        assertEquals(mapOf("INR" to BigDecimal("20")), summary.borrowedByCurrency)
        assertEquals(mapOf("INR" to BigDecimal("80"), "USD" to BigDecimal("10")), summary.netByCurrency)
        assertEquals(3, summary.activeLoanCount)
        assertTrue(summary.hasSettledLoans)
    }

    private fun loan(
        direction: LoanDirection,
        remaining: String,
        currency: String,
        status: LoanStatus = LoanStatus.ACTIVE,
    ) = LoanEntity(
        personName = "Taylor",
        personId = 7L,
        direction = direction,
        originalAmount = BigDecimal(remaining),
        remainingAmount = BigDecimal(remaining),
        currency = currency,
        status = status,
        createdAt = now,
        updatedAt = now,
    )
}
