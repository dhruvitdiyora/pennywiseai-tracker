package com.pennywiseai.tracker.data.repository

import androidx.room.withTransaction
import com.pennywiseai.shared.data.bootstrap.DefaultCategoryData
import com.pennywiseai.tracker.data.database.dao.CategoryDao
import com.pennywiseai.tracker.data.database.PennyWiseDatabase
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.hierarchical
import com.pennywiseai.tracker.data.database.entity.RuleEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val database: PennyWiseDatabase
) {
    
    fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories().map { it.hierarchical() }
    }
    
    fun getExpenseCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getExpenseCategories().map { it.hierarchical() }
    }
    
    fun getIncomeCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getIncomeCategories().map { it.hierarchical() }
    }

    // Visible-only variants for pickers — hidden categories are excluded (#736).
    fun getVisibleCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getVisibleCategories().map { it.hierarchical() }
    }

    fun getVisibleExpenseCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getVisibleExpenseCategories().map { it.hierarchical() }
    }

    fun getVisibleIncomeCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getVisibleIncomeCategories().map { it.hierarchical() }
    }

    suspend fun setCategoryHidden(categoryId: Long, hidden: Boolean) {
        categoryDao.setCategoryHidden(categoryId, hidden)
    }

    /**
     * Flips a category's hidden flag and returns the row as it now stands.
     * See [CategoryDao.toggleCategoryHidden] for why this isn't a read,
     * flip and write from the caller.
     */
    suspend fun toggleCategoryHidden(categoryId: Long): CategoryEntity? =
        categoryDao.toggleCategoryHiddenCascading(categoryId)

    suspend fun getCategoryById(categoryId: Long): CategoryEntity? {
        return categoryDao.getCategoryById(categoryId)
    }
    
    suspend fun getCategoryByName(categoryName: String): CategoryEntity? {
        return categoryDao.getCategoryByName(categoryName)
    }
    
    suspend fun createCategory(
        name: String,
        color: String,
        isIncome: Boolean = false,
        icon: String? = null,
        parentId: Long? = null
    ): Long {
        val category = CategoryEntity(
            name = name,
            color = color,
            icon = icon,
            parentId = parentId,
            isSystem = false,
            isIncome = isIncome,
            displayOrder = 999
        )
        return categoryDao.insertCategory(category)
    }
    
    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(
            category.copy(updatedAt = LocalDateTime.now())
        )
    }
    
    suspend fun deleteCategory(categoryId: Long): Boolean {
        // Legacy callers do not have a replacement-selection UI. Keeping this
        // overload safe means it can only delete an unreferenced custom row.
        return deleteOrReassignCategory(categoryId, null) is CategoryDeletionResult.Deleted
    }

    /** Returns all in-scope references before the confirmation UI is shown. */
    suspend fun getCategoryDeletionImpact(categoryId: Long): CategoryDeletionImpact? {
        val category = categoryDao.getCategoryById(categoryId) ?: return null
        return inspectCategory(category).impact
    }

    /**
     * Deletes a custom category, optionally retargeting all references to an
     * explicitly selected category of the same income/expense kind.
     */
    suspend fun deleteOrReassignCategory(
        categoryId: Long,
        targetCategoryId: Long?
    ): CategoryDeletionResult = database.withTransaction {
        deleteOrReassignCategoryInTransaction(categoryId, targetCategoryId)
    }

    /** Runs only while [deleteOrReassignCategory] holds the Room transaction. */
    private suspend fun deleteOrReassignCategoryInTransaction(
        categoryId: Long,
        targetCategoryId: Long?
    ): CategoryDeletionResult {
        val source = categoryDao.getCategoryById(categoryId)
            ?: return CategoryDeletionResult.Rejected("Category no longer exists.")
        if (source.isSystem) {
            return CategoryDeletionResult.Rejected("System categories cannot be deleted.")
        }
        if (targetCategoryId == categoryId) {
            return CategoryDeletionResult.Rejected("Choose a different replacement category.")
        }

        val inspection = inspectCategory(source)
        val impact = inspection.impact
        if (inspection.unreadableRuleCount > 0) {
            return CategoryDeletionResult.Rejected(
                "One or more rules contain invalid category JSON and cannot be changed safely.",
                impact
            )
        }

        val target = targetCategoryId?.let { categoryDao.getCategoryById(it) }
        if (targetCategoryId != null && target == null) {
            return CategoryDeletionResult.Rejected("Replacement category no longer exists.", impact)
        }
        if (target != null && target.isIncome != source.isIncome) {
            return CategoryDeletionResult.Rejected(
                "Replacement category must have the same income/expense type.",
                impact
            )
        }

        if (target == null && impact.hasReferences) {
            return CategoryDeletionResult.TargetRequired(impact)
        }

        val conflictingBudgets = if (target != null) {
            categoryDao.getActiveBudgetConflicts(source.name, target.name)
        } else {
            emptyList()
        }
        if (target != null && conflictingBudgets.isNotEmpty()) {
            return CategoryDeletionResult.TargetBudgetConflict(
                impact = impact,
                targetCategoryId = target.id,
                conflictingBudgetIds = conflictingBudgets,
                conflictingBudgetNames = categoryDao.getActiveBudgetConflictNames(source.name, target.name)
            )
        }

        val updatedRules = if (target != null) {
            inspection.rules.mapNotNull { rewriteRule(it, source.name, target.name) }
        } else {
            emptyList()
        }
        val applied = categoryDao.applyCategoryDeletion(
            categoryId = source.id,
            sourceCategoryName = source.name,
            targetCategoryName = target?.name,
            updatedRules = updatedRules,
            updatedAt = LocalDateTime.now()
        )
        if (applied) return CategoryDeletionResult.Deleted(impact)

        // The transaction rechecks the budget conflict and source row. Return
        // a useful conflict if another writer added the target bucket while
        // the confirmation dialog was open.
        val retryConflicts = if (target != null) {
            categoryDao.getActiveBudgetConflicts(source.name, target.name)
        } else {
            emptyList()
        }
        if (target != null && retryConflicts.isNotEmpty()) {
            return CategoryDeletionResult.TargetBudgetConflict(
                impact = impact,
                targetCategoryId = target.id,
                conflictingBudgetIds = retryConflicts,
                conflictingBudgetNames = categoryDao.getActiveBudgetConflictNames(source.name, target.name)
            )
        }
        return CategoryDeletionResult.Rejected("Category changed before deletion could complete.", impact)
    }

    /** Convenience overload for callers that already use the old method name. */
    suspend fun deleteCategory(
        categoryId: Long,
        targetCategoryId: Long?
    ): CategoryDeletionResult = deleteOrReassignCategory(categoryId, targetCategoryId)

    private data class CategoryInspection(
        val impact: CategoryDeletionImpact,
        val rules: List<RuleEntity>,
        val unreadableRuleCount: Int
    )

    private suspend fun inspectCategory(category: CategoryEntity): CategoryInspection {
        val rules = categoryDao.getAllRulesForCategoryDeletion()
        var conditionReferences = 0
        var actionReferences = 0
        var unreadableRules = 0
        for (rule in rules) {
            val conditions = inspectRuleJson(rule.conditions, category.name)
            val actions = inspectRuleJson(rule.actions, category.name)
            if (conditions.invalid || actions.invalid) unreadableRules++
            conditionReferences += conditions.references
            actionReferences += actions.references
        }

        val impact = CategoryDeletionImpact(
            categoryId = category.id,
            categoryName = category.name,
            isIncome = category.isIncome,
            transactionCount = categoryDao.countTransactionsForCategory(category.name),
            transactionSplitCount = categoryDao.countTransactionSplitsForCategory(category.name),
            subscriptionCount = categoryDao.countSubscriptionsForCategory(category.name),
            recurringTransactionCount = categoryDao.countRecurringTransactionsForCategory(category.name),
            merchantMappingCount = categoryDao.countMerchantMappingsForCategory(category.name),
            ruleConditionCount = conditionReferences,
            ruleActionCount = actionReferences,
            activeBudgetCategoryCount = categoryDao.countActiveBudgetCategoriesForCategory(category.name),
            activeBudgetIds = categoryDao.getActiveBudgetIdsForCategory(category.name),
            unreadableRuleCount = unreadableRules
        )
        return CategoryInspection(impact, rules, unreadableRules)
    }

    private data class RuleJsonInspection(
        val references: Int = 0,
        val invalid: Boolean = false
    )

    private data class RuleJsonRewrite(
        val json: String,
        val references: Int
    )

    private fun inspectRuleJson(raw: String, sourceCategoryName: String): RuleJsonInspection {
        // A rule we can't read only blocks deletion when it might mention this
        // category; otherwise one bad row would block every category delete.
        val invalid = if (mayMentionCategory(raw, sourceCategoryName)) {
            RuleJsonInspection(invalid = true)
        } else {
            RuleJsonInspection()
        }
        val parsed = runCatching { ruleJson.parseToJsonElement(raw) }.getOrNull()
            ?: return invalid
        val array = parsed as? JsonArray ?: return invalid
        var references = 0
        for (element in array) {
            val objectElement = element as? JsonObject ?: return invalid
            val field = objectElement["field"].asStringOrNull()
            if (!field.equals(CATEGORY_FIELD, ignoreCase = true)) continue
            val value = objectElement["value"].asStringOrNull()
                ?: return invalid
            val operator = objectElement["operator"].asStringOrNull()
            references += countCategoryValueReferences(value, sourceCategoryName, operator)
        }
        return RuleJsonInspection(references = references)
    }

    private fun mayMentionCategory(raw: String, sourceCategoryName: String): Boolean {
        val escaped = ruleJson.encodeToString(sourceCategoryName).removeSurrounding("\"")
        return raw.contains(sourceCategoryName, ignoreCase = true) ||
            raw.contains(escaped, ignoreCase = true)
    }

    private fun rewriteRule(
        rule: RuleEntity,
        sourceCategoryName: String,
        targetCategoryName: String
    ): RuleEntity? {
        val conditions = rewriteRuleJson(rule.conditions, sourceCategoryName, targetCategoryName)
            ?: return null
        val actions = rewriteRuleJson(rule.actions, sourceCategoryName, targetCategoryName)
            ?: return null
        if (conditions.references == 0 && actions.references == 0) return null
        return rule.copy(
            conditions = conditions.json,
            actions = actions.json,
            updatedAt = LocalDateTime.now()
        )
    }

    private fun rewriteRuleJson(
        raw: String,
        sourceCategoryName: String,
        targetCategoryName: String
    ): RuleJsonRewrite? {
        val parsed = runCatching { ruleJson.parseToJsonElement(raw) }.getOrNull() ?: return null
        val array = parsed as? JsonArray ?: return null
        var references = 0
        val rewritten = mutableListOf<JsonElement>()
        for (element in array) {
            val objectElement = element as? JsonObject ?: return null
            val field = objectElement["field"].asStringOrNull()
            if (!field.equals(CATEGORY_FIELD, ignoreCase = true)) {
                rewritten += element
                continue
            }
            val value = objectElement["value"].asStringOrNull() ?: return null
            val operator = objectElement["operator"].asStringOrNull()
            val replacement = replaceCategoryValue(value, sourceCategoryName, targetCategoryName, operator)
            references += replacement.references
            rewritten += if (replacement.value == value) {
                element
            } else {
                JsonObject(objectElement + ("value" to JsonPrimitive(replacement.value)))
            }
        }
        return RuleJsonRewrite(ruleJson.encodeToString(JsonArray(rewritten)), references)
    }

    private data class CategoryValueReplacement(val value: String, val references: Int)

    private fun replaceCategoryValue(
        value: String,
        sourceCategoryName: String,
        targetCategoryName: String,
        operator: String?
    ): CategoryValueReplacement {
        if (operator.equals("IN", ignoreCase = true) || operator.equals("NOT_IN", ignoreCase = true)) {
            var references = 0
            val replaced = value.split(",").joinToString(",") { token ->
                val trimmed = token.trim()
                if (!trimmed.equals(sourceCategoryName, ignoreCase = true)) return@joinToString token
                references++
                val prefixLength = token.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
                val suffixStart = token.indexOfLast { !it.isWhitespace() }.let { if (it < 0) token.length else it + 1 }
                token.substring(0, prefixLength) + targetCategoryName + token.substring(suffixStart)
            }
            return CategoryValueReplacement(replaced, references)
        }
        return if (value.equals(sourceCategoryName, ignoreCase = true)) {
            CategoryValueReplacement(targetCategoryName, 1)
        } else {
            CategoryValueReplacement(value, 0)
        }
    }

    private fun countCategoryValueReferences(value: String, sourceCategoryName: String, operator: String?): Int =
        if (operator.equals("IN", ignoreCase = true) || operator.equals("NOT_IN", ignoreCase = true)) {
            value.split(",").count { it.trim().equals(sourceCategoryName, ignoreCase = true) }
        } else {
            if (value.equals(sourceCategoryName, ignoreCase = true)) 1 else 0
        }

    private fun JsonElement?.asStringOrNull(): String? =
        (this as? JsonPrimitive)?.contentOrNull

    private companion object {
        const val CATEGORY_FIELD = "CATEGORY"
        val ruleJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            // Preserve the compact rule shape already emitted by RuleEngine;
            // rewriting a category must not add unrelated default keys.
            encodeDefaults = false
        }
    }
    
    suspend fun categoryExists(categoryName: String): Boolean {
        return categoryDao.categoryExists(categoryName)
    }
    
    suspend fun initializeDefaultCategories() {
        // Only initialize if no categories exist
        if (categoryDao.getCategoryCount() == 0) {
            val defaultCategories = DefaultCategoryData.ALL.map { seed ->
                CategoryEntity(
                    name = seed.name,
                    color = seed.colorHex,
                    isSystem = true,
                    isIncome = seed.isIncome,
                    displayOrder = DefaultCategoryData.ALL.indexOf(seed) + 1
                )
            }
            categoryDao.insertCategories(defaultCategories)
        }
    }
}
