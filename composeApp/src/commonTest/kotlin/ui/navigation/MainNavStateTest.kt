package ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainNavStateTest {
    @Test
    fun `open selects the destination's tab at its root`() {
        val nav = MainNavState()

        nav.open(StartDestination.Marketplace(MarketplaceRoute.Listings))

        assertEquals(Tab.MARKETPLACE, nav.selectedTab)
        assertEquals(MarketplaceRoute.Listings, nav.marketplaceNav.current.value)
        assertFalse(nav.marketplaceNav.canGoBack)
    }

    @Test
    fun `open pushes a nested route above its tab root so back returns to the root`() {
        val nav = MainNavState()

        nav.open(StartDestination.Profile(ProfileRoute.CreateListing))

        assertEquals(Tab.PROFILE, nav.selectedTab)
        assertEquals(ProfileRoute.CreateListing, nav.profileNav.current.value)
        assertTrue(nav.goBack())
        assertEquals(ProfileRoute.Profile, nav.profileNav.current.value)
    }

    @Test
    fun `open leaves the other tabs at their roots`() {
        val nav = MainNavState()

        nav.open(StartDestination.Profile(ProfileRoute.CreateListing))

        assertEquals(HomeRoute.Feed, nav.homeNav.current.value)
        assertFalse(nav.homeNav.canGoBack)
    }
}
