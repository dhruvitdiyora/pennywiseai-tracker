package com.pennywiseai.tracker.presentation.subscriptions

import com.pennywiseai.tracker.data.database.entity.SubscriptionDirection
import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionExpenseTotalsTest {

    @Test
    fun `native mode totals expenses per currency and never mixes them`() {
        val totals = expenseSubscriptionTotals(
            subscriptions = listOf(
                subscription(1, "100", currency = "INR"),
                subscription(2, "50", currency = "INR"),
                subscription(3, "10", currency = "USD"),
            ),
            isUnified = false,
            displayCurrency = "INR",
            convertedAmounts = emptyMap(),
        )

        assertEquals(setOf("INR", "USD"), totals.monthly.keys)
        assertEquals(0, BigDecimal("150").compareTo(totals.monthly.getValue("INR").amount))
        assertEquals(0, BigDecimal("10").compareTo(totals.monthly.getValue("USD").amount))
    }

    @Test
    fun `income subscriptions are not counted as a cost`() {
        val totals = expenseSubscriptionTotals(
            subscriptions = listOf(
                subscription(1, "100"),
                subscription(2, "5000", direction = SubscriptionDirection.INCOME),
            ),
            isUnified = false,
            displayCurrency = "INR",
            convertedAmounts = emptyMap(),
        )

        assertEquals(0, BigDecimal("100").compareTo(totals.monthly.getValue("INR").amount))
    }

    @Test
    fun `yearly is twelve times the monthly equivalent of each cycle`() {
        val totals = expenseSubscriptionTotals(
            subscriptions = listOf(
                subscription(1, "100", billingCycle = "Monthly"),
                // 1200 a year is 100 a month.
                subscription(2, "1200", billingCycle = "Annual"),
            ),
            isUnified = false,
            displayCurrency = "INR",
            convertedAmounts = emptyMap(),
        )

        val monthly = totals.monthly.getValue("INR").amount
        val yearly = totals.yearly.getValue("INR").amount
        assertEquals(0, BigDecimal("200").compareTo(monthly.setScale(2, java.math.RoundingMode.HALF_UP)))
        assertEquals(0, BigDecimal("2400").compareTo(yearly.setScale(2, java.math.RoundingMode.HALF_UP)))
    }

    @Test
    fun `unified mode counts display currency rows and converted rows, and skips unconverted ones`() {
        val totals = expenseSubscriptionTotals(
            subscriptions = listOf(
                subscription(1, "100", currency = "INR"),
                subscription(2, "10", currency = "USD"),
                subscription(3, "7", currency = "EUR"),
            ),
            isUnified = true,
            displayCurrency = "INR",
            // The USD row converts to 800 INR; the EUR row has no rate.
            convertedAmounts = mapOf(2L to BigDecimal("800")),
        )

        assertEquals(setOf("INR"), totals.monthly.keys)
        assertEquals(0, BigDecimal("900").compareTo(totals.monthly.getValue("INR").amount))
        assertEquals(0, BigDecimal("10800").compareTo(totals.yearly.getValue("INR").amount))
    }

    @Test
    fun `unified mode without a display currency yields no totals`() {
        val totals = expenseSubscriptionTotals(
            subscriptions = listOf(subscription(1, "100")),
            isUnified = true,
            displayCurrency = null,
            convertedAmounts = emptyMap(),
        )

        assertTrue(totals.monthly.isEmpty())
        assertTrue(totals.yearly.isEmpty())
    }

    private fun subscription(
        id: Long,
        amount: String,
        currency: String = "INR",
        direction: SubscriptionDirection = SubscriptionDirection.EXPENSE,
        billingCycle: String = "Monthly",
    ) = SubscriptionEntity(
        id = id,
        merchantName = "Service $id",
        amount = BigDecimal(amount),
        nextPaymentDate = LocalDate.of(2026, 9, 20),
        currency = currency,
        direction = direction,
        billingCycle = billingCycle,
    )
}
