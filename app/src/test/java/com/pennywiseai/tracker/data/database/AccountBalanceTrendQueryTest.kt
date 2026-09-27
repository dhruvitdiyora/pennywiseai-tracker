package com.pennywiseai.tracker.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AccountBalanceTrendQueryTest {
    private lateinit var database: PennyWiseDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PennyWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `returns window rows and only the latest pre-window seed per account`() = runTest {
        val start = LocalDate.of(2026, 9, 1)
        val end = LocalDate.of(2026, 9, 3)
        listOf(
            row("Primary", "1111", "80", start.minusDays(10)),
            row("Primary", "1111", "90", start.minusDays(1)),
            row("Reserve", "2222", "50", start.minusDays(2)),
            row("Primary", "1111", "100", start.plusDays(1)),
            row("Primary", "1111", "999", end.plusDays(1)),
        ).forEach { database.accountBalanceDao().insertBalance(it) }

        val result = database.accountBalanceDao().getBalanceTrendRows(
            startDate = start.atStartOfDay(),
            endDate = end.atTime(23, 59, 59),
        ).first()

        assertEquals(
            listOf("Reserve:50", "Primary:90", "Primary:100"),
            result.map { "${it.bankName}:${it.balance.toPlainString()}" },
        )
    }

    private fun row(
        bankName: String,
        last4: String,
        balance: String,
        day: LocalDate,
    ) = AccountBalanceEntity(
        bankName = bankName,
        accountLast4 = last4,
        balance = BigDecimal(balance),
        timestamp = LocalDateTime.of(day.year, day.month, day.dayOfMonth, 12, 0),
        currency = "INR",
    )
}
