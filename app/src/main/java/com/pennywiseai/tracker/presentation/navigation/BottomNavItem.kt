package com.pennywiseai.tracker.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )
    
    data object Analytics : BottomNavItem(
        route = "analytics",
        title = "Analytics",
        icon = Icons.Default.Analytics
    )
    
    /**
     * Replaced the Chat tab (D4). Chat was a third of the navigation while being unusable
     * until a model download, and everything else the app can do — Subscriptions, Loans,
     * Budgets, Categories, Rules, Groups, Import, Exchange Rates, Settings — had no home in
     * the nav at all. Chat now lives inside More.
     */
    data object More : BottomNavItem(
        route = "more",
        title = "More",
        icon = Icons.Default.GridView
    )
}