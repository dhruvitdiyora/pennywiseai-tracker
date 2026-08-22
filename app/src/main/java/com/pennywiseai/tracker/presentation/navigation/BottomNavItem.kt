package com.pennywiseai.tracker.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.icons.iconsax.FavoriteChart
import com.pennywiseai.tracker.ui.icons.iconsax.Home
import com.pennywiseai.tracker.ui.icons.iconsax.Iconsax
import com.pennywiseai.tracker.ui.icons.iconsax.Messages
import com.pennywiseai.tracker.ui.icons.iconsax.ReceiptItem

/**
 * Titles are string resources, not literals: the bottom bar is the most-read
 * text in the app and there is no reason for it to be the least translatable.
 */
sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    data object Home : BottomNavItem(
        route = "home",
        titleRes = R.string.nav_home,
        icon = Iconsax.Home
    )

    data object Transactions : BottomNavItem(
        route = "transactions",
        titleRes = R.string.nav_transactions,
        icon = Iconsax.ReceiptItem
    )

    data object Analytics : BottomNavItem(
        route = "analytics",
        titleRes = R.string.nav_analytics,
        icon = Iconsax.FavoriteChart
    )

    data object Chat : BottomNavItem(
        route = "chat",
        titleRes = R.string.nav_chat,
        icon = Iconsax.Messages
    )
}
