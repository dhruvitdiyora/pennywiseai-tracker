package com.pennywiseai.tracker.presentation.home

import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import java.math.BigDecimal
import java.time.LocalDate

internal data class BalanceTrendResult(
    val values: List<BigDecimal>,
    val isApproximate: Boolean,
)

/**
 * Builds a daily cash-account portfolio series without ever mixing native
 * currencies. The latest row for an account on each day wins and is carried
 * forward until that account receives another balance update.
 */
internal suspend fun buildBalanceTrend(
    balances: List<AccountBalanceEntity>,
    startDate: LocalDate,
    endDate: LocalDate,
    selectedProfileId: Long?,
    hiddenAccounts: Set<String>,
    selectedCurrency: String,
    unifiedMode: Boolean,
    convert: suspend (amount: BigDecimal, fromCurrency: String, toCurrency: String) -> BigDecimal?,
): BalanceTrendResult {
    if (endDate < startDate) return BalanceTrendResult(emptyList(), false)

    val eligible = balances
        .asSequence()
        .filterNot { it.isCreditCard }
        .filter { it.timestamp.toLocalDate() <= endDate }
        .filter { selectedProfileId == null || it.profileId == selectedProfileId }
        .filterNot { "${it.bankName}_${it.accountLast4}" in hiddenAccounts }
        .sortedBy { it.timestamp }
        .toList()

    var missingConversion = false
    val convertedRows = buildList {
        for (row in eligible) {
            val convertedBalance = when {
                row.currency.equals(selectedCurrency, ignoreCase = true) -> row.balance
                !unifiedMode -> null
                else -> convert(row.balance, row.currency, selectedCurrency).also {
                    if (it == null) missingConversion = true
                }
            }
            if (convertedBalance != null) add(row to convertedBalance)
        }
    }

    // A line needs at least two recorded portfolio states. Carrying one lone
    // snapshot across 180 days would imply history the app never observed.
    val observedDays = convertedRows
        .map { (row, _) -> row.timestamp.toLocalDate().coerceAtLeast(startDate) }
        .distinct()
    if (observedDays.size < 2) {
        return BalanceTrendResult(emptyList(), missingConversion)
    }

    data class AccountKey(val bankName: String, val accountLast4: String)

    val latestByAccount = mutableMapOf<AccountKey, BigDecimal>()
    val rowsByDay = convertedRows.groupBy { (row, _) -> row.timestamp.toLocalDate() }
    convertedRows
        .filter { (row, _) -> row.timestamp.toLocalDate() < startDate }
        .forEach { (row, value) ->
            latestByAccount[AccountKey(row.bankName, row.accountLast4)] = value
        }

    val firstObservedDay = observedDays.minOrNull() ?: return BalanceTrendResult(emptyList(), missingConversion)
    val values = mutableListOf<BigDecimal>()
    var day = firstObservedDay
    while (!day.isAfter(endDate)) {
        rowsByDay[day]
            .orEmpty()
            .sortedBy { (row, _) -> row.timestamp }
            .forEach { (row, value) ->
                latestByAccount[AccountKey(row.bankName, row.accountLast4)] = value
            }
        if (latestByAccount.isNotEmpty()) {
            values += latestByAccount.values.fold(BigDecimal.ZERO, BigDecimal::add)
        }
        day = day.plusDays(1)
    }

    return BalanceTrendResult(values, missingConversion)
}
