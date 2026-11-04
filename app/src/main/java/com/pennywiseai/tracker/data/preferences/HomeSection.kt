package com.pennywiseai.tracker.data.preferences

/**
 * The toggleable / reorderable sections of the Home screen (#770). The fixed
 * header (balance, share prompt, cash-flow) is deliberately not here.
 *
 * Declaration order is NOT the display order: [HomeSectionLayout.DEFAULT] is.
 * Declaration order only decides where a section missing from a stored layout
 * is appended by [HomeSectionLayout.decode], so keep it stable - reordering it
 * would shift existing users' saved layouts.
 */
enum class HomeSection(val label: String) {
    SAFE_TO_SPEND("Safe to spend"),
    BUDGETS("Budgets"),
    RECENT_TRANSACTIONS("Recent Transactions"),
    ACCOUNTS("Bank Accounts"),
    SUBSCRIPTIONS("Upcoming Subscriptions"),
    LOANS("Loans"),
    GOALS("Goals"),
    GROUPS("Groups"),
    MONEY_IN_MOTION("Money in motion"),
    ACTIVITY("Activity"),
}

/**
 * Codec for the `home_sections` preference: comma-separated section names in
 * display order, a leading `!` marking a hidden one, e.g. `BUDGETS,!GROUPS,…`.
 * Pure so it is unit-testable without DataStore.
 */
object HomeSectionLayout {
    /**
     * Order for installs with no saved layout (Cashiro's): recent transactions
     * right after the summary card, then accounts and subscriptions, then the
     * rest. Only affects a blank preference - a stored layout decodes as saved.
     */
    val DEFAULT: List<Pair<HomeSection, Boolean>> = listOf(
        HomeSection.SAFE_TO_SPEND,
        HomeSection.RECENT_TRANSACTIONS,
        HomeSection.GOALS,
        HomeSection.ACCOUNTS,
        HomeSection.SUBSCRIPTIONS,
        HomeSection.BUDGETS,
        HomeSection.LOANS,
        HomeSection.GROUPS,
        HomeSection.MONEY_IN_MOTION,
        HomeSection.ACTIVITY,
    ).map { it to true }

    fun encode(layout: List<Pair<HomeSection, Boolean>>): String =
        layout.joinToString(",") { (section, visible) -> (if (visible) "" else "!") + section.name }

    /**
     * Unknown names are dropped; enum values absent from the string are inserted
     * visible at their default position (clamped to the end), so a section added
     * in a later version shows up where a new user would see it.
     */
    fun decode(raw: String?): List<Pair<HomeSection, Boolean>> {
        if (raw.isNullOrBlank()) return DEFAULT
        val parsed = raw.split(',').mapNotNull { token ->
            val t = token.trim()
            val hidden = t.startsWith("!")
            HomeSection.entries.firstOrNull { it.name == t.removePrefix("!") }?.let { it to !hidden }
        }.distinctBy { it.first }
        val seen = parsed.mapTo(HashSet()) { it.first }
        val layout = parsed.toMutableList()
        HomeSection.entries.filter { it !in seen }
            .forEach { layout.add(it.ordinal.coerceAtMost(layout.size), it to true) }
        return layout
    }
}
