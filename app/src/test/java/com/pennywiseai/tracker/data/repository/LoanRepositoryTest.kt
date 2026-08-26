package com.pennywiseai.tracker.data.repository

import com.pennywiseai.tracker.data.database.dao.LoanDao
import com.pennywiseai.tracker.data.database.dao.TransactionDao
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.time.LocalDateTime

class LoanRepositoryTest {

    @Test
    fun `active loan lookup keeps currencies separate`() = runBlocking {
        val inrLoan = loan(id = 1, currency = "INR")
        val dao = fakeLoanDao(inrLoan)
        val repository = LoanRepository(dao, fakeTransactionDao())

        assertEquals(inrLoan, repository.findActiveLoanForPerson("Person A", LoanDirection.LENT, "INR"))
        assertNull(repository.findActiveLoanForPerson("Person A", LoanDirection.LENT, "USD"))
    }

    @Test
    fun `mixed currency contribution cannot merge into existing loan`() = runBlocking {
        val inrLoan = loan(id = 1, currency = "INR", originalAmount = "100")
        val usdTransaction = transaction(id = 9, currency = "USD", amount = "10")
        val loanDaoProbe = LoanDaoProbe()
        val loanDao = fakeLoanDao(inrLoan, loanDaoProbe)
        val transactionDao = fakeTransactionDao(usdTransaction)
        val repository = LoanRepository(loanDao, transactionDao)

        assertCurrencyMismatch {
            repository.addToExistingLoan(1, BigDecimal("10"), usdTransaction.id)
        }

        assertNull(loanDaoProbe.updatedLoan)
        assertNull(loanDaoProbe.linkedTransactionId)
    }

    @Test
    fun `mixed currency repayment cannot link to existing loan`() = runBlocking {
        val inrLoan = loan(id = 1, currency = "INR", originalAmount = "100")
        val usdTransaction = transaction(id = 9, currency = "USD", amount = "10", type = TransactionType.INCOME)
        val loanDaoProbe = LoanDaoProbe()
        val loanDao = fakeLoanDao(inrLoan, loanDaoProbe)
        val transactionDao = fakeTransactionDao(usdTransaction)
        val repository = LoanRepository(loanDao, transactionDao)

        assertCurrencyMismatch {
            repository.recordRepayment(1, usdTransaction.id, BigDecimal("10"))
        }

        assertNull(loanDaoProbe.linkedTransactionId)
    }

    private fun loan(
        id: Long,
        currency: String,
        originalAmount: String = "100"
    ) = LoanEntity(
        id = id,
        personName = "Person A",
        personId = "person-a",
        direction = LoanDirection.LENT,
        originalAmount = BigDecimal(originalAmount),
        remainingAmount = BigDecimal(originalAmount),
        currency = currency
    )

    private fun transaction(
        id: Long,
        currency: String,
        amount: String,
        type: TransactionType = TransactionType.EXPENSE
    ) = TransactionEntity(
        id = id,
        amount = BigDecimal(amount),
        merchantName = "Person A",
        category = "Others",
        transactionType = type,
        dateTime = LocalDateTime.of(2026, 1, 1, 0, 0),
        description = "loan test",
        transactionHash = "loan-test-$id",
        currency = currency
    )

    private class LoanDaoProbe {
        var updatedLoan: LoanEntity? = null
        var linkedTransactionId: Long? = null
    }

    private fun fakeLoanDao(initialLoan: LoanEntity, probe: LoanDaoProbe? = null): LoanDao {
        var currentLoan: LoanEntity? = initialLoan
        return Proxy.newProxyInstance(
            LoanDao::class.java.classLoader,
            arrayOf(LoanDao::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getLoanById" -> if (args!![0] == initialLoan.id) currentLoan else null
                "getActiveLoanByPersonAndDirection" -> {
                    val personName = args!![0] as String
                    val direction = args[1] as String
                    val currency = args[2] as String
                    currentLoan?.takeIf {
                        it.personName == personName &&
                            it.direction.name == direction &&
                            it.currency == currency &&
                            it.status.name == "ACTIVE"
                    }
                }
                "updateLoan" -> {
                    currentLoan = args!![0] as LoanEntity
                    probe?.updatedLoan = currentLoan
                    Unit
                }
                "linkTransaction" -> {
                    probe?.linkedTransactionId = args!![0] as Long
                    Unit
                }
                "getTotalRepaidByType" -> BigDecimal.ZERO
                else -> defaultValue(method.returnType)
            }
        } as LoanDao
    }

    private fun fakeTransactionDao(vararg transactions: TransactionEntity): TransactionDao {
        val rows = transactions.associateBy { it.id }
        return Proxy.newProxyInstance(
            TransactionDao::class.java.classLoader,
            arrayOf(TransactionDao::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getTransactionById" -> rows[args!![0] as Long]
                "updateTransaction" -> Unit
                else -> defaultValue(method.returnType)
            }
        } as TransactionDao
    }

    private fun defaultValue(returnType: Class<*>): Any? = when {
        returnType == Boolean::class.javaPrimitiveType -> false
        returnType == Int::class.javaPrimitiveType -> 0
        returnType == Long::class.javaPrimitiveType -> 0L
        returnType == Double::class.javaPrimitiveType -> 0.0
        returnType == Float::class.javaPrimitiveType -> 0.0f
        returnType == Short::class.javaPrimitiveType -> 0.toShort()
        returnType == Byte::class.javaPrimitiveType -> 0.toByte()
        returnType == Char::class.javaPrimitiveType -> '\u0000'
        returnType == Void.TYPE -> Unit
        Flow::class.java.isAssignableFrom(returnType) -> flowOf<Any>()
        else -> null
    }

    private suspend fun assertCurrencyMismatch(block: suspend () -> Unit) {
        try {
            block()
        } catch (_: IllegalArgumentException) {
            return
        }
        throw AssertionError("expected the mixed-currency operation to be rejected")
    }

}
