import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.example.repository.MusicRepository
import com.example.models.Song

class MusicRepositoryTest {

    private lateinit var musicRepository: MusicRepository

    @Before
    fun setUp() {
        musicRepository = MusicRepository() // Initialize with required dependencies
    }

    @Test
    fun testToggleFavorite() {
        val song = Song(id = 1, title = "Test Song", artist = "Test Artist")
        musicRepository.addSong(song)
        assertFalse(musicRepository.isFavorite(song.id))
        musicRepository.toggleFavorite(song.id)
        assertTrue(musicRepository.isFavorite(song.id))
        musicRepository.toggleFavorite(song.id)
        assertFalse(musicRepository.isFavorite(song.id))
    }

    @Test
    fun testHistory() {
        val song1 = Song(id = 1, title = "Song 1", artist = "Artist 1")
        val song2 = Song(id = 2, title = "Song 2", artist = "Artist 2")
        musicRepository.addSong(song1)
        musicRepository.addSong(song2)

        musicRepository.play(song1.id)
        musicRepository.play(song2.id)

        val history = musicRepository.getHistory()
        assertEquals(2, history.size)
        assertTrue(history.contains(song1))
        assertTrue(history.contains(song2))
    }

    @Test
    fun testPlaylists() {
        val song = Song(id = 1, title = "Song", artist = "Artist")
        val playlistName = "My Playlist"
        musicRepository.createPlaylist(playlistName)
        musicRepository.addSongToPlaylist(playlistName, song)

        val playlist = musicRepository.getPlaylist(playlistName)
        assertTrue(playlist.contains(song))
    }

    @Test
    fun testRetrieveSongs() {
        val song1 = Song(id = 1, title = "Song 1", artist = "Artist 1")
        val song2 = Song(id = 2, title = "Song 2", artist = "Artist 2")
        musicRepository.addSong(song1)
        musicRepository.addSong(song2)

        val songs = musicRepository.getAllSongs()
        assertEquals(2, songs.size)
        assertTrue(songs.contains(song1))
        assertTrue(songs.contains(song2))
    }
}