package ui.navigation

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.painterResource
import scent.composeapp.generated.resources.Res
import scent.composeapp.generated.resources.ic_home
import ui.components.BottomNavItem

@Composable
fun getBottomNavTabs() = listOf(
    BottomNavItem("Home", painterResource(Res.drawable.ic_home)),
    BottomNavItem("Search", painterResource(Res.drawable.ic_home)),
    BottomNavItem("Profile", painterResource(Res.drawable.ic_home))
)
