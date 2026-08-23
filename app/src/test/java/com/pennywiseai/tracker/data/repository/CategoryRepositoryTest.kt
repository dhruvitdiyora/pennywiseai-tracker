package com.pennywiseai.tracker.data.repository

import com.pennywiseai.shared.data.bootstrap.DefaultCategoryData
import com.pennywiseai.tracker.data.database.dao.CategoryDao
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Covers schema 58→59: the seed icon names, the unique-name guard on update,
 * reset-to-default, and the backfill for installs that predate the new columns.
 *
 * Uses a hand-rolled in-memory [CategoryDao] rather than Room — this module runs
 * unit tests on the JVM with `isReturnDefaultValues = true` and no Robolectric,
 * so a real database is not available here.
 */
class CategoryRepositoryTest {

    private class FakeCategoryDao(
        seed: List<CategoryEntity> = emptyList()
    ) : CategoryDao {
        val rows = seed.toMutableList()
        private var nextId = (seed.maxOfOrNull { it.id } ?: 0L) + 1

        override fun getAllCategories(): Flow<List<CategoryEntity>> = flowOf(rows.toList())
        override fun getExpenseCategories(): Flow<List<CategoryEntity>> =
            flowOf(rows.filter { !it.isIncome })
        override fun getIncomeCategories(): Flow<List<CategoryEntity>> =
            flowOf(rows.filter { it.isIncome })

        override suspend fun getCategoryById(categoryId: Long): CategoryEntity? =
            rows.firstOrNull { it.id == categoryId }

        override suspend fun getCategoryByName(categoryName: String): CategoryEntity? =
            rows.firstOrNull { it.name == categoryName }

        override suspend fun insertCategory(category: CategoryEntity): Long {
            if (rows.any { it.name == category.name }) return -1L
            val id = nextId++
            rows += category.copy(id = id)
            return id
        }

        override suspend fun insertCategories(categories: List<CategoryEntity>) {
            categories.forEach { insertCategory(it) }
        }

        override suspend fun updateCategory(category: CategoryEntity) {
            val index = rows.indexOfFirst { it.id == category.id }
            if (index >= 0) rows[index] = category
        }

        override suspend fun deleteCategory(categoryId: Long) {
            rows.removeAll { it.id == categoryId && !it.isSystem }
        }

        override suspend fun getCategoryCount(): Int = rows.size

        override suspend fun getSystemCategories(): List<CategoryEntity> =
            rows.filter { it.isSystem }

        override suspend fun categoryExists(categoryName: String): Boolean =
            rows.any { it.name == categoryName }

        override suspend fun deleteAllCategories() = rows.clear()
    }

    /** A pre-59 row: no icon, no defaults — what an upgrading install looks like. */
    private fun legacySystemRow(id: Long, name: String, color: String) = CategoryEntity(
        id = id,
        name = name,
        color = color,
        isSystem = true
    )

    // ── seed data ────────────────────────────────────────────────────────────

    @Test
    fun `every seed iconName has a matching drawable on disk`() {
        // An unresolvable name does not fail loudly — it silently renders the
        // fallback icon, so every default category would quietly look identical.
        // This is the check that keeps the seed table honest.
        val dir = File("src/main/res/drawable-nodpi")
        assertTrue("expected drawable-nodpi at ${dir.absolutePath}", dir.exists())

        val onDisk = dir.list()!!
            .filter { it.startsWith("type_") }
            .map { it.substringBeforeLast('.') }
            .toSet()

        val missing = DefaultCategoryData.ALL.filter { it.iconName !in onDisk }
        assertTrue("seed iconName with no drawable: ${missing.map { it.name to it.iconName }}",
            missing.isEmpty())
    }

    @Test
    fun `every seed has a non-blank iconName`() {
        val blank = DefaultCategoryData.ALL.filter { it.iconName.isBlank() }
        assertTrue("seeds missing an iconName: ${blank.map { it.name }}", blank.isEmpty())
    }

    // ── unique-name guard ────────────────────────────────────────────────────

    @Test
    fun `updateCategory refuses a name already held by another category`() = runBlocking {
        val dao = FakeCategoryDao(
            listOf(
                legacySystemRow(1, "Food & Dining", "#FC8019"),
                legacySystemRow(2, "Groceries", "#5AC85A")
            )
        )
        val repo = CategoryRepository(dao)

        val renamed = dao.rows.first { it.id == 2L }.copy(name = "Food & Dining")
        assertFalse("expected the colliding rename to be refused", repo.updateCategory(renamed))
        assertEquals("Groceries", dao.rows.first { it.id == 2L }.name)
    }

    @Test
    fun `updateCategory allows a category to keep its own name`() = runBlocking {
        val dao = FakeCategoryDao(listOf(legacySystemRow(1, "Food & Dining", "#FC8019")))
        val repo = CategoryRepository(dao)

        val recoloured = dao.rows.first().copy(color = "#000000")
        assertTrue(repo.updateCategory(recoloured))
        assertEquals("#000000", dao.rows.first().color)
    }

    // ── backfill ─────────────────────────────────────────────────────────────

    @Test
    fun `backfill fills icon and defaults on pre-59 system rows`() = runBlocking {
        val dao = FakeCategoryDao(
            DefaultCategoryData.ALL.mapIndexed { index, seed ->
                legacySystemRow(index + 1L, seed.name, seed.colorHex)
            }
        )
        val repo = CategoryRepository(dao)

        repo.backfillSystemCategoryDefaults()

        val seedsByName = DefaultCategoryData.ALL.associateBy { it.name }
        dao.rows.forEach { row ->
            val seed = seedsByName.getValue(row.name)
            assertEquals(seed.iconName, row.iconName)
            assertEquals(seed.name, row.defaultName)
            assertEquals(seed.colorHex, row.defaultColor)
            assertEquals(seed.iconName, row.defaultIconName)
            assertNotNull(row.defaultDescription)
        }
    }

    @Test
    fun `backfill does not clobber a user's own icon`() = runBlocking {
        val dao = FakeCategoryDao(
            listOf(legacySystemRow(1, "Groceries", "#5AC85A").copy(iconName = "type_sports_trophy"))
        )
        val repo = CategoryRepository(dao)

        repo.backfillSystemCategoryDefaults()

        assertEquals("type_sports_trophy", dao.rows.first().iconName)
        // The default still gets recorded, so reset can restore the seed icon.
        assertEquals("type_groceries_basket", dao.rows.first().defaultIconName)
    }

    @Test
    fun `backfill skips a renamed category rather than guessing`() = runBlocking {
        val dao = FakeCategoryDao(listOf(legacySystemRow(1, "Weekly Shop", "#5AC85A")))
        val repo = CategoryRepository(dao)

        repo.backfillSystemCategoryDefaults()

        // Name is the only stable key a pre-59 row has. No match means no write —
        // inventing a mapping here would attach the wrong icon and the wrong
        // "original" to a category the user renamed deliberately.
        assertEquals("", dao.rows.first().iconName)
        assertEquals(null, dao.rows.first().defaultName)
    }

    @Test
    fun `backfill is idempotent`() = runBlocking {
        val dao = FakeCategoryDao(listOf(legacySystemRow(1, "Banking", "#004C8F")))
        val repo = CategoryRepository(dao)

        repo.backfillSystemCategoryDefaults()
        val afterFirst = dao.rows.first()
        repo.backfillSystemCategoryDefaults()

        assertEquals(afterFirst, dao.rows.first())
    }

    @Test
    fun `backfill leaves user-created categories alone`() = runBlocking {
        val userRow = CategoryEntity(id = 1, name = "Boat Fund", color = "#123456", isSystem = false)
        val dao = FakeCategoryDao(listOf(userRow))
        val repo = CategoryRepository(dao)

        repo.backfillSystemCategoryDefaults()

        assertEquals(userRow, dao.rows.first())
    }

    // ── reset to default ─────────────────────────────────────────────────────

    @Test
    fun `reset restores name colour icon and description`() = runBlocking {
        val dao = FakeCategoryDao(
            listOf(
                CategoryEntity(
                    id = 1,
                    name = "Eating Out",
                    color = "#000000",
                    isSystem = true,
                    iconName = "type_sports_trophy",
                    description = "my note",
                    defaultName = "Food & Dining",
                    defaultColor = "#FC8019",
                    defaultIconName = "type_food_dining",
                    defaultDescription = ""
                )
            )
        )
        val repo = CategoryRepository(dao)

        assertTrue(repo.resetCategoryToDefault(1))

        val row = dao.rows.first()
        assertEquals("Food & Dining", row.name)
        assertEquals("#FC8019", row.color)
        assertEquals("type_food_dining", row.iconName)
        assertEquals("", row.description)
    }

    @Test
    fun `reset refuses a user-created category`() = runBlocking {
        val dao = FakeCategoryDao(
            listOf(CategoryEntity(id = 1, name = "Boat Fund", color = "#123456"))
        )
        val repo = CategoryRepository(dao)

        assertFalse(repo.resetCategoryToDefault(1))
        assertEquals("Boat Fund", dao.rows.first().name)
    }

    @Test
    fun `reset refuses when the original name is now taken`() = runBlocking {
        val dao = FakeCategoryDao(
            listOf(
                CategoryEntity(
                    id = 1,
                    name = "Eating Out",
                    color = "#000000",
                    isSystem = true,
                    defaultName = "Food & Dining",
                    defaultColor = "#FC8019",
                    defaultIconName = "type_food_dining"
                ),
                CategoryEntity(id = 2, name = "Food & Dining", color = "#FC8019")
            )
        )
        val repo = CategoryRepository(dao)

        // Restoring would violate the unique index; refusing beats crashing.
        assertFalse(repo.resetCategoryToDefault(1))
        assertEquals("Eating Out", dao.rows.first { it.id == 1L }.name)
    }

    @Test
    fun `reset refuses a missing category`() = runBlocking {
        val repo = CategoryRepository(FakeCategoryDao())
        assertFalse(repo.resetCategoryToDefault(404))
    }
}
