package ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import ui.components.BottomNavItem

fun mainNavTabs(): List<BottomNavItem> =
    listOf(
        BottomNavItem(title = "Home", icon = Icons.Rounded.Home),
        BottomNavItem(title = "Search", icon = Icons.Rounded.Search),
        BottomNavItem(title = "Profile", icon = Icons.Rounded.AccountCircle),
        BottomNavItem(title = "Market", icon = Icons.Rounded.AddBusiness),
    )
