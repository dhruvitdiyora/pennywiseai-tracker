package com.pennywiseai.tracker.domain.usecase

import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.presentation.common.filterAccountsByProfile
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetWorthCalculatorTest {

    private val noRates: suspend (BigDecimal, String, String) -> BigDecimal? = { _, _, _ -> null }

    @Test
    fun `single currency sums accounts and wallets`() = runTest {
        val result = NetWorthCalculator.calculate(
            latestBalances = listOf(
                account("Bank A", "1111", "1000.50"),
                account("Bank B", "2222", "250"),
                account("Mobile Wallet", AccountBalanceEntity.WALLET_ACCOUNT_MARKER, "49.50"),
            ),
            selectedCurrency = "INR",
            unifiedMode = false,
            convert = noRates,
        )

        assertEquals(BigDecimal("1300.00"), result.netWorth)
        assertEquals(BigDecimal("1300.00"), result.assets)
        assertTrue(result.hasAccounts)
        assertFalse(result.isApproximate)
    }

    @Test
    fun `credit card outstanding is subtracted as a liability`() = runTest {
        val result = NetWorthCalculator.calculate(
            latestBalances = listOf(
                account("Bank A", "1111", "5000"),
                account("Card", "9999", "1200", isCreditCard = true).copy(creditLimit = BigDecimal("50000")),
            ),
            selectedCurrency = "INR",
            unifiedMode = false,
            convert = noRates,
        )

        // Outstanding (balance) counts, never the unused credit limit.
        assertEquals(BigDecimal("3800"), result.netWorth)
        assertEquals(BigDecimal("5000"), result.assets)
        assertEquals(BigDecimal("1200"), result.liabilities)
    }

    @Test
    fun `net worth can be negative when cards owe more than accounts hold`() = runTest {
        val result = NetWorthCalculator.calculate(
            latestBalances = listOf(
                account("Bank A", "1111", "100"),
                account("Card", "9999", "400", isCreditCard = true),
            ),
            selectedCurrency = "INR",
            unifiedMode = false,
            convert = noRates,
        )

        assertEquals(BigDecimal("-300"), result.netWorth)
    }

    @Test
    fun `multi currency without unified mode never cross sums`() = runTest {
        val balances = listOf(
            account("Rupee Bank", "1111", "1000"),
            account("Dollar Bank", "2222", "10", currency = "USD"),
            account("Dollar Card", "3333", "4", currency = "USD", isCreditCard = true),
        )
        var convertCalled = false
        val convert: suspend (BigDecimal, String, String) -> BigDecimal? = { a, _, _ ->
            convertCalled = true
            a
        }

        val inr = NetWorthCalculator.calculate(balances, "INR", unifiedMode = false, convert = convert)
        val usd = NetWorthCalculator.calculate(balances, "USD", unifiedMode = false, convert = convert)
        val eur = NetWorthCalculator.calculate(balances, "EUR", unifiedMode = false, convert = convert)

        assertEquals(BigDecimal("1000"), inr.netWorth)
        assertEquals(BigDecimal("6"), usd.netWorth)
        assertFalse(eur.hasAccounts)
        assertEquals(BigDecimal.ZERO, eur.netWorth)
        assertFalse(convertCalled)
        assertFalse(inr.isApproximate)

        val (assets, liabilities) = NetWorthCalculator.byCurrency(balances)
        assertEquals(setOf("INR", "USD"), assets.keys)
        assertEquals(setOf("USD"), liabilities.keys)
    }

    @Test
    fun `unified mode converts each currency subtotal`() = runTest {
        val balances = listOf(
            account("Rupee Bank", "1111", "1000"),
            account("Dollar Bank", "2222", "10", currency = "USD"),
            account("Dollar Card", "3333", "4", currency = "USD", isCreditCard = true),
        )
        val convert: suspend (BigDecimal, String, String) -> BigDecimal? = { amount, from, to ->
            if (from == "USD" && to == "INR") amount.multiply(BigDecimal("80")) else null
        }

        val result = NetWorthCalculator.calculate(balances, "INR", unifiedMode = true, convert = convert)

        // 1000 + 10*80 - 4*80
        assertEquals(BigDecimal("1480"), result.netWorth)
        assertEquals(BigDecimal("1800"), result.assets)
        assertFalse(result.isApproximate)
    }

    @Test
    fun `unified mode leaves out a currency with no rate and flags approximate`() = runTest {
        val result = NetWorthCalculator.calculate(
            latestBalances = listOf(
                account("Rupee Bank", "1111", "1000"),
                account("Odd Bank", "2222", "999", currency = "ZZZ"),
            ),
            selectedCurrency = "INR",
            unifiedMode = true,
            convert = noRates,
        )

        assertEquals(BigDecimal("1000"), result.netWorth)
        assertTrue(result.isApproximate)
    }

    @Test
    fun `profile filter and hidden accounts are applied before totalling`() = runTest {
        val all = listOf(
            account("Personal Bank", "1111", "1000"),
            account("Hidden Bank", "2222", "700"),
            account("Business Bank", "3333", "9000", profileId = 2L),
            account("Business Card", "4444", "500", profileId = 2L, isCreditCard = true),
        )

        val personal = NetWorthCalculator.calculate(
            filterAccountsByProfile(all, setOf("Hidden Bank_2222"), ProfileEntity.PERSONAL_ID),
            "INR", unifiedMode = false, convert = noRates,
        )
        val business = NetWorthCalculator.calculate(
            filterAccountsByProfile(all, emptySet(), 2L),
            "INR", unifiedMode = false, convert = noRates,
        )
        val everyone = NetWorthCalculator.calculate(
            filterAccountsByProfile(all, setOf("Hidden Bank_2222"), null),
            "INR", unifiedMode = false, convert = noRates,
        )

        assertEquals(BigDecimal("1000"), personal.netWorth)
        assertEquals(BigDecimal("8500"), business.netWorth)
        assertEquals(BigDecimal("9500"), everyone.netWorth)
    }

    @Test
    fun `month over month percent`() {
        assertEquals(10, NetWorthCalculator.changePercent(BigDecimal("1100"), BigDecimal("1000")))
        assertEquals(-25, NetWorthCalculator.changePercent(BigDecimal("750"), BigDecimal("1000")))
        // Rounds half up to a whole percent.
        assertEquals(13, NetWorthCalculator.changePercent(BigDecimal("1125"), BigDecimal("1000")))
        // A negative starting point still reports the direction of change.
        assertEquals(50, NetWorthCalculator.changePercent(BigDecimal("-500"), BigDecimal("-1000")))
        assertEquals(-50, NetWorthCalculator.changePercent(BigDecimal("-1500"), BigDecimal("-1000")))
    }

    @Test
    fun `month over month percent with zero or unknown previous`() {
        assertNull(NetWorthCalculator.changePercent(BigDecimal("500"), BigDecimal.ZERO))
        assertEquals(0, NetWorthCalculator.changePercent(BigDecimal.ZERO, BigDecimal("0.00")))
        assertNull(NetWorthCalculator.changePercent(BigDecimal("500"), null))
    }

    private fun account(
        bankName: String,
        last4: String,
        balance: String,
        currency: String = "INR",
        profileId: Long = ProfileEntity.PERSONAL_ID,
        isCreditCard: Boolean = false,
    ) = AccountBalanceEntity(
        bankName = bankName,
        accountLast4 = last4,
        balance = BigDecimal(balance),
        timestamp = LocalDateTime.of(2026, 9, 1, 12, 0),
        currency = currency,
        profileId = profileId,
        isCreditCard = isCreditCard,
    )
}
