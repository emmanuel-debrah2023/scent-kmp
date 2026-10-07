package ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
actual fun VideoPlayer(
    url: String,
    thumbnailUrl: String?,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val exoPlayer =
        remember(url) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = true
                prepare()
            }
        }

    var playback by remember(url) { mutableStateOf(VideoPlaybackState.Loading) }

    DisposableEffect(exoPlayer) {
        val listener =
            object : Player.Listener {
                override fun onEvents(
                    player: Player,
                    events: Player.Events,
                ) {
                    playback = videoPlaybackStateOf(player.isPlaying, player.playbackState, player.playerError != null)
                }
            }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    VideoPlaybackFrame(state = playback, modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

internal fun videoPlaybackStateOf(
    isPlaying: Boolean,
    playbackState: Int,
    hasError: Boolean,
): VideoPlaybackState =
    when {
        hasError -> VideoPlaybackState.Unavailable
        isPlaying -> VideoPlaybackState.Playing
        playbackState == Player.STATE_IDLE || playbackState == Player.STATE_BUFFERING -> VideoPlaybackState.Loading
        else -> VideoPlaybackState.Paused
    }
