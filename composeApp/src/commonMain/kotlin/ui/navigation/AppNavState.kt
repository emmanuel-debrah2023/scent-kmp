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
class AppNavState(
    startDestination: StartDestination? = null,
) {
    val authNav = NavigationState<AuthRoute>(AuthRoute.Login)
    val mainNav = MainNavState().apply { startDestination?.let(::open) }
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

    /** Selects the destination's tab and pushes its route; a tab root is a no-op push. */
    fun open(destination: StartDestination) {
        when (destination) {
            is StartDestination.Home -> {
                selectedTab = Tab.HOME
                homeNav.navigateTo(destination.route)
            }
            is StartDestination.Search -> {
                selectedTab = Tab.SEARCH
                searchNav.navigateTo(destination.route)
            }
            is StartDestination.Profile -> {
                selectedTab = Tab.PROFILE
                profileNav.navigateTo(destination.route)
            }
            is StartDestination.Marketplace -> {
                selectedTab = Tab.MARKETPLACE
                marketplaceNav.navigateTo(destination.route)
            }
        }
    }
}
