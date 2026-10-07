package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ui.accessibility.clearedDescription
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

enum class VideoPlaybackState { Loading, Playing, Paused, Unavailable }

fun videoPlaybackDescription(state: VideoPlaybackState): String =
    when (state) {
        VideoPlaybackState.Loading -> "Video, loading"
        VideoPlaybackState.Playing -> "Video, playing"
        VideoPlaybackState.Paused -> "Video, paused"
        VideoPlaybackState.Unavailable -> "Video, unavailable"
    }

/**
 * Exposes a video's playback state as one accessibility node. The state lives in the
 * content description (not stateDescription) so TalkBack and Maestro both read it.
 */
@Composable
internal fun VideoPlaybackFrame(
    state: VideoPlaybackState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.clearedDescription(videoPlaybackDescription(state)), content = content)
}

@Preview
@Composable
private fun VideoPlaybackFramePreview() {
    ScentTheme {
        VideoPlaybackFrame(
            state = VideoPlaybackState.Playing,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(ScentThemeExtras.spacing.cardHeroHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(text = videoPlaybackDescription(VideoPlaybackState.Playing))
        }
    }
}
