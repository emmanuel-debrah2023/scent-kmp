package ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoPlaybackDescriptionTest {
    @Test
    fun `each playback state has its own spoken description`() {
        assertEquals("Video, loading", videoPlaybackDescription(VideoPlaybackState.Loading))
        assertEquals("Video, playing", videoPlaybackDescription(VideoPlaybackState.Playing))
        assertEquals("Video, paused", videoPlaybackDescription(VideoPlaybackState.Paused))
        assertEquals("Video, unavailable", videoPlaybackDescription(VideoPlaybackState.Unavailable))
    }
}
