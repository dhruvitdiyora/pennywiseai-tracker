package com.pennywiseai.tracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywiseai.tracker.data.database.PennyWiseDatabase
import com.pennywiseai.tracker.data.database.entity.BudgetCategoryEntity
import com.pennywiseai.tracker.data.database.entity.BudgetCategoryMonthSnapshotEntity
import com.pennywiseai.tracker.data.database.entity.BudgetEntity
import com.pennywiseai.tracker.data.database.entity.BudgetPeriodType
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.MerchantMappingEntity
import com.pennywiseai.tracker.data.database.entity.RecurringTransactionEntity
import com.pennywiseai.tracker.data.database.entity.RuleEntity
import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionSplitEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CategoryRepositoryDeletionTest {
    private lateinit var database: PennyWiseDatabase
    private lateinit var repository: CategoryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PennyWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = CategoryRepository(database.categoryDao(), database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun referencedRowsRequireExplicitSameTypeTargetAndAreRetargetedStructurally() = runBlocking {
        val sourceId = repository.createCategory("Side Work", "#111111", isIncome = true)
        val targetId = repository.createCategory("Salary", "#222222", isIncome = true)
        val now = LocalDateTime.of(2026, 9, 7, 12, 0)
        val transactionId = database.transactionDao().insertTransaction(
            TransactionEntity(
                amount = BigDecimal("10"),
                merchantName = "Example merchant",
                category = "Side Work",
                transactionType = TransactionType.INCOME,
                dateTime = now,
                transactionHash = "category-delete-test",
                isDeleted = true,
            )
        )
        database.transactionSplitDao().insertSplit(
            TransactionSplitEntity(
                transactionId = transactionId,
                category = "Side Work",
                amount = BigDecimal("10"),
            )
        )
        database.subscriptionDao().insertSubscription(
            SubscriptionEntity(
                merchantName = "Example subscription",
                amount = BigDecimal("10"),
                nextPaymentDate = LocalDate.of(2026, 10, 1),
                category = "Side Work",
            )
        )
        database.recurringTransactionDao().insert(
            RecurringTransactionEntity(
                merchantName = "Example recurring",
                amount = BigDecimal("10"),
                category = "Side Work",
                nextDueDate = LocalDate.of(2026, 10, 1),
            )
        )
        database.merchantMappingDao().insertMapping(
            MerchantMappingEntity("Example mapping", "Side Work")
        )
        database.ruleDao().insertRule(
            RuleEntity(
                id = "category-delete-rule",
                name = "Category rule",
                description = null,
                priority = 1,
                conditions = "[{\"field\":\"CATEGORY\",\"operator\":\"IN\",\"value\":\"Side Work, Other\",\"logicalOperator\":\"AND\"}]",
                actions = "[{\"field\":\"CATEGORY\",\"actionType\":\"SET\",\"value\":\"Side Work\"}]",
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )
        )

        val impact = repository.getCategoryDeletionImpact(sourceId)
        assertNotNull(impact)
        assertEquals(1, impact?.transactionCount)
        assertEquals(1, impact?.transactionSplitCount)
        assertEquals(1, impact?.subscriptionCount)
        assertEquals(1, impact?.recurringTransactionCount)
        assertEquals(1, impact?.merchantMappingCount)
        assertEquals(1, impact?.ruleConditionCount)
        assertEquals(1, impact?.ruleActionCount)

        assertTrue(repository.deleteOrReassignCategory(sourceId, null) is CategoryDeletionResult.TargetRequired)
        assertTrue(repository.deleteOrReassignCategory(sourceId, targetId) is CategoryDeletionResult.Deleted)

        assertEquals("Salary", database.transactionDao().getTransactionById(transactionId)?.category)
        assertEquals("Salary", database.transactionSplitDao().getSplitsForTransactionSync(transactionId).single().category)
        assertEquals("Salary", database.subscriptionDao().getSubscriptionById(1)?.category)
        assertEquals("Salary", database.recurringTransactionDao().getById(1)?.category)
        assertEquals("Salary", database.merchantMappingDao().getAllMappingsList().single().category)
        val rule = database.ruleDao().getRuleById("category-delete-rule")
        assertNotNull(rule)
        assertTrue(rule!!.conditions.contains("Salary"))
        assertFalse(rule.conditions.contains("Side Work"))
        assertEquals(null, database.categoryDao().getCategoryById(sourceId))
    }

    @Test
    fun existingTargetInSameActiveBudgetBlocksWithoutMergingRows() = runBlocking {
        val sourceId = repository.createCategory("Old bucket", "#111111")
        val targetId = repository.createCategory("New bucket", "#222222")
        val budgetId = database.budgetDao().insertBudget(
            BudgetEntity(
                name = "Example budget",
                limitAmount = BigDecimal("100"),
                periodType = BudgetPeriodType.MONTHLY,
                startDate = LocalDate.of(2026, 9, 1),
                endDate = LocalDate.of(2026, 9, 30),
            )
        )
        database.budgetDao().insertBudgetCategory(
            BudgetCategoryEntity(budgetId = budgetId, categoryName = "Old bucket", budgetAmount = BigDecimal("40"))
        )
        database.budgetDao().insertBudgetCategory(
            BudgetCategoryEntity(budgetId = budgetId, categoryName = "New bucket", budgetAmount = BigDecimal("60"))
        )

        val result = repository.deleteOrReassignCategory(sourceId, targetId)
        assertTrue(result is CategoryDeletionResult.TargetBudgetConflict)
        assertEquals(listOf("Example budget"), (result as CategoryDeletionResult.TargetBudgetConflict).conflictingBudgetNames)
        assertNotNull(database.categoryDao().getCategoryById(sourceId))
        assertEquals(2, database.budgetDao().getCategoriesForBudgetList(budgetId).size)
    }

    @Test
    fun crossTypeTargetIsRejectedWithoutChangingReferences() = runBlocking {
        val sourceId = repository.createCategory("Expense source", "#111111", isIncome = false)
        val incomeTargetId = repository.createCategory("Income target", "#222222", isIncome = true)
        val transactionId = database.transactionDao().insertTransaction(
            TransactionEntity(
                amount = BigDecimal("25"),
                merchantName = "Example merchant",
                category = "Expense source",
                transactionType = TransactionType.EXPENSE,
                dateTime = LocalDateTime.of(2026, 9, 7, 12, 0),
                transactionHash = "category-delete-cross-type",
            )
        )

        val result = repository.deleteOrReassignCategory(sourceId, incomeTargetId)

        assertTrue(result is CategoryDeletionResult.Rejected)
        assertNotNull(database.categoryDao().getCategoryById(sourceId))
        assertEquals("Expense source", database.transactionDao().getTransactionById(transactionId)?.category)
    }

    @Test
    fun unreferencedCustomDeletesWithoutTargetButSystemCategoryDoesNot() = runBlocking {
        val customId = repository.createCategory("Unused custom", "#111111")
        val systemId = database.categoryDao().insertCategory(
            CategoryEntity(
                name = "Protected system",
                color = "#222222",
                isSystem = true,
            )
        )

        assertTrue(repository.deleteOrReassignCategory(customId, null) is CategoryDeletionResult.Deleted)
        assertEquals(null, database.categoryDao().getCategoryById(customId))
        assertTrue(repository.deleteOrReassignCategory(systemId, null) is CategoryDeletionResult.Rejected)
        assertNotNull(database.categoryDao().getCategoryById(systemId))
    }

    @Test
    fun malformedRuleOnlyBlocksDeletionOfCategoriesItMayMention() = runBlocking {
        val unrelatedId = repository.createCategory("Unused custom", "#111111")
        val mentionedId = repository.createCategory("Side Work", "#222222")
        val now = LocalDateTime.of(2026, 9, 7, 12, 0)
        database.ruleDao().insertRule(
            RuleEntity(
                id = "malformed-rule",
                name = "Malformed rule",
                description = null,
                priority = 1,
                conditions = "{\"field\":\"CATEGORY\",\"value\":\"Side Work\"",
                actions = "[]",
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )
        )

        assertTrue(repository.deleteOrReassignCategory(unrelatedId, null) is CategoryDeletionResult.Deleted)
        assertTrue(repository.deleteOrReassignCategory(mentionedId, null) is CategoryDeletionResult.Rejected)
        assertNotNull(database.categoryDao().getCategoryById(mentionedId))
    }

    @Test
    fun deletingParentPromotesSubCategoriesToTopLevel() = runBlocking {
        val parentId = repository.createCategory("Parent bucket", "#111111")
        val childId = repository.createCategory("Child bucket", "#222222", parentId = parentId)

        assertTrue(repository.deleteOrReassignCategory(parentId, null) is CategoryDeletionResult.Deleted)
        assertEquals(null, database.categoryDao().getCategoryById(childId)?.parentId)
        assertNotNull(database.categoryDao().getCategoryById(childId))
    }

    @Test
    fun activeBudgetRowMovesButHistoricalSnapshotStaysUnchanged() = runBlocking {
        val sourceId = repository.createCategory("Retired bucket", "#111111")
        val targetId = repository.createCategory("Current bucket", "#222222")
        val budgetId = database.budgetDao().insertBudget(
            BudgetEntity(
                name = "History budget",
                limitAmount = BigDecimal("100"),
                periodType = BudgetPeriodType.MONTHLY,
                startDate = LocalDate.of(2026, 9, 1),
                endDate = LocalDate.of(2026, 9, 30),
            )
        )
        database.budgetDao().insertBudgetCategory(
            BudgetCategoryEntity(budgetId = budgetId, categoryName = "Retired bucket", budgetAmount = BigDecimal("40"))
        )
        database.budgetSnapshotDao().insertCategorySnapshots(
            listOf(
                BudgetCategoryMonthSnapshotEntity(
                    budgetId = budgetId,
                    year = 2026,
                    month = 8,
                    categoryName = "Retired bucket",
                    budgetAmount = BigDecimal("35"),
                )
            )
        )

        assertTrue(repository.deleteOrReassignCategory(sourceId, targetId) is CategoryDeletionResult.Deleted)
        assertEquals("Current bucket", database.budgetDao().getCategoriesForBudgetList(budgetId).single().categoryName)
        assertEquals("Retired bucket", database.budgetSnapshotDao().getCategorySnapshots(2026, 8).single().categoryName)
    }
}
