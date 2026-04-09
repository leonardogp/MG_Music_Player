package com.lg.monkeymusicplayer.core.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import com.lg.monkeymusicplayer.core.tracker.StatTracker
import com.lg.monkeymusicplayer.data.model.Song
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@UnstableApi
class MusicPlayerManagerTest {
    private lateinit var musicPlayerManager: MusicPlayerManager
    private val context: Context = mock()
    private val mockApplicationContext: Context = mock()
    private val statTracker: StatTracker = mock()

    @Before
    fun setup() {
        org.mockito.kotlin.whenever(context.applicationContext).thenReturn(mockApplicationContext)
        musicPlayerManager = MusicPlayerManager(context, statTracker)
    }

    @Test
    fun testPlaybackControls() {
        // MusicPlayerManager uses StateFlows for state
        musicPlayerManager.pause()
        assertFalse(musicPlayerManager.isPlaying.value)
    }

    @Test
    fun testShuffle() {
        // Just verifying the call doesn't crash, as actual behavior depends on MediaController
        musicPlayerManager.toggleShuffle()
    }

    @Test
    fun testRepeatMode() {
        // Just verifying the call doesn't crash
        musicPlayerManager.cycleRepeatMode()
    }

    @Test
    fun testSeeking() {
        musicPlayerManager.seekTo(30000L)
        assertEquals(30000L, musicPlayerManager.currentPosition.value)
    }

    @Test
    fun testQueueManagement() {
        val song = Song(
            id = 1L,
            albumId = 10L,
            title = "Title",
            artist = "Artist",
            album = "Album",
            genre = "Genre",
            folder = "Folder",
            path = "Path",
            albumArtUri = "uri",
            isFavorite = false,
            lyricsPath = null
        )
        musicPlayerManager.addToQueue(song)
        // In this unit test, controller is null so it won't actually add to a real queue
        // but we verify the method can be called.
    }
}
