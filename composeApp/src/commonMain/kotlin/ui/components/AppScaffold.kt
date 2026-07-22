package ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import ui.navigation.mainNavTabs

/**
 * Reusable scaffold that wires [TopAppBar] and [BottomNavigation] together.
 *
 * Dependency-free: no ViewModel, no NavigationState. Callers supply the
 * selected-tab index and a callback; content is rendered inside the
 * scaffold's inner-padding [Box].
 *
 * @param selectedTab   Index of the currently active tab.
 * @param onTabSelected Called when the user taps a bottom-nav item.
 * @param onSearchClick Forwarded to [TopAppBar]; defaults to no-op.
 * @param onNotificationsClick Forwarded to [TopAppBar]; defaults to no-op.
 * @param onProfileClick Forwarded to [TopAppBar]; defaults to no-op.
 * @param tabs          Nav items shown in the bottom bar; defaults to [mainNavTabs].
 * @param content       Screen content, receives scaffold [PaddingValues].
 */
@Composable
fun AppScaffold(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onSearchClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    tabs: List<BottomNavItem> = mainNavTabs(),
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                onSearchClick = onSearchClick,
                onNotificationsClick = onNotificationsClick,
                onProfileClick = onProfileClick,
            )
        },
        bottomBar = {
            BottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                tabs = tabs,
            )
        },
    ) { innerPadding ->
        content(innerPadding)
    }
}
