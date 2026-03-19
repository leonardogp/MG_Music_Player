import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MusicViewModelTest {
    private lateinit var musicViewModel: MusicViewModel

    @Before
    fun setUp() {
        musicViewModel = MusicViewModel() // Initialize MusicViewModel
    }

    @Test
    fun testSearchFunctionality() {
        musicViewModel.search("test song")
        // Verify that search results contain expected song
        assertTrue(musicViewModel.searchResults.contains("test song"))
    }

    @Test
    fun testAddToPlaylist() {
        musicViewModel.addToPlaylist("test song")
        // Verify that the playlist contains the added song
        assertTrue(musicViewModel.playlist.contains("test song"))
    }

    @Test
    fun testRemoveFromPlaylist() {
        musicViewModel.addToPlaylist("test song")
        musicViewModel.removeFromPlaylist("test song")
        // Verify that the song is removed from the playlist
        assertFalse(musicViewModel.playlist.contains("test song"))
    }

    @Test
    fun testToggleFavorite() {
        val song = "test song"
        musicViewModel.toggleFavorite(song)
        // Verify that the song is toggled as favorite
        assertTrue(musicViewModel.favorites.contains(song))
        musicViewModel.toggleFavorite(song)
        // Verify that the song is no longer favorite
        assertFalse(musicViewModel.favorites.contains(song))
    }

    @Test
    fun testPlaySong() {
        val song = "test song"
        musicViewModel.playSong(song)
        // Verify that the current song is the one being played
        assertEquals(song, musicViewModel.currentSong)
    }

    @Test
    fun testPauseSong() {
        val song = "test song"
        musicViewModel.playSong(song)
        musicViewModel.pauseSong()
        // Verify that the song is paused
        assertFalse(musicViewModel.isPlaying)
    }

    @Test
    fun testStopSong() {
        val song = "test song"
        musicViewModel.playSong(song)
        musicViewModel.stopSong()
        // Verify that playback has stopped
        assertNull(musicViewModel.currentSong)
    }
}