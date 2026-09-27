package com.pennywiseai.tracker.presentation.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywiseai.tracker.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomNavItemTest {
    private val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Transactions,
        BottomNavItem.Analytics,
        BottomNavItem.Chat,
    )

    @Test
    fun bottomNavigationItemsKeepStableRoutesAndLocalizedLabels() {
        assertEquals(
            listOf("home", "transactions", "analytics", "chat"),
            items.map { it.route },
        )
        assertEquals(
            listOf(
                R.string.nav_home,
                R.string.nav_transactions,
                R.string.nav_analytics,
                R.string.nav_chat,
            ),
            items.map { it.titleRes },
        )
        assertTrue(items.all { it.titleRes != 0 })
        assertEquals(items.size, items.map { it.route }.toSet().size)
        assertEquals(items.size, items.map { it.titleRes }.toSet().size)
    }

    @Test
    fun navigationItemsHaveDistinctIcons() {
        assertEquals(items.size, items.map { it.icon }.toSet().size)
    }
}
