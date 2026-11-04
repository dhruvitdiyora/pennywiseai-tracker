package com.pennywiseai.tracker.domain.usecase

import com.pennywiseai.tracker.data.database.withTransaction
import com.pennywiseai.tracker.data.database.PennyWiseDatabase
import com.pennywiseai.tracker.data.database.dao.TransactionDao
import com.pennywiseai.tracker.data.database.dao.TransactionSplitDao
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.repository.AccountBalanceRepository
import com.pennywiseai.tracker.data.repository.TagRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/** One field of a batch edit: leave it, set it to a value, or clear it. */
sealed interface FieldEdit<out T> {
    data object Unchanged : FieldEdit<Nothing>
    data class Set<T>(val value: T) : FieldEdit<T>
    data object Clear : FieldEdit<Nothing>
}

enum class TagEditMode { ADD, REMOVE, REPLACE }

/** Tag change for a batch edit. REPLACE with no [tags] clears every tag. */
data class TagEdit(val mode: TagEditMode, val tags: List<String>)

/**
 * What a batch edit changes. Deliberately limited to fields with no balance,
 * currency, account or loan consequence: category, the date part and time part
 * of `dateTime`, the note (`description`) and tags. Amount, account, currency
 * and type are never batch-edited.
 */
data class BatchEditRequest(
    val category: String? = null,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val note: FieldEdit<String> = FieldEdit.Unchanged,
    val tags: TagEdit? = null,
) {
    val changesCategory get() = category != null
    val changesDateTime get() = date != null || time != null
    val changesNote get() = note != FieldEdit.Unchanged
    val changesTags get() = tags != null
    val isEmpty get() = !changesCategory && !changesDateTime && !changesNote && !changesTags
}

/** Why a selected row was left untouched. */
enum class BatchSkipReason { MISSING, TRANSFER, SPLIT_CATEGORY }

/** The edited fields of one row, used for Undo's before/after comparison. */
data class BatchRowState(
    val category: String,
    val dateTime: LocalDateTime,
    val note: String?,
    val tags: Set<String>,
)

data class BatchRowChange(val id: Long, val before: BatchRowState, val after: BatchRowState)

/** In-memory Undo token for one Apply. Never persisted. */
data class BatchEditUndo(val request: BatchEditRequest, val changes: List<BatchRowChange>)

data class BatchEditResult(
    val updated: Int,
    val skipped: Map<BatchSkipReason, Int>,
    val undo: BatchEditUndo?,
) {
    val skippedCount get() = skipped.values.sum()
}

/**
 * Multi-field batch edit over a set of transaction ids.
 *
 * Apply re-reads every id inside ONE Room transaction (never trusts the UI's
 * snapshot), skips rows whose invariants the edit would break, and either
 * commits every eligible row or — on any failure — none of them. A row is
 * edited whole or not at all, so a skipped row keeps all of its fields.
 *
 * Skip rules:
 *  - missing or soft-deleted rows;
 *  - TRANSFER rows for category or date/time edits (the paired-transfer model
 *    is a separate operation); note/tag-only edits still apply to them;
 *  - rows with category splits for a category edit (the parent category must
 *    stay in step with its splits, as in the single-row editor).
 *
 * Loan-linked and split-bill rows are allowed: none of these fields feed loan
 * contribution, remaining amount or share totals. Subscription / recurring
 * templates are never touched; materialised rows are matched by hash, not
 * by date or category.
 *
 * Date/time edits go through [AccountBalanceRepository]'s balance-shift owner,
 * exactly as the single-row editor: manual openings are pinned first, the
 * shift owner nets the (same account, same amount) change, and each affected
 * manual account is recomputed once.
 *
 * [undo] restores, in one transaction, only rows whose edited fields still
 * equal this operation's post-state, so it never overwrites a later edit.
 */
@Singleton
open class BatchEditTransactionsUseCase @Inject constructor(
    private val database: PennyWiseDatabase,
    private val transactionDao: TransactionDao,
    private val transactionSplitDao: TransactionSplitDao,
    private val tagRepository: TagRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
) {
    internal open suspend fun <R> runInTransaction(block: suspend () -> R): R =
        database.withTransaction(block)

    /** Writes [tags] for one row. Open so tests can inject a mid-batch failure. */
    internal open suspend fun writeTags(transactionId: Long, edit: TagEdit) {
        val names = normalizeTags(edit.tags)
        when (edit.mode) {
            TagEditMode.ADD -> tagRepository.applyTagChanges(transactionId, add = names, remove = emptyList())
            TagEditMode.REMOVE -> tagRepository.applyTagChanges(transactionId, add = emptyList(), remove = names)
            TagEditMode.REPLACE -> tagRepository.setTagsForTransaction(transactionId, names)
        }
    }

    suspend fun apply(ids: Collection<Long>, request: BatchEditRequest): BatchEditResult {
        if (ids.isEmpty() || request.isEmpty) return BatchEditResult(0, emptyMap(), null)
        return runInTransaction {
            val skipped = mutableMapOf<BatchSkipReason, Int>()
            fun skip(reason: BatchSkipReason) { skipped[reason] = (skipped[reason] ?: 0) + 1 }

            val eligible = mutableListOf<TransactionEntity>()
            for (id in ids.distinct()) {
                val row = transactionDao.getTransactionById(id)
                when {
                    row == null || row.isDeleted -> skip(BatchSkipReason.MISSING)
                    row.transactionType == TransactionType.TRANSFER &&
                        (request.changesCategory || request.changesDateTime) -> skip(BatchSkipReason.TRANSFER)
                    request.changesCategory && row.category != request.category &&
                        transactionSplitDao.hasSplits(row.id) -> skip(BatchSkipReason.SPLIT_CATEGORY)
                    else -> eligible += row
                }
            }

            // Pin manual-account openings from the pre-edit snapshot (as the
            // single-row editor does) before any row moves.
            val dateMoved = if (request.changesDateTime) {
                eligible.filter { newDateTime(it, request) != it.dateTime }
            } else emptyList()
            val manualAccounts = dateMoved.mapNotNull(::accountKey).distinct()
            manualAccounts.forEach { (bank, acct) -> accountBalanceRepository.ensureManualOpening(bank, acct) }

            val changes = mutableListOf<BatchRowChange>()
            val now = LocalDateTime.now()
            for (row in eligible) {
                val before = stateOf(row)
                val updated = row.copy(
                    category = request.category ?: row.category,
                    dateTime = newDateTime(row, request),
                    description = when (val n = request.note) {
                        FieldEdit.Unchanged -> row.description
                        FieldEdit.Clear -> null
                        is FieldEdit.Set -> n.value.trim().ifEmpty { null }
                    },
                )
                val rowChanged = updated.category != row.category ||
                    updated.dateTime != row.dateTime || updated.description != row.description
                if (rowChanged) {
                    transactionDao.updateTransaction(updated.copy(updatedAt = now))
                    if (updated.dateTime != row.dateTime) {
                        accountBalanceRepository.applyTransactionBalanceShift(original = row, updated = updated)
                    }
                }
                request.tags?.let { writeTags(row.id, it) }
                val after = before.copy(
                    category = updated.category,
                    dateTime = updated.dateTime,
                    note = updated.description,
                    tags = if (request.changesTags) tagSet(row.id) else before.tags,
                )
                if (after != before) changes += BatchRowChange(row.id, before, after)
            }

            manualAccounts.forEach { (bank, acct) -> accountBalanceRepository.recomputeManualBalance(bank, acct) }

            BatchEditResult(
                updated = eligible.size,
                skipped = skipped,
                undo = if (changes.isEmpty()) null else BatchEditUndo(request, changes),
            )
        }
    }

    /**
     * Restores the rows of [undo] that still equal its post-state, in one
     * transaction. Returns how many rows were restored; rows edited since,
     * deleted, or missing are left alone.
     */
    suspend fun undo(undo: BatchEditUndo): Int = runInTransaction {
        val request = undo.request
        val restorable = undo.changes.mapNotNull { change ->
            val row = transactionDao.getTransactionById(change.id) ?: return@mapNotNull null
            if (row.isDeleted) return@mapNotNull null
            val current = stateOf(row).copy(
                tags = if (request.changesTags) tagSet(row.id) else change.after.tags,
            )
            if (!matchesPostState(current, change.after, request)) return@mapNotNull null
            row to change
        }

        val manualAccounts = restorable
            .filter { (row, change) -> row.dateTime != change.before.dateTime }
            .mapNotNull { (row, _) -> accountKey(row) }
            .distinct()
        manualAccounts.forEach { (bank, acct) -> accountBalanceRepository.ensureManualOpening(bank, acct) }

        val now = LocalDateTime.now()
        for ((row, change) in restorable) {
            val restored = row.copy(
                category = change.before.category,
                dateTime = change.before.dateTime,
                description = change.before.note,
            )
            if (restored.category != row.category || restored.dateTime != row.dateTime ||
                restored.description != row.description
            ) {
                transactionDao.updateTransaction(restored.copy(updatedAt = now))
                if (restored.dateTime != row.dateTime) {
                    accountBalanceRepository.applyTransactionBalanceShift(original = row, updated = restored)
                }
            }
            if (request.changesTags) {
                tagRepository.setTagsForTransaction(row.id, change.before.tags.toList())
            }
        }

        manualAccounts.forEach { (bank, acct) -> accountBalanceRepository.recomputeManualBalance(bank, acct) }
        restorable.size
    }

    private fun matchesPostState(current: BatchRowState, after: BatchRowState, request: BatchEditRequest) =
        (!request.changesCategory || current.category == after.category) &&
            (!request.changesDateTime || current.dateTime == after.dateTime) &&
            (!request.changesNote || current.note == after.note) &&
            (!request.changesTags || sameTags(current.tags, after.tags))

    private fun newDateTime(row: TransactionEntity, request: BatchEditRequest): LocalDateTime {
        val date = request.date ?: row.dateTime.toLocalDate()
        val time = request.time ?: row.dateTime.toLocalTime()
        return LocalDateTime.of(date, time)
    }

    private fun accountKey(row: TransactionEntity): Pair<String, String>? {
        val bank = row.bankName ?: return null
        val acct = row.accountNumber ?: return null
        return bank to acct
    }

    private suspend fun tagSet(id: Long): Set<String> =
        tagRepository.getTagNamesForTransaction(id).toSet()

    private suspend fun stateOf(row: TransactionEntity) = BatchRowState(
        category = row.category,
        dateTime = row.dateTime,
        note = row.description,
        tags = tagSet(row.id),
    )

    companion object {
        /** Trimmed, non-blank, case-insensitively distinct tag names. */
        fun normalizeTags(tags: List<String>): List<String> =
            tags.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }

        private fun sameTags(a: Set<String>, b: Set<String>) =
            a.mapTo(HashSet()) { it.lowercase() } == b.mapTo(HashSet()) { it.lowercase() }
    }
}
