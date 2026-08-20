package com.pennywiseai.tracker.data.preferences

/**
 * The panels Home can be composed from.
 *
 * Home is a user-arranged stack rather than a fixed screen: each entry here can be switched
 * off and reordered (see [HomePanelLayout]). That is also how features stop being buried —
 * a user who cares about Loans can pull them onto Home instead of digging through More.
 *
 * **The [name] of each entry is persisted.** Renaming one silently resets that panel to its
 * default position for every existing user; add new entries instead, and see
 * [HomePanelLayout.decode] for how unknown and missing ids are handled.
 *
 * **Every entry here must be something Home can actually render.** Declaration order is the
 * default order, and it deliberately matches the order Home used before it became
 * panel-driven, so a user who never opens the settings screen sees exactly what they saw
 * before. Panels planned but not built (Needs review, Spending by category, Calendar,
 * Insights) are absent on purpose: listing a toggle that does nothing is worse than not
 * offering it. `decode` appends new entries with their default, so adding them later is safe.
 *
 * [defaultEnabled] should be false for anything not obviously worth the space on a first run.
 */
enum class HomePanel(
    val title: String,
    val description: String,
    val defaultEnabled: Boolean = true,
) {
    MONTH_SUMMARY(
        title = "Month summary",
        description = "What you've spent this month, your balance and accounts",
    ),
    CASH_FLOW(
        title = "Cash flow",
        description = "Money in against money out — hides itself in a quiet month",
    ),
    BUDGET_PROGRESS(
        title = "Budgets",
        description = "How much of each budget is used, and what's left",
    ),
    LOANS(
        title = "Loans & EMIs",
        description = "Money lent and borrowed, and what's outstanding",
    ),
    RECENT_TRANSACTIONS(
        title = "Recent transactions",
        description = "Your latest transactions, newest first",
    ),
    ACCOUNTS(
        title = "Accounts",
        description = "Balance of each account you're tracking",
    ),
    SUBSCRIPTIONS_DUE(
        title = "Subscriptions due",
        description = "Renewals coming up in the next few days",
    ),
    ACTIVITY_HEATMAP(
        title = "Activity",
        description = "A year of spending activity at a glance",
    ),
}

/** One panel's user-chosen state. */
data class HomePanelState(
    val panel: HomePanel,
    val enabled: Boolean,
)

/**
 * The ordered, enabled/disabled list of Home panels, and its storage format.
 *
 * Persisted as a single string — `MONTH_SUMMARY:1,NEEDS_REVIEW:0,…` — because the list is
 * always read and written whole, so a preference per panel would buy nothing but bookkeeping.
 */
object HomePanelLayout {

    private const val ENTRY_SEPARATOR = ","
    private const val FIELD_SEPARATOR = ":"

    /** The layout a user who has never touched the panel settings sees. */
    val DEFAULT: List<HomePanelState> =
        HomePanel.entries.map { HomePanelState(it, it.defaultEnabled) }

    fun encode(states: List<HomePanelState>): String =
        states.joinToString(ENTRY_SEPARATOR) { state ->
            "${state.panel.name}$FIELD_SEPARATOR${if (state.enabled) "1" else "0"}"
        }

    /**
     * Reads a stored layout, tolerating both directions of version skew:
     *
     * - **Unknown ids are dropped** — a panel removed in a later version, or a downgrade
     *   reading a newer string, must not crash or leave a hole.
     * - **Missing ids are appended** in declaration order with their [HomePanel.defaultEnabled]
     *   value, so a panel added in a later version shows up for existing users instead of
     *   being invisible until they reset their layout.
     *
     * A null or malformed string yields [DEFAULT].
     */
    fun decode(stored: String?): List<HomePanelState> {
        if (stored.isNullOrBlank()) return DEFAULT

        val known = HomePanel.entries.associateBy { it.name }
        val seen = LinkedHashMap<HomePanel, Boolean>()

        stored.split(ENTRY_SEPARATOR).forEach { entry ->
            val parts = entry.split(FIELD_SEPARATOR)
            if (parts.size != 2) return@forEach
            val panel = known[parts[0].trim()] ?: return@forEach
            // Anything that isn't an explicit "0" counts as enabled, so a truncated or
            // hand-edited value fails towards showing the panel rather than hiding it.
            if (panel !in seen) seen[panel] = parts[1].trim() != "0"
        }

        if (seen.isEmpty()) return DEFAULT

        HomePanel.entries.forEach { panel ->
            if (panel !in seen) seen[panel] = panel.defaultEnabled
        }

        return seen.map { (panel, enabled) -> HomePanelState(panel, enabled) }
    }

    /** Moves the panel at [from] to [to], clamping to the list bounds. */
    fun reorder(states: List<HomePanelState>, from: Int, to: Int): List<HomePanelState> {
        if (from !in states.indices) return states
        val target = to.coerceIn(0, states.lastIndex)
        if (from == target) return states
        return states.toMutableList().apply { add(target, removeAt(from)) }
    }

    fun setEnabled(
        states: List<HomePanelState>,
        panel: HomePanel,
        enabled: Boolean
    ): List<HomePanelState> = states.map {
        if (it.panel == panel) it.copy(enabled = enabled) else it
    }
}
