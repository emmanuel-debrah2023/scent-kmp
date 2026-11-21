package ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ui.navigation.HomeRoute
import ui.navigation.PlaceholderScreen
import ui.theme.ScentTheme

// TODO(chore/unify-home-nav-tab-state): `topTab` below is a second, independent copy of the Fragrances/
// Community choice living outside AppNavState, so it resets if this host leaves composition. The
// feed/video screen itself now lives on MainNavState.homeNav as HomeRoute.
@Composable
fun ScentHomeHost(
    route: HomeRoute,
    onOpenVideo: (postId: String) -> Unit,
    onOpenUserProfile: (userId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavTabSelected: (Int) -> Unit = {},
) {
    var topTab by remember { mutableIntStateOf(0) }
    when (route) {
        is HomeRoute.VideoDetail ->
            VideoPostScreen(
                postId = route.postId,
                onBack = onBack,
                onOpenAuthor = onOpenUserProfile,
                modifier = modifier,
            )
        is HomeRoute.UserProfile -> PlaceholderScreen("User Profile", onBack = onBack)
        // Nothing pushes the other Home routes yet; they fall back to the feed rather than a blank screen.
        is HomeRoute.Feed,
        is HomeRoute.PostDetail,
        is HomeRoute.FragranceDetail,
        ->
            HomeFullBleedScreen(
                onOpenVideo = onOpenVideo,
                modifier = modifier,
                initialTab = topTab,
                onTopTabSelected = { topTab = it },
                onNavTabSelected = onNavTabSelected,
            )
    }
}

// ─────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────

@Preview(device = "spec:width=412dp,height=892dp")
@Composable
fun ScentHomeHostHomeFeedPreview() {
    ScentTheme {
        ScentHomeHost(
            route = HomeRoute.Feed,
            onOpenVideo = {},
            onOpenUserProfile = {},
            onBack = {},
        )
    }
}

@Preview(device = "spec:width=412dp,height=892dp")
@Composable
fun ScentHomeHostVideoPostPreview() {
    ScentTheme {
        ScentHomeHost(
            route = HomeRoute.VideoDetail("preview"),
            onOpenVideo = {},
            onOpenUserProfile = {},
            onBack = {},
        )
    }
}
