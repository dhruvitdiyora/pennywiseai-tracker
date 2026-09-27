package com.pennywiseai.tracker.presentation.budgetgroups

import com.pennywiseai.tracker.data.database.entity.BudgetPeriodType
import com.pennywiseai.tracker.data.repository.BudgetWindow
import com.pennywiseai.tracker.data.repository.PastWindowSpending
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetHistoryBudgetMathTest {
    @Test
    fun weeklyHistoryUsesEffectiveCategoryFundedLimitForEveryWindow() {
        assertEquals(
            BigDecimal("50000"),
            historyBudgetAmount(
                periodType = BudgetPeriodType.WEEKLY,
                windowBudgetAmount = BigDecimal("10000"),
                windowCount = 5,
            ),
        )
    }

    @Test
    fun monthlyHistoryKeepsSingleEffectiveLimit() {
        assertEquals(
            BigDecimal("10000"),
            historyBudgetAmount(
                periodType = BudgetPeriodType.MONTHLY,
                windowBudgetAmount = BigDecimal("10000"),
                windowCount = 1,
            ),
        )
    }

    @Test
    fun futureWindowsAreExcludedFromTrend() {
        val asOf = LocalDate.of(2026, 9, 2)
        val past = window(LocalDate.of(2026, 8, 24))
        val current = window(LocalDate.of(2026, 9, 1))
        val future = window(LocalDate.of(2026, 9, 8))

        assertEquals(
            listOf(past, current),
            eligibleTrendWindows(listOf(future, current, past), asOf),
        )
    }

    private fun window(start: LocalDate) = PastWindowSpending(
        window = BudgetWindow(start, start.plusDays(6), 7),
        spent = BigDecimal.ZERO,
    )
}
