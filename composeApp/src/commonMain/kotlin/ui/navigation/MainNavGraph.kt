package ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.scent.project.domain.model.AuthUser
import ui.components.BlendedScaffold
import ui.home.ScentHomeHost
import ui.listing.CreateListingScreen
import ui.listing.EditListingScreen
import ui.marketplace.MarketplaceScreen
import ui.profile.ProfileScreen

@Composable
fun MainGraph(
    user: AuthUser,
    nav: MainNavState,
    onLogout: () -> Unit,
) {
    when (nav.selectedTab) {
        Tab.HOME ->
            ScentHomeHost(
                onNavTabSelected = { nav.selectedTab = Tab.entries[it] },
            )
        else ->
            BlendedScaffold(
                selectedTab = nav.selectedTab.ordinal,
                onTabSelected = { nav.selectedTab = Tab.entries[it] },
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (nav.selectedTab) {
                        Tab.HOME -> Unit
                        Tab.SEARCH -> SearchNavHost(nav.searchNav)
                        Tab.PROFILE -> ProfileNavHost(nav.profileNav, user, onLogout)
                        Tab.MARKETPLACE -> MarketplaceNavHost(nav.marketplaceNav)
                    }
                }
            }
    }
}

// ─────────────────────────────────────────────
// Per-tab nav hosts
// Each reads current route via derivedStateOf for scoped recomposition.
// Replace the when block with NavDisplay when adopting Nav3.
// ─────────────────────────────────────────────

@Composable
private fun SearchNavHost(nav: NavigationState<SearchRoute>) {
    val current by nav.current
    when (current) {
        is SearchRoute.Search -> PlaceholderScreen("Search")
        is SearchRoute.FragranceDetail -> PlaceholderScreen("Fragrance Detail")
        is SearchRoute.ListingDetail -> PlaceholderScreen("Listing Detail")
        is SearchRoute.UserProfile -> PlaceholderScreen("User Profile")
    }
}

@Composable
private fun ProfileNavHost(
    nav: NavigationState<ProfileRoute>,
    user: AuthUser,
    onLogout: () -> Unit,
) {
    val current by nav.current
    when (val route = current) {
        is ProfileRoute.Profile ->
            ProfileScreen(
                authUser = user,
                onLogout = onLogout,
                onCreateListing = { nav.navigateTo(ProfileRoute.CreateListing) },
                onEditListing = { listingId -> nav.navigateTo(ProfileRoute.EditListing(listingId)) },
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
    }
}

@Composable
private fun MarketplaceNavHost(nav: NavigationState<MarketplaceRoute>) {
    val current by nav.current
    when (current) {
        is MarketplaceRoute.Listings ->
            MarketplaceScreen(
                onListingClick = { id -> nav.navigateTo(MarketplaceRoute.ListingDetail(id)) },
            )
        is MarketplaceRoute.ListingDetail -> PlaceholderScreen("Listing Detail")
        is MarketplaceRoute.FragranceDetail -> PlaceholderScreen("Fragrance Detail")
    }
}

// ─────────────────────────────────────────────
// Stubs — replaced by real screens in feature tickets
// ─────────────────────────────────────────────

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
