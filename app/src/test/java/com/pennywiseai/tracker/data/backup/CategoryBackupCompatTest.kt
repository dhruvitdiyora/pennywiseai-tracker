package com.pennywiseai.tracker.data.backup

import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.SubcategoryEntity
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A backup written **before** schema 58→59 must still import.
 *
 * This is the round-trip half of hard constraint 3, encoded as a test rather than
 * left as a manual step: the failure it guards against (#414, "can't restore an old
 * backup") is invisible until a real user tries it, by which point their data is
 * already on the floor.
 *
 * The JSON below is a real pre-59 category object — the eight columns that existed
 * at schema 58, and nothing else.
 */
class CategoryBackupCompatTest {

    private val pre59Category = """
        {
          "id": 7,
          "name": "Banking",
          "color": "#004C8F",
          "isSystem": true,
          "isIncome": false,
          "displayOrder": 9,
          "createdAt": "2025-01-04T10:15:30",
          "updatedAt": "2025-01-04T10:15:30"
        }
    """.trimIndent()

    @Test
    fun `a pre-59 category deserialises`() {
        val category = backupJson.decodeFromString(CategoryEntity.serializer(), pre59Category)

        assertEquals(7L, category.id)
        assertEquals("Banking", category.name)
        assertEquals("#004C8F", category.color)
        assertTrue(category.isSystem)
        assertEquals(9, category.displayOrder)
    }

    @Test
    fun `the new columns fall back to their defaults, not to a crash`() {
        val category = backupJson.decodeFromString(CategoryEntity.serializer(), pre59Category)

        // Empty rather than null: these are non-null columns with a `defaultValue`.
        assertEquals("", category.iconName)
        assertEquals("", category.description)
        assertEquals(0, category.iconResId)

        // Null is meaningful here — it is how `resetCategoryToDefault` recognises a
        // row with nothing to restore. The backfill fills these in on next launch.
        assertNull(category.defaultName)
        assertNull(category.defaultColor)
        assertNull(category.defaultIconName)
        assertNull(category.defaultDescription)
    }

    @Test
    fun `a pre-60 snapshot with no subcategories key at all still imports`() {
        // Schema 60 added a whole table. A backup written before it has no
        // "subcategories" key — the `= emptyList()` default is what makes that
        // survivable, and this asserts it rather than assuming it.
        val snapshot = """{"categories":[$pre59Category]}"""
        val decoded = backupJson.decodeFromString(DatabaseSnapshot.serializer(), snapshot)

        assertTrue(decoded.subcategories.isEmpty())
        assertEquals(1, decoded.categories.size)
    }

    @Test
    fun `a snapshot carrying subcategories round-trips`() {
        val snapshot = DatabaseSnapshot(
            categories = listOf(
                CategoryEntity(id = 1, name = "Food & Dining", color = "#FC8019", isSystem = true)
            ),
            subcategories = listOf(
                SubcategoryEntity(
                    id = 5,
                    categoryId = 1,
                    name = "Tea & Coffee",
                    iconName = "type_beverages_coffee",
                    color = "#FC8019",
                    isSystem = true,
                    defaultName = "Tea & Coffee",
                    defaultIconName = "type_beverages_coffee",
                    defaultColor = "#FC8019"
                )
            )
        )

        val json = backupJson.encodeToString(DatabaseSnapshot.serializer(), snapshot)
        val restored = backupJson.decodeFromString(DatabaseSnapshot.serializer(), json)

        assertEquals(snapshot.subcategories, restored.subcategories)
        assertEquals(1L, restored.subcategories.first().categoryId)
    }

    @Test
    fun `a subcategory missing its required fields still deserialises`() {
        // categoryId and name carry Kotlin defaults precisely so a malformed or
        // partial row imports and can be cleaned up, rather than failing the whole
        // restore (#414). A nonsense row beats a refused backup.
        val partial = """{"id": 9}"""
        val subcategory = backupJson.decodeFromString(SubcategoryEntity.serializer(), partial)

        assertEquals(9L, subcategory.id)
        assertEquals(0L, subcategory.categoryId)
        assertEquals("", subcategory.name)
        assertEquals("#757575", subcategory.color)
    }

    @Test
    fun `a snapshot whose category list predates the new columns still imports`() {
        val snapshot = """{"categories":[$pre59Category]}"""
        val decoded = backupJson.decodeFromString(DatabaseSnapshot.serializer(), snapshot)

        assertEquals(1, decoded.categories.size)
        assertEquals("Banking", decoded.categories.first().name)
    }

    @Test
    fun `a 59-era category round-trips through the backup format`() {
        val original = CategoryEntity(
            id = 3,
            name = "Groceries",
            color = "#5AC85A",
            isSystem = true,
            displayOrder = 2,
            iconName = "type_groceries_basket",
            description = "weekly shop",
            defaultName = "Groceries",
            defaultColor = "#5AC85A",
            defaultIconName = "type_groceries_basket",
            defaultDescription = ""
        )

        val json = backupJson.encodeToString(CategoryEntity.serializer(), original)
        val restored = backupJson.decodeFromString(CategoryEntity.serializer(), json)

        assertEquals(original, restored)
    }

    /**
     * Guards the *forward* direction too: a backup from a newer build opened by an
     * older one. `ignoreUnknownKeys` is what makes that survivable, so a change to
     * the decoder config would break old clients silently.
     */
    @Test
    fun `unknown future columns are ignored rather than fatal`() {
        val fromTheFuture = """
            {
              "id": 1,
              "name": "Banking",
              "color": "#004C8F",
              "someColumnFromAFutureSchema": "surprise"
            }
        """.trimIndent()

        val category = backupJson.decodeFromString(CategoryEntity.serializer(), fromTheFuture)
        assertEquals("Banking", category.name)
    }
}
