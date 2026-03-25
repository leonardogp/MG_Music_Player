package com.lg.monkeymusicplayer.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.data.database.PlaylistEntity
import com.lg.monkeymusicplayer.data.database.HistoryEntity
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.ui.SortOrder
import com.lg.monkeymusicplayer.ui.PlayerState
import com.lg.monkeymusicplayer.ui.LibraryUiState
import com.lg.monkeymusicplayer.ui.MusicViewModel
import com.lg.monkeymusicplayer.core.result.Result
import com.lg.monkeymusicplayer.ui.components.MediaProgressSlider
import com.lg.monkeymusicplayer.ui.components.PlayerControls
import com.lg.monkeymusicplayer.ui.theme.PrimaryOrange
import androidx.media3.common.Player
import kotlinx.coroutines.launch
import java.util.Calendar
import com.lg.monkeymusicplayer.util.TimeFormatter
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.lg.monkeymusicplayer.ui.components.LyricsView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass
) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            LibraryMainContent(
                uiState = uiState,
                viewModel = viewModel,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onSortOrderChanged = viewModel::setSortOrder,
                onPlayPause = viewModel::togglePlayPause,
                onPlay = { song, playlist -> viewModel.playSong(song, playlist) },
                onAddToQueue = viewModel::addToQueue,
                onScanMusic = { viewModel.scanMusic() },
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
                onUpdateSongTags = { song, t, a, al, g -> viewModel.updateSongTags(song, t, a, al, g) },
                onOpenEqualizer = { viewModel.openEqualizer(viewModel.context) },
                onSetSleepTimer = viewModel::setSleepTimer,
                onChangeLanguage = { lang ->
                    val appLocale: LocaleListCompat = if (lang.isEmpty()) {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(lang)
                    }
                    AppCompatDelegate.setApplicationLocales(appLocale)
                },
                onPlayerClick = { navController.navigate("player") },
                onMenuClick = { navController.navigate("settings") }
            )
        }
        composable("player") {
            if (uiState.playerState.currentSong != null) {
                FullPlayerScreen(
                    song = uiState.playerState.currentSong!!,
                    queue = uiState.playerState.currentQueue,
                    lyrics = uiState.playerState.lyrics,
                    isLoadingLyrics = viewModel.isLoadingLyrics.collectAsState().value,
                    isPlaying = uiState.playerState.isPlaying,
                    isShuffleMode = uiState.playerState.isShuffleMode,
                    repeatMode = uiState.playerState.repeatMode,
                    currentPosition = uiState.playerState.currentPosition,
                    duration = uiState.playerState.duration,
                    audioSessionId = uiState.playerState.audioSessionId,
                    onClose = { navController.popBackStack() },
                    onPlayPause = viewModel::togglePlayPause,
                    onSkipNext = viewModel::skipNext,
                    onSkipPrevious = viewModel::skipPrevious,
                    onSeekTo = viewModel::seekTo,
                    onSeekForward = viewModel::seekForward,
                    onSeekBack = viewModel::seekBack,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeatMode = viewModel::cycleRepeatMode,
                    onToggleFavorite = { viewModel.toggleFavorite(uiState.playerState.currentSong!!) },
                    onAddToPlaylist = { /* handle */ },
                    onEditSong = { song ->
                        navController.popBackStack()
                        viewModel.requestEditSong(song)
                    },
                    onPlayFromQueue = { viewModel.playSong(it, uiState.playerState.currentQueue) }
                )
            }
        }
        composable("settings") {
            SettingsScreen(
                uiState = uiState,
                navController = navController,
                onBack = { navController.popBackStack() },
                onScanMusic = { viewModel.scanMusic() },
                onOpenEqualizer = { viewModel.openEqualizer(viewModel.context) },
                onSetSleepTimer = viewModel::setSleepTimer,
                onChangeLanguage = { lang ->
                    val appLocale: LocaleListCompat = if (lang.isEmpty()) {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(lang)
                    }
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            )
        }
        composable("equalizer") {
            EqualizerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: LibraryUiState,
    navController: androidx.navigation.NavController,
    onBack: () -> Unit,
    onScanMusic: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onChangeLanguage: (String) -> Unit
) {
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentMinutes = uiState.playerState.sleepTimerMinutes,
            onDismiss = { showSleepTimerDialog = false },
            onConfirm = { minutes ->
                onSetSleepTimer(minutes)
                showSleepTimerDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { lang ->
                onChangeLanguage(lang)
                showLanguageDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            ListItem(
                modifier = Modifier.clickable { onScanMusic() },
                headlineContent = { Text(stringResource(R.string.scan_music)) },
                leadingContent = {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                },
                supportingContent = {
                    if (uiState.isScanning) {
                        Column {
                            val progress = if (uiState.scanTotal > 0) uiState.scanProgress.toFloat() / uiState.scanTotal else 0f
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                            Text(
                                stringResource(R.string.scanning_progress, uiState.scanProgress, uiState.scanTotal),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            )
            ListItem(
                modifier = Modifier.clickable { showSleepTimerDialog = true },
                headlineContent = {
                    val timerText = if (uiState.playerState.sleepTimerMinutes > 0) {
                        stringResource(R.string.timer_active, TimeFormatter.formatDuration(uiState.playerState.sleepTimerRemainingMillis))
                    } else {
                        stringResource(R.string.sleep_timer)
                    }
                    Text(timerText)
                },
                leadingContent = { Icon(Icons.Default.Timer, contentDescription = null) }
            )
            ListItem(
                modifier = Modifier.clickable { navController.navigate("equalizer") },
                headlineContent = { Text(stringResource(R.string.equalizer)) },
                leadingContent = { Icon(Icons.Default.GraphicEq, contentDescription = null) }
            )
            ListItem(
                modifier = Modifier.clickable { showLanguageDialog = true },
                headlineContent = { Text(stringResource(R.string.language)) },
                leadingContent = { Icon(Icons.Default.Language, contentDescription = null) }
            )
        }
    }
}

@Composable
fun LibraryMainContent(
    uiState: LibraryUiState,
    // ── CAMBIO 1: recibimos el viewModel para escuchar tagUpdateResult ──
    viewModel: MusicViewModel,
    onSearchQueryChanged: (String) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    onPlayPause: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onScanMusic: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBack: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onLoadPlaylistSongs: (String) -> Unit,
    onPlayerClick: () -> Unit,
    onMenuClick: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onAddSongToPlaylist: (String, Song) -> Unit,
    onAddSongsToPlaylist: (String, List<Song>) -> Unit,
    onRemoveSongFromPlaylist: (String, Long) -> Unit,
    onUpdateSongTags: (Song, String, String, String, String) -> Unit,
    onOpenEqualizer: () -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onChangeLanguage: (String) -> Unit
) {
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var editingSong by remember { mutableStateOf<Song?>(null) }
    var songsToAddToPlaylist by remember { mutableStateOf<List<Song>?>(null) }

    // ── CAMBIO 2: estado de guardado y snackbar ──
    var isSavingTags by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // ── PUNTO 5: observar el permiso de almacenamiento ──
    val hasManageStoragePermission by viewModel.hasManageStoragePermission.collectAsState()

    // ── CAMBIO 3: escuchar el resultado del guardado de tags ──
    // El diálogo se cierra SOLO cuando el resultado llega (éxito o error)
    val strTagsSavedOk = stringResource(R.string.tags_saved_ok)
    val strTagsSaveError = stringResource(R.string.tags_save_error)
    val strTagsPermissionNeeded = stringResource(R.string.tags_permission_needed)
    val strTagsPermissionGrant = stringResource(R.string.tags_permission_grant)
    LaunchedEffect(Unit) {
        viewModel.tagUpdateResult.collect { result ->
            isSavingTags = false
            editingSong = null  // cerrar el diálogo aquí, no en onSave
            coroutineScope.launch {
                val message = when (result) {
                    is Result.Success -> strTagsSavedOk
                    is Result.Error   -> strTagsSaveError.format(result.message)
                    is Result.Loading -> return@launch
                }
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // ── CORRECCIÓN: escuchar evento de edición desde FullPlayerScreen ──
    // Cuando el usuario toca "Editar" en el reproductor, vuelve a la librería
    // y este LaunchedEffect abre el editor con la canción correcta.
    LaunchedEffect(Unit) {
        viewModel.requestEditSongEvent.collect { song ->
            editingSong = song
        }
    }

    // ── CAMBIO 4: diálogo con isSaving para mostrar loading y bloquear doble tap ──
    if (editingSong != null) {
        EditTagsDialog(
            song = editingSong!!,
            isSaving = isSavingTags,
            onDismiss = {
                if (!isSavingTags) editingSong = null
            },
            onSave = { title, artist, album, genre ->
                // ── PUNTO 5: verificar permiso antes de guardar ──
                if (!hasManageStoragePermission) {
                    editingSong = null
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = strTagsPermissionNeeded,
                            actionLabel = strTagsPermissionGrant,
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.requestManageStoragePermission()
                        }
                    }
                } else {
                    isSavingTags = true
                    onUpdateSongTags(editingSong!!, title, artist, album, genre)
                }
            }
        )
    }

    if (songsToAddToPlaylist != null) {
        AddToPlaylistDialog(
            playlists = uiState.playlists,
            onDismiss = { songsToAddToPlaylist = null },
            onPlaylistSelected = { playlistId ->
                val songsToAdd = songsToAddToPlaylist!!
                if (songsToAdd.size == 1) {
                    onAddSongToPlaylist(playlistId, songsToAdd[0])
                } else {
                    onAddSongsToPlaylist(playlistId, songsToAdd)
                }
                songsToAddToPlaylist = null
            },
            onCreateNew = { showCreatePlaylistDialog = true }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreatePlaylistDialog = false
            }
        )
    }

    // ── CAMBIO 5: SnackbarHost dentro del Scaffold de MobileLayout ──
    MobileLayout(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onSearchQueryChanged = onSearchQueryChanged,
        onSortOrderChanged = onSortOrderChanged,
        onPlayPause = onPlayPause,
        onPlay = onPlay,
        onAddToQueue = onAddToQueue,
        onScanMusic = onScanMusic,
        onSkipNext = onSkipNext,
        onSkipPrevious = onSkipPrevious,
        onToggleShuffle = onToggleShuffle,
        onLoadPlaylistSongs = onLoadPlaylistSongs,
        onPlayerClick = onPlayerClick,
        onMenuClick = onMenuClick,
        onAddSongToPlaylist = { songsToAddToPlaylist = listOf(it) },
        onAddSongsToPlaylist = { songsToAddToPlaylist = it },
        onRemoveSongFromPlaylist = onRemoveSongFromPlaylist,
        onCreatePlaylist = { showCreatePlaylistDialog = true },
        onDeletePlaylist = onDeletePlaylist,
        onEditSong = { editingSong = it },
        onToggleFavorite = onToggleFavorite,
        onOpenEqualizer = onOpenEqualizer
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileLayout(
    uiState: LibraryUiState,
    // ── CAMBIO 6: recibir snackbarHostState para mostrarlo en el Scaffold ──
    snackbarHostState: SnackbarHostState,
    onSearchQueryChanged: (String) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    onPlayPause: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onScanMusic: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onLoadPlaylistSongs: (String) -> Unit,
    onPlayerClick: () -> Unit,
    onMenuClick: () -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onAddSongsToPlaylist: (List<Song>) -> Unit,
    onRemoveSongFromPlaylist: (String, Long) -> Unit,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onEditSong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onOpenEqualizer: () -> Unit
) {
    val tabs = listOf(
        R.string.tab_main,
        R.string.tab_songs,
        R.string.tab_genres,
        R.string.tab_artists,
        R.string.tab_albums,
        R.string.tab_folders,
        R.string.tab_playlists,
        R.string.tab_favorites,
        R.string.tab_history
    )
    var selectedCategoryItem by remember { mutableStateOf<String?>(null) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        // ── CAMBIO 7: agregar snackbarHost al Scaffold ──
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        val currentTab = pagerState.currentPage
                        AnimatedContent(targetState = isSearchActive, label = "search_transition") { active ->
                            if (active) {
                                TextField(
                                    value = uiState.searchQuery,
                                    onValueChange = onSearchQueryChanged,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        val hintId = when (currentTab) {
                                            2 -> R.string.search_genres
                                            3 -> R.string.search_artists
                                            4 -> R.string.search_albums
                                            5 -> R.string.search_folders
                                            6 -> R.string.search_playlists
                                            else -> R.string.search_songs
                                        }
                                        Text(stringResource(hintId))
                                    },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            isSearchActive = false
                                            onSearchQueryChanged("")
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_search))
                                        }
                                    }
                                )
                            } else {
                                if (currentTab in 2..5 && selectedCategoryItem != null) {
                                    Text(text = selectedCategoryItem!!, fontWeight = FontWeight.Bold)
                                } else if (currentTab == 6 && selectedPlaylistId != null) {
                                    val title = uiState.playlists.find { it.id.toString() == selectedPlaylistId }?.name ?: stringResource(R.string.playlist)
                                    Text(text = title, fontWeight = FontWeight.Bold)
                                } else {
                                    Text(text = stringResource(tabs[currentTab]), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        val currentTab = pagerState.currentPage
                        if (!isSearchActive && ((currentTab in 2..5 && selectedCategoryItem != null) || (currentTab == 6 && selectedPlaylistId != null))) {
                            IconButton(onClick = {
                                selectedCategoryItem = null
                                selectedPlaylistId = null
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                            }
                        } else if (!isSearchActive) {
                            IconButton(onClick = onMenuClick) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_monkey_head),
                                    contentDescription = stringResource(R.string.settings_title),
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.Unspecified
                                )
                            }
                        }
                    },
                    actions = {
                        if (!isSearchActive) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                            }
                            if (pagerState.currentPage == 1 && selectedCategoryItem == null) {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
                                }
                                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                    SortOrder.entries.forEach { order ->
                                        DropdownMenuItem(
                                            text = { Text(order.name) },
                                            onClick = { onSortOrderChanged(order); showSortMenu = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(tabs) { index, titleId ->
                        val isSelected = pagerState.currentPage == index
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                selectedCategoryItem = null
                                selectedPlaylistId = null
                                isSearchActive = false
                                onSearchQueryChanged("")
                            },
                            color = if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                        ) {
                            Text(
                                text = stringResource(titleId),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            PlayerBottomBar(
                uiState.playerState.currentSong,
                uiState.playerState.isPlaying,
                uiState.playerState.currentPosition,
                uiState.playerState.duration,
                onPlayPause,
                onSkipNext,
                onSkipPrevious,
                onPlayerClick
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
                    )
                ),
            beyondViewportPageCount = 1,
            userScrollEnabled = (selectedCategoryItem == null && selectedPlaylistId == null)
        ) { page ->
            when (page) {
                0 -> MainTab(uiState.songs, onPlay, onAddToQueue, onAddSongToPlaylist, onEditSong, onToggleFavorite)
                1 -> SongList(uiState.songs, uiState.songs, onPlay, onAddToQueue, onAddSongToPlaylist, null, onEditSong, onToggleFavorite)
                2 -> CategoryNavigation(uiState.genres, selectedCategoryItem, onPlay, onAddToQueue, { selectedCategoryItem = it }, Icons.Default.LibraryMusic, onAddSongToPlaylist, onEditSong, onToggleFavorite)
                3 -> CategoryNavigation(uiState.artists, selectedCategoryItem, onPlay, onAddToQueue, { selectedCategoryItem = it }, Icons.Default.Person, onAddSongToPlaylist, onEditSong, onToggleFavorite)
                4 -> CategoryNavigation(uiState.albums, selectedCategoryItem, onPlay, onAddToQueue, { selectedCategoryItem = it }, Icons.Default.Album, onAddSongToPlaylist, onEditSong, onToggleFavorite)
                5 -> CategoryNavigation(uiState.folders, selectedCategoryItem, onPlay, onAddToQueue, { selectedCategoryItem = it }, Icons.Default.Folder, onAddSongToPlaylist, onEditSong, onToggleFavorite)
                6 -> {
                    if (selectedPlaylistId != null) {
                        LaunchedEffect(selectedPlaylistId) { onLoadPlaylistSongs(selectedPlaylistId!!) }
                        SongList(
                            uiState.currentPlaylistSongs,
                            uiState.currentPlaylistSongs,
                            onPlay,
                            onAddToQueue,
                            onAddSongToPlaylist,
                            { songId -> onRemoveSongFromPlaylist(selectedPlaylistId!!, songId) },
                            onEditSong,
                            onToggleFavorite
                        )
                    } else {
                        PlaylistSummaryList(uiState.playlists, onItemClick = { selectedPlaylistId = it }, onCreatePlaylist, onDeletePlaylist)
                    }
                }
                7 -> {
                    // ── CORRECCIÓN: remember evita recalcular filter en cada recomposición ──
                    val favSongs = remember(uiState.songs) { uiState.songs.filter { it.isFavorite } }
                    SongList(favSongs, favSongs, onPlay, onAddToQueue, onAddSongToPlaylist, null, onEditSong, onToggleFavorite)
                }
                8 -> {
                    // ── CORRECCIÓN: lookup O(1) con Map en lugar de O(N²) con find{} ──
                    // Antes: por cada entrada del historial se hacía uiState.songs.find{} →
                    //        50 entradas × 1000 canciones = 50.000 comparaciones por recomposición.
                    // Ahora: un solo Map construido una vez, lookup en O(1) por entrada.
                    val songIndex = remember(uiState.songs) { uiState.songs.associateBy { it.id } }
                    val historySongs = remember(uiState.history, songIndex) {
                        uiState.history.mapNotNull { songIndex[it.songId] }.distinctBy { it.id }
                    }
                    SongList(historySongs, historySongs, onPlay, onAddToQueue, onAddSongToPlaylist, null, onEditSong, onToggleFavorite)
                }
            }
        }
    }
}

@Composable
fun MainTab(
    songs: List<Song>,
    onPlay: (Song, List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onEditSong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit
) {
    val favoriteSongs = songs.filter { it.isFavorite }
    val greeting = getGreeting()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(greeting),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (favoriteSongs.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 240.dp)
            ) {
                items(favoriteSongs.take(6)) { song ->
                    FavoriteGridItem(song, onClick = { onPlay(song, favoriteSongs) })
                }
            }
        } else {
            Box(
                modifier = Modifier.height(120.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_favorites_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(stringResource(R.string.recent), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(songs.take(15)) { song ->
                SongItem(song, songs, onPlay, onAddToQueue, onAddSongToPlaylist, null, onEditSong, onToggleFavorite)
            }
        }
    }
}

@Composable
fun FavoriteGridItem(song: Song, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(4.dp)).clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_monkey_head)
            )
            Text(
                text = song.title,
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun getGreeting(): Int {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> R.string.greeting_morning
        in 12..18 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
}

@Composable
fun PlayerBottomBar(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onPlayerClick: () -> Unit
) {
    if (currentSong != null) {
        Surface(
            tonalElevation = 8.dp,
            modifier = Modifier
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onPlayerClick() }
                .shadow(10.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp).copy(alpha = 0.95f)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = currentSong.albumArtUri,
                        contentDescription = null,
                        error = painterResource(R.drawable.ic_monkey_head),
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = currentSong.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, fontWeight = FontWeight.Bold)
                        Text(text = currentSong.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onSkipPrevious) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.previous), modifier = Modifier.size(28.dp))
                        }
                        IconButton(onClick = { onPlayPause() }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.play_pause),
                                modifier = Modifier.size(36.dp),
                                tint = PrimaryOrange
                            )
                        }
                        IconButton(onClick = onSkipNext) {
                            Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.next), modifier = Modifier.size(28.dp))
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = { if (duration > 0) currentPosition.toFloat() / duration else 0f },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = PrimaryOrange,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    song: Song,
    queue: List<Song>,
    lyrics: List<com.lg.monkeymusicplayer.data.model.LyricLine>,
    isLoadingLyrics: Boolean,
    isPlaying: Boolean,
    isShuffleMode: Boolean,
    repeatMode: Int,
    currentPosition: Long,
    duration: Long,
    audioSessionId: Int,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBack: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditSong: (Song) -> Unit,
    onPlayFromQueue: (Song) -> Unit
) {
    BackHandler { onClose() }

    // ── Tabs: 0 = Player, 1 = Lyrics, 2 = Queue ──────────────────────────────
    var selectedTab by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().blur(80.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.4f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
        )

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        ) {
            // ── Cabecera fija ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.playlist).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f), letterSpacing = 1.sp)
                    Text(song.album, style = MaterialTheme.typography.labelLarge,
                        color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                // Placeholder simétrico
                Box(modifier = Modifier.size(48.dp))
            }

            // ── Tabs ──────────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryOrange
                    )
                },
                divider = {}
            ) {
                listOf(
                    stringResource(R.string.playlist),
                    stringResource(R.string.lyrics_tab),
                    stringResource(R.string.queue_tab)
                ).forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(title,
                                color = if (selectedTab == index) Color.White else Color.White.copy(0.5f),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal)
                        }
                    )
                }
            }

            // ── Contenido del tab ─────────────────────────────────────────────
            when (selectedTab) {

                // Tab 0: Player
                0 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Portada
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(8.dp)
                                .shadow(20.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(song.albumArtUri).crossfade(true).build(),
                                contentDescription = null,
                                error = painterResource(R.drawable.ic_monkey_head),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Título + acciones
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold, color = Color.White,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(0.7f), maxLines = 1)
                            }
                            IconButton(onClick = { onEditSong(song) }) {
                                Icon(Icons.Default.Edit, stringResource(R.string.tags_edit_current_song),
                                    tint = Color.White.copy(0.7f), modifier = Modifier.size(22.dp))
                            }
                            IconButton(onClick = onToggleFavorite) {
                                Icon(
                                    if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    stringResource(R.string.favorites),
                                    tint = if (song.isFavorite) Color.Red else Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Seekbar
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                value = currentPosition.toFloat(),
                                onValueChange = { onSeekTo(it.toLong()) },
                                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(0.2f))
                            )
                            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(TimeFormatter.formatDuration(currentPosition),
                                    color = Color.White.copy(0.6f), style = MaterialTheme.typography.bodySmall)
                                Text(TimeFormatter.formatDuration(duration),
                                    color = Color.White.copy(0.6f), style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        // Controles principales
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onToggleShuffle) {
                                Icon(Icons.Default.Shuffle, stringResource(R.string.shuffle),
                                    tint = if (isShuffleMode) PrimaryOrange else Color.White.copy(0.6f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                IconButton(onClick = onSkipPrevious) {
                                    Icon(Icons.Default.SkipPrevious, stringResource(R.string.previous),
                                        modifier = Modifier.size(44.dp), tint = Color.White)
                                }
                                Box(modifier = Modifier.size(72.dp).clip(CircleShape)
                                    .background(Color.White).clickable { onPlayPause() },
                                    contentAlignment = Alignment.Center) {
                                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        stringResource(R.string.play_pause),
                                        modifier = Modifier.size(40.dp), tint = Color.Black)
                                }
                                IconButton(onClick = onSkipNext) {
                                    Icon(Icons.Default.SkipNext, stringResource(R.string.skip_next),
                                        modifier = Modifier.size(44.dp), tint = Color.White)
                                }
                            }
                            IconButton(onClick = onCycleRepeatMode) {
                                Icon(
                                    when (repeatMode) {
                                        Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                        else -> Icons.Default.Repeat
                                    },
                                    stringResource(R.string.repeat),
                                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) PrimaryOrange else Color.White.copy(0.6f)
                                )
                            }
                        }

                        // Controles secundarios
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onSeekBack) {
                                Icon(Icons.Default.Replay10, stringResource(R.string.seek_back_10),
                                    tint = Color.White.copy(0.7f))
                            }
                            IconButton(onClick = onSeekForward) {
                                Icon(Icons.Default.Forward10, stringResource(R.string.seek_forward_10),
                                    tint = Color.White.copy(0.7f))
                            }
                            IconButton(onClick = onAddToPlaylist) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistAdd,
                                    stringResource(R.string.add_to_playlist), tint = Color.White.copy(0.7f))
                            }
                        }
                    }
                }

                // Tab 1: Lyrics
                1 -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isLoadingLyrics) {
                            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                CircularProgressIndicator(color = PrimaryOrange)
                                Spacer(Modifier.height(12.dp))
                                Text(stringResource(R.string.lyrics_searching),
                                    color = Color.White.copy(0.6f), style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            LyricsView(
                                lyrics = lyrics,
                                currentPosition = currentPosition,
                                accentColor = PrimaryOrange,
                                onLyricClick = onSeekTo
                            )
                        }
                    }
                }

                // Tab 2: Queue
                2 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(queue) { queueSong ->
                            val isCurrent = queueSong.id == song.id
                            ListItem(
                                modifier = Modifier.clickable { onPlayFromQueue(queueSong) },
                                colors = ListItemDefaults.colors(
                                    containerColor = if (isCurrent) Color.White.copy(0.08f) else Color.Transparent
                                ),
                                headlineContent = {
                                    Text(queueSong.title,
                                        color = if (isCurrent) PrimaryOrange else Color.White,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                                },
                                supportingContent = {
                                    Text(queueSong.artist, color = Color.White.copy(0.6f))
                                },
                                leadingContent = {
                                    AsyncImage(
                                        model = queueSong.albumArtUri,
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                },
                                trailingContent = {
                                    if (isCurrent) Icon(Icons.Default.PlayArrow, null, tint = PrimaryOrange)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongItem(
    song: Song,
    contextPlaylist: List<Song>,
    onPlay: (Song, List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onEditSong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        modifier = Modifier.clickable { onPlay(song, contextPlaylist) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text("${song.artist} • ${song.album}", style = MaterialTheme.typography.bodyMedium, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                error = painterResource(R.drawable.ic_monkey_head),
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        },
        trailingContent = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.options))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(if (song.isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites)) },
                    leadingIcon = { Icon(if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (song.isFavorite) Color.Red else LocalContentColor.current) },
                    onClick = { onToggleFavorite(song); showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.add_to_queue)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                    onClick = { onAddToQueue(song); showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.add_to_playlist)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                    onClick = { onAddSongToPlaylist(song); showMenu = false }
                )
                if (onRemoveFromPlaylist != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.remove_from_playlist)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = { onRemoveFromPlaylist(); showMenu = false }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_tags)) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = { onEditSong(song); showMenu = false }
                )
            }
        }
    )
}

@Composable
fun SongList(
    songs: List<Song>,
    contextPlaylist: List<Song>,
    onPlay: (Song, List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onRemoveFromPlaylist: ((Long) -> Unit)? = null,
    onEditSong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_songs)) }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(songs) { song ->
                SongItem(
                    song = song,
                    contextPlaylist = contextPlaylist,
                    onPlay = onPlay,
                    onAddToQueue = onAddToQueue,
                    onAddSongToPlaylist = onAddSongToPlaylist,
                    onRemoveFromPlaylist = onRemoveFromPlaylist?.let { { it(song.id) } },
                    onEditSong = onEditSong,
                    onToggleFavorite = onToggleFavorite
                )
            }
        }
    }
}

@Composable
fun CategoryNavigation(
    data: Map<String, List<Song>>,
    selectedItem: String?,
    onPlay: (Song, List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onItemClick: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onAddSongToPlaylist: (Song) -> Unit,
    onEditSong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit
) {
    if (selectedItem == null) {
        CategorySummaryList(data, onItemClick, icon)
    } else {
        val categorySongs = data[selectedItem] ?: emptyList()
        SongList(categorySongs, categorySongs, onPlay, onAddToQueue, onAddSongToPlaylist, null, onEditSong, onToggleFavorite)
    }
}

@Composable
fun CategorySummaryList(
    data: Map<String, List<Song>>,
    onItemClick: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    if (data.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_items)) }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(data.keys.toList().sorted()) { key ->
                val songCount = data[key]?.size ?: 0
                val firstSong = data[key]?.firstOrNull()
                ListItem(
                    modifier = Modifier.clickable { onItemClick(key) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(key, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(stringResource(R.string.songs_count, songCount)) },
                    leadingContent = {
                        if (firstSong != null && icon == Icons.Default.Album) {
                            AsyncImage(
                                model = firstSong.albumArtUri,
                                contentDescription = null,
                                error = painterResource(R.drawable.ic_monkey_head),
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(icon, contentDescription = null, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PlaylistSummaryList(
    playlists: List<PlaylistEntity>,
    onItemClick: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ListItem(
            modifier = Modifier.clickable { onCreatePlaylist() },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text(stringResource(R.string.create_new_playlist), fontWeight = FontWeight.Bold) },
            leadingContent = {
                Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(12.dp), tint = PrimaryOrange)
                }
            }
        )
        if (playlists.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_playlists))
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(playlists) { playlist ->
                    var showMenu by remember { mutableStateOf(false) }
                    ListItem(
                        modifier = Modifier.clickable { onItemClick(playlist.id.toString()) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(playlist.name, fontWeight = FontWeight.SemiBold) },
                        leadingContent = {
                            Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, modifier = Modifier.padding(12.dp))
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.playlist_options))
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete_playlist)) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    onClick = { onDeletePlaylist(playlist); showMenu = false }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            Column {
                if (playlists.isEmpty()) {
                    Text(stringResource(R.string.no_playlists))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(playlists) { playlist ->
                            ListItem(
                                modifier = Modifier.clickable { onPlaylistSelected(playlist.id.toString()) },
                                headlineContent = { Text(playlist.name) },
                                leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) }
                            )
                        }
                    }
                }
                TextButton(onClick = onCreateNew, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.create_new_playlist))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_playlist)) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

// ── CAMBIO 8: EditTagsDialog con soporte de isSaving ──
@Composable
fun EditTagsDialog(
    song: Song,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var genre by remember { mutableStateOf(song.genre) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(R.string.edit_tags)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (!isSaving) title = it },
                    label = { Text(stringResource(R.string.title)) },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { if (!isSaving) artist = it },
                    label = { Text(stringResource(R.string.artist)) },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { if (!isSaving) album = it },
                    label = { Text(stringResource(R.string.album)) },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { if (!isSaving) genre = it },
                    label = { Text(stringResource(R.string.genre)) },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, artist, album, genre) },
                enabled = !isSaving
            ) {
                // ── CAMBIO 9: spinner dentro del botón mientras guarda ──
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.tags_saving))
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun SleepTimerDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val options = listOf(0, 5, 15, 30, 45, 60, 90, 120)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_timer)) },
        text = {
            Column {
                Text(stringResource(R.string.select_sleep_timer))
                Spacer(modifier = Modifier.height(8.dp))
                options.forEach { minutes ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onConfirm(minutes) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentMinutes == minutes, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (minutes == 0) stringResource(R.string.disabled) else stringResource(R.string.minutes, minutes))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun LanguageDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val languages = listOf(
        "" to stringResource(R.string.system_default),
        "en" to "English",
        "es" to "Español",
        "pt" to "Português",
        "fr" to "Français",
        "de" to "Deutsch",
        "it" to "Italiano",
        "ru" to "Русский",
        "tr" to "Türkçe",
        "ar" to "العربية",
        "hi" to "हिन्दी",
        "zh-Hans" to "简体中文",
        "zh-Hant" to "繁體中文",
        "ja" to "日本語",
        "ko" to "한국어",
        "in" to "Bahasa Indonesia",
        "fa" to "فارسی",
        "uk" to "Українська",
        "sv" to "Svenska"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language)) },
        text = {
            LazyColumn {
                items(languages) { (code, name) ->
                    ListItem(
                        modifier = Modifier.clickable { onLanguageSelected(code) },
                        headlineContent = { Text(name) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

