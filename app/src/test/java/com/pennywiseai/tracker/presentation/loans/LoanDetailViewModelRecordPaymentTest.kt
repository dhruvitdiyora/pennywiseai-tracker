package com.pennywiseai.tracker.presentation.loans

import android.content.Context
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywiseai.tracker.data.database.PennyWiseDatabase
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.repository.LoanRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Recording a repayment writes a transaction, which re-emits the sheet's
 * "recent unlinked" list. That re-emission used to re-open the Record Payment
 * sheet right after "Add", leaving it stuck open.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LoanDetailViewModelRecordPaymentTest {
    private lateinit var database: PennyWiseDatabase
    private lateinit var repository: LoanRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PennyWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LoanRepository(
            loanDao = database.loanDao(),
            personDao = database.personDao(),
            transactionDao = database.transactionDao(),
            database = database,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** Runs main-looper work (viewModelScope) while Room works off-thread. */
    private fun settle(millis: Long = 600, until: () -> Boolean = { false }) {
        val deadline = System.currentTimeMillis() + millis
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (until()) return
            Thread.sleep(20)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun sheetStaysClosedAfterManualRepayment() {
        val loanId = runBlocking {
            repository.createManualLoan(
                personName = "Test Person",
                direction = LoanDirection.LENT,
                amount = BigDecimal("1000"),
                currency = "INR",
                note = null,
                dateTime = LocalDateTime.of(2026, 9, 2, 10, 30),
            )
        }
        val viewModel = LoanDetailViewModel(repository, SavedStateHandle(mapOf("loanId" to loanId)))
        settle(3_000) { viewModel.uiState.value.loan != null }

        viewModel.showRecordPayment()
        settle(3_000) { viewModel.uiState.value.showRecordPaymentSheet }
        assertTrue(viewModel.uiState.value.showRecordPaymentSheet)

        viewModel.recordManualRepayment(BigDecimal("200"))
        settle(3_000) { viewModel.uiState.value.loan?.remainingAmount?.compareTo(BigDecimal("800")) == 0 }
        assertEquals(0, viewModel.uiState.value.loan!!.remainingAmount.compareTo(BigDecimal("800")))

        // Give Room's invalidation time to re-emit the picker list.
        settle()
        assertFalse(viewModel.uiState.value.showRecordPaymentSheet)
        assertFalse(viewModel.uiState.value.repaymentFailed)
    }
}
