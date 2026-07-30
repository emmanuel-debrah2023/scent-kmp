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
) {
    var screen by remember { mutableStateOf(start) }
    when (screen) {
        ScentScreen.HomeFeed ->
            HomeFullBleedScreen(
                onOpenVideo = { screen = ScentScreen.VideoPost },
                modifier = modifier,
            )
        ScentScreen.VideoPost ->
            VideoPostScreen(
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
