package com.pennywiseai.tracker.data.repository

import com.pennywiseai.shared.data.bootstrap.DefaultCategoryData
import com.pennywiseai.tracker.data.database.dao.CategoryDao
import com.pennywiseai.tracker.data.database.dao.SubcategoryDao
import com.pennywiseai.tracker.data.database.entity.SubcategoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubcategoryRepository @Inject constructor(
    private val subcategoryDao: SubcategoryDao,
    private val categoryDao: CategoryDao
) {

    fun getSubcategoriesByCategoryId(categoryId: Long): Flow<List<SubcategoryEntity>> =
        subcategoryDao.getSubcategoriesByCategoryId(categoryId)

    fun getAllSubcategories(): Flow<List<SubcategoryEntity>> =
        subcategoryDao.getAllSubcategories()

    suspend fun getSubcategoryById(id: Long): SubcategoryEntity? =
        subcategoryDao.getSubcategoryById(id)

    /**
     * @return the new row's id, or **null** when [name] is already used under
     *   [categoryId].
     *
     * Null rather than an exception so the caller can say *"'Coffee' already
     * exists under Food & Dining"*. Scoped to the parent deliberately — the same
     * name under a different category is allowed, and is the reason the unique
     * index is composite.
     */
    suspend fun createSubcategory(
        categoryId: Long,
        name: String,
        iconName: String = "",
        color: String = "#757575"
    ): Long? {
        if (subcategoryDao.getByCategoryAndName(categoryId, name) != null) return null

        val id = subcategoryDao.insertSubcategory(
            SubcategoryEntity(
                categoryId = categoryId,
                name = name,
                iconName = iconName,
                color = color,
                isSystem = false
            )
        )
        // The DAO inserts with IGNORE, which returns -1 on conflict rather than
        // throwing. Treat that as the same refusal the pre-check produces.
        return id.takeIf { it != -1L }
    }

    /**
     * @return false when the new name is already held by a *sibling*. The row's
     *   own name is fine — otherwise no edit that keeps the name could ever save.
     */
    suspend fun updateSubcategory(subcategory: SubcategoryEntity): Boolean {
        val clash = subcategoryDao.getByCategoryAndName(subcategory.categoryId, subcategory.name)
        if (clash != null && clash.id != subcategory.id) return false
        subcategoryDao.updateSubcategory(subcategory.copy(updatedAt = LocalDateTime.now()))
        return true
    }

    suspend fun deleteSubcategory(id: Long) = subcategoryDao.deleteSubcategoryById(id)

    /**
     * Restores a system subcategory's original name, icon and colour.
     *
     * @return false when the row is missing or has no stored defaults — i.e. a
     *   user-created subcategory, which never had an original.
     */
    suspend fun resetSubcategoryToDefault(id: Long): Boolean {
        val subcategory = subcategoryDao.getSubcategoryById(id) ?: return false
        val defaultName = subcategory.defaultName ?: return false

        return updateSubcategory(
            subcategory.copy(
                name = defaultName,
                iconName = subcategory.defaultIconName ?: subcategory.iconName,
                color = subcategory.defaultColor ?: subcategory.color
            )
        )
    }

    /**
     * Seeds the built-in subcategories, once.
     *
     * Parents are resolved by **name**, because the seed table cannot know the
     * ids a given install assigned. A seed whose parent is absent is **skipped
     * silently** — the user may have deleted or renamed that category, and
     * recreating it here would resurrect something they removed on purpose.
     */
    suspend fun initializeDefaultSubcategories() {
        if (subcategoryDao.getSubcategoryCount() > 0) return

        val rows = DefaultCategoryData.SUBCATEGORIES.mapNotNull { seed ->
            val parent = categoryDao.getCategoryByName(seed.parentName) ?: return@mapNotNull null
            SubcategoryEntity(
                categoryId = parent.id,
                name = seed.name,
                iconName = seed.iconName,
                // Inherit the parent's colour so a subcategory reads as part of it
                // rather than as an unrelated third thing.
                color = parent.color,
                isSystem = true,
                defaultName = seed.name,
                defaultIconName = seed.iconName,
                defaultColor = parent.color
            )
        }
        if (rows.isNotEmpty()) subcategoryDao.insertSubcategories(rows)
    }
}
