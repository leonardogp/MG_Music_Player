import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class MusicRepositoryTest {

    @Mock
    private MusicDataSource musicDataSource;
    private MusicRepository musicRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        musicRepository = new MusicRepository(musicDataSource);
    }

    @Test
    public void testAddToFavorites() {
        Song song = new Song("Test Title", "Test Artist");
        musicRepository.addToFavorites(song);
        verify(musicDataSource, times(1)).addToFavorites(song);
    }

    @Test
    public void testRemoveFromFavorites() {
        Song song = new Song("Test Title", "Test Artist");
        musicRepository.removeFromFavorites(song);
        verify(musicDataSource, times(1)).removeFromFavorites(song);
    }

    @Test
    public void testGetFavorites() {
        List<Song> expectedFavorites = new ArrayList<>();
        when(musicDataSource.getFavorites()).thenReturn(expectedFavorites);
        List<Song> actualFavorites = musicRepository.getFavorites();
        assertSame(expectedFavorites, actualFavorites);
    }

    @Test
    public void testAddToHistory() {
        Song song = new Song("Test Title", "Test Artist");
        musicRepository.addToHistory(song);
        verify(musicDataSource, times(1)).addToHistory(song);
    }

    @Test
    public void testGetHistory() {
        List<Song> expectedHistory = new ArrayList<>();
        when(musicDataSource.getHistory()).thenReturn(expectedHistory);
        List<Song> actualHistory = musicRepository.getHistory();
        assertSame(expectedHistory, actualHistory);
    }

    @Test
    public void testAddToPlaylist() {
        Playlist playlist = new Playlist("Test Playlist");
        Song song = new Song("Test Title", "Test Artist");
        musicRepository.addToPlaylist(playlist, song);
        verify(musicDataSource, times(1)).addToPlaylist(playlist, song);
    }

    @Test
    public void testGetPlaylists() {
        List<Playlist> expectedPlaylists = new ArrayList<>();
        when(musicDataSource.getPlaylists()).thenReturn(expectedPlaylists);
        List<Playlist> actualPlaylists = musicRepository.getPlaylists();
        assertSame(expectedPlaylists, actualPlaylists);
    }

    @Test
    public void testRetrieveSong() {
        Song expectedSong = new Song("Test Title", "Test Artist");
        when(musicDataSource.retrieveSong("Test Title")).thenReturn(expectedSong);
        Song actualSong = musicRepository.retrieveSong("Test Title");
        assertSame(expectedSong, actualSong);
    }

    @Test
    public void testClearHistory() {
        musicRepository.clearHistory();
        verify(musicDataSource, times(1)).clearHistory();
    }

    // Additional tests for edge cases and error handling
    @Test(expected = SongNotFoundException.class)
    public void testRetrieveNonExistentSong() {
        when(musicDataSource.retrieveSong("Non-existent Title")).thenThrow(new SongNotFoundException());
        musicRepository.retrieveSong("Non-existent Title");
    }

    @Test
    public void testEmptyFavorites() {
        when(musicDataSource.getFavorites()).thenReturn(new ArrayList<>());
        assertTrue(musicRepository.getFavorites().isEmpty());
    }

    @Test
    public void testAddDuplicateToFavorites() {
        Song song = new Song("Test Title", "Test Artist");
        musicRepository.addToFavorites(song);
        musicRepository.addToFavorites(song); // Adding again
        verify(musicDataSource, times(1)).addToFavorites(song); // Should only be called once
    }

    @Test
    public void testEmptyHistory() {
        when(musicDataSource.getHistory()).thenReturn(new ArrayList<>());
        assertTrue(musicRepository.getHistory().isEmpty());
    }

    @Test
    public void testAddToNonexistentPlaylist() {
        Playlist playlist = new Playlist("Nonexistent Playlist");
        Song song = new Song("Test Title", "Test Artist");
        musicRepository.addToPlaylist(playlist, song);
        verify(musicDataSource, times(1)).addToPlaylist(playlist, song);
    }
}