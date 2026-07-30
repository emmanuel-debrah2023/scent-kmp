package ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun ScentHomeHost(
    start: ScentScreen = ScentScreen.HomeFeed,
    modifier: Modifier = Modifier,
    onNavTabSelected: (Int) -> Unit = {},
) {
    var screen by remember { mutableStateOf(start) }
    var selectedVideoUrl by remember { mutableStateOf("") }
    when (screen) {
        ScentScreen.HomeFeed ->
            HomeFullBleedScreen(
                onOpenVideo = { url ->
                    selectedVideoUrl = url
                    screen = ScentScreen.VideoPost
                },
                modifier = modifier,
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
