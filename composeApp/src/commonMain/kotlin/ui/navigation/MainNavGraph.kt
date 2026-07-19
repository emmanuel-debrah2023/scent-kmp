package ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.scent.project.domain.model.AuthUser
import ui.components.BottomNavigation

@Composable
fun MainGraph(
    user: AuthUser,
    nav: MainNavState,
    onLogout: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            BottomNavigation(
                selectedTab = nav.selectedTab.ordinal,
                onTabSelected = { nav.selectedTab = Tab.entries[it] },
                tabs = mainNavTabs(),
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (nav.selectedTab) {
                Tab.HOME -> HomeNavHost(nav.homeNav)
                Tab.SEARCH -> SearchNavHost(nav.searchNav)
                Tab.PROFILE -> ProfileNavHost(nav.profileNav, user, onLogout)
                Tab.MARKETPLACE -> MarketplaceNavHost(nav.marketplaceNav)
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
private fun HomeNavHost(nav: NavigationState<HomeRoute>) {
    val current by nav.current
    when (current) {
        is HomeRoute.Feed -> HomeScreenStub()
        is HomeRoute.PostDetail -> PlaceholderScreen("Post Detail")
        is HomeRoute.FragranceDetail -> PlaceholderScreen("Fragrance Detail")
        is HomeRoute.UserProfile -> PlaceholderScreen("User Profile")
    }
}

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
    when (current) {
        is ProfileRoute.Profile -> ProfileScreenStub(user, onLogout)
    }
}

@Composable
private fun MarketplaceNavHost(nav: NavigationState<MarketplaceRoute>) {
    val current by nav.current
    when (current) {
        is MarketplaceRoute.Listings -> PlaceholderScreen("Marketplace")
        is MarketplaceRoute.ListingDetail -> PlaceholderScreen("Listing Detail")
        is MarketplaceRoute.FragranceDetail -> PlaceholderScreen("Fragrance Detail")
    }
}

// ─────────────────────────────────────────────
// Stubs — replaced by real screens in feature tickets
// ─────────────────────────────────────────────

@Composable
private fun HomeScreenStub() {
    PlaceholderScreen("Feed")
}

@Composable
private fun ProfileScreenStub(
    user: AuthUser,
    onLogout: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onLogout) { Text("Logout") }
        }
    }
}

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
