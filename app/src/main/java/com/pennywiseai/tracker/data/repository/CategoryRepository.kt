package com.pennywiseai.tracker.data.repository

import com.pennywiseai.shared.data.bootstrap.DefaultCategoryData
import com.pennywiseai.tracker.data.database.dao.CategoryDao
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    
    fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories()
    }
    
    fun getExpenseCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getExpenseCategories()
    }
    
    fun getIncomeCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getIncomeCategories()
    }
    
    suspend fun getCategoryById(categoryId: Long): CategoryEntity? {
        return categoryDao.getCategoryById(categoryId)
    }
    
    suspend fun getCategoryByName(categoryName: String): CategoryEntity? {
        return categoryDao.getCategoryByName(categoryName)
    }
    
    /**
     * @return the new row's id, or **null** when the name is already taken.
     *
     * Inserts the row fully populated in one go. The previous shape returned a
     * raw `Long` and callers ignored it — but the DAO inserts with `IGNORE`, so a
     * name collision returns `-1` rather than throwing, and a caller that then
     * looked the name up and "finished" the row would happily overwrite the
     * *existing* category's icon and description.
     */
    suspend fun createCategory(
        name: String,
        color: String,
        isIncome: Boolean = false,
        iconName: String = "",
        description: String = ""
    ): Long? {
        if (findByNameIgnoringCase(name) != null) return null

        val id = categoryDao.insertCategory(
            CategoryEntity(
                name = name,
                color = color,
                isSystem = false,
                isIncome = isIncome,
                displayOrder = 999,
                iconName = iconName,
                description = description
            )
        )
        return id.takeIf { it != -1L }
    }

    /**
     * Name lookup that ignores case.
     *
     * The unique index on `categories.name` uses SQLite's default (case-sensitive)
     * collation, so the database alone would happily hold both "Food" and "food" —
     * two categories a user cannot tell apart in a list. Guarding here costs one
     * in-memory scan of a table that holds ~20 rows; changing the index to NOCASE
     * would need its own migration and is worth doing separately.
     */
    private suspend fun findByNameIgnoringCase(name: String): CategoryEntity? =
        categoryDao.getAllCategories().first().firstOrNull { it.name.equals(name, ignoreCase = true) }
    
    /**
     * @return false when [category]'s name is already taken by a different row.
     *
     * `categories` has a unique index on `name`, and until now only [createCategory]
     * checked it — an update that collided threw `SQLiteConstraintException` out of
     * the repository. That was unreachable while system categories could not be
     * renamed at all; doc 14 makes them renameable, which turns it into a real crash.
     * Returning a boolean lets the caller show a message instead.
     */
    suspend fun updateCategory(category: CategoryEntity): Boolean {
        val clash = findByNameIgnoringCase(category.name)
        if (clash != null && clash.id != category.id) return false
        categoryDao.updateCategory(
            category.copy(updatedAt = LocalDateTime.now())
        )
        return true
    }

    /**
     * Restores a system category's original name, colour, icon and description.
     *
     * @return false when the category does not exist, or has no stored defaults —
     *   a user-created category, which never had an original to go back to.
     */
    suspend fun resetCategoryToDefault(categoryId: Long): Boolean {
        val category = categoryDao.getCategoryById(categoryId) ?: return false
        val defaultName = category.defaultName ?: return false

        val restored = category.copy(
            name = defaultName,
            color = category.defaultColor ?: category.color,
            iconName = category.defaultIconName ?: category.iconName,
            description = category.defaultDescription ?: category.description,
            updatedAt = LocalDateTime.now()
        )
        // Goes through the same uniqueness guard: the original name may have been
        // taken by another category while this one was renamed away from it.
        return updateCategory(restored)
    }

    /**
     * Fills in the columns added by schema 58→59 for installs that predate them.
     *
     * Two populations need this: anyone upgrading from 58, and anyone whose
     * categories were inserted by `Migration7To8` (which cannot write these columns
     * — they do not exist at version 8). A fresh install is already complete, seeded
     * by `DatabaseModule.seedCategories`, and this is a no-op for it.
     *
     * Matches on `name`, the only stable key a pre-59 row has, and writes **only**
     * fields that are still empty or null. So it is idempotent, and a category the
     * user renamed simply will not match — which is the correct outcome, not a miss.
     */
    suspend fun backfillSystemCategoryDefaults() {
        val seedsByName = DefaultCategoryData.ALL.associateBy { it.name }
        for (category in categoryDao.getSystemCategories()) {
            val seed = seedsByName[category.name] ?: continue

            val patched = category.copy(
                iconName = category.iconName.ifEmpty { seed.iconName },
                defaultName = category.defaultName ?: seed.name,
                defaultColor = category.defaultColor ?: seed.colorHex,
                defaultIconName = category.defaultIconName ?: seed.iconName,
                defaultDescription = category.defaultDescription ?: ""
            )
            // Skip the write entirely when nothing changed, so a warm start does not
            // churn `updated_at` on 18 rows every launch.
            if (patched != category) {
                categoryDao.updateCategory(patched)
            }
        }
    }
    
    suspend fun deleteCategory(categoryId: Long): Boolean {
        // Only delete non-system categories
        val category = categoryDao.getCategoryById(categoryId)
        if (category != null && !category.isSystem) {
            categoryDao.deleteCategory(categoryId)
            return true
        }
        return false
    }
    
    /** Case-insensitive — see [findByNameIgnoringCase]. */
    suspend fun categoryExists(categoryName: String): Boolean {
        return findByNameIgnoringCase(categoryName) != null
    }
    
}