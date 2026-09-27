package com.pennywiseai.tracker.data.repository

import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualLoanTransactionTest {
    private val now = LocalDateTime.of(2026, 9, 2, 10, 30)

    @Test
    fun lentMoneyCreatesExpenseLinkedToOthers() {
        val transaction = manualLoanTransaction(
            personName = "Aarav",
            direction = LoanDirection.LENT,
            amount = BigDecimal("1250.50"),
            currency = "INR",
            note = "Train tickets",
            dateTime = now,
        )

        assertEquals(TransactionType.EXPENSE, transaction.transactionType)
        assertEquals("Others", transaction.category)
        assertEquals("INR", transaction.currency)
        assertEquals("Aarav", transaction.merchantName)
        assertEquals("Train tickets", transaction.description)
        assertEquals(now, transaction.dateTime)
        assertTrue(transaction.transactionHash.startsWith("manual_loan_"))
    }

    @Test
    fun borrowedMoneyCreatesIncomeAndDropsBlankNote() {
        val transaction = manualLoanTransaction(
            personName = "Mira",
            direction = LoanDirection.BORROWED,
            amount = BigDecimal("80"),
            currency = "USD",
            note = "   ",
            dateTime = now,
        )

        assertEquals(TransactionType.INCOME, transaction.transactionType)
        assertEquals("Income", transaction.category)
        assertEquals("USD", transaction.currency)
        assertNull(transaction.description)
    }
}
