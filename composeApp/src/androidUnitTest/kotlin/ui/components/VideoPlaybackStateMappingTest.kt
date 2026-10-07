package ui.components

import androidx.media3.common.Player
import org.junit.Test
import kotlin.test.assertEquals

class VideoPlaybackStateMappingTest {
    @Test
    fun `an error beats playing`() {
        assertEquals(
            VideoPlaybackState.Unavailable,
            videoPlaybackStateOf(isPlaying = true, playbackState = Player.STATE_READY, hasError = true),
        )
    }

    @Test
    fun `isPlaying maps to Playing`() {
        assertEquals(
            VideoPlaybackState.Playing,
            videoPlaybackStateOf(isPlaying = true, playbackState = Player.STATE_READY, hasError = false),
        )
    }

    @Test
    fun `idle maps to Loading`() {
        assertEquals(
            VideoPlaybackState.Loading,
            videoPlaybackStateOf(isPlaying = false, playbackState = Player.STATE_IDLE, hasError = false),
        )
    }

    @Test
    fun `buffering maps to Loading`() {
        assertEquals(
            VideoPlaybackState.Loading,
            videoPlaybackStateOf(isPlaying = false, playbackState = Player.STATE_BUFFERING, hasError = false),
        )
    }

    @Test
    fun `ready but not playing maps to Paused`() {
        assertEquals(
            VideoPlaybackState.Paused,
            videoPlaybackStateOf(isPlaying = false, playbackState = Player.STATE_READY, hasError = false),
        )
    }

    @Test
    fun `ended maps to Paused`() {
        assertEquals(
            VideoPlaybackState.Paused,
            videoPlaybackStateOf(isPlaying = false, playbackState = Player.STATE_ENDED, hasError = false),
        )
    }
}
