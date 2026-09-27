package com.pennywiseai.tracker.data.database.dao

import androidx.room.*
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.RuleEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories ORDER BY display_order ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE is_income = 0 ORDER BY display_order ASC, name ASC")
    fun getExpenseCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE is_income = 1 ORDER BY display_order ASC, name ASC")
    fun getIncomeCategories(): Flow<List<CategoryEntity>>

    // Visible-only variants for the category PICKERS — hidden categories are kept
    // in the DB (so existing transactions keep their category and still show in
    // analytics) but excluded from selection (#736).
    @Query("SELECT * FROM categories WHERE is_hidden = 0 ORDER BY display_order ASC, name ASC")
    fun getVisibleCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE is_income = 0 AND is_hidden = 0 ORDER BY display_order ASC, name ASC")
    fun getVisibleExpenseCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE is_income = 1 AND is_hidden = 0 ORDER BY display_order ASC, name ASC")
    fun getVisibleIncomeCategories(): Flow<List<CategoryEntity>>

    @Query("UPDATE categories SET is_hidden = :hidden WHERE id = :categoryId")
    suspend fun setCategoryHidden(categoryId: Long, hidden: Boolean)

    /**
     * Flips the flag in the database rather than writing a value the caller
     * worked out beforehand.
     *
     * The UI can only ever hold a snapshot of the row: between a write landing
     * and the Room Flow reaching Compose, a second tap would compute its "next"
     * value from the pre-write state and write the same thing again, so a
     * quick hide-then-show left the category hidden. Flipping in SQL has no such
     * window.
     */
    @Query("UPDATE categories SET is_hidden = NOT is_hidden WHERE id = :categoryId")
    suspend fun toggleCategoryHidden(categoryId: Long)

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): CategoryEntity?

    @Query("SELECT * FROM categories ORDER BY display_order ASC, name ASC")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Query("UPDATE categories SET is_hidden = :hidden WHERE parent_id = :parentId")
    suspend fun setChildrenHidden(parentId: Long, hidden: Boolean)

    /**
     * Flips a category's hidden flag and cascades in one transaction (#374):
     * a hidden parent hides its children; un-hiding a child restores its
     * parent — so the hierarchy can never be committed half-way.
     */
    @Transaction
    suspend fun toggleCategoryHiddenCascading(categoryId: Long): CategoryEntity? {
        toggleCategoryHidden(categoryId)
        val updated = getCategoryById(categoryId) ?: return null
        setChildrenHidden(categoryId, updated.isHidden)
        if (!updated.isHidden) updated.parentId?.let { setCategoryHidden(it, false) }
        return updated
    }

    /** Deleting a parent promotes its children to top level (#374). */
    @Query("UPDATE categories SET parent_id = NULL WHERE parent_id = :parentId")
    suspend fun detachChildren(parentId: Long)
    
    @Query("SELECT * FROM categories WHERE name = :categoryName LIMIT 1")
    suspend fun getCategoryByName(categoryName: String): CategoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)
    
    @Update
    suspend fun updateCategory(category: CategoryEntity)
    
    // Category deletion/reassignment impact queries. These deliberately do
    // not filter transactions by is_deleted: trash is still user data and
    // must be retargeted before the category can be removed.
    @Query("SELECT COUNT(*) FROM transactions WHERE category = :categoryName")
    suspend fun countTransactionsForCategory(categoryName: String): Int

    @Query("SELECT COUNT(*) FROM transaction_splits WHERE category = :categoryName")
    suspend fun countTransactionSplitsForCategory(categoryName: String): Int

    @Query("SELECT COUNT(*) FROM subscriptions WHERE category = :categoryName")
    suspend fun countSubscriptionsForCategory(categoryName: String): Int

    @Query("SELECT COUNT(*) FROM recurring_transactions WHERE category = :categoryName")
    suspend fun countRecurringTransactionsForCategory(categoryName: String): Int

    @Query("SELECT COUNT(*) FROM merchant_mappings WHERE category = :categoryName")
    suspend fun countMerchantMappingsForCategory(categoryName: String): Int

    @Query("SELECT COUNT(*) FROM budget_categories bc INNER JOIN budgets b ON b.id = bc.budget_id WHERE bc.category_name = :categoryName AND b.is_active = 1")
    suspend fun countActiveBudgetCategoriesForCategory(categoryName: String): Int

    @Query("SELECT DISTINCT bc.budget_id FROM budget_categories bc INNER JOIN budgets b ON b.id = bc.budget_id WHERE bc.category_name = :categoryName AND b.is_active = 1 ORDER BY bc.budget_id")
    suspend fun getActiveBudgetIdsForCategory(categoryName: String): List<Long>

    @Query("""
        SELECT DISTINCT source.budget_id
        FROM budget_categories source
        INNER JOIN budgets b ON b.id = source.budget_id AND b.is_active = 1
        INNER JOIN budget_categories target
            ON target.budget_id = source.budget_id
           AND target.category_name = :targetCategoryName
        WHERE source.category_name = :sourceCategoryName
        ORDER BY source.budget_id
    """)
    suspend fun getActiveBudgetConflicts(
        sourceCategoryName: String,
        targetCategoryName: String
    ): List<Long>

    @Query("""
        SELECT b.name
        FROM budgets b
        INNER JOIN budget_categories source ON source.budget_id = b.id
        INNER JOIN budget_categories target
            ON target.budget_id = source.budget_id
           AND target.category_name = :targetCategoryName
        WHERE source.category_name = :sourceCategoryName
          AND b.is_active = 1
        ORDER BY b.id
    """)
    suspend fun getActiveBudgetConflictNames(
        sourceCategoryName: String,
        targetCategoryName: String
    ): List<String>

    @Query("SELECT * FROM transaction_rules")
    suspend fun getAllRulesForCategoryDeletion(): List<RuleEntity>

    @Query("UPDATE transactions SET category = :targetCategoryName, updated_at = :updatedAt WHERE category = :sourceCategoryName")
    suspend fun reassignTransactionCategories(
        sourceCategoryName: String,
        targetCategoryName: String,
        updatedAt: LocalDateTime
    ): Int

    @Query("UPDATE transaction_splits SET category = :targetCategoryName WHERE category = :sourceCategoryName")
    suspend fun reassignTransactionSplitCategories(
        sourceCategoryName: String,
        targetCategoryName: String
    ): Int

    @Query("UPDATE subscriptions SET category = :targetCategoryName, updated_at = :updatedAt WHERE category = :sourceCategoryName")
    suspend fun reassignSubscriptionCategories(
        sourceCategoryName: String,
        targetCategoryName: String,
        updatedAt: LocalDateTime
    ): Int

    @Query("UPDATE recurring_transactions SET category = :targetCategoryName, updated_at = :updatedAt WHERE category = :sourceCategoryName")
    suspend fun reassignRecurringTransactionCategories(
        sourceCategoryName: String,
        targetCategoryName: String,
        updatedAt: LocalDateTime
    ): Int

    @Query("UPDATE merchant_mappings SET category = :targetCategoryName, updated_at = :updatedAt WHERE category = :sourceCategoryName")
    suspend fun reassignMerchantMappingCategories(
        sourceCategoryName: String,
        targetCategoryName: String,
        updatedAt: LocalDateTime
    ): Int

    @Query("UPDATE budget_categories SET category_name = :targetCategoryName WHERE category_name = :sourceCategoryName AND budget_id IN (SELECT id FROM budgets WHERE is_active = 1)")
    suspend fun reassignActiveBudgetCategories(
        sourceCategoryName: String,
        targetCategoryName: String
    ): Int

    @Update
    suspend fun updateRulesAfterCategoryReassignment(rules: List<RuleEntity>)

    @Query("DELETE FROM categories WHERE id = :categoryId AND is_system = 0")
    suspend fun deleteCustomCategoryForReassignment(categoryId: Long): Int

    /**
     * Applies all name retargeting and removes the source category atomically.
     * The conflict check lives inside the transaction as well as in the
     * repository preflight, so a concurrent budget edit cannot silently merge
     * two budget rows.
     */
    @Transaction
    suspend fun applyCategoryDeletion(
        categoryId: Long,
        sourceCategoryName: String,
        targetCategoryName: String?,
        updatedRules: List<RuleEntity>,
        updatedAt: LocalDateTime
    ): Boolean {
        val source = getCategoryById(categoryId)
        if (source == null || source.isSystem || source.name != sourceCategoryName) return false

        if (targetCategoryName != null &&
            getActiveBudgetConflicts(sourceCategoryName, targetCategoryName).isNotEmpty()
        ) {
            return false
        }

        if (targetCategoryName != null) {
            reassignTransactionCategories(sourceCategoryName, targetCategoryName, updatedAt)
            reassignTransactionSplitCategories(sourceCategoryName, targetCategoryName)
            reassignSubscriptionCategories(sourceCategoryName, targetCategoryName, updatedAt)
            reassignRecurringTransactionCategories(sourceCategoryName, targetCategoryName, updatedAt)
            reassignMerchantMappingCategories(sourceCategoryName, targetCategoryName, updatedAt)
            reassignActiveBudgetCategories(sourceCategoryName, targetCategoryName)
            if (updatedRules.isNotEmpty()) updateRulesAfterCategoryReassignment(updatedRules)
        }

        // Deleting a parent promotes its sub-categories to top level (#374).
        detachChildren(categoryId)
        return deleteCustomCategoryForReassignment(categoryId) == 1
    }
    
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
    
    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE name = :categoryName)")
    suspend fun categoryExists(categoryName: String): Boolean
    
    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}
