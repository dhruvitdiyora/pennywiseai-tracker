package com.pennywiseai.tracker.presentation.loans

import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.LoanStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class LoanListFilterTest {

    private val t0 = LocalDateTime.of(2026, 9, 1, 10, 0)

    private fun loan(
        id: Long,
        name: String,
        direction: LoanDirection,
        remaining: String,
        currency: String = "INR",
        status: LoanStatus = LoanStatus.ACTIVE,
    ) = LoanEntity(
        id = id,
        personName = name,
        direction = direction,
        originalAmount = BigDecimal(remaining).max(BigDecimal("1")),
        remainingAmount = BigDecimal(remaining),
        currency = currency,
        status = status,
        updatedAt = t0.plusDays(id),
    )

    private val people = groupLoansByPerson(
        listOf(
            loan(1, "Owes Me", LoanDirection.LENT, "100"),
            loan(2, "I Owe", LoanDirection.BORROWED, "40"),
            // Owed in one currency, owing in another: shows under both directions.
            loan(3, "Both Ways", LoanDirection.LENT, "10", currency = "USD"),
            loan(4, "Both Ways", LoanDirection.BORROWED, "500"),
            loan(5, "Done", LoanDirection.LENT, "0", status = LoanStatus.SETTLED),
        ),
    )

    private fun names(filter: LoanListFilter, query: String = "") =
        filterLoanPeople(people, filter, query).map { it.name }.toSet()

    @Test
    fun `all keeps everyone`() {
        assertEquals(setOf("Owes Me", "I Owe", "Both Ways", "Done"), names(LoanListFilter.ALL))
    }

    @Test
    fun `direction filters follow each currency balance and never net across currencies`() {
        assertEquals(setOf("Owes Me", "Both Ways"), names(LoanListFilter.YOU_GET))
        assertEquals(setOf("I Owe", "Both Ways"), names(LoanListFilter.YOU_OWE))
    }

    @Test
    fun `settled shows only people with nothing open`() {
        assertEquals(setOf("Done"), names(LoanListFilter.SETTLED))
    }

    @Test
    fun `search matches names case-insensitively and ignores surrounding spaces`() {
        assertEquals(setOf("Both Ways"), names(LoanListFilter.ALL, "  both "))
        assertEquals(emptySet<String>(), names(LoanListFilter.SETTLED, "owe"))
    }

    @Test
    fun `headline decimals split only on a trailing decimal separator`() {
        assertEquals("₹1,234" to ".56", splitDecimalPart("₹1,234.56"))
        assertEquals("₹1,800" to "", splitDecimalPart("₹1,800"))
        assertEquals("€1.234" to ",56", splitDecimalPart("€1.234,56"))
        assertEquals("1.234,56 €" to "", splitDecimalPart("1.234,56 €"))
        assertEquals("35.70 S$" to "", splitDecimalPart("35.70 S$"))
    }
}
