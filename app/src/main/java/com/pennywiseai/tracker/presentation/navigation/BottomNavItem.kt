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
}
