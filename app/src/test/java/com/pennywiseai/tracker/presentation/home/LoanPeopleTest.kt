package com.pennywiseai.tracker.presentation.home

import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.PersonEntity
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class LoanPeopleTest {

    private fun loan(name: String, personId: Long? = null) = LoanEntity(
        personName = name,
        personId = personId,
        direction = LoanDirection.LENT,
        originalAmount = BigDecimal("100"),
        remainingAmount = BigDecimal("100"),
        currency = "USD",
    )

    @Test
    fun `one entry per counterparty in first-seen order`() {
        val people = loanPeople(
            listOf(loan("Alex"), loan("Sam"), loan(" alex ")),
            emptyMap()
        )

        assertEquals(listOf("Alex", "Sam"), people.map { it.name })
    }

    @Test
    fun `linked contacts use the contact's current name and colour`() {
        val contact = PersonEntity(id = 7, name = "Alexandra", color = "#3F51B5")
        val people = loanPeople(
            listOf(loan("Alex", personId = 7), loan("Old name", personId = 7)),
            mapOf(7L to contact)
        )

        assertEquals(listOf(LoanPerson("Alexandra", "#3F51B5")), people)
    }

    @Test
    fun `unlinked loans have no colour`() {
        val people = loanPeople(listOf(loan("Sam", personId = 99)), emptyMap())

        assertEquals(listOf(LoanPerson("Sam", null)), people)
    }
}
