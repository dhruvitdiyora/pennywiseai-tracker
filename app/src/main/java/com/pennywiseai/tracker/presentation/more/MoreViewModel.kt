package com.pennywiseai.tracker.presentation.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.repository.AccountBalanceRepository
import com.pennywiseai.tracker.data.repository.BudgetGroupRepository
import com.pennywiseai.tracker.data.repository.CategoryRepository
import com.pennywiseai.tracker.data.repository.LoanRepository
import com.pennywiseai.tracker.data.repository.SubscriptionRepository
import com.pennywiseai.tracker.data.repository.TransactionGroupRepository
import com.pennywiseai.tracker.data.repository.UnrecognizedSmsRepository
import com.pennywiseai.tracker.domain.repository.RuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Live status for each tile on the More hub.
 *
 * The point of the hub is that a tile says what's *in* there before you tap it — "8 active"
 * beats "Subscriptions ›". A null means "not loaded yet", which the tile renders as blank
 * rather than as a misleading "0".
 */
data class MoreUiState(
    val activeSubscriptions: Int? = null,
    val activeLoans: Int? = null,
    val budgetGroups: Int? = null,
    val transactionGroups: Int? = null,
    val unreviewedSms: Int? = null,
    val categories: Int? = null,
    val accounts: Int? = null,
    val rules: Int? = null,
)

@HiltViewModel
class MoreViewModel @Inject constructor(
    subscriptionRepository: SubscriptionRepository,
    loanRepository: LoanRepository,
    budgetGroupRepository: BudgetGroupRepository,
    transactionGroupRepository: TransactionGroupRepository,
    unrecognizedSmsRepository: UnrecognizedSmsRepository,
    categoryRepository: CategoryRepository,
    accountBalanceRepository: AccountBalanceRepository,
    ruleRepository: RuleRepository,
) : ViewModel() {

    // combine() caps out at 5 flows per overload, so the counts are gathered in two
    // batches and merged. Each source is already a cheap COUNT or a small list.
    private val countsA = combine(
        subscriptionRepository.getActiveSubscriptions().map { it.size },
        loanRepository.getActiveLoanCount(),
        budgetGroupRepository.getActiveGroups().map { it.size },
        transactionGroupRepository.getAllGroups().map { it.size },
    ) { subs, loans, budgets, groups -> listOf(subs, loans, budgets, groups) }

    private val countsB = combine(
        unrecognizedSmsRepository.getUnreportedCount(),
        categoryRepository.getAllCategories().map { it.size },
        accountBalanceRepository.getAccountCount(),
        ruleRepository.getAllRules().map { rules -> rules.count { it.isActive } },
    ) { sms, categories, accounts, rules -> listOf(sms, categories, accounts, rules) }

    val uiState: StateFlow<MoreUiState> = combine(countsA, countsB) { a, b ->
        MoreUiState(
            activeSubscriptions = a[0],
            activeLoans = a[1],
            budgetGroups = a[2],
            transactionGroups = a[3],
            unreviewedSms = b[0],
            categories = b[1],
            accounts = b[2],
            rules = b[3],
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoreUiState())
}
