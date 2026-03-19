package com.lg.monkeymusicplayer.ui

import android.content.Context
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.lg.monkeymusicplayer.core.player.MusicPlayerManager
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class MusicViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: MusicViewModel
    private val repository: MusicRepository = mock()
    private val playerManager: MusicPlayerManager = mock()
    private val context: Context = mock()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        whenever(repository.allSongsFlow).thenReturn(emptyFlow())
        whenever(repository.playlists).thenReturn(emptyFlow())
        whenever(repository.history).thenReturn(emptyFlow())
        
        whenever(playerManager.currentSong).thenReturn(MutableStateFlow<Song?>(null))
        whenever(playerManager.isPlaying).thenReturn(MutableStateFlow(false))
        whenever(playerManager.isShuffleMode).thenReturn(MutableStateFlow(false))
        whenever(playerManager.repeatMode).thenReturn(MutableStateFlow(0))
        whenever(playerManager.currentPosition).thenReturn(MutableStateFlow(0L))
        whenever(playerManager.duration).thenReturn(MutableStateFlow(0L))
        whenever(playerManager.currentQueue).thenReturn(MutableStateFlow(emptyList<Song>()))
        whenever(playerManager.audioSessionId).thenReturn(MutableStateFlow(0))
        whenever(playerManager.equalizerData).thenReturn(MutableStateFlow<Bundle?>(null))

        viewModel = MusicViewModel(repository, playerManager, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSearchQueryChanged() = runTest {
        val query = "test query"
        viewModel.onSearchQueryChanged(query)
        assertEquals(query, viewModel.searchQuery.value)
    }

    @Test
    fun testPlaySong() = runTest {
        val song = Song(1, 1, "Title", "Artist", "Album", "Genre", "Folder", "Path", "Uri", false)
        viewModel.playSong(song)
        verify(playerManager).setPlaylist(any())
        verify(playerManager).play(song)
    }

    @Test
    fun testTogglePlayPause() {
        viewModel.togglePlayPause()
        verify(playerManager).togglePlayPause()
    }

    @Test
    fun testToggleFavorite() = runTest {
        val song = Song(1, 1, "Title", "Artist", "Album", "Genre", "Folder", "Path", "Uri", false)
        viewModel.toggleFavorite(song)
        verify(repository).toggleFavorite(eq(1L), eq(true))
    }
}
