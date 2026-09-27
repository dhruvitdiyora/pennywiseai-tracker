package com.pennywiseai.tracker.presentation.transactions

import com.pennywiseai.tracker.presentation.common.AmountRangeValidation
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.TransactionAmountFilter
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.presentation.common.parseAmountRange
import java.time.LocalDate

data class TransactionFilterDraftValidation(
    val amount: AmountRangeValidation,
    val customDateRequired: Boolean,
) {
    val isValid: Boolean
        get() = amount.isValid && !customDateRequired
}

/**
 * Editable copy of every non-search, non-sort Transactions filter.
 *
 * The filter sheet owns this value while it is open. Dismissing the sheet drops
 * the copy; only Apply sends it to [TransactionsViewModel]. This keeps back,
 * swipe, and Cancel behavior honest instead of mutating the visible list live.
 */
data class TransactionFilterDraft(
    val period: TimePeriod = TimePeriod.THIS_MONTH,
    val customDateRange: Pair<LocalDate, LocalDate>? = null,
    val category: String? = null,
    val navigationCategories: List<String>? = null,
    /** True while [navigationCategories] is still the list a budget opened us with. */
    val categoriesFromBudget: Boolean = false,
    val transactionType: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val profileId: Long? = null,
    val accountKey: String? = null,
    val tag: String? = null,
    val minimumText: String = "",
    val maximumText: String = "",
    val originalCurrencies: Set<String> = emptySet(),
) {
    val amountValidation: AmountRangeValidation
        get() = parseAmountRange(minimumText, maximumText)

    val validation: TransactionFilterDraftValidation
        get() = TransactionFilterDraftValidation(
            amount = amountValidation,
            customDateRequired = period == TimePeriod.CUSTOM && customDateRange == null,
        )

    val hasAmountOrCurrencyFilter: Boolean
        get() = minimumText.isNotBlank() ||
            maximumText.isNotBlank() ||
            originalCurrencies.isNotEmpty()

    val activeFilterCount: Int
        get() = listOf(
            period != TimePeriod.THIS_MONTH || customDateRange != null,
            category != null || !navigationCategories.isNullOrEmpty(),
            transactionType != TransactionTypeFilter.ALL,
            profileId != null,
            accountKey != null,
            tag != null,
            hasAmountOrCurrencyFilter,
        ).count { it }

    fun reset(): TransactionFilterDraft = TransactionFilterDraft()

    /** Categories currently shown; every available one when nothing is filtered. */
    fun selectedCategories(available: List<String>): Set<String> =
        navigationCategories?.toSet()
            ?: category?.let { setOf(it) }
            ?: available.toSet()

    /**
     * Include or exclude one category (#786). Unticking narrows the selection;
     * ticking the last one back, or emptying it, returns to no category filter.
     */
    fun toggleCategory(category: String, available: List<String>): TransactionFilterDraft {
        val current = selectedCategories(available)
        val next = if (category in current) current - category else current + category
        val kept = available.filter { it in next }
        return when {
            kept.isEmpty() || kept.size == available.size ->
                copy(category = null, navigationCategories = null, categoriesFromBudget = false)
            kept.size == 1 ->
                copy(category = kept.single(), navigationCategories = null, categoriesFromBudget = false)
            else ->
                copy(category = null, navigationCategories = kept, categoriesFromBudget = false)
        }
    }

    fun validatedAmountFilter(unifiedMode: Boolean): TransactionAmountFilter? {
        val range = validation.amount.range?.takeIf { validation.isValid } ?: return null
        return TransactionAmountFilter(
            range = range,
            originalCurrencies = if (unifiedMode) {
                originalCurrencies.map { it.uppercase() }.toSet()
            } else {
                emptySet()
            },
        )
    }
}
