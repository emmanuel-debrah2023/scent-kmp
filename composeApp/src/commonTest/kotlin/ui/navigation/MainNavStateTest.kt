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
        assertTrue(nav.onBackPressed())
        assertEquals(ProfileRoute.Profile, nav.profileNav.current.value)
    }

    @Test
    fun `open leaves the other tabs at their roots`() {
        val nav = MainNavState()

        nav.open(StartDestination.Profile(ProfileRoute.CreateListing))

        assertEquals(HomeRoute.Feed, nav.homeNav.current.value)
        assertFalse(nav.homeNav.canGoBack)
    }

    @Test
    fun `back pops within a non-Home tab and lands on its root`() {
        val nav = MainNavState()
        nav.open(StartDestination.Profile(ProfileRoute.Settings))

        assertTrue(nav.onBackPressed())

        assertEquals(Tab.PROFILE, nav.selectedTab)
        assertEquals(ProfileRoute.Profile, nav.profileNav.current.value)
    }

    @Test
    fun `back at a non-Home root switches to Home and leaves that tab's stack untouched`() {
        val nav = MainNavState()
        nav.open(StartDestination.Marketplace(MarketplaceRoute.Listings))

        assertTrue(nav.onBackPressed())

        assertEquals(Tab.HOME, nav.selectedTab)
        assertEquals(MarketplaceRoute.Listings, nav.marketplaceNav.current.value)
    }

    @Test
    fun `back at Home root is not handled and the handler is disabled`() {
        val nav = MainNavState()

        assertFalse(nav.canHandleBack)
        assertFalse(nav.onBackPressed())
        assertEquals(Tab.HOME, nav.selectedTab)
    }

    @Test
    fun `canHandleBack is true when Home has a pushed screen`() {
        val nav = MainNavState()
        nav.homeNav.navigateTo(HomeRoute.VideoDetail("https://example.com/v.m3u8"))

        assertTrue(nav.canHandleBack)
    }

    @Test
    fun `canHandleBack is true on a non-Home tab at its root`() {
        val nav = MainNavState()
        nav.selectTab(Tab.SEARCH)

        assertTrue(nav.canHandleBack)
    }

    @Test
    fun `video detail is pushed on the Home stack and back pops it`() {
        val nav = MainNavState()

        nav.homeNav.navigateTo(HomeRoute.VideoDetail("u"))
        assertTrue(nav.onBackPressed())

        assertEquals(HomeRoute.Feed, nav.homeNav.current.value)
        assertFalse(nav.canHandleBack)
    }

    @Test
    fun `re-tapping the active tab pops it to root and keeps the tab`() {
        val nav = MainNavState()
        nav.open(StartDestination.Profile(ProfileRoute.Settings))
        nav.profileNav.navigateTo(ProfileRoute.EditProfile)

        nav.selectTab(Tab.PROFILE)

        assertEquals(Tab.PROFILE, nav.selectedTab)
        assertEquals(ProfileRoute.Profile, nav.profileNav.current.value)
        assertFalse(nav.profileNav.canGoBack)
    }

    @Test
    fun `re-tapping the Home tab pops its pushed video to the feed`() {
        val nav = MainNavState()
        nav.homeNav.navigateTo(HomeRoute.VideoDetail("u"))

        nav.selectTab(Tab.HOME)

        assertEquals(HomeRoute.Feed, nav.homeNav.current.value)
        assertEquals(Tab.HOME, nav.selectedTab)
    }

    @Test
    fun `re-tapping at root is a no-op`() {
        val nav = MainNavState()

        nav.selectTab(Tab.HOME)

        assertEquals(Tab.HOME, nav.selectedTab)
        assertEquals(HomeRoute.Feed, nav.homeNav.current.value)
        assertFalse(nav.homeNav.canGoBack)
    }

    @Test
    fun `selecting another tab keeps both stacks`() {
        val nav = MainNavState()
        nav.homeNav.navigateTo(HomeRoute.VideoDetail("u"))

        nav.selectTab(Tab.PROFILE)
        nav.profileNav.navigateTo(ProfileRoute.Settings)
        nav.selectTab(Tab.HOME)

        assertEquals(HomeRoute.VideoDetail("u"), nav.homeNav.current.value)
        assertEquals(ProfileRoute.Settings, nav.profileNav.current.value)
    }
}
