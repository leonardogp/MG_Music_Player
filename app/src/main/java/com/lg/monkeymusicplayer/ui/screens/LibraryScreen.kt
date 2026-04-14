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
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.draw.alpha
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
import com.lg.monkeymusicplayer.data.model.SmartPlaylist
import com.lg.monkeymusicplayer.data.model.SmartPlaylistType
import com.lg.monkeymusicplayer.data.repository.BackupRepository
import com.lg.monkeymusicplayer.data.repository.StatsRepository
import com.lg.monkeymusicplayer.ui.LibraryLoadState
import com.lg.monkeymusicplayer.data.repository.ExcludedFoldersRepository
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass
) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()

    SharedTransitionLayout {
    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            LibraryMainContent(
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this@composable,
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
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
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
        composable("excluded_folders") {
            val excludedFolders by viewModel.excludedFolders.collectAsState()
            ExcludedFoldersScreen(
                excludedFolders = excludedFolders,
                onBack = { navController.popBackStack() },
                onScanMusic = { viewModel.scanMusic() },
                onAddExcludedFolder = viewModel::addExcludedFolder,
                onRemoveExcludedFolder = viewModel::removeExcludedFolder
            )
        }
        composable("equalizer") {
            EqualizerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("stats") {
            StatsScreen(
                statsRepository = viewModel.statsRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable("backup") {
            BackupScreen(
                backupRepository = viewModel.backupRepository,
                onBack = { navController.popBackStack() }
            )
        }
    }
    } // SharedTransitionLayout
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ListItem(
                modifier = Modifier.clickable { onScanMusic() },
                headlineContent = { Text(stringResource(R.string.scan_music)) },
                leadingContent = {
                    val scanning = uiState.loadState as? LibraryLoadState.Scanning
                    if (scanning != null) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                },
                supportingContent = {
                    val scanning = uiState.loadState as? LibraryLoadState.Scanning
                    when {
                        scanning != null -> Column {
                            LinearProgressIndicator(
                                progress = { scanning.fraction },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                            if (!scanning.isIndeterminate) {
                                Text(
                                    stringResource(R.string.scanning_progress, scanning.progress, scanning.total),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        uiState.loadState is LibraryLoadState.Error -> Text(
                            (uiState.loadState as LibraryLoadState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        else -> {}
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
            ListItem(
                modifier = Modifier.clickable { navController.navigate("stats") },
                headlineContent = { Text(stringResource(R.string.stats_menu_item)) },
                leadingContent = { Icon(Icons.Default.BarChart, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) }
            )
            ListItem(
                modifier = Modifier.clickable { navController.navigate("backup") },
                headlineContent = { Text(stringResource(R.string.backup_menu_item)) },
                leadingContent = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) }
            )
            ListItem(
                modifier = Modifier.clickable { navController.navigate("excluded_folders") },
                headlineContent = { Text(stringResource(R.string.excluded_folders)) },
                supportingContent = { Text(stringResource(R.string.excluded_folders_subtitle)) },
                leadingContent = { Icon(Icons.Default.FolderOff, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) }
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LibraryMainContent(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope,
    uiState: LibraryUiState,
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

    var isSavingTags by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val hasManageStoragePermission by viewModel.hasManageStoragePermission.collectAsState()

    val strTagsSavedOk = stringResource(R.string.tags_saved_ok)
    val strTagsSaveError = stringResource(R.string.tags_save_error)
    val strTagsPermissionNeeded = stringResource(R.string.tags_permission_needed)
    val strTagsPermissionGrant = stringResource(R.string.tags_permission_grant)
    LaunchedEffect(Unit) {
        viewModel.tagUpdateResult.collect { result ->
            isSavingTags = false
            editingSong = null
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

    LaunchedEffect(Unit) {
        viewModel.requestEditSongEvent.collect { song ->
            editingSong = song
        }
    }

    if (editingSong != null) {
        EditTagsDialog(
            song = editingSong!!,
            isSaving = isSavingTags,
            onDismiss = {
                if (!isSavingTags) editingSong = null
            },
            onSave = { title, artist, album, genre ->
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
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MobileLayout(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope,
    uiState: LibraryUiState,
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
        R.string.tab_for_you,
        R.string.tab_main,
        R.string.tab_songs,
        R.string.tab_genres,
        R.string.tab_artists,
        R.string.tab_albums,
        R.string.tab_folders,
        R.string.tab_playlists,
        R.string.tab_favorites
    )
    var selectedCategoryItem by remember { mutableStateOf<String?>(null) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
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
                                            3 -> R.string.search_genres
                                            4 -> R.string.search_artists
                                            5 -> R.string.search_albums
                                            6 -> R.string.search_folders
                                            7 -> R.string.search_playlists
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
                                if (currentTab in 3..6 && selectedCategoryItem != null) {
                                    Text(text = selectedCategoryItem!!, fontWeight = FontWeight.Bold)
                                } else if (currentTab == 7 && selectedPlaylistId != null) {
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
                        if (!isSearchActive && ((currentTab in 3..6 && selectedCategoryItem != null) || (currentTab == 7 && selectedPlaylistId != null))) {
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
                            if (pagerState.currentPage == 2 && selectedCategoryItem == null) {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
                                }
                                if (showSortMenu) {
                                    ModalBottomSheet(
                                        onDismissRequest = { showSortMenu = false },
                                        containerColor = Color(0xFF181818),
                                        dragHandle = {
                                            Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                                                contentAlignment = Alignment.Center) {
                                                Box(modifier = Modifier.width(40.dp).height(4.dp)
                                                    .background(Color.White.copy(0.25f), RoundedCornerShape(2.dp)))
                                            }
                                        }
                                    ) {
                                        Text(stringResource(R.string.sort), color = Color.White, fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                                        HorizontalDivider(color = Color.White.copy(0.08f), modifier = Modifier.padding(horizontal = 20.dp))
                                        Spacer(Modifier.height(8.dp))
                                        SortOrder.entries.forEach { order ->
                                            Row(modifier = Modifier.fillMaxWidth()
                                                .clickable { onSortOrderChanged(order); showSortMenu = false }
                                                .padding(horizontal = 20.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(40.dp).background(PrimaryOrange.copy(0.1f), RoundedCornerShape(10.dp)),
                                                    contentAlignment = Alignment.Center) {
                                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null,
                                                        tint = PrimaryOrange, modifier = Modifier.size(20.dp))
                                                }
                                                Spacer(Modifier.width(16.dp))
                                                Text(order.name, color = Color.White.copy(0.9f), style = MaterialTheme.typography.bodyLarge)
                                            }
                                        }
                                        Spacer(Modifier.height(24.dp))
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
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                currentSong = uiState.playerState.currentSong,
                lastPlayedSong = uiState.playerState.lastPlayedSong,
                isPlaying = uiState.playerState.isPlaying,
                currentPosition = uiState.playerState.currentPosition,
                duration = uiState.playerState.duration,
                onPlayPause = onPlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onPlayerClick = onPlayerClick
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
                0 -> ForYouTab(
                    smartPlaylists = uiState.smartPlaylists,
                    onPlay = onPlay,
                    onAddToQueue = onAddToQueue,
                    onAddSongToPlaylist = onAddSongToPlaylist,
                    onEditSong = onEditSong,
                    onToggleFavorite = onToggleFavorite
                )
                1 -> {
                    val songIndex = remember(uiState.songs) { uiState.songs.associateBy { it.id } }
                    val historySongs = remember(uiState.history, songIndex) {
                        uiState.history.mapNotNull { songIndex[it.songId] }.distinctBy { it.id }
                    }
                    MainTab(uiState.songs, historySongs, onPlay, onAddToQueue, onAddSongToPlaylist, onEditSong, onToggleFavorite)
                }
                2 -> SongList(uiState.songs, uiState.songs, onPlay, onAddToQueue, onAddSongToPlaylist, null, onEditSong, onToggleFavorite)
                3 -> CategoryNavigation(uiState.genres, selectedCategoryItem, onPlay, onAddToQueue, { selectedCategoryItem = it }, Icons.Default.LibraryMusic, onAddSongToPlaylist, onEditSong, onToggleFavorite)
                4 -> CategoryNavigation(uiState.artists, selectedCategoryItem, onPlay, onAddToQueue, { selectedCategoryItem = it }, Icons.Default.Person, onAddSongToPlaylist, onEditSong, onToggleFavorite)
                5 -> CategoryNavigation(uiState.albums, selectedCategoryItem, onPlay, onAddToQueue, { selectedCategoryItem = it }, Icons.Default.Album, onAddSongToPlaylist, onEditSong, onToggleFavorite)
                6 -> CategoryNavigation(uiState.folders, selectedCategoryItem, onPlay, onAddToQueue, { selectedCategoryItem = it }, Icons.Default.Folder, onAddSongToPlaylist, onEditSong, onToggleFavorite)
                7 -> {
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
                8 -> {
                    val favSongs = remember(uiState.songs) { uiState.songs.filter { it.isFavorite } }
                    SongList(favSongs, favSongs, onPlay, onAddToQueue, onAddSongToPlaylist, null, onEditSong, onToggleFavorite)
                }
            }
        }
    }
}

@Composable
fun ForYouTab(
    smartPlaylists: List<SmartPlaylist>,
    onPlay: (Song, List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onEditSong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit
) {
    if (smartPlaylists.isEmpty()) {
        // Estado vacío — el usuario aún no tiene suficientes reproducciones
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null,
                    modifier = Modifier.size(64.dp), tint = PrimaryOrange.copy(0.5f))
                Text(stringResource(R.string.smart_empty_title),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = Color.White, textAlign = TextAlign.Center)
                Text(stringResource(R.string.smart_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.55f), textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp))
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        smartPlaylists.forEach { playlist ->
            if (playlist.isEmpty) return@forEach

            item {
                SmartPlaylistSection(
                    playlist = playlist,
                    onPlay = onPlay,
                    onAddToQueue = onAddToQueue,
                    onAddSongToPlaylist = onAddSongToPlaylist,
                    onEditSong = onEditSong,
                    onToggleFavorite = onToggleFavorite
                )
            }
        }
    }
}

@Composable
private fun SmartPlaylistSection(
    playlist: SmartPlaylist,
    onPlay: (Song, List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onEditSong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit
) {
    val (titleRes, subtitleRes, icon) = when (playlist.type) {
        SmartPlaylistType.DAILY_MIX -> Triple(
            R.string.smart_daily_mix_title,
            R.string.smart_daily_mix_subtitle,
            Icons.Default.AutoAwesome
        )
        SmartPlaylistType.REDISCOVER -> Triple(
            R.string.smart_rediscover_title,
            R.string.smart_rediscover_subtitle,
            Icons.Default.History
        )
        SmartPlaylistType.TOP_SONGS -> Triple(
            R.string.smart_top_songs_title,
            R.string.smart_top_songs_subtitle,
            Icons.Default.TrendingUp
        )
    }

    Column {
        // Header de sección
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp)
                    .background(PrimaryOrange.copy(0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(titleRes), color = Color.White,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(subtitleRes), color = Color.White.copy(0.55f),
                    style = MaterialTheme.typography.bodySmall)
            }
            // Botón play all
            IconButton(
                onClick = { if (playlist.songs.isNotEmpty()) onPlay(playlist.songs.first(), playlist.songs) }
            ) {
                Box(
                    modifier = Modifier.size(36.dp)
                        .background(PrimaryOrange, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play_pause),
                        tint = Color.Black, modifier = Modifier.size(20.dp))
                }
            }
        }

        HorizontalDivider(
            color = Color.White.copy(0.06f),
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        // Lista de canciones (máx 10 visibles, sin scroll propio)
        playlist.songs.take(10).forEach { song ->
            SongItem(
                song = song,
                contextPlaylist = playlist.songs,
                onPlay = onPlay,
                onAddToQueue = onAddToQueue,
                onAddSongToPlaylist = onAddSongToPlaylist,
                onEditSong = onEditSong,
                onToggleFavorite = onToggleFavorite
            )
        }
    }
}


@Composable
fun MainTab(
    songs: List<Song>,
    historySongs: List<Song>,
    onPlay: (Song, List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onEditSong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit
) {
    val favoriteSongs = remember(songs) { songs.filter { it.isFavorite } }
    val recentSongs   = historySongs.take(8)
    val greeting      = getGreeting()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(greeting),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        item(span = { GridItemSpan(2) }) {
            Text(
                text = stringResource(R.string.favorites),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (favoriteSongs.isNotEmpty()) {
            items(
                items = favoriteSongs.take(6),
                span  = { GridItemSpan(1) }
            ) { song ->
                FavoriteGridItem(song, onClick = { onPlay(song, favoriteSongs) })
            }
        } else {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .height(80.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.no_favorites_yet),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(12.dp))
        }
        item(span = { GridItemSpan(2) }) {
            Text(
                text = stringResource(R.string.recent),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (recentSongs.isNotEmpty()) {
            items(
                items = recentSongs,
                span  = { GridItemSpan(1) }
            ) { song ->
                HistoryGridItem(
                    song    = song,
                    onClick = { onPlay(song, recentSongs) }
                )
            }
        } else {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .height(80.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.no_songs),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
fun HistoryGridItem(song: Song, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_monkey_head)
            )
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerBottomBar(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope,
    currentSong: Song?,
    lastPlayedSong: Song?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onPlayerClick: () -> Unit
) {
    val displaySong = currentSong ?: lastPlayedSong
    val isActive = currentSong != null
    val isIdle = currentSong == null

    Surface(
        tonalElevation = 8.dp,
        modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (isActive) Modifier.clickable { onPlayerClick() } else Modifier)
            .shadow(10.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp).copy(alpha = 0.95f)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (displaySong != null) {
                    with(sharedTransitionScope) {
                    AsyncImage(
                        model = displaySong.albumArtUri,
                        contentDescription = null,
                        error = painterResource(R.drawable.ic_monkey_head),
                        modifier = Modifier
                            .sharedElement(
                                state = rememberSharedContentState(key = "album_art"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    androidx.compose.animation.core.spring(
                                        dampingRatio = 0.8f,
                                        stiffness = 380f
                                    )
                                }
                            )
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .then(
                                if (isIdle) Modifier.alpha(0.45f) else Modifier
                            ),
                        contentScale = ContentScale.Crop
                    )
                    } // with(sharedTransitionScope)
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            displaySong != null -> displaySong.title
                            else -> stringResource(R.string.player_idle_title)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = when {
                                isActive -> 1f
                                displaySong != null -> 0.5f
                                else -> 0.35f
                            }
                        )
                    )
                    Text(
                        text = when {
                            displaySong != null -> displaySong.artist
                            else -> stringResource(R.string.player_idle_subtitle)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = when {
                                isActive -> 1f
                                displaySong != null -> 0.45f
                                else -> 0.3f
                            }
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSkipPrevious, enabled = isActive) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = stringResource(R.string.previous),
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (isActive) 1f else 0.25f
                            )
                        )
                    }
                    IconButton(onClick = onPlayPause, enabled = isActive) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.play_pause),
                            modifier = Modifier.size(36.dp),
                            tint = if (isActive) PrimaryOrange
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                    }
                    IconButton(onClick = onSkipNext, enabled = isActive) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = stringResource(R.string.next),
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (isActive) 1f else 0.25f
                            )
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = {
                    if (isActive && duration > 0) currentPosition.toFloat() / duration
                    else 0f
                },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = PrimaryOrange,
                trackColor = Color.Transparent
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FullPlayerScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope,
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
                Box(modifier = Modifier.size(48.dp))
            }

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

            when (selectedTab) {
                0 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        with(sharedTransitionScope) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(song.albumArtUri).crossfade(false).build(),
                            contentDescription = null,
                            error = painterResource(R.drawable.ic_monkey_head),
                            modifier = Modifier
                                .sharedElement(
                                    state = rememberSharedContentState(key = "album_art"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    boundsTransform = { _, _ ->
                                        androidx.compose.animation.core.spring(
                                            dampingRatio = 0.8f,
                                            stiffness = 380f
                                        )
                                    }
                                )
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(8.dp)
                                .shadow(20.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        } // with(sharedTransitionScope)

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

@OptIn(ExperimentalMaterial3Api::class)
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
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF181818),
            dragHandle = {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.width(40.dp).height(4.dp)
                        .background(Color.White.copy(0.25f), RoundedCornerShape(2.dp)))
                }
            }
        ) {
            // Header con info de la canción
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    error = painterResource(R.drawable.ic_monkey_head),
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, color = Color.White, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium)
                    Text(song.artist, color = Color.White.copy(0.55f), maxLines = 1,
                        overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider(color = Color.White.copy(0.08f), modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))

            // Opciones
            SongSheetOption(
                icon = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = stringResource(if (song.isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites),
                iconTint = if (song.isFavorite) Color(0xFFFF4D6D) else Color.White.copy(0.85f)
            ) { onToggleFavorite(song); showSheet = false }

            SongSheetOption(Icons.AutoMirrored.Filled.PlaylistAdd, stringResource(R.string.add_to_queue)) {
                onAddToQueue(song); showSheet = false
            }
            SongSheetOption(Icons.AutoMirrored.Filled.QueueMusic, stringResource(R.string.add_to_playlist)) {
                onAddSongToPlaylist(song); showSheet = false
            }
            if (onRemoveFromPlaylist != null) {
                SongSheetOption(Icons.Default.Delete, stringResource(R.string.remove_from_playlist),
                    iconTint = Color(0xFFFF4D6D)) {
                    onRemoveFromPlaylist(); showSheet = false
                }
            }
            SongSheetOption(Icons.Default.Edit, stringResource(R.string.edit_tags)) {
                onEditSong(song); showSheet = false
            }

            Spacer(Modifier.height(24.dp))
        }
    }

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
            IconButton(onClick = { showSheet = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.options))
            }
        }
    )
}

@Composable
private fun SongSheetOption(
    icon: ImageVector,
    label: String,
    iconTint: Color = Color.White.copy(0.85f),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp)
                .background(Color.White.copy(0.07f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(label, color = Color.White.copy(0.9f), style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
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
                            if (showMenu) {
                                ModalBottomSheet(
                                    onDismissRequest = { showMenu = false },
                                    containerColor = Color(0xFF181818),
                                    dragHandle = {
                                        Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                                            contentAlignment = Alignment.Center) {
                                            Box(modifier = Modifier.width(40.dp).height(4.dp)
                                                .background(Color.White.copy(0.25f), RoundedCornerShape(2.dp)))
                                        }
                                    }
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(56.dp).background(Color.White.copy(0.07f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center) {
                                            Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null,
                                                tint = PrimaryOrange, modifier = Modifier.size(28.dp))
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Text(playlist.name, color = Color.White, fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium)
                                    }
                                    HorizontalDivider(color = Color.White.copy(0.08f), modifier = Modifier.padding(horizontal = 20.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth().clickable { onDeletePlaylist(playlist); showMenu = false }
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(40.dp).background(Color(0xFFFF4D6D).copy(0.12f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Delete, contentDescription = null,
                                                tint = Color(0xFFFF4D6D), modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(Modifier.width(16.dp))
                                        Text(stringResource(R.string.delete_playlist), color = Color(0xFFFF4D6D),
                                            style = MaterialTheme.typography.bodyLarge)
                                    }
                                    Spacer(Modifier.height(24.dp))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddToPlaylistDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(40.dp).height(4.dp)
                    .background(Color.White.copy(0.25f), RoundedCornerShape(2.dp)))
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(PrimaryOrange.copy(0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.add_to_playlist), color = Color.White, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(color = Color.White.copy(0.08f), modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))

        // Create new
        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onCreateNew)
            .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(PrimaryOrange.copy(0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(stringResource(R.string.create_new_playlist), color = PrimaryOrange,
                fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
        }

        if (playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_playlists), color = Color.White.copy(0.4f))
            }
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(playlists) { playlist ->
                    Row(modifier = Modifier.fillMaxWidth()
                        .clickable { onPlaylistSelected(playlist.id.toString()) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(Color.White.copy(0.07f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null,
                                tint = Color.White.copy(0.7f), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(playlist.name, color = Color.White.copy(0.9f), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(40.dp).height(4.dp)
                    .background(Color.White.copy(0.25f), RoundedCornerShape(2.dp)))
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(PrimaryOrange.copy(0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.new_playlist), color = Color.White, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(color = Color.White.copy(0.08f), modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = name, onValueChange = { name = it },
            placeholder = { Text(stringResource(R.string.playlist_name), color = Color.White.copy(0.35f)) },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryOrange, unfocusedBorderColor = Color.White.copy(0.2f),
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                cursorColor = PrimaryOrange),
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = Color.White.copy(0.5f)) }
        )
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.2f))) {
                Text(stringResource(R.string.cancel), color = Color.White.copy(0.8f))
            }
            Button(onClick = { if (name.isNotBlank()) onConfirm(name) }, enabled = name.isNotBlank(),
                modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)) {
                Text(stringResource(R.string.create), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        containerColor = Color(0xFF181818),
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(40.dp).height(4.dp)
                    .background(Color.White.copy(0.25f), RoundedCornerShape(2.dp)))
            }
        }
    ) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(PrimaryOrange.copy(0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.edit_tags), color = Color.White, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(color = Color.White.copy(0.08f), modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(16.dp))

        // Fields
        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryOrange,
                unfocusedBorderColor = Color.White.copy(0.2f),
                focusedLabelColor = PrimaryOrange,
                unfocusedLabelColor = Color.White.copy(0.5f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = PrimaryOrange
            )
            OutlinedTextField(value = title, onValueChange = { if (!isSaving) title = it },
                label = { Text(stringResource(R.string.title)) }, enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(), colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.5f)) })
            OutlinedTextField(value = artist, onValueChange = { if (!isSaving) artist = it },
                label = { Text(stringResource(R.string.artist)) }, enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(), colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.White.copy(0.5f)) })
            OutlinedTextField(value = album, onValueChange = { if (!isSaving) album = it },
                label = { Text(stringResource(R.string.album)) }, enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(), colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Album, null, tint = Color.White.copy(0.5f)) })
            OutlinedTextField(value = genre, onValueChange = { if (!isSaving) genre = it },
                label = { Text(stringResource(R.string.genre)) }, enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(), colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Category, null, tint = Color.White.copy(0.5f)) })
        }

        Spacer(Modifier.height(20.dp))

        // Buttons
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onDismiss, enabled = !isSaving,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.2f))
            ) { Text(stringResource(R.string.cancel), color = Color.White.copy(0.8f)) }

            Button(
                onClick = { onSave(title, artist, album, genre) }, enabled = !isSaving,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.tags_saving), color = Color.White)
                } else {
                    Text(stringResource(R.string.save), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcludedFoldersScreen(
    excludedFolders: List<String>,
    onBack: () -> Unit,
    onScanMusic: () -> Unit,
    onAddExcludedFolder: (String) -> Unit,
    onRemoveExcludedFolder: (String) -> Unit
) {
    val whatsappPaths = ExcludedFoldersRepository.buildDefaultExclusions()
    val allWhatsappExcluded = whatsappPaths.all { excludedFolders.contains(it) }
    var exclusionsChanged by remember { mutableStateOf(false) }

    // SAF: ACTION_OPEN_DOCUMENT_TREE abre el explorador nativo del sistema.
    // El usuario navega y selecciona una carpeta; el resultado es un Uri tipo
    // content://com.android.externalstorage.documents/tree/primary:Music/...
    // que convertimos a ruta absoluta (/storage/emulated/0/Music/...).
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val absolutePath = resolveTreeUriToPath(uri)
            if (absolutePath != null) {
                onAddExcludedFolder(absolutePath)
                exclusionsChanged = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.excluded_folders)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                Text(
                    stringResource(R.string.excluded_folders_desc),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                ListItem(
                    modifier = Modifier.clickable {
                        if (allWhatsappExcluded) whatsappPaths.forEach { onRemoveExcludedFolder(it) }
                        else whatsappPaths.forEach { onAddExcludedFolder(it) }
                        exclusionsChanged = true
                    },
                    headlineContent = { Text(stringResource(R.string.exclude_whatsapp)) },
                    supportingContent = {
                        Text(
                            stringResource(R.string.exclude_whatsapp_desc),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingContent = {
                        Icon(
                            if (allWhatsappExcluded) Icons.Default.Block else Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = if (allWhatsappExcluded) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = allWhatsappExcluded,
                            onCheckedChange = { checked ->
                                if (checked) whatsappPaths.forEach { onAddExcludedFolder(it) }
                                else whatsappPaths.forEach { onRemoveExcludedFolder(it) }
                                exclusionsChanged = true
                            }
                        )
                    }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    modifier = Modifier.clickable {
                        // Lanzar el explorador de archivos nativo del sistema.
                        // null = sin URI inicial (el OS decide dónde empezar).
                        folderPickerLauncher.launch(null)
                    },
                    headlineContent = { Text(stringResource(R.string.add_excluded_folder)) },
                    leadingContent = {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                    }
                )
                HorizontalDivider()
            }

            if (excludedFolders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.excluded_folders_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(excludedFolders, key = { it }) { path ->
                    ListItem(
                        headlineContent = {
                            Text(
                                path.substringAfterLast('/').ifBlank { path },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text(
                                path,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.FolderOff,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                onRemoveExcludedFolder(path)
                                exclusionsChanged = true
                            }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }

            if (exclusionsChanged) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                stringResource(R.string.rescan_required),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                onScanMusic()
                                exclusionsChanged = false
                            }) {
                                Text(stringResource(R.string.scan_music))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Convierte un Uri devuelto por ACTION_OPEN_DOCUMENT_TREE a ruta absoluta del filesystem.
 *
 * El SAF devuelve Uris con formato:
 *   content://com.android.externalstorage.documents/tree/primary:<relative_path>
 *   content://com.android.externalstorage.documents/tree/<volumeId>:<relative_path>
 *
 * Estrategia:
 *  1. Extraer el treeDocumentId via DocumentsContract.
 *  2. Parsear volumeId y relativePath del documentId.
 *  3. Mapear "primary" → Environment.getExternalStorageDirectory().
 *     Otros volumeIds → /storage/<volumeId> (tarjetas SD externas).
 *
 * Retorna null si el Uri no puede resolverse (proveedor de documentos de terceros,
 * Google Drive, etc.) — casos donde no hay ruta filesystem real.
 */
private fun resolveTreeUriToPath(uri: Uri): String? {
    return try {
        val docId = DocumentsContract.getTreeDocumentId(uri)
        // docId tiene forma "primary:Music/Podcasts" o "1A2B-3C4D:Folder"
        val parts = docId.split(":", limit = 2)
        if (parts.size < 2) return null

        val volumeId = parts[0]
        val relativePath = parts[1]

        val root = when (volumeId.lowercase()) {
            "primary" -> Environment.getExternalStorageDirectory().absolutePath
            else -> "/storage/$volumeId"
        }

        val result = if (relativePath.isEmpty()) root else "$root/$relativePath"
        result.trimEnd('/')
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val options = listOf(0, 5, 15, 30, 45, 60, 90, 120)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(40.dp).height(4.dp)
                    .background(Color.White.copy(0.25f), RoundedCornerShape(2.dp)))
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(PrimaryOrange.copy(0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Timer, null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.sleep_timer), color = Color.White, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(color = Color.White.copy(0.08f), modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))

        options.forEach { minutes ->
            val isSelected = currentMinutes == minutes
            Row(modifier = Modifier.fillMaxWidth()
                .clickable { onConfirm(minutes); onDismiss() }
                .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (minutes == 0) stringResource(R.string.disabled) else stringResource(R.string.minutes, minutes),
                    color = if (isSelected) PrimaryOrange else Color.White.copy(0.85f),
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (isSelected) {
                    Icon(Icons.Default.Check, null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(40.dp).height(4.dp)
                    .background(Color.White.copy(0.25f), RoundedCornerShape(2.dp)))
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(PrimaryOrange.copy(0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Language, null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.language), color = Color.White, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(color = Color.White.copy(0.08f), modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.heightIn(max = 460.dp)) {
            items(languages) { (code, name) ->
                Row(modifier = Modifier.fillMaxWidth()
                    .clickable { onLanguageSelected(code); onDismiss() }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = Color.White.copy(0.9f), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
