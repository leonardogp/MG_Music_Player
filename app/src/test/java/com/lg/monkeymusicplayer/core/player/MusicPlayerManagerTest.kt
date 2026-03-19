import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MusicPlayerManagerTest {
    private lateinit var musicPlayerManager: MusicPlayerManager

    @Before
    fun setup() {
        musicPlayerManager = MusicPlayerManager()
    }

    @Test
    fun testPlaybackControls() {
        musicPlayerManager.play()
        assertTrue(musicPlayerManager.isPlaying)
        musicPlayerManager.pause()
        assertFalse(musicPlayerManager.isPlaying)
        musicPlayerManager.stop()
        assertFalse(musicPlayerManager.isPlaying)
    }

    @Test
    fun testShuffle() {
        musicPlayerManager.enableShuffle()
        assertTrue(musicPlayerManager.isShuffleEnabled)
        musicPlayerManager.disableShuffle()
        assertFalse(musicPlayerManager.isShuffleEnabled)
    }

    @Test
    fun testRepeatMode() {
        musicPlayerManager.setRepeatMode(RepeatMode.ALL)
        assertEquals(RepeatMode.ALL, musicPlayerManager.repeatMode)
        musicPlayerManager.setRepeatMode(RepeatMode.NONE)
        assertEquals(RepeatMode.NONE, musicPlayerManager.repeatMode)
    }

    @Test
    fun testSeeking() {
        musicPlayerManager.loadTrack("track1")
        musicPlayerManager.seekTo(30)
        assertEquals(30, musicPlayerManager.currentPosition)
    }

    @Test
    fun testQueueManagement() {
        musicPlayerManager.addToQueue("track1")
        musicPlayerManager.addToQueue("track2")
        assertEquals(2, musicPlayerManager.queue.size)
        musicPlayerManager.removeFromQueue("track1")
        assertEquals(1, musicPlayerManager.queue.size)
        assertFalse(musicPlayerManager.queue.contains("track1"))
    }
}