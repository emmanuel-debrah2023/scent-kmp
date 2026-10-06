package ui.navigation

/**
 * A screen the main graph opens on instead of Home, chosen from outside the app.
 * Today only the debug-only E2E launch argument produces one; each case carries its
 * own tab's typed route, so opening it can never land a route on the wrong back stack.
 */
sealed interface StartDestination {
    data class Home(
        val route: HomeRoute,
    ) : StartDestination

    data class Search(
        val route: SearchRoute,
    ) : StartDestination

    data class Profile(
        val route: ProfileRoute,
    ) : StartDestination

    data class Marketplace(
        val route: MarketplaceRoute,
    ) : StartDestination
}
