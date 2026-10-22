package com.pennywiseai.tracker.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * End-to-end upgrades into v68 (People): a database is built from an exported
 * schema JSON, then opened through Room with [PennyWiseDatabase.ALL_MIGRATIONS],
 * so Room's own post-migration schema validation runs against 68.json.
 *
 * - 67 → 68: upstream's latest schema gains the people table and loans.person_id,
 *   and existing loans are linked to one person per case-insensitive name.
 * - Cashiro-port v63 → 68: that branch once shipped People as its own 62→63,
 *   colliding with upstream's 62→63 (webhooks). Such a database must still reach
 *   v68 with both the webhook tables and the people data intact.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PeopleMigrationUpgradeTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB)
    }

    @Test
    fun upgrade67To68_createsPeopleAndLinksLoans() {
        createFromSchema(67) { db ->
            insertLoan(db, 1, " Asha ")
            insertLoan(db, 2, "asha")
            insertLoan(db, 3, "Ravi")
            insertLoan(db, 4, "   ")
        }

        val room = openWithRoom()
        try {
            val db = room.openHelper.writableDatabase
            assertEquals(SCHEMA_VERSION, db.version)
            assertEquals(2, count(db, "SELECT COUNT(*) FROM people"))
            // Both spellings of Asha point at the same person.
            assertEquals(
                1,
                count(db, "SELECT COUNT(DISTINCT person_id) FROM loans WHERE id IN (1, 2)")
            )
            assertEquals(1, count(db, "SELECT COUNT(*) FROM loans WHERE person_id IS NULL"))
        } finally {
            room.close()
        }
    }

    @Test
    fun cashiroPortV63_upgradesWithWebhookTablesHealed() {
        createFromSchema(62) { db ->
            // What the Cashiro-port branch's own 62→63 left on disk.
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `people` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `normalized_name` TEXT NOT NULL, " +
                    "`phone_number` TEXT, `notes` TEXT, `avatar` TEXT, `category` TEXT, " +
                    "`color` TEXT NOT NULL DEFAULT '#4CAF50', " +
                    "`is_archived` INTEGER NOT NULL DEFAULT 0, " +
                    "`created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_people_normalized_name` ON `people` (`normalized_name`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_people_is_archived` ON `people` (`is_archived`)")
            db.execSQL("ALTER TABLE `loans` ADD COLUMN `person_id` INTEGER DEFAULT NULL")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_loans_person_id` ON `loans` (`person_id`)")
            db.execSQL(
                "INSERT INTO people (id, name, normalized_name, created_at, updated_at) " +
                    "VALUES (7, 'Asha', 'asha', '2026-01-01T00:00', '2026-01-01T00:00')"
            )
            insertLoan(db, 1, "Asha", personId = 7)
        }

        val room = openWithRoom(fromVersion = 63)
        try {
            val db = room.openHelper.writableDatabase
            assertEquals(SCHEMA_VERSION, db.version)
            assertEquals(1, count(db, "SELECT COUNT(*) FROM people"))
            assertEquals(7, count(db, "SELECT person_id FROM loans WHERE id = 1"))
            assertEquals(0, count(db, "SELECT COUNT(*) FROM webhook_profiles"))
        } finally {
            room.close()
        }
    }

    private fun openWithRoom(@Suppress("UNUSED_PARAMETER") fromVersion: Int = 67): PennyWiseDatabase =
        Room.databaseBuilder(context, PennyWiseDatabase::class.java, DB)
            .addMigrations(*PennyWiseDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
            .also { assertNotNull(it.openHelper.writableDatabase) }

    /** Builds [DB] exactly as the exported [version].json describes, at that user_version. */
    private fun createFromSchema(version: Int, seed: (SupportSQLiteDatabase) -> Unit) {
        val json = JSONObject(schemaFile(version).readText()).getJSONObject("database")
        val targetVersion = if (version == 62) 63 else version
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DB)
                .callback(object : SupportSQLiteOpenHelper.Callback(targetVersion) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val entities = json.getJSONArray("entities")
                        for (i in 0 until entities.length()) {
                            val entity = entities.getJSONObject(i)
                            val table = entity.getString("tableName")
                            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                            val indices = entity.optJSONArray("indices") ?: continue
                            for (j in 0 until indices.length()) {
                                db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                            }
                        }
                        val views = json.optJSONArray("views")
                        if (views != null) {
                            for (i in 0 until views.length()) {
                                val view = views.getJSONObject(i)
                                db.execSQL(view.getString("createSql").replace("\${VIEW_NAME}", view.getString("viewName")))
                            }
                        }
                        seed(db)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase
        helper.close()
    }

    private fun schemaFile(version: Int): File {
        val relative = "schemas/com.pennywiseai.tracker.data.database.PennyWiseDatabase/$version.json"
        return listOf(File(relative), File("app/$relative")).firstOrNull { it.exists() }
            ?: error("Schema $version.json not found from ${File(".").absolutePath}")
    }

    private fun insertLoan(db: SupportSQLiteDatabase, id: Long, name: String, personId: Long? = null) {
        val columns = mutableListOf("id", "person_name", "direction", "original_amount", "remaining_amount", "currency", "status", "created_at", "updated_at")
        val values = mutableListOf<Any?>(id, name, "LENT", "100", "100", "INR", "ACTIVE", "2026-01-01T00:00", "2026-01-01T00:00")
        if (personId != null) {
            columns += "person_id"
            values += personId
        }
        db.execSQL(
            "INSERT INTO loans (${columns.joinToString()}) VALUES (${columns.joinToString { "?" }})",
            values.toTypedArray()
        )
    }

    private fun count(db: SupportSQLiteDatabase, sql: String): Int =
        db.query(sql).use { c ->
            assertTrue(c.moveToFirst())
            c.getInt(0)
        }

    private companion object {
        const val DB = "people-upgrade-test.db"
    }
}
