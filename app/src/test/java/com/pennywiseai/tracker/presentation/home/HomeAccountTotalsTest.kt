package com.pennywiseai.tracker.presentation.home

import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeAccountTotalsTest {

    @Test
    fun `native mode totals only accounts in the selected currency`() {
        val totals = calculateAccountTotals(
            regularAccounts = listOf(
                account(balance = "1250.00", currency = "INR"),
                account(balance = "25.00", currency = "USD")
            ),
            creditCards = emptyList(),
            selectedCurrency = "USD",
            isUnifiedMode = false
        )

        assertAmountEquals("25.00", totals.totalBalance)
        assertEquals(0, totals.excludedAccountCount)
    }

    @Test
    fun `single currency balance is unchanged`() {
        val totals = calculateAccountTotals(
            regularAccounts = listOf(
                account(balance = "100.00", currency = "USD"),
                account(balance = "50.00", currency = "USD")
            ),
            creditCards = emptyList(),
            selectedCurrency = "USD",
            isUnifiedMode = false
        )

        assertAmountEquals("150.00", totals.totalBalance)
        assertEquals(0, totals.excludedAccountCount)
    }

    @Test
    fun `unified mode excludes and counts an unconvertible account`() {
        val totals = calculateAccountTotals(
            regularAccounts = listOf(
                account(balance = "80.00", currency = "USD"),
                account(balance = "500.00", currency = "CAD")
            ),
            creditCards = emptyList(),
            selectedCurrency = "USD",
            isUnifiedMode = true
        )

        assertAmountEquals("80.00", totals.totalBalance)
        assertEquals(1, totals.excludedAccountCount)
    }

    @Test
    fun `available credit obeys the selected currency rule`() {
        val totals = calculateAccountTotals(
            regularAccounts = emptyList(),
            creditCards = listOf(
                creditCard(balance = "250.00", limit = "1000.00", currency = "USD"),
                creditCard(balance = "100.00", limit = "600.00", currency = "INR")
            ),
            selectedCurrency = "USD",
            isUnifiedMode = false
        )

        assertAmountEquals("750.00", totals.totalAvailableCredit)
        assertEquals(0, totals.excludedAccountCount)
    }

    private fun account(balance: String, currency: String) = AccountBalanceEntity(
        bankName = "Fixture Bank",
        accountLast4 = "0000",
        balance = BigDecimal(balance),
        timestamp = FIXED_TIME,
        currency = currency
    )

    private fun creditCard(balance: String, limit: String, currency: String) =
        account(balance = balance, currency = currency).copy(
            creditLimit = BigDecimal(limit),
            isCreditCard = true
        )

    private fun assertAmountEquals(expected: String, actual: BigDecimal) {
        assertEquals(0, BigDecimal(expected).compareTo(actual))
    }

    private companion object {
        val FIXED_TIME: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)
    }
}
