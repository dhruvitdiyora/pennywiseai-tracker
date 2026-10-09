package com.pennywiseai.tracker.navigation

import kotlinx.serialization.Serializable

// Define navigation destinations using Kotlin Serialization
@Serializable
object AppLock

@Serializable
object OnBoarding

@Serializable
object Permission

@Serializable
object Home

@Serializable
object Transactions

@Serializable
object Settings

@Serializable
object SettingsCurrencyFormats

@Serializable
object SettingsBackupImport

@Serializable
object SettingsPrivacySecurity

@Serializable
object SettingsAdvanced

@Serializable
object Categories

@Serializable
object Analytics

@Serializable
object Chat

@Serializable
data class TransactionDetail(val transactionId: Long)

@Serializable
data class AddTransaction(val sourceTransactionId: Long? = null)

@Serializable
data class AccountDetail(val bankName: String, val accountLast4: String)

@Serializable
object UnrecognizedSms

@Serializable
object Faq

@Serializable
object Rules

@Serializable
data class CreateRule(val ruleId: String? = null, val duplicateFromId: String? = null)

@Serializable
object ExchangeRates

@Serializable
object BudgetGroups

@Serializable
data class BudgetGroupEdit(val groupId: Long = -1L)

/**
 * Per-budget history drill-down. The (year, month) selects the period;
 * the screen renders the per-window list (Weekly sub-list or Monthly /
 * One-time window) with each row labelled "Live" or "Frozen as of …".
 */
@Serializable
data class BudgetHistory(
    val groupId: Long,
    val year: Int,
    val month: Int
)

@Serializable
data class BudgetDetail(
    val groupId: Long,
    val year: Int,
    val month: Int,
)

@Serializable
object Loans

@Serializable
object Contacts

@Serializable
object PersonalDashboard

@Serializable
data class PersonDetail(val personId: Long)

@Serializable
object RecurringTransactions

@Serializable
data class LoanDetail(val loanId: Long)

@Serializable
object TransactionGroups

@Serializable
data class TransactionGroupDetail(val groupId: Long)

@Serializable
object ImportStatement

@Serializable
data class TransactionsWithFilter(
    val category: String,
    val period: String? = null,
    val currency: String? = null
)

// Settings sub-screens reachable from the root-level Settings route (opened from
// the standalone Transactions screen). MainScreen has its own string routes for
// the same screens; these mirror them on the root nav graph.
@Serializable
object SettingsDataPrivacy

@Serializable
object SettingsAppearance

@Serializable
object SettingsAbout

@Serializable
object SettingsLicenses

@Serializable
object SettingsProfiles

@Serializable
object ManageAccounts

@Serializable
object AddAccount

@Serializable
data class BalanceHistory(val bankName: String, val accountLast4: String)
