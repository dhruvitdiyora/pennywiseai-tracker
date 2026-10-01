package com.pennywiseai.tracker.presentation.subscriptions

import com.pennywiseai.tracker.data.database.entity.SubscriptionDirection
import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import com.pennywiseai.tracker.domain.model.SubscriptionBillingCycle
import com.pennywiseai.tracker.utils.Money
import com.pennywiseai.tracker.utils.sumByCurrency
import java.math.BigDecimal

/**
 * What the active subscriptions cost, per month and per year.
 *
 * Both maps are keyed by currency code. In native mode there is one entry per
 * currency in use; in unified mode there is a single entry in the display
 * currency. They are never merged into one figure across currencies — render
 * them with `CurrencyFormatter.formatByCurrency`.
 */
internal data class SubscriptionExpenseTotals(
    val monthly: Map<String, Money>,
    val yearly: Map<String, Money>,
)

private val MONTHS_PER_YEAR = BigDecimal(12)

/**
 * Totals the **expense** subscriptions only: money coming in (an income-
 * direction subscription such as an allowance) is not a cost and must not be
 * added to what the user spends.
 *
 * Every charge is first reduced to its monthly equivalent with
 * [SubscriptionBillingCycle.monthlyEquivalent], so weekly, quarterly, annual and
 * custom cycles all land on the same basis; yearly is that monthly figure x 12.
 *
 * - Native mode ([isUnified] false): bucketed per currency, so a rupee and a
 *   dollar subscription are totalled separately.
 * - Unified mode: each row is expressed in [displayCurrency] — its own amount
 *   when it is already in that currency, otherwise its entry in
 *   [convertedAmounts]. A row with no usable conversion is left out rather than
 *   counted at face value in the wrong currency.
 */
internal fun expenseSubscriptionTotals(
    subscriptions: List<SubscriptionEntity>,
    isUnified: Boolean,
    displayCurrency: String?,
    convertedAmounts: Map<Long, BigDecimal>,
): SubscriptionExpenseTotals {
    val expenses = subscriptions.filter { it.direction == SubscriptionDirection.EXPENSE }

    val monthly: Map<String, Money> = if (isUnified) {
        if (displayCurrency == null) {
            emptyMap()
        } else {
            val total = expenses
                .mapNotNull { subscription ->
                    val amount = if (subscription.currency.equals(displayCurrency, ignoreCase = true)) {
                        subscription.amount
                    } else {
                        convertedAmounts[subscription.id]
                    }
                    amount?.let {
                        SubscriptionBillingCycle.monthlyEquivalent(it, subscription.billingCycle)
                    }
                }
                .fold(Money.zero(displayCurrency)) { acc, monthlyAmount ->
                    acc + Money(monthlyAmount, displayCurrency)
                }
            mapOf(displayCurrency to total)
        }
    } else {
        expenses.sumByCurrency(
            currencySelector = { it.currency },
            amountSelector = { SubscriptionBillingCycle.monthlyEquivalent(it.amount, it.billingCycle) },
        )
    }

    val yearly = monthly.mapValues { (currency, total) ->
        Money(total.amount.multiply(MONTHS_PER_YEAR), currency)
    }
    return SubscriptionExpenseTotals(monthly = monthly, yearly = yearly)
}
