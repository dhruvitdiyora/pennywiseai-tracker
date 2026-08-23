package com.pennywiseai.tracker.data.database.dao

import androidx.room.*
import com.pennywiseai.tracker.data.database.entity.SubcategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubcategoryDao {

    @Query("SELECT * FROM subcategories WHERE category_id = :categoryId ORDER BY name ASC")
    fun getSubcategoriesByCategoryId(categoryId: Long): Flow<List<SubcategoryEntity>>

    @Query("SELECT * FROM subcategories ORDER BY category_id ASC, name ASC")
    fun getAllSubcategories(): Flow<List<SubcategoryEntity>>

    @Query("SELECT * FROM subcategories WHERE id = :id")
    suspend fun getSubcategoryById(id: Long): SubcategoryEntity?

    /**
     * Global name lookup, and therefore **ambiguous** once two parents share a
     * subcategory name — which the composite unique index explicitly allows.
     *
     * Kept for the rules engine (doc 19), which matches on a bare name. Do not
     * use it for uniqueness checks; that is [getByCategoryAndName].
     */
    @Query("SELECT * FROM subcategories WHERE name = :name LIMIT 1")
    suspend fun getSubcategoryByName(name: String): SubcategoryEntity?

    @Query("SELECT * FROM subcategories WHERE category_id = :categoryId AND name = :name LIMIT 1")
    suspend fun getByCategoryAndName(categoryId: Long, name: String): SubcategoryEntity?

    /** One-shot read for `BackupExporter`; the Flow variants are for UI. */
    @Query("SELECT * FROM subcategories ORDER BY category_id ASC, name ASC")
    suspend fun getAllOnce(): List<SubcategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubcategory(subcategory: SubcategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubcategories(subcategories: List<SubcategoryEntity>)

    @Update
    suspend fun updateSubcategory(subcategory: SubcategoryEntity)

    @Delete
    suspend fun deleteSubcategory(subcategory: SubcategoryEntity)

    @Query("DELETE FROM subcategories WHERE id = :id")
    suspend fun deleteSubcategoryById(id: Long)

    @Query("SELECT COUNT(*) FROM subcategories")
    suspend fun getSubcategoryCount(): Int

    /** For `BackupImporter`'s replace mode. */
    @Query("DELETE FROM subcategories")
    suspend fun deleteAll()
}
