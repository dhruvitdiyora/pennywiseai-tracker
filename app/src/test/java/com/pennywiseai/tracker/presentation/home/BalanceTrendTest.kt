package com.pennywiseai.tracker.presentation.home

import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceTrendTest {
    private val start = LocalDate.of(2026, 9, 1)
    private val end = LocalDate.of(2026, 9, 3)

    @Test
    fun `uses latest row per account per day and carries balances forward`() = runTest {
        val result = trend(
            rows = listOf(
                row("Primary", "1111", "100", start, hour = 8),
                row("Reserve", "2222", "50", start, hour = 9),
                row("Primary", "1111", "120", start.plusDays(1), hour = 8),
                row("Primary", "1111", "130", start.plusDays(1), hour = 18),
                row("Reserve", "2222", "75", start.plusDays(2), hour = 9),
            ),
        )

        assertEquals(listOf("150", "180", "205"), result.values.map(BigDecimal::toPlainString))
        assertFalse(result.isApproximate)
    }

    @Test
    fun `excludes credit cards hidden accounts and other profiles`() = runTest {
        val rows = listOf(
            row("Personal", "1111", "100", start),
            row("Personal", "1111", "120", start.plusDays(1)),
            row("Hidden", "2222", "500", start),
            row("Hidden", "2222", "600", start.plusDays(1)),
            row("Business", "3333", "900", start, profileId = 2L),
            row("Business", "3333", "950", start.plusDays(1), profileId = 2L),
            row("Card", "4444", "700", start, isCreditCard = true),
            row("Card", "4444", "800", start.plusDays(1), isCreditCard = true),
        )

        val result = trend(
            rows = rows,
            selectedProfileId = ProfileEntity.PERSONAL_ID,
            hiddenAccounts = setOf("Hidden_2222"),
        )

        assertEquals(listOf("100", "120", "120"), result.values.map(BigDecimal::toPlainString))
    }

    @Test
    fun `native mode never sums a foreign currency`() = runTest {
        val result = trend(
            rows = listOf(
                row("Rupee", "1111", "100", start),
                row("Rupee", "1111", "120", start.plusDays(1)),
                row("Dollar", "2222", "10", start, currency = "USD"),
                row("Dollar", "2222", "12", start.plusDays(1), currency = "USD"),
            ),
        )

        assertEquals(listOf("100", "120", "120"), result.values.map(BigDecimal::toPlainString))
        assertFalse(result.isApproximate)
    }

    @Test
    fun `unified mode converts known currencies and marks missing rates approximate`() = runTest {
        val result = trend(
            rows = listOf(
                row("Rupee", "1111", "100", start),
                row("Rupee", "1111", "110", start.plusDays(1)),
                row("Dollar", "2222", "10", start, currency = "USD"),
                row("Dollar", "2222", "12", start.plusDays(1), currency = "USD"),
                row("Unknown", "3333", "4", start, currency = "ZZZ"),
            ),
            unifiedMode = true,
            convert = { amount, from, _ ->
                if (from == "USD") amount.multiply(BigDecimal("80")) else null
            },
        )

        assertEquals(listOf("900", "1070", "1070"), result.values.map(BigDecimal::toPlainString))
        assertTrue(result.isApproximate)
    }

    @Test
    fun `uses a pre-window snapshot as the opening point but ignores future rows`() = runTest {
        val result = trend(
            rows = listOf(
                row("Primary", "1111", "90", start.minusDays(10)),
                row("Primary", "1111", "120", start.plusDays(1)),
                row("Primary", "1111", "999", end.plusDays(1)),
            ),
        )

        assertEquals(listOf("90", "120", "120"), result.values.map(BigDecimal::toPlainString))
    }

    @Test
    fun `one observed portfolio state returns an honest empty trend`() = runTest {
        val result = trend(rows = listOf(row("Primary", "1111", "100", start)))

        assertTrue(result.values.isEmpty())
    }

    private suspend fun trend(
        rows: List<AccountBalanceEntity>,
        selectedProfileId: Long? = null,
        hiddenAccounts: Set<String> = emptySet(),
        unifiedMode: Boolean = false,
        convert: suspend (BigDecimal, String, String) -> BigDecimal? = { _, _, _ -> null },
    ) = buildBalanceTrend(
        balances = rows,
        startDate = start,
        endDate = end,
        selectedProfileId = selectedProfileId,
        hiddenAccounts = hiddenAccounts,
        selectedCurrency = "INR",
        unifiedMode = unifiedMode,
        convert = convert,
    )

    private fun row(
        bankName: String,
        last4: String,
        balance: String,
        day: LocalDate,
        hour: Int = 12,
        currency: String = "INR",
        profileId: Long = ProfileEntity.PERSONAL_ID,
        isCreditCard: Boolean = false,
    ) = AccountBalanceEntity(
        bankName = bankName,
        accountLast4 = last4,
        balance = BigDecimal(balance),
        timestamp = LocalDateTime.of(day.year, day.month, day.dayOfMonth, hour, 0),
        currency = currency,
        profileId = profileId,
        isCreditCard = isCreditCard,
    )
}
