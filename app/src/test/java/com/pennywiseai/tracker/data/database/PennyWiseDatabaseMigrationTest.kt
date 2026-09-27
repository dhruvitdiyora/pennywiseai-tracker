package com.pennywiseai.tracker.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
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
    private lateinit var openHelper: SupportSQLiteOpenHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DATABASE)
        openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(TEST_DATABASE)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                "CREATE TABLE `loans` (" +
                                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                    "`person_name` TEXT NOT NULL, " +
                                    "`person_id` INTEGER DEFAULT NULL)",
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )
    }

    @After
    fun tearDown() {
        openHelper.close()
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migration62To63_handlesPreexistingPersonColumnWithoutDuplicates() {
        val db = openHelper.writableDatabase
        db.execSQL("INSERT INTO `loans` (`person_name`) VALUES ('Example')")

        PennyWiseDatabase.MIGRATION_62_63.migrate(db)
        PennyWiseDatabase.MIGRATION_62_63.migrate(db)

        db.query("PRAGMA table_info(`loans`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            var personIdColumns = 0
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == "person_id") personIdColumns++
            }
            assertEquals(1, personIdColumns)
        }
        db.query("SELECT COUNT(*) FROM `people`").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT `person_id` FROM `loans` LIMIT 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.getLong(0) > 0L)
        }
    }

    private companion object {
        const val TEST_DATABASE = "people-migration-test.db"
    }
}
