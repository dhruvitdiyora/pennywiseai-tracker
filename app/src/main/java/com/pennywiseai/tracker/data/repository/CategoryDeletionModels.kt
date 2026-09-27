package com.pennywiseai.tracker.data.repository

/**
 * Counts the rows that would be affected by deleting a category. Counts are
 * intentionally name-based because category references in the legacy schema
 * are stored as category names rather than foreign keys.
 */
data class CategoryDeletionImpact(
    val categoryId: Long,
    val categoryName: String,
    val isIncome: Boolean,
    val transactionCount: Int = 0,
    val transactionSplitCount: Int = 0,
    val subscriptionCount: Int = 0,
    val recurringTransactionCount: Int = 0,
    val merchantMappingCount: Int = 0,
    val ruleConditionCount: Int = 0,
    val ruleActionCount: Int = 0,
    val activeBudgetCategoryCount: Int = 0,
    val activeBudgetIds: List<Long> = emptyList(),
    /** Rules whose JSON cannot be inspected safely and therefore block writes. */
    val unreadableRuleCount: Int = 0,
) {
    val totalReferences: Int
        get() = transactionCount + transactionSplitCount + subscriptionCount +
            recurringTransactionCount + merchantMappingCount + ruleConditionCount +
            ruleActionCount + activeBudgetCategoryCount

    val hasReferences: Boolean
        get() = totalReferences > 0
}

/** Result of the safe category delete/reassignment operation. */
sealed interface CategoryDeletionResult {
    data class Deleted(val impact: CategoryDeletionImpact) : CategoryDeletionResult

    data class TargetRequired(
        val impact: CategoryDeletionImpact,
        val reason: String = "Category references exist; choose a replacement category."
    ) : CategoryDeletionResult

    data class TargetBudgetConflict(
        val impact: CategoryDeletionImpact,
        val targetCategoryId: Long,
        val conflictingBudgetIds: List<Long>,
        val conflictingBudgetNames: List<String> = emptyList(),
        val reason: String = "The replacement category already exists in one or more active budgets."
    ) : CategoryDeletionResult

    data class Rejected(
        val reason: String,
        val impact: CategoryDeletionImpact? = null
    ) : CategoryDeletionResult
}
