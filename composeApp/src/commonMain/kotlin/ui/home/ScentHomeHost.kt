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
    onOpenFragrance: (fragranceId: Int) -> Unit,
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
        // TODO(feature/get-profile-by-id-endpoint): another user's profile can't be loaded yet, so the
        // author row from VideoPostScreen lands on a placeholder.
        is HomeRoute.UserProfile -> PlaceholderScreen("User Profile", onBack = onBack)
        // TODO(feature/fragrance-detail-a2ui-screen): the Fragrances tab can now open a fragrance, but its
        // detail screen does not exist yet.
        is HomeRoute.FragranceDetail -> PlaceholderScreen("Fragrance Detail", onBack = onBack)
        // Nothing pushes PostDetail yet; it falls back to the feed rather than a blank screen.
        is HomeRoute.Feed,
        is HomeRoute.PostDetail,
        ->
            HomeFullBleedScreen(
                onOpenVideo = onOpenVideo,
                onOpenFragrance = onOpenFragrance,
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
            onOpenFragrance = {},
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
            onOpenFragrance = {},
            onOpenUserProfile = {},
            onBack = {},
        )
    }
}
