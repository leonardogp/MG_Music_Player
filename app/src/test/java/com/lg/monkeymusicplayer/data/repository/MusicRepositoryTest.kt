package com.lg.monkeymusicplayer.data.repository

import android.content.Context
import com.lg.monkeymusicplayer.data.database.MusicDao
import com.lg.monkeymusicplayer.data.model.Song
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MusicRepositoryTest {

    private lateinit var musicRepository: MusicRepository
    private val context: Context = mock()
    private val musicDao: MusicDao = mock()
    private val excludedFolders: ExcludedFoldersRepository = mock()

    @Before
    fun setUp() {
        musicRepository = MusicRepository(context, musicDao, excludedFolders)
    }

    @Test
    fun testToggleFavorite() = runBlocking {
        val songId = 1L
        
        // Test adding to favorite
        musicRepository.toggleFavorite(songId, true)
        verify(musicDao).insertFavorite(any())

        // Test removing from favorite
        musicRepository.toggleFavorite(songId, false)
        verify(musicDao).deleteFavorite(any())
    }

    @Test
    fun testAddToHistory() = runBlocking {
        val songId = 1L
        musicRepository.addToHistory(songId)
        verify(musicDao).addToHistory(any())
    }

    @Test
    fun testCreatePlaylist() = runBlocking {
        val playlistName = "My Playlist"
        musicRepository.createPlaylist(playlistName)
        verify(musicDao).createPlaylist(any())
    }

    @Test
    fun testGetSongs() = runBlocking {
        whenever(musicDao.getAllSongs()).thenReturn(emptyList())
        val songs = musicRepository.getSongs()
        assertTrue(songs.isEmpty())
        verify(musicDao).getAllSongs()
    }
}
