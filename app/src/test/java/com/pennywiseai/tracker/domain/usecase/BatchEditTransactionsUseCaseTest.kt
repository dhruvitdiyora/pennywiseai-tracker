package com.pennywiseai.tracker.domain.usecase

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywiseai.tracker.data.database.PennyWiseDatabase
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionSplitEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.repository.AccountBalanceRepository
import com.pennywiseai.tracker.data.repository.TagRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class BatchEditTransactionsUseCaseTest {
    private lateinit var database: PennyWiseDatabase
    private lateinit var tags: TagRepository
    private lateinit var balances: AccountBalanceRepository
    private lateinit var useCase: BatchEditTransactionsUseCase

    private val baseTime = LocalDateTime.of(2026, 3, 10, 14, 30)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PennyWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tags = TagRepository(database, database.tagDao())
        balances = AccountBalanceRepository(database.accountBalanceDao(), database.transactionDao(), database)
        useCase = newUseCase()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun newUseCase(failTagWriteOn: Long? = null) = object : BatchEditTransactionsUseCase(
        database, database.transactionDao(), database.transactionSplitDao(), tags, balances
    ) {
        override suspend fun writeTags(transactionId: Long, edit: TagEdit) {
            super.writeTags(transactionId, edit)
            if (transactionId == failTagWriteOn) error("injected failure")
        }
    }

    private suspend fun insert(
        hash: String,
        type: TransactionType = TransactionType.EXPENSE,
        category: String = "Food",
        note: String? = null,
        loanId: Long? = null,
        bank: String? = null,
        account: String? = null,
    ): Long = database.transactionDao().insertTransaction(
        TransactionEntity(
            amount = BigDecimal("100.00"),
            merchantName = "Store $hash",
            category = category,
            transactionType = type,
            dateTime = baseTime,
            description = note,
            transactionHash = hash,
            currency = "INR",
            loanId = loanId,
            bankName = bank,
            accountNumber = account,
        )
    )

    private suspend fun row(id: Long) = database.transactionDao().getTransactionById(id)!!
    private suspend fun tagsOf(id: Long) = tags.getTagNamesForTransaction(id).toSet()

    @Test
    fun appliesEveryFieldToEligibleRowsAndCountsSkips() = runBlocking {
        val a = insert("a")
        val loanLinked = insert("b", loanId = 7L)
        val transfer = insert("c", type = TransactionType.TRANSFER, category = "Transfer")
        val split = insert("d")
        database.transactionSplitDao().insertSplit(
            TransactionSplitEntity(transactionId = split, category = "Food", amount = BigDecimal("50.00"))
        )
        val missing = 9_999L

        val result = useCase.apply(
            listOf(a, loanLinked, transfer, split, missing),
            BatchEditRequest(
                category = "Travel",
                date = LocalDate.of(2026, 4, 1),
                note = FieldEdit.Set("  trip  "),
            ),
        )

        assertEquals(2, result.updated)
        assertEquals(3, result.skippedCount)
        assertEquals(1, result.skipped[BatchSkipReason.TRANSFER])
        assertEquals(1, result.skipped[BatchSkipReason.SPLIT_CATEGORY])
        assertEquals(1, result.skipped[BatchSkipReason.MISSING])

        for (id in listOf(a, loanLinked)) {
            val r = row(id)
            assertEquals("Travel", r.category)
            assertEquals(LocalDateTime.of(LocalDate.of(2026, 4, 1), LocalTime.of(14, 30)), r.dateTime)
            assertEquals("trip", r.note())
        }
        assertEquals(7L, row(loanLinked).loanId)
        // Skipped rows keep every field, not only the blocked one.
        assertEquals("Transfer", row(transfer).category)
        assertEquals(baseTime, row(transfer).dateTime)
        assertNull(row(transfer).description)
        assertEquals("Food", row(split).category)
        assertNull(row(split).description)
    }

    @Test
    fun noteAndTagOnlyEditsStillApplyToTransfersAndSplits() = runBlocking {
        val transfer = insert("t", type = TransactionType.TRANSFER)
        val split = insert("s")
        database.transactionSplitDao().insertSplit(
            TransactionSplitEntity(transactionId = split, category = "Food", amount = BigDecimal("50.00"))
        )

        val result = useCase.apply(
            listOf(transfer, split),
            BatchEditRequest(note = FieldEdit.Set("shared"), tags = TagEdit(TagEditMode.ADD, listOf("Work"))),
        )

        assertEquals(2, result.updated)
        assertEquals(0, result.skippedCount)
        assertEquals("shared", row(transfer).description)
        assertEquals(setOf("Work"), tagsOf(split))
    }

    @Test
    fun timeEditKeepsEachRowsDate() = runBlocking {
        val a = insert("a")
        useCase.apply(listOf(a), BatchEditRequest(time = LocalTime.of(9, 5)))
        assertEquals(LocalDateTime.of(2026, 3, 10, 9, 5), row(a).dateTime)
    }

    @Test
    fun noteClearRemovesNote() = runBlocking {
        val a = insert("a", note = "old")
        useCase.apply(listOf(a), BatchEditRequest(note = FieldEdit.Clear))
        assertNull(row(a).description)
    }

    @Test
    fun tagModesAddRemoveReplace() = runBlocking {
        val a = insert("a")
        val b = insert("b")
        tags.setTagsForTransaction(a, listOf("Home", "Trip"))
        tags.setTagsForTransaction(b, listOf("Trip"))

        useCase.apply(listOf(a, b), BatchEditRequest(tags = TagEdit(TagEditMode.ADD, listOf("Work", " work ", ""))))
        assertEquals(setOf("Home", "Trip", "Work"), tagsOf(a))
        assertEquals(setOf("Trip", "Work"), tagsOf(b))

        useCase.apply(listOf(a, b), BatchEditRequest(tags = TagEdit(TagEditMode.REMOVE, listOf("trip"))))
        assertEquals(setOf("Home", "Work"), tagsOf(a))
        assertEquals(setOf("Work"), tagsOf(b))

        useCase.apply(listOf(a, b), BatchEditRequest(tags = TagEdit(TagEditMode.REPLACE, listOf("Solo"))))
        assertEquals(setOf("Solo"), tagsOf(a))
        assertEquals(setOf("Solo"), tagsOf(b))

        useCase.apply(listOf(a, b), BatchEditRequest(tags = TagEdit(TagEditMode.REPLACE, emptyList())))
        assertTrue(tagsOf(a).isEmpty())
        assertTrue(tagsOf(b).isEmpty())
    }

    @Test
    fun failureMidBatchRollsBackEveryRow() = runBlocking {
        val a = insert("a", note = "keep")
        val b = insert("b", note = "keep")
        tags.setTagsForTransaction(a, listOf("Home"))
        val failing = newUseCase(failTagWriteOn = b)

        try {
            failing.apply(
                listOf(a, b),
                BatchEditRequest(category = "Travel", note = FieldEdit.Clear, tags = TagEdit(TagEditMode.REPLACE, listOf("X"))),
            )
            fail("expected the injected failure")
        } catch (e: IllegalStateException) {
            // expected
        }

        for (id in listOf(a, b)) {
            assertEquals("Food", row(id).category)
            assertEquals("keep", row(id).description)
        }
        assertEquals(setOf("Home"), tagsOf(a))
        assertTrue(tagsOf(b).isEmpty())
    }

    @Test
    fun undoRestoresOnlyRowsStillAtPostState() = runBlocking {
        val a = insert("a", note = "n1")
        val b = insert("b", note = "n2")
        tags.setTagsForTransaction(a, listOf("Home"))

        val result = useCase.apply(
            listOf(a, b),
            BatchEditRequest(
                category = "Travel",
                date = LocalDate.of(2026, 5, 2),
                note = FieldEdit.Set("trip"),
                tags = TagEdit(TagEditMode.ADD, listOf("Work")),
            ),
        )
        val undo = assertNotNullAndGet(result.undo)

        // A later edit to b must survive Undo.
        database.transactionDao().updateTransaction(row(b).copy(category = "Shopping"))

        assertEquals(1, useCase.undo(undo))

        val ra = row(a)
        assertEquals("Food", ra.category)
        assertEquals(baseTime, ra.dateTime)
        assertEquals("n1", ra.description)
        assertEquals(setOf("Home"), tagsOf(a))

        val rb = row(b)
        assertEquals("Shopping", rb.category)
        assertEquals("trip", rb.description)
        assertEquals(setOf("Work"), tagsOf(b))
    }

    @Test
    fun undoSkipsRowWhoseTagsChangedSince() = runBlocking {
        val a = insert("a")
        val result = useCase.apply(listOf(a), BatchEditRequest(tags = TagEdit(TagEditMode.ADD, listOf("Work"))))
        tags.setTagsForTransaction(a, listOf("Work", "Later"))

        assertEquals(0, useCase.undo(result.undo!!))
        assertEquals(setOf("Work", "Later"), tagsOf(a))
    }

    @Test
    fun undoSkipsDeletedRows() = runBlocking {
        val a = insert("a")
        val result = useCase.apply(listOf(a), BatchEditRequest(category = "Travel"))
        database.transactionDao().updateTransaction(row(a).copy(isDeleted = true))

        assertEquals(0, useCase.undo(result.undo!!))
        assertEquals("Travel", row(a).category)
    }

    @Test
    fun noOpRowsCountAsUpdatedButLeaveNoUndo() = runBlocking {
        val a = insert("a", category = "Travel")
        val result = useCase.apply(listOf(a), BatchEditRequest(category = "Travel"))
        assertEquals(1, result.updated)
        assertNull(result.undo)
    }

    @Test
    fun dateEditOnManualAccountKeepsDerivedBalance() = runBlocking {
        database.accountBalanceDao().insertBalance(
            AccountBalanceEntity(
                bankName = "Cash",
                accountLast4 = "0000",
                balance = BigDecimal("1000.00"),
                timestamp = baseTime.minusDays(30),
                sourceType = AccountBalanceRepository.SOURCE_MANUAL,
                currency = "INR",
            )
        )
        balances.ensureManualOpening("Cash", "0000")
        val a = insert("a", bank = "Cash", account = "0000")
        balances.recomputeManualBalance("Cash", "0000")
        val before = database.accountBalanceDao().getLatestBalance("Cash", "0000")!!.balance

        val result = useCase.apply(listOf(a), BatchEditRequest(date = LocalDate.of(2026, 1, 1)))
        assertEquals(1, result.updated)
        val after = database.accountBalanceDao().getLatestBalance("Cash", "0000")!!.balance
        assertEquals(0, before.compareTo(after))

        useCase.undo(result.undo!!)
        assertEquals(baseTime, row(a).dateTime)
        val undone = database.accountBalanceDao().getLatestBalance("Cash", "0000")!!.balance
        assertEquals(0, before.compareTo(undone))
    }

    @Test
    fun emptyRequestOrSelectionWritesNothing() = runBlocking {
        val a = insert("a")
        val r1 = useCase.apply(listOf(a), BatchEditRequest())
        val r2 = useCase.apply(emptyList(), BatchEditRequest(category = "Travel"))
        assertEquals(0, r1.updated + r2.updated)
        assertEquals("Food", row(a).category)
    }

    private fun TransactionEntity.note() = description

    private fun <T> assertNotNullAndGet(value: T?): T {
        assertNotNull(value)
        return value!!
    }
}
