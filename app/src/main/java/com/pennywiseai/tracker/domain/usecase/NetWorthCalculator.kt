package com.pennywiseai.tracker.domain.usecase

import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.utils.Money
import com.pennywiseai.tracker.utils.sumByCurrency
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Net worth for one currency: what the visible accounts hold minus what the
 * visible credit cards and loan accounts owe.
 *
 * Mirrors Cashiro's Home net worth (latest balance of every asset account,
 * minus the outstanding balance of every liability — a credit card or a bank
 * loan account, see [AccountBalanceEntity.isLiability]; lend/borrow
 * records are not part of it), with one PennyWise rule on top: currencies are
 * never added together. Without unified mode only accounts already in the
 * requested currency count; in unified mode each currency's subtotal is
 * converted, and a currency with no exchange rate is left out and the result
 * flagged approximate instead of being added at face value.
 *
 * @property hasAccounts false when no visible account or card is held in (or
 *   could be converted to) [currency]; the headline then has nothing to show.
 */
data class NetWorth(
    val currency: String,
    val assets: BigDecimal,
    val liabilities: BigDecimal,
    val hasAccounts: Boolean,
    val isApproximate: Boolean,
) {
    val netWorth: BigDecimal get() = assets - liabilities
}

object NetWorthCalculator {

    /**
     * Native per-currency assets and liabilities. Each map only ever adds
     * amounts of one currency (via [sumByCurrency]).
     *
     * [latestBalances] must already be the latest row per account, filtered
     * to the selected profile and with user-hidden accounts removed — the same
     * list Home's account section shows.
     */
    fun byCurrency(latestBalances: List<AccountBalanceEntity>): Pair<Map<String, Money>, Map<String, Money>> {
        val assets = latestBalances
            .filterNot { it.isLiability }
            .sumByCurrency({ it.currency.uppercase() }, { it.balance })
        val liabilities = latestBalances
            .filter { it.isLiability }
            .sumByCurrency({ it.currency.uppercase() }, { it.balance })
        return assets to liabilities
    }

    /**
     * Net worth in [selectedCurrency].
     *
     * @param convert returns the converted amount, or null when no rate exists
     *   for the pair. Only consulted in [unifiedMode].
     */
    suspend fun calculate(
        latestBalances: List<AccountBalanceEntity>,
        selectedCurrency: String,
        unifiedMode: Boolean,
        convert: suspend (amount: BigDecimal, fromCurrency: String, toCurrency: String) -> BigDecimal?,
    ): NetWorth {
        val target = selectedCurrency.uppercase()
        val (assetsByCurrency, liabilitiesByCurrency) = byCurrency(latestBalances)
        val currencies = assetsByCurrency.keys + liabilitiesByCurrency.keys

        var assets = BigDecimal.ZERO
        var liabilities = BigDecimal.ZERO
        var hasAccounts = false
        var approximate = false

        for (currency in currencies) {
            val nativeAssets = assetsByCurrency[currency]?.amount ?: BigDecimal.ZERO
            val nativeLiabilities = liabilitiesByCurrency[currency]?.amount ?: BigDecimal.ZERO
            when {
                currency == target -> {
                    assets += nativeAssets
                    liabilities += nativeLiabilities
                    hasAccounts = true
                }
                !unifiedMode -> Unit // other currencies are shown via the currency chip, never summed in
                else -> {
                    val convertedAssets = if (nativeAssets.signum() == 0) BigDecimal.ZERO
                    else convert(nativeAssets, currency, selectedCurrency)
                    val convertedLiabilities = if (nativeLiabilities.signum() == 0) BigDecimal.ZERO
                    else convert(nativeLiabilities, currency, selectedCurrency)
                    if (convertedAssets == null || convertedLiabilities == null) {
                        approximate = true
                    } else {
                        assets += convertedAssets
                        liabilities += convertedLiabilities
                        hasAccounts = true
                    }
                }
            }
        }

        return NetWorth(
            currency = selectedCurrency,
            assets = assets,
            liabilities = liabilities,
            hasAccounts = hasAccounts,
            isApproximate = approximate,
        )
    }

    /**
     * Whole-percent change from [previous] to [current], or null when it has no
     * honest value: no previous figure, or a previous figure of zero with a
     * non-zero current one (growth from nothing is not a percentage). The
     * denominator is |previous| so the sign always follows the direction of
     * change, even when net worth was negative.
     */
    fun changePercent(current: BigDecimal, previous: BigDecimal?): Int? {
        if (previous == null) return null
        if (previous.signum() == 0) return if (current.signum() == 0) 0 else null
        return (current - previous)
            .multiply(BigDecimal(100))
            .divide(previous.abs(), 0, RoundingMode.HALF_UP)
            .toInt()
    }
}
