package ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Top-level navigation state for the entire app.
 *
 * [authNav] is active while the user is unauthenticated.
 * [mainNav] is active once authenticated; it owns one [NavigationState]
 * per [Tab] so switching tabs preserves each tab's back stack position.
 */
class AppNavState {
    val authNav = NavigationState<AuthRoute>(AuthRoute.Login)
    val mainNav = MainNavState()
}

class MainNavState {
    var selectedTab: Tab by mutableStateOf(Tab.HOME)

    val homeNav = NavigationState<HomeRoute>(HomeRoute.Feed)
    val searchNav = NavigationState<SearchRoute>(SearchRoute.Search)
    val profileNav = NavigationState<ProfileRoute>(ProfileRoute.Profile)
    val marketplaceNav = NavigationState<MarketplaceRoute>(MarketplaceRoute.Listings)

    /** The active tab's back stack — used to route back-press to the right stack. */
    val activeNav: NavigationState<*>
        get() =
            when (selectedTab) {
                Tab.HOME -> homeNav
                Tab.SEARCH -> searchNav
                Tab.PROFILE -> profileNav
                Tab.MARKETPLACE -> marketplaceNav
            }

    fun goBack(): Boolean = activeNav.goBack()
}
