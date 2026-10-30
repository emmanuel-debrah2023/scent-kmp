package ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.scent.project.domain.model.AuthUser
import ui.accessibility.accessiblePane
import ui.ask.AskScreen
import ui.components.BackButton
import ui.components.BlendedScaffold
import ui.components.ScentTabRow
import ui.home.ScentHomeHost
import ui.listing.CreateListingScreen
import ui.listing.EditListingScreen
import ui.marketplace.MarketplaceScreen
import ui.profile.EditProfileScreen
import ui.profile.ProfileFollowersScreen
import ui.profile.ProfileFollowingScreen
import ui.profile.ProfileNavActions
import ui.profile.ProfileScreen
import ui.profile.SettingsScreen
import ui.theme.ScentThemeExtras

@Composable
fun MainGraph(
    user: AuthUser,
    nav: MainNavState,
    onLogout: () -> Unit,
) {
    MainBackHandler(nav)
    when (nav.selectedTab) {
        Tab.HOME -> {
            val homeRoute by nav.homeNav.current
            ScentHomeHost(
                route = homeRoute,
                onOpenVideo = { url -> nav.homeNav.navigateTo(HomeRoute.VideoDetail(url)) },
                onBack = { nav.onBackPressed() },
                onNavTabSelected = { nav.selectTab(Tab.entries[it]) },
            )
        }
        else -> {
            val snackbarHostState = remember { SnackbarHostState() }
            BlendedScaffold(
                selectedTab = nav.selectedTab.ordinal,
                onTabSelected = { nav.selectTab(Tab.entries[it]) },
                snackbarHostState = snackbarHostState,
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (nav.selectedTab) {
                        Tab.HOME -> Unit
                        Tab.SEARCH ->
                            SearchNavHost(
                                nav = nav.searchNav,
                                subTab = nav.searchSubTab,
                                onSubTabSelected = { nav.searchSubTab = it },
                            )
                        Tab.PROFILE ->
                            ProfileNavHost(nav.profileNav, user, onLogout, snackbarHostState)
                        Tab.MARKETPLACE -> MarketplaceNavHost(nav.marketplaceNav, snackbarHostState)
                    }
                }
            }
        }
    }
}

/** Disabled at Home root so the system plays its own back-to-home animation. */
@Composable
internal fun MainBackHandler(nav: MainNavState) {
    SystemBackHandler(enabled = nav.canHandleBack) { nav.onBackPressed() }
}

// ─────────────────────────────────────────────
// Per-tab nav hosts
// Each reads current route via derivedStateOf for scoped recomposition.
// Replace the when block with NavDisplay when adopting Nav3.
// ─────────────────────────────────────────────

@Composable
private fun SearchNavHost(
    nav: NavigationState<SearchRoute>,
    subTab: SearchSubTab,
    onSubTabSelected: (SearchSubTab) -> Unit,
) {
    val current by nav.current
    when (current) {
        is SearchRoute.Search -> SearchRoot(subTab, onSubTabSelected)
        is SearchRoute.FragranceDetail -> PlaceholderScreen("Fragrance Detail", onBack = { nav.goBack() })
        is SearchRoute.ListingDetail -> PlaceholderScreen("Listing Detail", onBack = { nav.goBack() })
        is SearchRoute.UserProfile -> PlaceholderScreen("User Profile", onBack = { nav.goBack() })
    }
}

@Composable
private fun SearchRoot(
    subTab: SearchSubTab,
    onSubTabSelected: (SearchSubTab) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScentTabRow(
            labels = SearchSubTab.entries.map { it.label },
            selectedIndex = subTab.ordinal,
            onTabSelected = { onSubTabSelected(SearchSubTab.entries[it]) },
            modifier = Modifier.padding(horizontal = ScentThemeExtras.spacing.screenPadding),
        )
        when (subTab) {
            SearchSubTab.SEARCH -> PlaceholderRootScreen("Search")
            SearchSubTab.ASK -> AskScreen(modifier = Modifier.weight(1f))
        }
    }
}

private val SearchSubTab.label: String
    get() =
        when (this) {
            SearchSubTab.SEARCH -> "Search"
            SearchSubTab.ASK -> "Ask"
        }

@Composable
private fun ProfileNavHost(
    nav: NavigationState<ProfileRoute>,
    user: AuthUser,
    onLogout: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val current by nav.current
    when (val route = current) {
        is ProfileRoute.Profile ->
            ProfileScreen(
                authUser = user,
                navActions =
                    ProfileNavActions(
                        onNavigateToSettings = { nav.navigateTo(ProfileRoute.Settings) },
                        onNavigateToEditProfile = { nav.navigateTo(ProfileRoute.EditProfile) },
                        onNavigateToFollowers = { nav.navigateTo(ProfileRoute.Followers) },
                        onNavigateToFollowing = { nav.navigateTo(ProfileRoute.Following) },
                        onNavigateToFragrance = { id -> nav.navigateTo(ProfileRoute.FragranceDetail(id)) },
                        onCreateListing = { nav.navigateTo(ProfileRoute.CreateListing) },
                        onEditListing = { listingId -> nav.navigateTo(ProfileRoute.EditListing(listingId)) },
                    ),
                snackbarHostState = snackbarHostState,
            )

        is ProfileRoute.CreateListing ->
            CreateListingScreen(
                onBack = { nav.goBack() },
                onCreated = { nav.goBack() },
            )
        is ProfileRoute.EditListing ->
            EditListingScreen(
                listingId = route.listingId,
                onBack = { nav.goBack() },
                onSaved = { nav.goBack() },
            )
        is ProfileRoute.Settings ->
            SettingsScreen(
                userId = user.id,
                onLogout = onLogout,
                onBack = { nav.goBack() },
                onEditProfile = { nav.navigateTo(ProfileRoute.EditProfile) },
            )
        is ProfileRoute.EditProfile ->
            EditProfileScreen(
                userId = user.id,
                onBack = { nav.goBack() },
                snackbarHostState = snackbarHostState,
            )
        is ProfileRoute.Followers ->
            ProfileFollowersScreen(
                userId = user.id,
                onBack = { nav.goBack() },
            )
        is ProfileRoute.Following ->
            ProfileFollowingScreen(
                userId = user.id,
                onBack = { nav.goBack() },
            )
        is ProfileRoute.FragranceDetail -> PlaceholderScreen("Fragrance Detail", onBack = { nav.goBack() })
    }
}

@Composable
private fun MarketplaceNavHost(
    nav: NavigationState<MarketplaceRoute>,
    snackbarHostState: SnackbarHostState,
) {
    val current by nav.current
    when (current) {
        is MarketplaceRoute.Listings ->
            MarketplaceScreen(
                onListingClick = { id -> nav.navigateTo(MarketplaceRoute.ListingDetail(id)) },
                snackbarHostState = snackbarHostState,
            )
        is MarketplaceRoute.ListingDetail -> PlaceholderScreen("Listing Detail", onBack = { nav.goBack() })
        is MarketplaceRoute.FragranceDetail -> PlaceholderScreen("Fragrance Detail", onBack = { nav.goBack() })
    }
}

// ─────────────────────────────────────────────
// Stubs — replaced by real screens in feature tickets
// ─────────────────────────────────────────────

@Composable
internal fun PlaceholderScreen(
    name: String,
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().accessiblePane(name)) {
        BackButton(onBack = onBack, modifier = Modifier.align(Alignment.TopStart))
        PlaceholderTitle(name, Modifier.align(Alignment.Center))
    }
}

@Composable
private fun PlaceholderRootScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PlaceholderTitle(name)
    }
}

@Composable
private fun PlaceholderTitle(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = name,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
