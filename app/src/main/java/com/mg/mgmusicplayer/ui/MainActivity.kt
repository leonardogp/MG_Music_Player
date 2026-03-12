package com.mg.mgmusicplayer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.mg.mgmusicplayer.core.utils.SongCoverFetcher
import com.mg.mgmusicplayer.ui.screens.LibraryScreen
import com.mg.mgmusicplayer.ui.theme.MGMusicPlayerTheme

class MainActivity : ComponentActivity(), ImageLoaderFactory {

    private val viewModel: MusicViewModel by viewModels()

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SongCoverFetcher.Factory(this@MainActivity))
            }
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            if (results.values.all { it }) {
                checkManageExternalStorage()
            } else {
                Toast.makeText(this, "Permisos necesarios para el funcionamiento", Toast.LENGTH_LONG).show()
            }
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            checkManageExternalStorage()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun checkManageExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:${packageName}")
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
        setupUI()
    }

    private fun setupUI() {
        setContent {
            MGMusicPlayerTheme {
                val songs by viewModel.songs.collectAsState()
                val genres by viewModel.genres.collectAsState()
                val artists by viewModel.artists.collectAsState()
                val albums by viewModel.albums.collectAsState()
                val folders by viewModel.folders.collectAsState()
                val playlists by viewModel.playlists.collectAsState()
                val currentPlaylistSongs by viewModel.currentPlaylistSongs.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val sortOrder by viewModel.sortOrder.collectAsState()
                val currentSong by viewModel.currentSong.collectAsState()
                val isPlaying by viewModel.isPlaying.collectAsState()
                val isShuffleMode by viewModel.isShuffleMode.collectAsState()
                val repeatMode by viewModel.repeatMode.collectAsState()
                val currentPosition by viewModel.currentPosition.collectAsState()
                val duration by viewModel.duration.collectAsState()
                val audioSessionId = viewModel.getAudioSessionId()

                LibraryScreen(
                    songs = songs,
                    genres = genres,
                    artists = artists,
                    albums = albums,
                    folders = folders,
                    playlists = playlists,
                    currentPlaylistSongs = currentPlaylistSongs,
                    searchQuery = searchQuery,
                    sortOrder = sortOrder,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onSortOrderChanged = viewModel::setSortOrder,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    isShuffleMode = isShuffleMode,
                    repeatMode = repeatMode,
                    currentPosition = currentPosition,
                    duration = duration,
                    audioSessionId = audioSessionId,
                    onPlayPause = viewModel::togglePlayPause,
                    onPlay = viewModel::playSong,
                    onScanMusic = viewModel::scanMusic,
                    onSkipNext = viewModel::skipNext,
                    onSkipPrevious = viewModel::skipPrevious,
                    onSeekTo = viewModel::seekTo,
                    onSeekForward = viewModel::seekForward,
                    onSeekBack = viewModel::seekBack,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeatMode = viewModel::cycleRepeatMode,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onCreatePlaylist = viewModel::createPlaylist,
                    onAddSongToPlaylist = viewModel::addSongToPlaylist,
                    onAddSongsToPlaylist = viewModel::addSongsToPlaylist,
                    onLoadPlaylistSongs = viewModel::loadPlaylistSongs,
                    onUpdateSongTags = viewModel::updateSongTags
                )
            }
        }
    }
}
