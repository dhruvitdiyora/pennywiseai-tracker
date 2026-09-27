package com.pennywiseai.tracker.presentation.common

import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import java.math.BigDecimal

/** A validated inclusive amount range entered from the Transactions filter sheet. */
data class AmountRange(
    val minimum: BigDecimal? = null,
    val maximum: BigDecimal? = null
)

enum class AmountRangeError {
    INVALID_NUMBER,
    NEGATIVE_AMOUNT,
    MINIMUM_GREATER_THAN_MAXIMUM
}

/** Result of parsing the two free-form amount fields in the filter sheet. */
data class AmountRangeValidation(
    val range: AmountRange? = null,
    val error: AmountRangeError? = null
) {
    val isValid: Boolean get() = error == null
}

/**
 * The extra Transactions filters. Currency values are original/native codes,
 * not display currencies. The view model applies those values only in unified
 * mode so they cannot compete with the existing single-currency selector.
 */
data class TransactionAmountFilter(
    val range: AmountRange = AmountRange(),
    val originalCurrencies: Set<String> = emptySet()
) {
    val hasAmountRange: Boolean
        get() = range.minimum != null || range.maximum != null

    val isActive: Boolean
        get() = hasAmountRange || originalCurrencies.isNotEmpty()
}

/** Parse and validate inclusive minimum/maximum amount text using BigDecimal only. */
fun parseAmountRange(minimumText: String, maximumText: String): AmountRangeValidation {
    fun parse(value: String): BigDecimal? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return try {
            BigDecimal(trimmed)
        } catch (_: NumberFormatException) {
            null
        }
    }

    val minimumRaw = minimumText.trim()
    val maximumRaw = maximumText.trim()
    val minimum = if (minimumRaw.isEmpty()) null else parse(minimumRaw)
    val maximum = if (maximumRaw.isEmpty()) null else parse(maximumRaw)

    if ((minimumRaw.isNotEmpty() && minimum == null) ||
        (maximumRaw.isNotEmpty() && maximum == null)
    ) {
        return AmountRangeValidation(error = AmountRangeError.INVALID_NUMBER)
    }
    if (minimum?.signum() == -1 || maximum?.signum() == -1) {
        return AmountRangeValidation(error = AmountRangeError.NEGATIVE_AMOUNT)
    }
    if (minimum != null && maximum != null && minimum > maximum) {
        return AmountRangeValidation(error = AmountRangeError.MINIMUM_GREATER_THAN_MAXIMUM)
    }
    return AmountRangeValidation(range = AmountRange(minimum, maximum))
}

/**
 * Applies the amount range and original-currency selection to already filtered
 * transactions. In unified mode, a range is evaluated in [displayCurrency]
 * and rows without a conversion rate are excluded. In non-unified mode the
 * caller has already selected one native currency, so the native amount is
 * compared directly and original-currency chips are ignored.
 */
suspend fun filterTransactionsByAmount(
    transactions: List<TransactionEntity>,
    filter: TransactionAmountFilter,
    unifiedMode: Boolean,
    displayCurrency: String,
    convertAmountOrNull: suspend (amount: BigDecimal, fromCurrency: String, toCurrency: String) -> BigDecimal?
): List<TransactionEntity> {
    val minimum = filter.range.minimum
    val maximum = filter.range.maximum
    val hasRange = minimum != null || maximum != null
    val selectedNativeCurrencies = filter.originalCurrencies
        .map { it.uppercase() }
        .toSet()

    if (!hasRange && (!unifiedMode || selectedNativeCurrencies.isEmpty())) {
        return transactions
    }

    return transactions.filter { transaction ->
        if (unifiedMode && selectedNativeCurrencies.isNotEmpty() &&
            transaction.currency.uppercase() !in selectedNativeCurrencies
        ) {
            return@filter false
        }

        if (!hasRange) return@filter true

        val comparableAmount = if (unifiedMode &&
            !transaction.currency.equals(displayCurrency, ignoreCase = true)
        ) {
            convertAmountOrNull(
                transaction.amount,
                transaction.currency,
                displayCurrency
            )
        } else {
            transaction.amount
        }

        comparableAmount != null &&
            (minimum == null || comparableAmount >= minimum) &&
            (maximum == null || comparableAmount <= maximum)
    }
}
