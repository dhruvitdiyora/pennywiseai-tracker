package com.pennywiseai.tracker.presentation.transactions

import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.presentation.common.AmountRangeError
import com.pennywiseai.tracker.presentation.common.TransactionAmountFilter
import com.pennywiseai.tracker.presentation.common.filterTransactionsByAmount
import com.pennywiseai.tracker.presentation.common.parseAmountRange
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class TransactionAmountFilterTest {

    @Test
    fun `range parser accepts inclusive bounds and blank ends`() {
        val result = parseAmountRange(" 10.25 ", "")

        assertTrue(result.isValid)
        assertEquals(BigDecimal("10.25"), result.range?.minimum)
        assertEquals(null, result.range?.maximum)
    }

    @Test
    fun `range parser rejects malformed negative and inverted input`() {
        assertEquals(
            AmountRangeError.INVALID_NUMBER,
            parseAmountRange("not-a-number", "").error
        )
        assertEquals(
            AmountRangeError.NEGATIVE_AMOUNT,
            parseAmountRange("-1", "").error
        )
        assertEquals(
            AmountRangeError.MINIMUM_GREATER_THAN_MAXIMUM,
            parseAmountRange("20", "19.99").error
        )
    }

    @Test
    fun `non unified range compares native amount and ignores native chips`() = runBlocking {
        val transactions = listOf(
            tx(1, "10", "INR"),
            tx(2, "20", "INR")
        )

        val result = filterTransactionsByAmount(
            transactions = transactions,
            filter = TransactionAmountFilter(
                range = parseAmountRange("15", "20").range!!,
                originalCurrencies = setOf("USD")
            ),
            unifiedMode = false,
            displayCurrency = "INR",
            convertAmountOrNull = { _, _, _ ->
                error("non-unified filtering must not convert")
            }
        )

        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun `unified range converts and excludes rows without a rate`() = runBlocking {
        val transactions = listOf(
            tx(1, "10", "INR"),
            tx(2, "10", "USD"),
            tx(3, "10", "JPY")
        )

        val result = filterTransactionsByAmount(
            transactions = transactions,
            filter = TransactionAmountFilter(
                range = parseAmountRange("19", "21").range!!,
                originalCurrencies = setOf("USD", "JPY")
            ),
            unifiedMode = true,
            displayCurrency = "INR",
            convertAmountOrNull = { amount, from, _ ->
                when (from) {
                    "USD" -> amount.multiply(BigDecimal("2"))
                    "JPY" -> null
                    else -> amount
                }
            }
        )

        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun `unified range does not request a rate for display currency rows`() = runBlocking {
        val result = filterTransactionsByAmount(
            transactions = listOf(tx(1, "20", "INR")),
            filter = TransactionAmountFilter(
                range = parseAmountRange("19", "21").range!!
            ),
            unifiedMode = true,
            displayCurrency = "inr",
            convertAmountOrNull = { _, _, _ ->
                error("same-currency filtering must not request a rate")
            }
        )

        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun `currency-only filter does not require conversion`() = runBlocking {
        val transactions = listOf(tx(1, "10", "INR"), tx(2, "10", "USD"))

        val result = filterTransactionsByAmount(
            transactions = transactions,
            filter = TransactionAmountFilter(originalCurrencies = setOf("usd")),
            unifiedMode = true,
            displayCurrency = "INR",
            convertAmountOrNull = { _, _, _ -> error("currency-only filter must not convert") }
        )

        assertEquals(listOf(2L), result.map { it.id })
    }

    private fun tx(id: Long, amount: String, currency: String) = TransactionEntity(
        id = id,
        amount = BigDecimal(amount),
        merchantName = "Merchant",
        category = "Other",
        transactionType = TransactionType.EXPENSE,
        dateTime = LocalDateTime.of(2026, 9, 1, 12, 0),
        transactionHash = "hash-$id",
        currency = currency
    )
}
