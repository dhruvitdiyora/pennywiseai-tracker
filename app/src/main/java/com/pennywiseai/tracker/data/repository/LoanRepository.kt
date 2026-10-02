package com.pennywiseai.tracker.data.repository

import androidx.room.withTransaction
import com.pennywiseai.tracker.data.database.PennyWiseDatabase
import com.pennywiseai.tracker.data.database.dao.LoanDao
import com.pennywiseai.tracker.data.database.dao.PersonDao
import com.pennywiseai.tracker.data.database.dao.TransactionDao
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.LoanStatus
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepository @Inject constructor(
    private val loanDao: LoanDao,
    private val personDao: PersonDao,
    private val transactionDao: TransactionDao,
    private val database: PennyWiseDatabase,
) {
    fun getActiveLoans(): Flow<List<LoanEntity>> = loanDao.getActiveLoans()

    fun getAllLoans(): Flow<List<LoanEntity>> = loanDao.getAllLoans()

    fun getActiveLoanCount(): Flow<Int> = loanDao.getActiveLoanCount()

    fun getTotalLentRemaining(): Flow<BigDecimal> = loanDao.getTotalLentRemaining()

    fun getTotalBorrowedRemaining(): Flow<BigDecimal> = loanDao.getTotalBorrowedRemaining()

    fun getActiveLentTransactionsInPeriod(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionEntity>> = loanDao.getActiveLentTransactionsInPeriod(startDate, endDate)

    fun getLentLoansSettledInPeriod(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<LoanEntity>> = loanDao.getLentLoansSettledInPeriod(startDate, endDate)

    /**
     * Net loss on a settled LENT loan: principal minus what came back as INCOME repayments.
     * Returns zero if the loan was repaid in full (or over).
     */
    suspend fun getSettlementLoss(loan: LoanEntity): BigDecimal {
        if (loan.direction != LoanDirection.LENT) return BigDecimal.ZERO
        val repaid = loanDao.getTotalRepaidByType(loan.id, "INCOME")
        return (loan.originalAmount - repaid).coerceAtLeast(BigDecimal.ZERO)
    }

    fun getTransactionsForLoan(loanId: Long): Flow<List<TransactionEntity>> =
        loanDao.getTransactionsForLoan(loanId)

    // Only same-currency transactions can repay a loan (see getActiveLoanByPersonAndDirection).
    fun getRecentUnlinkedRepayments(direction: LoanDirection, currency: String, limit: Int = 20): Flow<List<TransactionEntity>> {
        val repaymentType = if (direction == LoanDirection.LENT) "INCOME" else "EXPENSE"
        return loanDao.getRecentUnlinkedTransactionsByType(repaymentType, currency, limit)
    }

    fun getRecentPersonNames(): Flow<List<String>> = loanDao.getRecentPersonNames()

    fun getLoansByPerson(personId: Long): Flow<List<LoanEntity>> =
        loanDao.getLoansByPerson(personId)

    /** Live currency-safe aggregates for the People surface. */
    fun observePersonSummary(personId: Long, personName: String = ""): Flow<PersonLoanSummary> =
        loanDao.getLoansByPerson(personId).map { loans ->
            summarizePersonLoans(personId, personName, loans)
        }

    suspend fun getPersonSummary(personId: Long, personName: String = ""): PersonLoanSummary =
        summarizePersonLoans(personId, personName, loanDao.getLoansByPersonOnce(personId))

    suspend fun getLoanById(loanId: Long): LoanEntity? = loanDao.getLoanById(loanId)

    suspend fun getOriginalTransactionForLoan(loanId: Long): TransactionEntity? =
        loanDao.getOriginalTransactionForLoan(loanId)

    /** Currency-aware compatibility lookup for legacy name-linked rows. */
    suspend fun findActiveLoanForPerson(
        personName: String,
        direction: LoanDirection,
        currency: String,
        personId: Long? = null,
    ): LoanEntity? {
        val normalizedCurrency = normalizeCurrency(currency)
        return if (personId != null) {
            loanDao.getActiveLoanByPersonIdAndDirectionAndCurrency(
                personId, direction.name, normalizedCurrency
            )
        } else {
            loanDao.getActiveLoanByPersonAndDirectionAndCurrency(
                normalizePersonName(personName), direction.name, normalizedCurrency
            )
        }
    }

    suspend fun findActiveLoanForPerson(
        personId: Long,
        direction: LoanDirection,
        currency: String,
    ): LoanEntity? = loanDao.getActiveLoanByPersonIdAndDirectionAndCurrency(
        personId, direction.name, normalizeCurrency(currency)
    )

    /**
     * Merge [transactionId] into an existing loan, bumping its principal by the
     * caller-supplied [contribution]. When [contribution] is less than the
     * transaction's own amount (a partial loan), it is persisted on the
     * transaction's `loan_contribution` column so the override survives future
     * recomputations.
     */
    suspend fun addToExistingLoan(loanId: Long, contribution: BigDecimal, transactionId: Long) {
        require(contribution > BigDecimal.ZERO) { "Loan contribution must be positive" }
        database.withTransaction {
            val loan = loanDao.getLoanById(loanId) ?: error("Loan not found")
            val transaction = transactionDao.getTransactionById(transactionId)
                ?: error("Transaction not found")
            require(normalizeCurrency(transaction.currency) == normalizeCurrency(loan.currency)) {
                "Transaction and loan currencies must match"
            }
            loanDao.updateLoan(
                loan.copy(
                    originalAmount = loan.originalAmount + contribution,
                    remainingAmount = loan.remainingAmount + contribution,
                    updatedAt = LocalDateTime.now()
                )
            )
            loanDao.linkTransaction(transactionId, loanId)
            persistContributionOverride(transactionId, contribution)
        }
    }

    /**
     * Create a new loan seeded from [sourceTransactionId]. [amount] is the
     * principal — typically the full transaction amount, but the caller may
     * supply a smaller value if only part of the payment is a loan.
     */
    suspend fun createLoan(
        personName: String,
        direction: LoanDirection,
        amount: BigDecimal,
        currency: String,
        note: String?,
        sourceTransactionId: Long,
        personId: Long? = null,
    ): Long {
        require(amount > BigDecimal.ZERO) { "Loan amount must be positive" }
        val normalizedCurrency = normalizeCurrency(currency)
        require(normalizedCurrency.isNotBlank()) { "Currency is required" }
        return database.withTransaction {
            insertLoanAndLink(
                personName = personName,
                personId = personId,
                direction = direction,
                amount = amount,
                currency = normalizedCurrency,
                note = note,
                sourceTransactionId = sourceTransactionId,
            )
        }
    }

    /**
     * Creates a manual lend/borrow entry and its linked transaction as one
     * atomic database operation. A failure cannot leave an orphan transaction
     * that looks like spending/income but has no corresponding loan ledger.
     */
    suspend fun createManualLoan(
        personName: String,
        direction: LoanDirection,
        amount: BigDecimal,
        currency: String,
        note: String?,
        dateTime: LocalDateTime = LocalDateTime.now(),
        personId: Long? = null,
    ): Long {
        val normalizedName = personName.trim()
        val normalizedCurrency = currency.trim().uppercase()
        require(normalizedName.isNotBlank()) { "Person name cannot be blank" }
        require(amount > BigDecimal.ZERO) { "Loan amount must be positive" }
        require(normalizedCurrency.isNotBlank()) { "Currency is required" }

        return database.withTransaction {
            val transaction = manualLoanTransaction(
                personName = normalizedName,
                direction = direction,
                amount = amount,
                currency = normalizedCurrency,
                note = note,
                dateTime = dateTime,
            )
            val transactionId = transactionDao.insertTransaction(transaction)
            check(transactionId != -1L) { "Manual loan transaction could not be inserted" }
            insertLoanAndLink(
                personName = normalizedName,
                personId = personId,
                direction = direction,
                amount = amount,
                currency = normalizedCurrency,
                note = note?.trim()?.takeIf(String::isNotBlank),
                sourceTransactionId = transactionId,
            )
        }
    }

    private suspend fun insertLoanAndLink(
        personName: String,
        personId: Long?,
        direction: LoanDirection,
        amount: BigDecimal,
        currency: String,
        note: String?,
        sourceTransactionId: Long,
    ): Long {
        val sourceTransaction = transactionDao.getTransactionById(sourceTransactionId)
            ?: error("Source transaction not found")
        require(normalizeCurrency(sourceTransaction.currency) == currency) {
            "Transaction and loan currencies must match"
        }
        val person = resolvePerson(personName, personId)
        val loanId = loanDao.insertLoan(
            LoanEntity(
                personName = person.name,
                personId = person.id,
                direction = direction,
                originalAmount = amount,
                remainingAmount = amount,
                currency = currency,
                note = note?.trim()?.takeIf(String::isNotBlank),
            )
        )
        check(loanId > 0L) { "Loan could not be inserted" }
        loanDao.linkTransaction(sourceTransactionId, loanId)
        persistContributionOverride(sourceTransactionId, amount)
        return loanId
    }

    private suspend fun resolvePerson(personName: String, personId: Long?): com.pennywiseai.tracker.data.database.entity.PersonEntity {
        if (personId != null) {
            val person = personDao.getPersonById(personId) ?: error("Person not found")
            require(!person.isArchived) { "Archived person cannot receive a new record" }
            return person
        }

        val displayName = personName.trim()
        require(displayName.isNotBlank()) { "Person name cannot be blank" }
        val normalizedName = normalizePersonName(displayName)
        personDao.getActivePersonByNormalizedName(normalizedName)?.let { return it }
        // "Deleting" a person who has loan history only archives them, and nothing
        // in the UI can bring them back. Reuse (and revive) the archived row rather
        // than creating a duplicate that orphans their existing loans.
        personDao.getArchivedPersonByNormalizedName(normalizedName)?.let { archived ->
            val revived = archived.copy(isArchived = false, updatedAt = LocalDateTime.now())
            personDao.updatePerson(revived)
            return revived
        }
        val newPerson = com.pennywiseai.tracker.data.database.entity.PersonEntity(
            name = displayName,
            normalizedName = normalizedName,
        )
        val id = personDao.insertPerson(newPerson)
        return newPerson.copy(id = id)
    }

    /**
     * Stores `loan_contribution` on the transaction only when the value differs
     * from the transaction's own amount. Equal contributions are treated as
     * "use the full amount" and left null so legacy data and the common case
     * stay unchanged.
     */
    private suspend fun persistContributionOverride(transactionId: Long, contribution: BigDecimal) {
        val txn = transactionDao.getTransactionById(transactionId) ?: return
        val override = if (contribution.compareTo(txn.amount) == 0) null else contribution
        if (txn.loanContribution == override) return
        transactionDao.updateTransaction(
            txn.copy(loanContribution = override, updatedAt = LocalDateTime.now())
        )
    }

    /**
     * Link [transactionId] to [loanId] as a repayment. When [contribution] is set
     * and differs from the transaction's own amount, only that portion counts
     * toward the loan's repaid total (via `loan_contribution`); a null
     * contribution preserves the legacy "full transaction amount" behaviour.
     */
    suspend fun recordRepayment(
        loanId: Long,
        transactionId: Long,
        contribution: BigDecimal? = null
    ) {
        contribution?.let { require(it > BigDecimal.ZERO) { "Repayment contribution must be positive" } }
        database.withTransaction {
            val loan = loanDao.getLoanById(loanId) ?: error("Loan not found")
            val transaction = transactionDao.getTransactionById(transactionId)
                ?: error("Transaction not found")
            require(normalizeCurrency(transaction.currency) == normalizeCurrency(loan.currency)) {
                "Transaction and loan currencies must match"
            }
            loanDao.linkTransaction(transactionId, loanId)
            if (contribution != null) {
                persistContributionOverride(transactionId, contribution)
            }
            recalculateRemaining(loanId)
        }
    }

    suspend fun recordManualRepayment(
        loanId: Long,
        amount: BigDecimal,
        personName: String,
        currency: String
    ): Long {
        require(amount > BigDecimal.ZERO) { "Repayment amount must be positive" }
        return database.withTransaction {
            val loan = loanDao.getLoanById(loanId) ?: return@withTransaction -1L
            require(normalizeCurrency(currency) == normalizeCurrency(loan.currency)) {
                "Repayment and loan currencies must match"
            }
            val txType = if (loan.direction == LoanDirection.LENT)
                TransactionType.INCOME else TransactionType.EXPENSE
            val transaction = TransactionEntity(
                amount = amount,
                merchantName = loan.personName,
                category = if (txType == TransactionType.INCOME) "Income" else "Others",
                transactionType = txType,
                dateTime = LocalDateTime.now(),
                description = "Loan repayment – ${loan.personName}",
                transactionHash = "loan_repayment_${loanId}_${System.currentTimeMillis()}",
                currency = normalizeCurrency(currency),
                loanId = loanId
            )
            val txId = transactionDao.insertTransaction(transaction)
            recalculateRemaining(loanId)
            txId
        }
    }

    suspend fun unlinkTransaction(transactionId: Long, loanId: Long) {
        loanDao.unlinkTransaction(transactionId)
        val loan = loanDao.getLoanById(loanId) ?: return
        // Recompute the principal from the remaining CONTRIBUTION transactions
        // (the loan's own direction: LENT->EXPENSE, BORROWED->INCOME). Repayments
        // are the opposite type and don't count toward principal.
        val contributionType = if (loan.direction == LoanDirection.LENT) "EXPENSE" else "INCOME"
        val remainingPrincipal = loanDao.getTotalRepaidByType(loanId, contributionType)
        if (remainingPrincipal.compareTo(BigDecimal.ZERO) == 0) {
            // No principal left — the loan is meaningless. Delete it; deleteLoan's
            // unlinkAllTransactions also detaches any leftover repayment rows so
            // they aren't stranded on a zeroed-out, auto-settled loan
            // (issue #444 / Greptile #445). Covers the sole-transaction case too.
            deleteLoan(loanId)
        } else {
            // Multi-entry loan with contributions remaining: shrink the principal
            // to match, so unlinking a source transaction doesn't leave an
            // inflated originalAmount/remaining.
            if (loan.originalAmount.compareTo(remainingPrincipal) != 0) {
                loanDao.updateLoan(loan.copy(originalAmount = remainingPrincipal, updatedAt = LocalDateTime.now()))
            }
            recalculateRemaining(loanId)
        }
    }

    suspend fun updateOriginalAmount(loanId: Long, newAmount: BigDecimal) {
        val loan = loanDao.getLoanById(loanId) ?: return
        loanDao.updateLoan(
            loan.copy(
                originalAmount = newAmount,
                updatedAt = LocalDateTime.now()
            )
        )
        recalculateRemaining(loanId)
    }

    suspend fun settleLoan(loanId: Long) {
        val loan = loanDao.getLoanById(loanId) ?: return
        loanDao.updateLoan(
            loan.copy(
                status = LoanStatus.SETTLED,
                remainingAmount = BigDecimal.ZERO,
                settledAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
    }

    /** Settles all [loanIds] atomically — same effect as [settleLoan] on each. */
    suspend fun settleLoans(loanIds: List<Long>) {
        loanDao.settleLoans(loanIds, LocalDateTime.now())
    }

    suspend fun reopenLoan(loanId: Long) {
        val loan = loanDao.getLoanById(loanId) ?: return
        val repaymentType = if (loan.direction == LoanDirection.LENT) "INCOME" else "EXPENSE"
        val totalRepaid = loanDao.getTotalRepaidByType(loanId, repaymentType)
        val remaining = (loan.originalAmount - totalRepaid).coerceAtLeast(BigDecimal.ZERO)
        loanDao.updateLoan(
            loan.copy(
                status = LoanStatus.ACTIVE,
                remainingAmount = remaining,
                settledAt = null,
                updatedAt = LocalDateTime.now()
            )
        )
    }

    suspend fun deleteLoan(loanId: Long) {
        database.withTransaction {
            val loan = loanDao.getLoanById(loanId) ?: return@withTransaction
            loanDao.unlinkAllTransactions(loanId)
            loanDao.deleteLoan(loan)
        }
    }

    private suspend fun recalculateRemaining(loanId: Long) {
        val loan = loanDao.getLoanById(loanId) ?: return
        val repaymentType = if (loan.direction == LoanDirection.LENT) "INCOME" else "EXPENSE"
        val totalRepaid = loanDao.getTotalRepaidByType(loanId, repaymentType)
        val remaining = (loan.originalAmount - totalRepaid).coerceAtLeast(BigDecimal.ZERO)
        val newStatus = if (remaining <= BigDecimal.ZERO) LoanStatus.SETTLED else LoanStatus.ACTIVE
        loanDao.updateLoan(
            loan.copy(
                remainingAmount = remaining,
                status = newStatus,
                settledAt = if (newStatus == LoanStatus.SETTLED) LocalDateTime.now() else null,
                updatedAt = LocalDateTime.now()
            )
        )
    }
}

data class PersonLoanSummary(
    val personId: Long,
    val personName: String,
    val lentByCurrency: Map<String, BigDecimal> = emptyMap(),
    val borrowedByCurrency: Map<String, BigDecimal> = emptyMap(),
    val netByCurrency: Map<String, BigDecimal> = emptyMap(),
    val activeLoanCount: Int = 0,
    val hasSettledLoans: Boolean = false,
)

internal fun summarizePersonLoans(
    personId: Long,
    personName: String,
    loans: List<LoanEntity>,
): PersonLoanSummary {
    val activeLoans = loans.filter { it.status == LoanStatus.ACTIVE }
    fun totals(direction: LoanDirection): Map<String, BigDecimal> = activeLoans
        .filter { it.direction == direction }
        .groupBy { normalizeCurrency(it.currency) }
        .mapValues { (_, currencyLoans) ->
            currencyLoans.fold(BigDecimal.ZERO) { total, loan -> total + loan.remainingAmount }
        }
        .filterValues { it.compareTo(BigDecimal.ZERO) != 0 }

    val lent = totals(LoanDirection.LENT)
    val borrowed = totals(LoanDirection.BORROWED)
    val net = (lent.keys + borrowed.keys).associateWith { currency ->
        (lent[currency] ?: BigDecimal.ZERO) - (borrowed[currency] ?: BigDecimal.ZERO)
    }.filterValues { it.compareTo(BigDecimal.ZERO) != 0 }

    return PersonLoanSummary(
        personId = personId,
        personName = personName.ifBlank { loans.firstOrNull()?.personName.orEmpty() },
        lentByCurrency = lent,
        borrowedByCurrency = borrowed,
        netByCurrency = net,
        activeLoanCount = activeLoans.size,
        hasSettledLoans = loans.any { it.status == LoanStatus.SETTLED },
    )
}

internal fun normalizePersonName(name: String): String = name.trim().lowercase(Locale.ROOT)

internal fun normalizeCurrency(currency: String): String = currency.trim().uppercase(Locale.ROOT)

internal fun manualLoanTransaction(
    personName: String,
    direction: LoanDirection,
    amount: BigDecimal,
    currency: String,
    note: String?,
    dateTime: LocalDateTime,
): TransactionEntity {
    val transactionType = if (direction == LoanDirection.LENT) {
        TransactionType.EXPENSE
    } else {
        TransactionType.INCOME
    }
    return TransactionEntity(
        amount = amount,
        merchantName = personName,
        category = if (transactionType == TransactionType.INCOME) "Income" else "Others",
        transactionType = transactionType,
        dateTime = dateTime,
        description = note?.trim()?.takeIf(String::isNotBlank),
        transactionHash = "manual_loan_${UUID.randomUUID()}",
        currency = currency,
        createdAt = dateTime,
        updatedAt = dateTime,
    )
}
