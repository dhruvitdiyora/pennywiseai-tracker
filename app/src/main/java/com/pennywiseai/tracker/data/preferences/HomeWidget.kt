package com.pennywiseai.tracker.data.preferences

import androidx.annotation.StringRes
import com.pennywiseai.tracker.R

enum class HomeWidget(@StringRes val titleRes: Int, val defaultOrder: Int) {
    BALANCE(R.string.home_widget_balance, 0), CASH_FLOW(R.string.home_widget_cash_flow, 1),
    BUDGETS(R.string.home_widget_budgets, 2), LOANS(R.string.home_widget_loans, 3),
    GROUPS(R.string.home_widget_groups, 4), RECENT_TRANSACTIONS(R.string.home_widget_recent_transactions, 5),
    ACCOUNTS(R.string.home_widget_accounts, 6),
    UPCOMING_SUBSCRIPTIONS(R.string.home_widget_upcoming_subscriptions, 7),
    ACTIVITY_HEATMAP(R.string.home_widget_activity, 8);
    companion object {
        val defaults = entries.sortedBy { it.defaultOrder }
        fun decode(stored: String?): List<HomeWidget> {
            val known = stored.orEmpty().split(',').mapNotNull { raw -> entries.firstOrNull { it.name == raw.trim() } }.distinct()
            return known + defaults.filterNot { it in known }
        }
    }
}
