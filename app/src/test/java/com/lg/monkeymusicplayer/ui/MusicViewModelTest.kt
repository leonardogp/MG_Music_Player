package com.lg.monkeymusicplayer.ui

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.lg.monkeymusicplayer.core.billing.BillingManager
import com.lg.monkeymusicplayer.core.cast.CastManager
import com.lg.monkeymusicplayer.core.feature.FeatureGate
import com.lg.monkeymusicplayer.core.player.MusicPlayerManager
import com.lg.monkeymusicplayer.core.queue.QueueManager
import com.lg.monkeymusicplayer.data.database.EqPresetEntity
import com.lg.monkeymusicplayer.data.database.HistoryEntity
import com.lg.monkeymusicplayer.data.database.PlaylistEntity
import com.lg.monkeymusicplayer.data.model.SmartPlaylist
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.data.repository.*
import com.lg.monkeymusicplayer.domain.usecase.*
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
    private val excludedFoldersRepository: ExcludedFoldersRepository = mock()
    private val smartRepository: SmartRepository = mock()
    private val statsRepository: StatsRepository = mock()
    private val backupRepository: BackupRepository = mock()
    private val cloudSyncRepository: CloudSyncRepository = mock()
    private val application: Application = mock()

    // Use Cases
    private val getSongsUseCase: GetSongsUseCase = mock()
    private val getSmartPlaylistsUseCase: GetSmartPlaylistsUseCase = mock()
    private val getUserStatsUseCase: GetUserStatsUseCase = mock()
    private val refreshMusicLibraryUseCase: RefreshMusicLibraryUseCase = mock()
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase = mock()
    private val updateSongTagsUseCase: UpdateSongTagsUseCase = mock()
    private val playSongUseCase: PlaySongUseCase = mock()

    private val featureGate: FeatureGate = mock()
    private val billingManager: BillingManager = mock()
    private val castManager: CastManager = mock()
    private val queueManager: QueueManager = mock()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        whenever(repository.allSongsFlow).thenReturn(emptyFlow<List<Song>>())
        whenever(repository.playlists).thenReturn(emptyFlow<List<PlaylistEntity>>())
        whenever(repository.history).thenReturn(emptyFlow<List<HistoryEntity>>())
        whenever(repository.favorites).thenReturn(MutableStateFlow<List<Long>>(emptyList()))
        whenever(repository.eqPresets).thenReturn(emptyFlow<List<EqPresetEntity>>())
        
        whenever(playerManager.currentSong).thenReturn(MutableStateFlow<Song?>(null))
        whenever(playerManager.isPlaying).thenReturn(MutableStateFlow<Boolean>(false))
        whenever(playerManager.isShuffleMode).thenReturn(MutableStateFlow<Boolean>(false))
        whenever(playerManager.repeatMode).thenReturn(MutableStateFlow<Int>(0))
        whenever(playerManager.currentPosition).thenReturn(MutableStateFlow<Long>(0L))
        whenever(playerManager.duration).thenReturn(MutableStateFlow<Long>(0L))
        whenever(playerManager.currentQueue).thenReturn(MutableStateFlow<List<Song>>(emptyList()))
        whenever(playerManager.audioSessionId).thenReturn(MutableStateFlow<Int>(0))
        whenever(playerManager.equalizerData).thenReturn(MutableStateFlow<Bundle?>(null))

        whenever(excludedFoldersRepository.excludedFolders).thenReturn(MutableStateFlow<List<String>>(emptyList()))
        whenever(smartRepository.smartPlaylists).thenReturn(emptyFlow<List<SmartPlaylist>>())

        // Stubbing for Use Cases called in init or uiState
        whenever(getSmartPlaylistsUseCase.invoke()).thenReturn(emptyFlow<List<SmartPlaylist>>())
        whenever(castManager.castStateCode).thenReturn(MutableStateFlow<Int>(0))
        whenever(castManager.isConnected).thenReturn(MutableStateFlow<Boolean>(false))
        whenever(queueManager.activeSongs).thenReturn(emptyList<Song>())
        whenever(queueManager.activeQueueName).thenReturn(MutableStateFlow<String>("main"))

        viewModel = MusicViewModel(
            applicationContext = application,
            repository = repository,
            playerManager = playerManager,
            excludedFoldersRepository = excludedFoldersRepository,
            smartRepository = smartRepository,
            statsRepository = statsRepository,
            backupRepository = backupRepository,
            cloudSyncRepository = cloudSyncRepository,
            getSongsUseCase = getSongsUseCase,
            getSmartPlaylistsUseCase = getSmartPlaylistsUseCase,
            getUserStatsUseCase = getUserStatsUseCase,
            refreshMusicLibraryUseCase = refreshMusicLibraryUseCase,
            toggleFavoriteUseCase = toggleFavoriteUseCase,
            updateSongTagsUseCase = updateSongTagsUseCase,
            playSongUseCase = playSongUseCase,
            featureGate = featureGate,
            billingManager = billingManager,
            castManager = castManager,
            queueManager = queueManager
        )
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
        val playlist = emptyList<Song>()
        viewModel.playSong(song, playlist)
        advanceUntilIdle()
        verify(playSongUseCase).invoke(eq(song), eq(playlist))
    }

    @Test
    fun testTogglePlayPause() {
        viewModel.togglePlayPause()
        verify(playerManager).togglePlayPause()
    }

    @Test
    fun testToggleFavorite() = runTest {
        val song = Song(1, 1, "Title", "Artist", "Album", "Genre", "Folder", "Path", "Uri", false)
        whenever(repository.favorites).thenReturn(MutableStateFlow<List<Long>>(emptyList()))
        viewModel.toggleFavorite(song)
        advanceUntilIdle()
        verify(toggleFavoriteUseCase).toggle(eq(1L), eq(false))
    }
}
