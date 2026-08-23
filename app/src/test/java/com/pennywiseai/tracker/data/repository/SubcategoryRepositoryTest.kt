package com.pennywiseai.tracker.data.repository

import com.pennywiseai.shared.data.bootstrap.DefaultCategoryData
import com.pennywiseai.tracker.data.database.dao.CategoryDao
import com.pennywiseai.tracker.data.database.dao.SubcategoryDao
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.SubcategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Covers schema 59→60. Hand-rolled in-memory DAOs rather than Room — this module
 * has no Robolectric and runs unit tests on the JVM.
 *
 * The fake [SubcategoryDao] reproduces the two behaviours that matter here: the
 * composite unique index (name unique *within* a parent), and `INSERT OR IGNORE`
 * returning -1 on conflict rather than throwing. Cascade delete is modelled in
 * the fake [CategoryDao], since the real one is enforced by SQLite.
 */
class SubcategoryRepositoryTest {

    private class FakeCategoryDao(
        seed: List<CategoryEntity> = emptyList(),
        private val onDelete: (Long) -> Unit = {}
    ) : CategoryDao {
        val rows = seed.toMutableList()

        override fun getAllCategories(): Flow<List<CategoryEntity>> = flowOf(rows.toList())
        override fun getExpenseCategories(): Flow<List<CategoryEntity>> = flowOf(rows.toList())
        override fun getIncomeCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override suspend fun getCategoryById(categoryId: Long) = rows.firstOrNull { it.id == categoryId }
        override suspend fun getCategoryByName(categoryName: String) =
            rows.firstOrNull { it.name == categoryName }
        override suspend fun insertCategory(category: CategoryEntity): Long = 0
        override suspend fun insertCategories(categories: List<CategoryEntity>) = Unit
        override suspend fun updateCategory(category: CategoryEntity) = Unit
        override suspend fun deleteCategory(categoryId: Long) {
            rows.removeAll { it.id == categoryId && !it.isSystem }
            onDelete(categoryId)
        }
        override suspend fun getCategoryCount(): Int = rows.size
        override suspend fun getSystemCategories(): List<CategoryEntity> = rows.filter { it.isSystem }
        override suspend fun categoryExists(categoryName: String) = rows.any { it.name == categoryName }
        override suspend fun deleteAllCategories() = rows.clear()
    }

    private class FakeSubcategoryDao(
        seed: List<SubcategoryEntity> = emptyList()
    ) : SubcategoryDao {
        val rows = seed.toMutableList()
        private var nextId = (seed.maxOfOrNull { it.id } ?: 0L) + 1

        override fun getSubcategoriesByCategoryId(categoryId: Long): Flow<List<SubcategoryEntity>> =
            flowOf(rows.filter { it.categoryId == categoryId })
        override fun getAllSubcategories(): Flow<List<SubcategoryEntity>> = flowOf(rows.toList())
        override suspend fun getSubcategoryById(id: Long) = rows.firstOrNull { it.id == id }
        override suspend fun getSubcategoryByName(name: String) = rows.firstOrNull { it.name == name }
        override suspend fun getByCategoryAndName(categoryId: Long, name: String) =
            rows.firstOrNull { it.categoryId == categoryId && it.name == name }
        override suspend fun getAllOnce(): List<SubcategoryEntity> = rows.toList()

        override suspend fun insertSubcategory(subcategory: SubcategoryEntity): Long {
            // Mirrors OnConflictStrategy.IGNORE against the composite unique index.
            if (rows.any { it.categoryId == subcategory.categoryId && it.name == subcategory.name }) {
                return -1L
            }
            val id = nextId++
            rows += subcategory.copy(id = id)
            return id
        }

        override suspend fun insertSubcategories(subcategories: List<SubcategoryEntity>) {
            subcategories.forEach { insertSubcategory(it) }
        }

        override suspend fun updateSubcategory(subcategory: SubcategoryEntity) {
            val index = rows.indexOfFirst { it.id == subcategory.id }
            if (index >= 0) rows[index] = subcategory
        }

        override suspend fun deleteSubcategory(subcategory: SubcategoryEntity) {
            rows.removeAll { it.id == subcategory.id }
        }

        override suspend fun deleteSubcategoryById(id: Long) {
            rows.removeAll { it.id == id }
        }

        override suspend fun getSubcategoryCount(): Int = rows.size
        override suspend fun deleteAll() = rows.clear()
    }

    private fun category(id: Long, name: String, color: String = "#FC8019") =
        CategoryEntity(id = id, name = name, color = color, isSystem = true)

    // ── seed data ────────────────────────────────────────────────────────────

    @Test
    fun `every subcategory seed iconName has a matching drawable on disk`() {
        val dir = File("src/main/res/drawable-nodpi")
        assertTrue("expected drawable-nodpi at ${dir.absolutePath}", dir.exists())

        val onDisk = dir.list()!!
            .filter { it.startsWith("type_") }
            .map { it.substringBeforeLast('.') }
            .toSet()

        val missing = DefaultCategoryData.SUBCATEGORIES.filter { it.iconName !in onDisk }
        assertTrue("subcategory seed with no drawable: ${missing.map { it.name to it.iconName }}",
            missing.isEmpty())
    }

    @Test
    fun `every subcategory seed names a real parent category`() {
        // The failure this guards is silent: Cashiro files these under
        // "Food & Drinks", which does not exist here, so a verbatim copy would
        // have seeded nothing and looked like the feature simply did not work.
        val categoryNames = DefaultCategoryData.ALL.map { it.name }.toSet()
        val orphans = DefaultCategoryData.SUBCATEGORIES.filter { it.parentName !in categoryNames }
        assertTrue("subcategory seeds with no such parent: ${orphans.map { it.parentName }}",
            orphans.isEmpty())
    }

    // ── uniqueness ───────────────────────────────────────────────────────────

    @Test
    fun `create then read back by parent`() = runBlocking {
        val subDao = FakeSubcategoryDao()
        val repo = SubcategoryRepository(subDao, FakeCategoryDao(listOf(category(1, "Food & Dining"))))

        val id = repo.createSubcategory(1, "Coffee", "type_beverages_coffee", "#FC8019")

        assertNotNull(id)
        assertEquals(1, subDao.rows.size)
        assertEquals("Coffee", subDao.rows.first().name)
        assertEquals(1L, subDao.rows.first().categoryId)
    }

    @Test
    fun `duplicate name under the same parent is refused`() = runBlocking {
        val subDao = FakeSubcategoryDao()
        val repo = SubcategoryRepository(subDao, FakeCategoryDao(listOf(category(1, "Food & Dining"))))

        repo.createSubcategory(1, "Coffee")
        val second = repo.createSubcategory(1, "Coffee")

        assertNull("expected the duplicate to be refused", second)
        assertEquals(1, subDao.rows.size)
    }

    @Test
    fun `the same name under a different parent is allowed`() = runBlocking {
        val subDao = FakeSubcategoryDao()
        val repo = SubcategoryRepository(
            subDao,
            FakeCategoryDao(listOf(category(1, "Bills & Utilities"), category(2, "Subscriptions")))
        )

        assertNotNull(repo.createSubcategory(1, "Monthly"))
        assertNotNull(repo.createSubcategory(2, "Monthly"))
        assertEquals(2, subDao.rows.size)
    }

    @Test
    fun `rename onto a sibling's name is rejected`() = runBlocking {
        val subDao = FakeSubcategoryDao(
            listOf(
                SubcategoryEntity(id = 1, categoryId = 1, name = "Coffee"),
                SubcategoryEntity(id = 2, categoryId = 1, name = "Tea")
            )
        )
        val repo = SubcategoryRepository(subDao, FakeCategoryDao(listOf(category(1, "Food & Dining"))))

        assertFalse(repo.updateSubcategory(subDao.rows.first { it.id == 2L }.copy(name = "Coffee")))
        assertEquals("Tea", subDao.rows.first { it.id == 2L }.name)
    }

    @Test
    fun `renaming to its own name is allowed`() = runBlocking {
        val subDao = FakeSubcategoryDao(
            listOf(SubcategoryEntity(id = 1, categoryId = 1, name = "Coffee", color = "#000000"))
        )
        val repo = SubcategoryRepository(subDao, FakeCategoryDao(listOf(category(1, "Food & Dining"))))

        // Otherwise no edit that keeps the name could ever be saved.
        assertTrue(repo.updateSubcategory(subDao.rows.first().copy(color = "#FFFFFF")))
        assertEquals("#FFFFFF", subDao.rows.first().color)
    }

    // ── seeding ──────────────────────────────────────────────────────────────

    @Test
    fun `seeding creates a row per seed whose parent exists`() = runBlocking {
        val categories = DefaultCategoryData.ALL.mapIndexed { i, seed ->
            category(i + 1L, seed.name, seed.colorHex)
        }
        val subDao = FakeSubcategoryDao()
        val repo = SubcategoryRepository(subDao, FakeCategoryDao(categories))

        repo.initializeDefaultSubcategories()

        assertEquals(DefaultCategoryData.SUBCATEGORIES.size, subDao.rows.size)
        subDao.rows.forEach { row ->
            assertTrue("seeded rows must be system rows", row.isSystem)
            assertEquals("defaults must be recorded so reset works", row.name, row.defaultName)
            assertTrue(row.iconName.isNotEmpty())
        }
    }

    @Test
    fun `seeding inherits the parent's colour`() = runBlocking {
        val subDao = FakeSubcategoryDao()
        val repo = SubcategoryRepository(
            subDao, FakeCategoryDao(listOf(category(1, "Food & Dining", "#FC8019")))
        )

        repo.initializeDefaultSubcategories()

        assertTrue(subDao.rows.isNotEmpty())
        assertTrue(subDao.rows.all { it.color == "#FC8019" })
    }

    @Test
    fun `seeding skips a seed whose parent is missing`() = runBlocking {
        // Only one of the three parent categories exists.
        val subDao = FakeSubcategoryDao()
        val repo = SubcategoryRepository(
            subDao, FakeCategoryDao(listOf(category(1, "Food & Dining")))
        )

        repo.initializeDefaultSubcategories()

        val expected = DefaultCategoryData.SUBCATEGORIES.count { it.parentName == "Food & Dining" }
        assertEquals(expected, subDao.rows.size)
        // A deleted category stays deleted — the seeder must not resurrect it.
        assertTrue(subDao.rows.all { it.categoryId == 1L })
    }

    @Test
    fun `seeding is a no-op on a second call`() = runBlocking {
        val subDao = FakeSubcategoryDao()
        val repo = SubcategoryRepository(
            subDao, FakeCategoryDao(listOf(category(1, "Food & Dining")))
        )

        repo.initializeDefaultSubcategories()
        val afterFirst = subDao.rows.toList()
        repo.initializeDefaultSubcategories()

        assertEquals(afterFirst, subDao.rows.toList())
    }

    // ── reset ────────────────────────────────────────────────────────────────

    @Test
    fun `reset restores name icon and colour`() = runBlocking {
        val subDao = FakeSubcategoryDao(
            listOf(
                SubcategoryEntity(
                    id = 1, categoryId = 1,
                    name = "Flat White", iconName = "type_sports_trophy", color = "#000000",
                    isSystem = true,
                    defaultName = "Tea & Coffee",
                    defaultIconName = "type_beverages_coffee",
                    defaultColor = "#FC8019"
                )
            )
        )
        val repo = SubcategoryRepository(subDao, FakeCategoryDao(listOf(category(1, "Food & Dining"))))

        assertTrue(repo.resetSubcategoryToDefault(1))

        val row = subDao.rows.first()
        assertEquals("Tea & Coffee", row.name)
        assertEquals("type_beverages_coffee", row.iconName)
        assertEquals("#FC8019", row.color)
    }

    @Test
    fun `reset refuses a user-created subcategory`() = runBlocking {
        val subDao = FakeSubcategoryDao(
            listOf(SubcategoryEntity(id = 1, categoryId = 1, name = "Espresso"))
        )
        val repo = SubcategoryRepository(subDao, FakeCategoryDao(listOf(category(1, "Food & Dining"))))

        assertFalse(repo.resetSubcategoryToDefault(1))
        assertEquals("Espresso", subDao.rows.first().name)
    }

    @Test
    fun `reset refuses when the original name is now held by a sibling`() = runBlocking {
        val subDao = FakeSubcategoryDao(
            listOf(
                SubcategoryEntity(
                    id = 1, categoryId = 1, name = "Flat White", isSystem = true,
                    defaultName = "Tea & Coffee", defaultIconName = "type_beverages_coffee"
                ),
                SubcategoryEntity(id = 2, categoryId = 1, name = "Tea & Coffee")
            )
        )
        val repo = SubcategoryRepository(subDao, FakeCategoryDao(listOf(category(1, "Food & Dining"))))

        assertFalse(repo.resetSubcategoryToDefault(1))
        assertEquals("Flat White", subDao.rows.first { it.id == 1L }.name)
    }

    @Test
    fun `reset refuses a missing subcategory`() = runBlocking {
        val repo = SubcategoryRepository(FakeSubcategoryDao(), FakeCategoryDao())
        assertFalse(repo.resetSubcategoryToDefault(404))
    }
}
