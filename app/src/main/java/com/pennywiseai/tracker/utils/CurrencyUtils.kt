package com.pennywiseai.tracker.utils

/**
 * Currency-list helpers.
 *
 * Formatting lives in [CurrencyFormatter] — it is currency-aware and honours the user's
 * number-format preference. This object deliberately holds no formatter: the INR-hardcoded
 * ones that used to live here were unused, and their `minimumFractionDigits = 1` contradicted
 * the app-wide rule that an amount renders at exactly the precision it was stored with.
 */
object CurrencyUtils {

    /**
     * Sorts a list of currency codes with INR prioritized first, then alphabetically.
     * This is the standard sorting for currency lists throughout the app.
     *
     * @param currencies List of currency codes to sort
     * @return Sorted list with INR first (if present), then alphabetically
     *
     * Example:
     * ```
     * sortCurrencies(listOf("USD", "EUR", "INR", "GBP"))
     * // Returns: ["INR", "EUR", "GBP", "USD"]
     * ```
     */
    fun sortCurrencies(currencies: List<String>): List<String> {
        return currencies.sortedWith { a, b ->
            when {
                a == "INR" -> -1 // INR first
                b == "INR" -> 1
                else -> a.compareTo(b) // Alphabetical for others
            }
        }
    }

    /**
     * Returns a comprehensive list of all supported currencies.
     * Includes currencies from supported banks and common international currencies.
     *
     * @return List of currency codes sorted with INR first, then alphabetically
     */
    fun getAllSupportedCurrencies(): List<String> {
        // Keep this in sync with the currencies our bank parsers emit
        // (BankParser.getCurrency()) so anything the app can auto-import is also
        // manually selectable. Every code here must be a valid ISO 4217 code.
        val currencies = listOf(
            // Major currencies
            "INR", "USD", "EUR", "GBP", "JPY", "CNY",
            // Middle East
            "AED", "SAR", "KWD", "OMR", "IRR", "JOD", "BHD",
            // South Asia
            "NPR", "LKR", "BDT", "PKR",
            // Asia Pacific
            "SGD", "AUD", "THB", "MYR", "KRW",
            // Americas
            "CAD", "MXN", "COP", "BRL",
            // Africa
            "ETB", "KES", "NGN", "TZS", "MZN", "EGP",
            // Europe
            "BYN", "CZK", "RUB", "TRY"
        )
        return sortCurrencies(currencies)
    }
}