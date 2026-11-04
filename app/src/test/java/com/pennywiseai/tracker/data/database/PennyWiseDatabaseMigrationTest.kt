package com.pennywiseai.tracker.data.database

import kotlinx.coroutines.runBlocking
import androidx.sqlite.execSQL
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.SQLiteConnection
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PennyWiseDatabaseMigrationTest {
    private lateinit var context: Context
    private lateinit var db: SQLiteConnection

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DATABASE)
        context.getDatabasePath(TEST_DATABASE).parentFile?.mkdirs()
        db = AndroidSQLiteDriver().open(context.getDatabasePath(TEST_DATABASE).path)
        db.execSQL(
            "CREATE TABLE `loans` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`person_name` TEXT NOT NULL, " +
                "`person_id` INTEGER DEFAULT NULL)",
        )
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migration67To68_handlesPreexistingPersonColumnWithoutDuplicates() = runBlocking {
        db.execSQL("INSERT INTO `loans` (`person_name`) VALUES ('Example')")

        PennyWiseDatabase.MIGRATION_67_68.migrate(db)
        PennyWiseDatabase.MIGRATION_67_68.migrate(db)

        db.prepare("PRAGMA table_info(`loans`)").use { stmt ->
            val nameIndex = stmt.getColumnNames().indexOf("name")
            var personIdColumns = 0
            while (stmt.step()) {
                if (stmt.getText(nameIndex) == "person_id") personIdColumns++
            }
            assertEquals(1, personIdColumns)
        }
        db.prepare("SELECT COUNT(*) FROM `people`").use { stmt ->
            assertTrue(stmt.step())
            assertEquals(1L, stmt.getLong(0))
        }
        db.prepare("SELECT `person_id` FROM `loans` LIMIT 1").use { stmt ->
            assertTrue(stmt.step())
            assertTrue(stmt.getLong(0) > 0L)
        }
    }

    private companion object {
        const val TEST_DATABASE = "people-migration-test.db"
    }
}
