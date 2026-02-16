package ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import scent.composeapp.generated.resources.Res
import scent.composeapp.generated.resources.ic_home
import ui.theme.BackgroundLight
import ui.theme.DeepForestGreen
import ui.theme.LightGray


@Composable
fun BottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<BottomNavItem>,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = BackgroundLight,
        contentColor = LightGray
    ) {
        tabs.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        painter = item.icon,
                        contentDescription = item.title,
                        tint = if (selectedTab == index) DeepForestGreen else LightGray
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 12.sp,
                        color = if (selectedTab == index) DeepForestGreen else LightGray
                    )
                }
            )
        }
    }
}

data class BottomNavItem(
    val title: String,
    val icon: Painter
)