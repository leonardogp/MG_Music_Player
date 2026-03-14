package com.mg.mgmusicplayer.ui

import androidx.compose.runtime.*
import com.mg.mgmusicplayer.ui.screens.LibraryScreen

@Composable
fun AppRoot(viewModel: MusicViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LibraryScreen(
        songs = uiState.songs,
        genres = uiState.genres,
        artists = uiState.artists,
        albums = uiState.albums,
        folders = uiState.folders,
        playlists = uiState.playlists,
        history = uiState.history,
        currentPlaylistSongs = uiState.currentPlaylistSongs,
        searchQuery = uiState.searchQuery,
        sortOrder = uiState.sortOrder,
        playerState = uiState.playerState,
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
        onUpdateSongTags = viewModel::updateSongTags
    )
}
