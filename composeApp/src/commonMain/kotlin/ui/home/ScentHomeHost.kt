package ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ui.theme.ScentTheme

// ─────────────────────────────────────────────
// In-process screen back-stack (no external nav dependency)
// ─────────────────────────────────────────────

sealed interface ScentScreen {
    data object HomeFeed : ScentScreen

    data object VideoPost : ScentScreen
}

// TODO(chore/unify-home-nav-tab-state): this `screen` state is a second, independent
// back stack living outside AppNavState/NavigationState<HomeRoute> (see
// docs/architecture-guidelines.md's per-tab navigation section) — it resets on every
// tab re-entry since it's local `remember` state, not part of the app's real nav graph.
// `topTab` below has the same duplication problem against the caller's own tab state.
@Composable
fun ScentHomeHost(
    start: ScentScreen = ScentScreen.HomeFeed,
    modifier: Modifier = Modifier,
    onNavTabSelected: (Int) -> Unit = {},
) {
    var screen by remember { mutableStateOf(start) }
    var selectedVideoUrl by remember { mutableStateOf("") }
    var topTab by remember { mutableIntStateOf(0) }
    when (screen) {
        ScentScreen.HomeFeed ->
            HomeFullBleedScreen(
                onOpenVideo = { url ->
                    selectedVideoUrl = url
                    screen = ScentScreen.VideoPost
                },
                modifier = modifier,
                initialTab = topTab,
                onTopTabSelected = { topTab = it },
                onNavTabSelected = onNavTabSelected,
            )
        ScentScreen.VideoPost ->
            VideoPostScreen(
                url = selectedVideoUrl,
                onBack = { screen = ScentScreen.HomeFeed },
                modifier = modifier,
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
        ScentHomeHost(start = ScentScreen.HomeFeed)
    }
}

@Preview(device = "spec:width=412dp,height=892dp")
@Composable
fun ScentHomeHostVideoPostPreview() {
    ScentTheme {
        ScentHomeHost(start = ScentScreen.VideoPost)
    }
}
