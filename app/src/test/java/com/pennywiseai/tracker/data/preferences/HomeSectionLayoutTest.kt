package com.pennywiseai.tracker.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSectionLayoutTest {

    @Test
    fun `round trips a reordered layout with a hidden flag`() {
        val layout = listOf(
            HomeSection.ACTIVITY to true,
            HomeSection.SAFE_TO_SPEND to false,
            HomeSection.GROUPS to false,
            HomeSection.BUDGETS to true,
            HomeSection.GOALS to false,
            HomeSection.LOANS to true,
            HomeSection.RECENT_TRANSACTIONS to false,
            HomeSection.ACCOUNTS to true,
            HomeSection.SUBSCRIPTIONS to true,
            HomeSection.MONEY_IN_MOTION to false,
        )
        val encoded = HomeSectionLayout.encode(layout)
        assertEquals("ACTIVITY,!SAFE_TO_SPEND,!GROUPS,BUDGETS,!GOALS,LOANS,!RECENT_TRANSACTIONS,ACCOUNTS,SUBSCRIPTIONS,!MONEY_IN_MOTION", encoded)
        assertEquals(layout, HomeSectionLayout.decode(encoded))
    }

    @Test
    fun `unknown names are dropped`() {
        val decoded = HomeSectionLayout.decode("SAFE_TO_SPEND,BUDGETS,WEATHER,!ACTIVITY")
        assertEquals(HomeSection.BUDGETS to true, decoded[1])
        assertEquals(HomeSection.ACTIVITY to false, decoded.single { it.first == HomeSection.ACTIVITY })
        assertEquals(HomeSection.entries.size, decoded.size)
    }

    @Test
    fun `missing sections are added visible at their default position`() {
        val decoded = HomeSectionLayout.decode("!ACTIVITY,ACCOUNTS")
        assertEquals(
            listOf(
                HomeSection.SAFE_TO_SPEND, HomeSection.BUDGETS, HomeSection.RECENT_TRANSACTIONS,
                HomeSection.SUBSCRIPTIONS, HomeSection.LOANS, HomeSection.GOALS, HomeSection.GROUPS,
                HomeSection.MONEY_IN_MOTION,
            ).map { it to true }.let { it.take(3) + listOf(HomeSection.ACTIVITY to false) + it.drop(3) } +
                listOf(HomeSection.ACCOUNTS to true),
            decoded,
        )
    }

    @Test
    fun `money in motion joins a saved layout late, not above the user's sections`() {
        // A layout saved before Money in motion was a section (it used to be a fixed card on top).
        val saved = "SAFE_TO_SPEND,BUDGETS,GOALS,LOANS,GROUPS,RECENT_TRANSACTIONS,ACCOUNTS,SUBSCRIPTIONS,ACTIVITY"
        val decoded = HomeSectionLayout.decode(saved).map { it.first.name }
        assertEquals(saved.split(','), decoded - "MONEY_IN_MOTION")
        assertEquals(8, decoded.indexOf("MONEY_IN_MOTION"))
    }

    @Test
    fun `a section added in an update lands where a new user sees it`() {
        // A layout saved before Safe to spend existed: user moved Activity up, hid Groups.
        val decoded = HomeSectionLayout.decode(
            "ACTIVITY,BUDGETS,GOALS,LOANS,!GROUPS,RECENT_TRANSACTIONS,ACCOUNTS,SUBSCRIPTIONS"
        )
        assertEquals(HomeSection.SAFE_TO_SPEND to true, decoded.first())
        assertEquals(
            listOf("ACTIVITY", "BUDGETS", "GOALS", "LOANS", "!GROUPS", "RECENT_TRANSACTIONS", "ACCOUNTS", "SUBSCRIPTIONS"),
            decoded.drop(1).filter { it.first != HomeSection.MONEY_IN_MOTION }.map { (s, v) -> (if (v) "" else "!") + s.name },
        )
    }

    @Test
    fun `blank or absent pref yields the default layout`() {
        assertEquals(HomeSectionLayout.DEFAULT, HomeSectionLayout.decode(null))
        assertEquals(HomeSectionLayout.DEFAULT, HomeSectionLayout.decode("  "))
        assertEquals(
            listOf(
                HomeSection.SAFE_TO_SPEND, HomeSection.RECENT_TRANSACTIONS, HomeSection.GOALS, HomeSection.ACCOUNTS,
                HomeSection.SUBSCRIPTIONS, HomeSection.BUDGETS, HomeSection.LOANS, HomeSection.GROUPS,
                HomeSection.MONEY_IN_MOTION, HomeSection.ACTIVITY,
            ).map { it to true },
            HomeSectionLayout.DEFAULT,
        )
        // Every section appears exactly once.
        assertEquals(HomeSection.entries.toSet(), HomeSectionLayout.DEFAULT.map { it.first }.toSet())
        assertEquals(HomeSection.entries.size, HomeSectionLayout.DEFAULT.size)
    }

    @Test
    fun `a stored layout in the previous default order decodes unchanged`() {
        val stored = "BUDGETS,LOANS,GROUPS,RECENT_TRANSACTIONS,ACCOUNTS,SUBSCRIPTIONS,ACTIVITY"
        assertEquals(
            listOf(
                // Sections added since land visible at their declared position.
                HomeSection.SAFE_TO_SPEND,
                HomeSection.BUDGETS, HomeSection.LOANS, HomeSection.GROUPS,
                HomeSection.RECENT_TRANSACTIONS, HomeSection.ACCOUNTS,
                HomeSection.GOALS,
                HomeSection.SUBSCRIPTIONS,
                HomeSection.MONEY_IN_MOTION,
                HomeSection.ACTIVITY,
            ).map { it to true },
            HomeSectionLayout.decode(stored),
        )
    }
}
