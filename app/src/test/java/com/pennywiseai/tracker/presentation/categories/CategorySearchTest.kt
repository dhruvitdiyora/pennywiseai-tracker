package com.pennywiseai.tracker.presentation.categories

import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CategorySearchTest {
    private val categories = listOf(
        CategoryEntity(id = 1, name = "Food & Dining", color = "#4CAF50"),
        CategoryEntity(id = 2, name = "Transportation", color = "#2196F3"),
        CategoryEntity(id = 3, name = "Shopping", color = "#9C27B0")
    )

    @Test
    fun blankQueryReturnsAllCategoriesInTheirExistingOrder() {
        assertEquals(categories, filterCategoriesForQuery(categories, ""))
    }

    @Test
    fun searchMatchesCategoryNamesWithoutCaseSensitivity() {
        assertEquals(
            listOf("Transportation"),
            filterCategoriesForQuery(categories, "TRANS").map { it.name }
        )
    }

    @Test
    fun searchTrimsWhitespaceBeforeMatching() {
        assertEquals(
            listOf("Food & Dining"),
            filterCategoriesForQuery(categories, "  food & dining  ").map { it.name }
        )
    }

    @Test
    fun unmatchedQueryReturnsAnEmptyList() {
        assertEquals(
            emptyList<CategoryEntity>(),
            filterCategoriesForQuery(categories, "Legacy category")
        )
    }
}
