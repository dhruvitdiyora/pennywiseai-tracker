package com.pennywiseai.tracker.ui.components

import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickCategoryPickerSheetTest {
    private val categories = listOf(
        CategoryEntity(id = 1, name = "Food & Dining", color = "#4CAF50"),
        CategoryEntity(id = 2, name = "Transportation", color = "#2196F3"),
        CategoryEntity(id = 3, name = "Shopping", color = "#9C27B0")
    )

    @Test
    fun searchIsCaseInsensitiveAndKeepsLiveCategoryOrdering() {
        assertEquals(
            listOf("Transportation"),
            filterCategoriesForQuery(categories, "  transport ").map { it.name }
        )
        assertEquals(
            categories.map { it.name },
            filterCategoriesForQuery(categories, "").map { it.name }
        )
    }

    @Test
    fun unmatchedSearchDoesNotInventOrRewriteLegacyValues() {
        assertEquals(
            emptyList<CategoryEntity>(),
            filterCategoriesForQuery(categories, "Legacy category")
        )
    }
}
