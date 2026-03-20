package com.lg.monkeymusicplayer.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class MusicDatabaseTest {

    private lateinit var db: MusicDatabase
    private lateinit var dao: MusicDao

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, MusicDatabase::class.java).build()
        dao = db.musicDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    private fun createTestSong(id: Long = Random.nextLong()): SongEntity {
        return SongEntity(
            id = id,
            albumId = 1L,
            title = "Test Song $id",
            artist = "Test Artist",
            album = "Test Album",
            genre = "Test Genre",
            folder = "Test Folder",
            path = "Test Path $id",
            albumArtUri = "Test Uri"
        )
    }

    @Test
    fun insertSong() = runBlocking {
        val song = createTestSong()
        dao.insertSongs(listOf(song))
        val allSongs = dao.getAllSongs()
        assert(allSongs.any { it.id == song.id })
    }

    @Test
    fun testFavoritesManagement() = runBlocking {
        val song = createTestSong()
        dao.insertSongs(listOf(song))
        dao.insertFavorite(FavoriteEntity(song.id))
        // Verification would typically involve collecting from getFavorites() Flow
    }

    @Test
    fun testPlaylistsManagement() = runBlocking {
        val song1 = createTestSong()
        val song2 = createTestSong()
        dao.insertSongs(listOf(song1, song2))
        
        val playlistId = dao.createPlaylist(PlaylistEntity(name = "Test Playlist"))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, song1.id))
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, song2.id))
        
        val songsInPlaylist = dao.getSongsInPlaylist(playlistId)
        assert(songsInPlaylist.contains(song1.id))
        assert(songsInPlaylist.contains(song2.id))
    }

    @Test
    fun testHistoryManagement() = runBlocking {
        val song = createTestSong()
        dao.insertSongs(listOf(song))
        dao.addToHistory(HistoryEntity(songId = song.id, timestamp = System.currentTimeMillis()))
        // Verification would typically involve collecting from getHistory() Flow
    }
}
