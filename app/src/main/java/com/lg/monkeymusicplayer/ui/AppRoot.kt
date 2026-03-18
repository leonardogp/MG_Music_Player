package com.lg.monkeymusicplayer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.ui.screens.LibraryScreen

@Composable
fun AppRoot(viewModel: MusicViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (uiState.isLoading) {
        LoadingScreen()
    } else {
        LibraryScreen(
            uiState = uiState,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onSortOrderChanged = viewModel::setSortOrder,
            onPlayPause = viewModel::togglePlayPause,
            onPlay = viewModel::playSong,
            onAddToQueue = viewModel::addToQueue,
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
            onDeletePlaylist = viewModel::deletePlaylist,
            onAddSongToPlaylist = viewModel::addSongToPlaylist,
            onAddSongsToPlaylist = viewModel::addSongsToPlaylist,
            onRemoveSongFromPlaylist = viewModel::removeSongFromPlaylist,
            onLoadPlaylistSongs = viewModel::loadPlaylistSongs,
            onUpdateSongTags = viewModel::updateSongTags,
            onOpenEqualizer = { viewModel.openEqualizer(context) },
            onSetSleepTimer = viewModel::setSleepTimer,
            onChangeLanguage = { viewModel.changeLanguage(context, it) }
        )
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_monkey_head),
                contentDescription = null,
                modifier = Modifier.size(140.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
