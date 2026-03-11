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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.mg.mgmusicplayer.ui.screens.LibraryScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

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
                    Toast.makeText(this, "Por favor, autorice el acceso a archivos para editar etiquetas", Toast.LENGTH_LONG).show()
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
            MaterialTheme {
                val songs by viewModel.songs.collectAsState()
                val genres by viewModel.genres.collectAsState()
                val artists by viewModel.artists.collectAsState()
                val albums by viewModel.albums.collectAsState()
                val folders by viewModel.folders.collectAsState()
                val playlists by viewModel.playlists.collectAsState()
                val recentSongs by viewModel.recentSongs.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val sortOrder by viewModel.sortOrder.collectAsState()
                val currentSong by viewModel.currentSong.collectAsState()
                val isPlaying by viewModel.isPlaying.collectAsState()
                val isShuffleMode by viewModel.isShuffleMode.collectAsState()
                val repeatMode by viewModel.repeatMode.collectAsState()

                LibraryScreen(
                    songs = songs,
                    genres = genres,
                    artists = artists,
                    albums = albums,
                    folders = folders,
                    playlists = playlists,
                    recentSongs = recentSongs,
                    searchQuery = searchQuery,
                    sortOrder = sortOrder,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onSortOrderChanged = viewModel::setSortOrder,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    isShuffleMode = isShuffleMode,
                    repeatMode = repeatMode,
                    onPlayPause = viewModel::togglePlayPause,
                    onPlay = viewModel::playSong,
                    onScanMusic = viewModel::scanMusic,
                    onSkipNext = viewModel::skipNext,
                    onSkipPrevious = viewModel::skipPrevious,
                    onSeekForward = viewModel::seekForward,
                    onSeekBack = viewModel::seekBack,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeatMode = viewModel::cycleRepeatMode,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onCreatePlaylist = viewModel::createPlaylist,
                    onAddSongToPlaylist = viewModel::addSongToPlaylist,
                    onUpdateSongTags = viewModel::updateSongTags
                )
            }
        }
    }
}