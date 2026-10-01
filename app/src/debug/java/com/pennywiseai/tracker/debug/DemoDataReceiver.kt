package com.pennywiseai.tracker.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.pennywiseai.tracker.data.database.PennyWiseDatabase
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Debug-only demo data seeder (never in release builds), so UI work can be
 * checked on a device without scanning real SMS. Guarded by
 * android.permission.DUMP, which adb shell holds and third-party apps can't:
 *   adb shell am broadcast -n <pkg>/com.pennywiseai.tracker.debug.DemoDataReceiver -a SEED_DEMO
 * Every row is fictional. Seeding twice adds a second copy; clear the app's
 * data to start over.
 */
class DemoDataReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                seed(PennyWiseDatabase.getInstance(context.applicationContext))
                Log.i(TAG, "Seeded demo data")
            } catch (e: Exception) {
                Log.e(TAG, "Seeding demo data failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun seed(db: PennyWiseDatabase) {
        val now = LocalDateTime.now().withSecond(0).withNano(0)

        val balances = db.accountBalanceDao()
        for (daysAgo in 30 downTo 0 step 5) {
            balances.insertBalance(
                AccountBalanceEntity(
                    bankName = BANK,
                    accountLast4 = BANK_LAST4,
                    balance = BigDecimal(52_000 - daysAgo * 90 + (daysAgo % 3) * 700),
                    timestamp = now.minusDays(daysAgo.toLong()),
                    sourceType = "MANUAL",
                    accountType = "SAVINGS",
                )
            )
        }
        balances.insertBalance(
            AccountBalanceEntity(
                bankName = "Example Card",
                accountLast4 = "5678",
                balance = BigDecimal(12_450),
                creditLimit = BigDecimal(100_000),
                isCreditCard = true,
                timestamp = now,
                sourceType = "MANUAL",
                accountType = "CREDIT",
            )
        )
        balances.insertBalance(
            AccountBalanceEntity(
                bankName = "Cash",
                accountLast4 = "0000",
                balance = BigDecimal(2_500),
                timestamp = now,
                sourceType = "MANUAL",
                accountType = "CASH",
            )
        )

        val transactions = demoTransactions().mapIndexed { index, demo ->
            TransactionEntity(
                amount = BigDecimal(demo.amount),
                merchantName = demo.merchant,
                category = demo.category,
                transactionType = demo.type,
                dateTime = now.minusDays(demo.daysAgo.toLong()).minusHours((index % 9).toLong()),
                bankName = BANK,
                accountNumber = BANK_LAST4,
                isRecurring = demo.recurring,
                currency = demo.currency,
                transactionHash = "demo-${now.toLocalDate()}-$index",
            )
        }
        db.transactionDao().insertTransactions(transactions)

        val subscriptions = db.subscriptionDao()
        listOf("Netflix" to "649", "Spotify" to "119", "YouTube Premium" to "149").forEachIndexed { i, (name, amount) ->
            subscriptions.insertSubscription(
                SubscriptionEntity(
                    merchantName = name,
                    amount = BigDecimal(amount),
                    nextPaymentDate = now.toLocalDate().plusDays(3L + i * 6),
                    category = "Entertainment",
                    bankName = BANK,
                )
            )
        }

        db.loanDao().insertLoan(
            LoanEntity(
                personName = "Alex Example",
                direction = LoanDirection.LENT,
                originalAmount = BigDecimal(3_000),
                remainingAmount = BigDecimal(1_800),
            )
        )
    }

    private data class Demo(
        val merchant: String,
        val category: String,
        val amount: String,
        val daysAgo: Int,
        val type: TransactionType = TransactionType.EXPENSE,
        val recurring: Boolean = false,
        val currency: String = "INR",
    )

    private fun demoTransactions(): List<Demo> = listOf(
        Demo("Salary", "Salary", "85000", 0, TransactionType.INCOME),
        Demo("Swiggy", "Food & Dining", "420", 0),
        Demo("Blinkit", "Groceries", "250", 0),
        Demo("Uber", "Transportation", "310", 1),
        Demo("Netflix", "Entertainment", "649", 2, recurring = true),
        Demo("Spotify", "Entertainment", "119", 2, recurring = true),
        Demo("Amazon", "Shopping", "1899", 3),
        Demo("Starbucks", "Food & Dining", "380", 4),
        Demo("Electricity Board", "Bills & Utilities", "1450", 5),
        Demo("Zomato", "Food & Dining", "560", 6),
        Demo("Big Bazaar", "Groceries", "2210", 8),
        Demo("Rapido", "Transportation", "95", 9),
        Demo("Apollo Pharmacy", "Healthcare", "740", 11),
        Demo("Freelance Payout", "Salary", "12000", 12, TransactionType.INCOME),
        Demo("PVR Cinemas", "Entertainment", "900", 14),
        Demo("Airtel", "Mobile", "599", 15, recurring = true),
        Demo("Myntra", "Shopping", "2499", 18),
        Demo("Steam", "Entertainment", "15", 20, currency = "USD"),
        Demo("Rent", "Bills & Utilities", "18000", 26),
        Demo("Dmart", "Groceries", "1830", 33),
        Demo("Swiggy", "Food & Dining", "610", 36),
        Demo("Salary", "Salary", "85000", 31, TransactionType.INCOME),
        Demo("Indian Oil", "Transportation", "2000", 40),
        Demo("Amazon", "Shopping", "999", 44),
    )

    private companion object {
        const val TAG = "DemoData"
        const val BANK = "Example Bank"
        const val BANK_LAST4 = "1234"
    }
}
