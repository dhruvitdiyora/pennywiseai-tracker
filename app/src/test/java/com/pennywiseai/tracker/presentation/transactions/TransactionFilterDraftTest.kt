package com.pennywiseai.tracker.presentation.transactions

import com.pennywiseai.tracker.presentation.common.AmountRangeError
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class TransactionFilterDraftTest {

    @Test
    fun `reset restores every sheet filter default`() {
        val populated = TransactionFilterDraft(
            period = TimePeriod.CUSTOM,
            customDateRange = LocalDate.of(2026, 8, 1) to LocalDate.of(2026, 8, 31),
            category = "Food",
            navigationCategories = listOf("Food", "Travel"),
            transactionType = TransactionTypeFilter.EXPENSE,
            profileId = 9L,
            accountKey = "Bank_1234",
            tag = "work",
            minimumText = "10",
            maximumText = "200",
            originalCurrencies = setOf("usd"),
        )

        assertEquals(TransactionFilterDraft(), populated.reset())
    }

    @Test
    fun `validation preserves exact decimal values and normalizes currency`() {
        val filter = TransactionFilterDraft(
            minimumText = "10.25",
            maximumText = "900.75",
            originalCurrencies = setOf("usd", "INR"),
        ).validatedAmountFilter(unifiedMode = true)

        assertEquals(BigDecimal("10.25"), filter?.range?.minimum)
        assertEquals(BigDecimal("900.75"), filter?.range?.maximum)
        assertEquals(setOf("USD", "INR"), filter?.originalCurrencies)
    }

    @Test
    fun `native mode leaves currency refinement to existing selector`() {
        val filter = TransactionFilterDraft(
            originalCurrencies = setOf("USD"),
        ).validatedAmountFilter(unifiedMode = false)

        assertEquals(emptySet<String>(), filter?.originalCurrencies)
    }

    @Test
    fun `invalid amount draft cannot produce an applied filter`() {
        val draft = TransactionFilterDraft(minimumText = "20", maximumText = "10")

        assertEquals(AmountRangeError.MINIMUM_GREATER_THAN_MAXIMUM, draft.amountValidation.error)
        assertNull(draft.validatedAmountFilter(unifiedMode = true))
    }

    @Test
    fun `custom period requires a confirmed date range`() {
        val draft = TransactionFilterDraft(period = TimePeriod.CUSTOM)

        assertEquals(true, draft.validation.customDateRequired)
        assertEquals(false, draft.validation.isValid)
        assertNull(draft.validatedAmountFilter(unifiedMode = true))
    }

    @Test
    fun `active count treats each filter family once`() {
        val draft = TransactionFilterDraft(
            period = TimePeriod.LAST_MONTH,
            category = "Food",
            transactionType = TransactionTypeFilter.EXPENSE,
            profileId = 4L,
            accountKey = "Bank_1234",
            tag = "work",
            minimumText = "10",
            originalCurrencies = setOf("USD"),
        )

        assertEquals(7, draft.activeFilterCount)
    }

    @Test
    fun `unticking categories excludes them and ticking all back clears the filter`() {
        val available = listOf("Food", "Travel", "Bills")

        val withoutTravel = TransactionFilterDraft().toggleCategory("Travel", available)
        assertEquals(listOf("Food", "Bills"), withoutTravel.navigationCategories)
        assertNull(withoutTravel.category)
        assertEquals(setOf("Food", "Bills"), withoutTravel.selectedCategories(available))

        val onlyFood = withoutTravel.toggleCategory("Bills", available)
        assertEquals("Food", onlyFood.category)
        assertNull(onlyFood.navigationCategories)

        val none = onlyFood.toggleCategory("Food", available)
        assertEquals(TransactionFilterDraft(), none)

        val restored = withoutTravel.toggleCategory("Travel", available)
        assertEquals(TransactionFilterDraft(), restored)
    }

    @Test
    fun `editing a budget drill-down list makes it the user's own selection`() {
        val available = listOf("Food", "Travel", "Bills")
        val fromBudget = TransactionFilterDraft(
            navigationCategories = listOf("Food", "Travel"),
            categoriesFromBudget = true,
        )

        val edited = fromBudget.toggleCategory("Bills", available)

        assertEquals(TransactionFilterDraft(), edited)
        val narrowed = fromBudget.toggleCategory("Travel", available)
        assertEquals("Food", narrowed.category)
        assertEquals(false, narrowed.categoriesFromBudget)
    }
}
