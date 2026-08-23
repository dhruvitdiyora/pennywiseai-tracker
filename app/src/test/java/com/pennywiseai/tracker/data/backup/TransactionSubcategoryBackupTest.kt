package com.pennywiseai.tracker.data.backup

import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Schema 60→61 added `transactions.subcategory`. A backup written before it has
 * no such key, and must still import (hard constraint 3, #414).
 *
 * The two new DAO queries are not covered here: Room verifies their SQL at
 * compile time, and this module has no Robolectric, so there is no database to
 * run them against. What *is* worth asserting is the serialisation boundary,
 * which nothing else checks.
 */
class TransactionSubcategoryBackupTest {

    private fun transaction(subcategory: String? = null) = TransactionEntity(
        id = 1,
        amount = BigDecimal("450.00"),
        merchantName = "SWIGGY",
        category = "Food & Dining",
        subcategory = subcategory,
        transactionType = TransactionType.EXPENSE,
        dateTime = LocalDateTime.of(2026, 1, 4, 10, 15, 30),
        smsBody = "",
        transactionHash = "hash-1"
    )

    @Test
    fun `a transaction round-trips with no subcategory`() {
        val json = backupJson.encodeToString(TransactionEntity.serializer(), transaction())
        val restored = backupJson.decodeFromString(TransactionEntity.serializer(), json)

        assertNull(restored.subcategory)
        assertEquals("Food & Dining", restored.category)
    }

    @Test
    fun `a transaction round-trips with a subcategory`() {
        val original = transaction(subcategory = "Tea & Coffee")
        val json = backupJson.encodeToString(TransactionEntity.serializer(), original)
        val restored = backupJson.decodeFromString(TransactionEntity.serializer(), json)

        assertEquals("Tea & Coffee", restored.subcategory)
    }

    @Test
    fun `a pre-61 transaction with no subcategory key imports as null`() {
        val pre61 = """
            {
              "id": 1,
              "amount": "450.00",
              "merchantName": "SWIGGY",
              "category": "Food & Dining",
              "transactionType": "EXPENSE",
              "dateTime": "2026-01-04T10:15:30",
              "smsBody": "",
              "transactionHash": "hash-1"
            }
        """.trimIndent()

        val restored = backupJson.decodeFromString(TransactionEntity.serializer(), pre61)

        assertNull("a pre-61 row must import, with no subcategory", restored.subcategory)
        assertEquals("SWIGGY", restored.merchantName)
    }

    @Test
    fun `a subcategory name that no longer resolves is still preserved`() {
        // Resolution is by name and is *allowed to miss* — the column is not a
        // foreign key precisely so a renamed or deleted subcategory cannot break
        // a restore. The stale name is kept rather than nulled, so renaming the
        // subcategory back makes the rows light up again.
        val restored = backupJson.decodeFromString(
            TransactionEntity.serializer(),
            backupJson.encodeToString(TransactionEntity.serializer(), transaction("Deleted Thing"))
        )

        assertEquals("Deleted Thing", restored.subcategory)
    }
}
