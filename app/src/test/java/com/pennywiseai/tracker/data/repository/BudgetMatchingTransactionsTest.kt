package com.pennywiseai.tracker.data.repository

import com.pennywiseai.tracker.data.database.entity.BudgetCategoryEntity
import com.pennywiseai.tracker.data.database.entity.BudgetEntity
import com.pennywiseai.tracker.data.database.entity.BudgetImpactType
import com.pennywiseai.tracker.data.database.entity.BudgetPeriodType
import com.pennywiseai.tracker.data.database.entity.BudgetWithCategories
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionSplitEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.database.entity.TransactionWithSplits
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetMatchingTransactionsTest {
    @Test
    fun categoryBudgetIncludesWholeRowWhenOneSplitMatches() = runTest {
        val split = transaction(
            id = 1,
            type = TransactionType.EXPENSE,
            category = "Shopping",
            splits = listOf("Food" to "40", "Shopping" to "60"),
        )

        val result = matchingTransactionsForBudget(categoryGroup("Food"), listOf(split))

        assertEquals(listOf(1L), result.map { it.transaction.id })
    }

    @Test
    fun blankSplitMatchesOthersBucket() = runTest {
        val result = matchingTransactionsForBudget(
            categoryGroup("Others"),
            listOf(transaction(id = 2, category = "", splits = listOf("" to "25"))),
        )

        assertEquals(listOf(2L), result.map { it.transaction.id })
    }

    @Test
    fun typeBucketIncludesInvestmentAndExcludesCategoryExpense() = runTest {
        val group = categoryGroup("Investments", matchType = TransactionType.INVESTMENT.name)
        val rows = listOf(
            transaction(id = 3, type = TransactionType.INVESTMENT, category = "Stocks"),
            transaction(id = 4, type = TransactionType.EXPENSE, category = "Investments"),
        )

        val result = matchingTransactionsForBudget(group, rows)

        assertEquals(listOf(3L), result.map { it.transaction.id })
    }

    @Test
    fun categoryBudgetIncludesRefundAndExtraLimitRows() = runTest {
        val rows = listOf(
            transaction(
                id = 5,
                type = TransactionType.INCOME,
                category = "Income",
                budgetCategory = "Food",
                impact = BudgetImpactType.DEDUCT_SPENT,
            ),
            transaction(
                id = 6,
                type = TransactionType.INCOME,
                category = "Income",
                budgetCategory = "Food",
                impact = BudgetImpactType.ADD_TO_LIMIT,
            ),
        )

        val result = matchingTransactionsForBudget(categoryGroup("Food"), rows)

        assertEquals(listOf(5L, 6L), result.map { it.transaction.id })
    }

    @Test
    fun transferAndExcludedRowsNeverAppear() = runTest {
        val rows = listOf(
            transaction(id = 7, type = TransactionType.TRANSFER, category = "Food"),
            transaction(id = 8, category = "Food", excluded = true),
        )

        val result = matchingTransactionsForBudget(categoryGroup("Food"), rows)

        assertEquals(emptyList<Long>(), result.map { it.transaction.id })
    }

    @Test
    fun nativeTrackingAllMirrorsExistingCreditLoanAndRefundBehavior() = runTest {
        val rows = listOf(
            transaction(id = 9, type = TransactionType.CREDIT),
            transaction(id = 10, type = TransactionType.EXPENSE, loanId = 2),
            transaction(
                id = 11,
                type = TransactionType.INCOME,
                budgetCategory = "Food",
                impact = BudgetImpactType.DEDUCT_SPENT,
            ),
        )

        val result = matchingTransactionsForBudget(allExpensesGroup(), rows)

        assertEquals(listOf(9L, 10L, 11L), result.map { it.transaction.id })
    }

    @Test
    fun unifiedTrackingAllMirrorsExistingExpenseInvestmentOnlyBehavior() = runTest {
        val rows = listOf(
            transaction(id = 12, type = TransactionType.EXPENSE),
            transaction(id = 13, type = TransactionType.INVESTMENT),
            transaction(id = 14, type = TransactionType.CREDIT),
            transaction(id = 15, type = TransactionType.EXPENSE, loanId = 3),
        )

        val result = matchingTransactionsForBudget(
            group = allExpensesGroup(),
            transactions = rows,
            nativeTrackingAllSemantics = false,
        )

        assertEquals(listOf(12L, 13L), result.map { it.transaction.id })
    }

    @Test
    fun unrepresentableUnifiedAmountIsOmitted() = runTest {
        val row = transaction(id = 16, category = "Food", currency = "USD")

        val result = matchingTransactionsForBudget(
            group = categoryGroup("Food"),
            transactions = listOf(row),
            canRepresentAmount = { currency, _ -> currency != "USD" },
            nativeTrackingAllSemantics = false,
        )

        assertEquals(emptyList<Long>(), result.map { it.transaction.id })
    }

    private fun categoryGroup(
        name: String,
        matchType: String? = null,
    ) = BudgetWithCategories(
        budget = budget(),
        categories = listOf(
            BudgetCategoryEntity(
                id = 1,
                budgetId = 1,
                categoryName = name,
                budgetAmount = BigDecimal("500"),
                matchType = matchType,
            ),
        ),
    )

    private fun allExpensesGroup() = BudgetWithCategories(
        budget = budget(),
        categories = emptyList(),
    )

    private fun budget() = BudgetEntity(
        id = 1,
        name = "Monthly budget",
        limitAmount = BigDecimal("1000"),
        periodType = BudgetPeriodType.MONTHLY,
        startDate = LocalDate.of(2026, 9, 1),
        endDate = LocalDate.of(2026, 9, 30),
    )

    private fun transaction(
        id: Long,
        type: TransactionType = TransactionType.EXPENSE,
        category: String = "Food",
        splits: List<Pair<String, String>> = emptyList(),
        budgetCategory: String? = null,
        impact: BudgetImpactType? = null,
        excluded: Boolean = false,
        currency: String = "INR",
        loanId: Long? = null,
    ): TransactionWithSplits {
        val entity = TransactionEntity(
            id = id,
            amount = BigDecimal("100"),
            merchantName = "Merchant $id",
            category = category,
            transactionType = type,
            dateTime = LocalDateTime.of(2026, 9, 12, 10, 30),
            transactionHash = "hash-$id",
            budgetCategory = budgetCategory,
            budgetImpactType = impact,
            excludedFromAnalytics = excluded,
            currency = currency,
            loanId = loanId,
        )
        return TransactionWithSplits(
            transaction = entity,
            splits = splits.mapIndexed { index, (splitCategory, amount) ->
                TransactionSplitEntity(
                    id = index.toLong() + 1,
                    transactionId = id,
                    category = splitCategory,
                    amount = BigDecimal(amount),
                )
            },
        )
    }
}
