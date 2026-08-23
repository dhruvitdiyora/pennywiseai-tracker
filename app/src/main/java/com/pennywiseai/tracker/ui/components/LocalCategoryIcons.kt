package com.pennywiseai.tracker.ui.components

import androidx.compose.runtime.compositionLocalOf
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.SubcategoryEntity

/**
 * Name → entity lookups so a transaction row can reach the icon and colour the
 * user chose for its category or subcategory.
 *
 * A transaction stores its category and subcategory **by name** (doc 11's design
 * note), which is what lets the SMS parser write one with no database round-trip.
 * The cost is that rendering needs a lookup, and this is it.
 *
 * Delivered as a CompositionLocal rather than as two more parameters on every
 * `TransactionItem` call site, following the `LocalMerchantDisplay` that already
 * sits beside it for exactly this kind of resolution.
 *
 * **Build the maps once per ViewModel emission.** Building them per row is O(rows)
 * map constructions on the hottest list in the app.
 */
data class CategoryIconLookup(
    val categoriesByName: Map<String, CategoryEntity> = emptyMap(),
    /**
     * Keyed by `(categoryName, subcategoryName)` — a subcategory name is unique
     * only *within* its parent (doc 11), so the name alone would collide across
     * categories and hand a row the wrong icon.
     */
    val subcategoriesByName: Map<Pair<String, String>, SubcategoryEntity> = emptyMap(),
) {
    fun category(name: String?): CategoryEntity? =
        name?.takeIf { it.isNotBlank() }?.let { categoriesByName[it] }

    fun subcategory(categoryName: String?, subcategoryName: String?): SubcategoryEntity? {
        if (categoryName.isNullOrBlank() || subcategoryName.isNullOrBlank()) return null
        return subcategoriesByName[categoryName to subcategoryName]
    }
}

/**
 * Empty by default, so a screen that has not been wired up renders exactly as it
 * did before rather than crashing or showing blanks. Missing lookups are silent
 * by design — a category can be deleted, renamed, or arrive from another
 * install's backup.
 */
val LocalCategoryIcons = compositionLocalOf { CategoryIconLookup() }
