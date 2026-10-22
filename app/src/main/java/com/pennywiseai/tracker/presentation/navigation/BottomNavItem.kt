package com.pennywiseai.tracker.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.icons.iconax.AiCommentary
import com.pennywiseai.tracker.ui.icons.iconax.FavoriteChart
import com.pennywiseai.tracker.ui.icons.iconax.Home
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.ReceiptItem

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    data object Home : BottomNavItem(
        route = "home",
        titleRes = R.string.nav_home,
        icon = Iconax.Home
    )

    data object Transactions : BottomNavItem(
        route = "transactions",
        titleRes = R.string.nav_transactions,
        icon = Iconax.ReceiptItem
    )

    data object Analytics : BottomNavItem(
        route = "analytics",
        titleRes = R.string.nav_analytics,
        icon = Iconax.FavoriteChart
    )

    data object Chat : BottomNavItem(
        route = "chat",
        titleRes = R.string.nav_chat,
        icon = Iconax.AiCommentary
    )

    companion object {
        // Getters, not vals: a companion val naming the subclasses can be read
        // before they're initialised and come back null.
        /** Every tab, in bar order. */
        val all: List<BottomNavItem> get() = listOf(Home, Transactions, Analytics, Chat)

        /** Tabs the user can hide from the bar (#912). Home always stays. */
        val hideable: List<BottomNavItem> get() = all - Home

        /** The tabs to draw, given the routes the user hid. */
        fun visible(hiddenRoutes: Set<String>): List<BottomNavItem> =
            all.filter { it == Home || it.route !in hiddenRoutes }

        /**
         * Where to go when [route] is on screen right after the user hid tabs:
         * Home if its tab is now hidden, else null (stay put).
         */
        fun fallbackFor(route: String, hiddenRoutes: Set<String>): String? =
            Home.route.takeIf { route != Home.route && route in hiddenRoutes }
    }
}
