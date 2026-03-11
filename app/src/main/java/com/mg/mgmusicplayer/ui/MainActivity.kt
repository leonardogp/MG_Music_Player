package com.mg.mgmusicplayer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val permissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                val allGranted = results.values.all { it }
                if (allGranted) {
                    setupUI()
                } else {
                    Toast.makeText(this, "Permisos necesarios para leer música", Toast.LENGTH_LONG).show()
                }
            }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            setupUI()
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun setupUI() {
        setContent {
            MaterialTheme {
                val songs by viewModel.songs.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val currentSong by viewModel.currentSong.collectAsState()
                val isPlaying by viewModel.isPlaying.collectAsState()
                val isShuffleMode by viewModel.isShuffleMode.collectAsState()

                LibraryScreen(
                    songs = songs,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    isShuffleMode = isShuffleMode,
                    onPlayPause = viewModel::togglePlayPause,
                    onPlay = viewModel::playSong,
                    onScanMusic = viewModel::scanMusic,
                    onSkipNext = viewModel::skipNext,
                    onSkipPrevious = viewModel::skipPrevious,
                    onToggleShuffle = viewModel::toggleShuffle
                )
            }
        }
    }
}