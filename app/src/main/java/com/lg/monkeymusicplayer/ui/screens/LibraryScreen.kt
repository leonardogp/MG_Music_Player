package com.lg.monkeymusicplayer.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import com.lg.monkeymusicplayer.core.cast.CastManager
import com.google.android.gms.cast.framework.CastState
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
import com.lg.monkeymusicplayer.core.feature.Feature
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass
 ) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val context = LocalContext.current
    var songForMenu by remember { mutableStateOf<Song?>(null) }
    var showSongMenu by remember { mutableStateOf(false) }
    var songToEdit by remember { mutableStateOf<Song?>(null) }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }

    // Escuchar eventos de edición solicitados desde el ViewModel
    LaunchedEffect(Unit) {
        viewModel.requestEditSongEvent.collect { song ->
            songToEdit = song
        }
    }

    // Mostrar feedback de actualización de tags
    LaunchedEffect(Unit) {
        viewModel.tagUpdateResult.collect { result ->
            when (result) {
                is Result.Success -> Toast.makeText(context, context.getString(R.string.tags_saved_ok), Toast.LENGTH_SHORT).show()
                is Result.Error -> Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    if (showSongMenu && songForMenu != null) {
        SongMenuSheet(
            song = songForMenu!!,
            onDismiss = { showSongMenu = false },
            onPlayNext = { 
                viewModel.addToQueue(it)
                Toast.makeText(context, context.getString(R.string.add_to_queue), Toast.LENGTH_SHORT).show()
                showSongMenu = false
            },
            onAddToPlaylist = { 
                songForPlaylist = it
                showSongMenu = false
            },
            onEditTags = { 
                songToEdit = it
                showSongMenu = false
            },
            onToggleFavorite = {
                viewModel.toggleFavorite(it)
                showSongMenu = false
            }
        )
    }

    if (songToEdit != null) {
        EditTagsDialog(
            song = songToEdit!!,
            isSaving = false,
            onDismiss = { songToEdit = null },
            onSave = { title, artist, album, genre ->
                viewModel.updateSongTags(songToEdit!!, title, artist, album, genre)
                songToEdit = null
            }
        )
    }

    if (songForPlaylist != null) {
        PlaylistPickerDialog(
            playlists = uiState.playlists,
            onDismiss = { songForPlaylist = null },
            onPlaylistSelected = { playlist ->
                viewModel.addSongToPlaylist(playlist.id.toString(), songForPlaylist!!)
                Toast.makeText(context, "${context.getString(R.string.add_to_playlist)}: ${playlist.name}", Toast.LENGTH_SHORT).show()
                songForPlaylist = null
            },
            onCreatePlaylist = { name -> viewModel.createPlaylist(name) }
        )
    }

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
                onMenuClick = { navController.navigate("settings") },
                onGenreClick = { genre -> navController.navigate("detail/genre/${Uri.encode(genre)}") },
                onArtistClick = { artist -> navController.navigate("detail/artist/${Uri.encode(artist)}") },
                onAlbumClick = { album -> navController.navigate("detail/album/${Uri.encode(album)}") },
                onFolderClick = { folder -> navController.navigate("detail/folder/${Uri.encode(folder)}") },
                onPlaylistClick = { playlist -> 
                    viewModel.loadPlaylistSongs(playlist.id.toString())
                    navController.navigate("playlist_detail/${playlist.id}/${Uri.encode(playlist.name)}")
                },
                onSmartPlaylistClick = { smart ->
                    navController.navigate("smart_playlist_detail/${smart.type.name}")
                },
                onSongMoreClick = { song ->
                    songForMenu = song
                    showSongMenu = true
                }
            )
        }
        composable("detail/{type}/{key}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: ""
            val key = backStackEntry.arguments?.getString("key") ?: ""
            val songs = when (type) {
                "genre" -> uiState.genres[key]
                "artist" -> uiState.artists[key]
                "album" -> uiState.albums[key]
                "folder" -> uiState.folders[key]
                else -> emptyList()
            } ?: emptyList()
            
            SongListDetailScreen(
                title = if (type == "folder") key.substringAfterLast("/") else key,
                songs = songs,
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onPlaySong = { song, list -> viewModel.playSong(song, list) },
                onMoreClick = { song ->
                    songForMenu = song
                    showSongMenu = true
                }
            )
        }
        composable("playlist_detail/{id}/{name}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: ""
            
            SongListDetailScreen(
                title = name,
                songs = uiState.currentPlaylistSongs,
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onPlaySong = { song, list -> viewModel.playSong(song, list) },
                onMoreClick = { song ->
                    songForMenu = song
                    showSongMenu = true
                }
            )
        }
        composable("smart_playlist_detail/{type}") { backStackEntry ->
            val typeStr = backStackEntry.arguments?.getString("type") ?: ""
            val type = try { SmartPlaylistType.valueOf(typeStr) } catch(e: Exception) { SmartPlaylistType.DAILY_MIX }
            val smart = uiState.smartPlaylists.find { it.type == type }
            val title = when (type) {
                SmartPlaylistType.DAILY_MIX -> stringResource(R.string.smart_daily_mix_title)
                SmartPlaylistType.REDISCOVER -> stringResource(R.string.smart_rediscover_title)
                SmartPlaylistType.TOP_SONGS -> stringResource(R.string.smart_top_songs_title)
            }
            
            SongListDetailScreen(
                title = title,
                songs = smart?.songs ?: emptyList(),
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onPlaySong = { song, list -> viewModel.playSong(song, list) },
                onMoreClick = { song ->
                    songForMenu = song
                    showSongMenu = true
                }
            )
        }
        composable("player") {
            val songToShow = uiState.playerState.currentSong ?: uiState.playerState.lastPlayedSong
            if (songToShow != null) {
                FullPlayerScreen(
                    song = songToShow,
                    queue = uiState.playerState.currentQueue,
                    lyrics = uiState.playerState.lyrics,
                    isLoadingLyrics = viewModel.isLoadingLyrics.collectAsState().value,
                    playerState = uiState.playerState,
                    onClose = { navController.popBackStack() },
                    onPlayPause = viewModel::togglePlayPause,
                    onSkipNext = viewModel::skipNext,
                    onSkipPrevious = viewModel::skipPrevious,
                    onSeekTo = viewModel::seekTo,
                    onSeekForward = viewModel::seekForward,
                    onSeekBack = viewModel::seekBack,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeatMode = viewModel::cycleRepeatMode,
                    onToggleFavorite = { songToShow?.let { viewModel.toggleFavorite(it) } },
                    onAddToPlaylist = { songForPlaylist = it },
                    onEditSong = { viewModel.requestEditSong(it) },
                    onPlayFromQueue = { viewModel.playSong(it, uiState.playerState.currentQueue) }
                )
            }
        }
        composable("settings") {
            SettingsScreen(
                uiState = uiState,
                viewModel = viewModel,
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
        composable("queues") {
            QueuesScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("cloud_sync") {
            CloudSyncScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("paywall") {
            PaywallScreen(
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
    viewModel: MusicViewModel,
    navController: androidx.navigation.NavController,
    onBack: () -> Unit,
    onScanMusic: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onChangeLanguage: (String) -> Unit
){
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
            // PRO upgrade CTA — solo visible si no tiene PRO
            if (!viewModel.featureGate.isProUnlocked()) {
                ListItem(
                    modifier = Modifier.clickable { navController.navigate("paywall") },
                    headlineContent = {
                        Text(
                            stringResource(R.string.pro_menu_item),
                            color = com.lg.monkeymusicplayer.ui.theme.PrimaryOrange,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = com.lg.monkeymusicplayer.ui.theme.PrimaryOrange
                        )
                    },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = com.lg.monkeymusicplayer.ui.theme.PrimaryOrange
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
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
                modifier = Modifier.clickable { navController.navigate("cloud_sync") },
                headlineContent = { Text(stringResource(R.string.cloud_sync_menu_item)) },
                supportingContent = { Text(stringResource(R.string.cloud_sync_subtitle)) },
                leadingContent = { Icon(Icons.Default.Cloud, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) }
            )
            ListItem(
                modifier = Modifier.clickable { navController.navigate("queues") },
                headlineContent = { Text(stringResource(R.string.queues_menu_item)) },
                leadingContent = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) }
            )
            ListItem(
                modifier = Modifier.clickable { navController.navigate("excluded_folders") },
                headlineContent = { Text(stringResource(R.string.excluded_folders)) },
                leadingContent = { Icon(Icons.Default.FolderOff, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) }
            )
        }
    }
}

@Composable
fun LibraryMainContent(
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
    onToggleFavorite: (Song) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onAddSongToPlaylist: (String, Song) -> Unit,
    onAddSongsToPlaylist: (String, List<Song>) -> Unit,
    onRemoveSongFromPlaylist: (String, Long) -> Unit,
    onLoadPlaylistSongs: (String) -> Unit,
    onUpdateSongTags: (Song, String, String, String, String) -> Unit,
    onOpenEqualizer: () -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onChangeLanguage: (String) -> Unit,
    onPlayerClick: () -> Unit,
    onMenuClick: () -> Unit,
    onGenreClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onFolderClick: (String) -> Unit,
    onPlaylistClick: (PlaylistEntity) -> Unit,
    onSmartPlaylistClick: (SmartPlaylist) -> Unit,
    onSongMoreClick: (Song) -> Unit
){
    // Performance: Memoize derived UI data to reduce recompositions
    val favoriteSongs = remember(uiState.songs) { uiState.songs.filter { it.isFavorite } }
    val recentSongs = remember(uiState.history, uiState.songs) {
        val songMap = uiState.songs.associateBy { it.id }
        uiState.history.mapNotNull { songMap[it.songId] }.distinctBy { it.id }.take(20)
    }

    val songsForTab = remember(uiState.songs) { uiState.songs }
    val playlistsForTab = remember(uiState.playlists) { uiState.playlists }
    val genresForTab = remember(uiState.genres) { uiState.genres }
    val artistsForTab = remember(uiState.artists) { uiState.artists }
    val albumsForTab = remember(uiState.albums) { uiState.albums }
    val foldersForTab = remember(uiState.folders) { uiState.folders }

    val tabs = listOf(
        stringResource(R.string.tab_main),
        stringResource(R.string.tab_songs),
        stringResource(R.string.tab_playlists),
        stringResource(R.string.tab_genres),
        stringResource(R.string.tab_artists),
        stringResource(R.string.tab_albums),
        stringResource(R.string.tab_folders)
    )
    
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                LibraryTopBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onMenuClick = onMenuClick
                )
                // Chips tipo pastilla (igual que la imagen de referencia)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(tabs) { index, title ->
                        val selected = pagerState.currentPage == index
                        Surface(
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            shape = RoundedCornerShape(50),
                            color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.White
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            PlayerBottomBar(
                playerState = uiState.playerState,
                onPlayPause = onPlayPause,
                onSkipNext = onSkipNext,
                onClick = onPlayerClick
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding).fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                0 -> HomeContent(
                    favoriteSongs = favoriteSongs,
                    recentSongs = recentSongs,
                    smartPlaylists = uiState.smartPlaylists,
                    onSongClick = { onPlay(it, uiState.songs) },
                    onSongMoreClick = onSongMoreClick,
                    onSmartPlaylistClick = { onSmartPlaylistClick(it) }
                )
                1 -> SongList(
                    songs = songsForTab,
                    currentSong = uiState.playerState.currentSong,
                    isPlaying = uiState.playerState.isPlaying,
                    onSongClick = { onPlay(it, songsForTab) },
                    onMoreClick = onSongMoreClick
                )
                2 -> PlaylistGrid(
                    playlists = playlistsForTab,
                    smartPlaylists = uiState.smartPlaylists,
                    onCreatePlaylist = onCreatePlaylist,
                    onPlaylistClick = onPlaylistClick,
                    onSmartPlaylistClick = onSmartPlaylistClick
                )
                3 -> GenreList(
                    genres = genresForTab,
                    onGenreClick = onGenreClick
                )
                4 -> ArtistList(
                    artists = artistsForTab,
                    onArtistClick = onArtistClick
                )
                5 -> AlbumGrid(
                    albums = albumsForTab,
                    onAlbumClick = onAlbumClick
                )
                6 -> FolderList(
                    folders = foldersForTab,
                    onFolderClick = onFolderClick
                )
            }
        }
    }
}

@Composable
fun HomeContent(
    favoriteSongs: List<Song>,
    recentSongs: List<Song>,
    smartPlaylists: List<SmartPlaylist>,
    onSongClick: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit,
    onSmartPlaylistClick: (SmartPlaylist) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Smart Playlists Horizontal
        if (smartPlaylists.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.tab_for_you))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(smartPlaylists) { smart ->
                        Box(modifier = Modifier.width(160.dp)) {
                            SmartPlaylistCard(smart, onClick = { onSmartPlaylistClick(smart) })
                        }
                    }
                }
            }
        }

        // Favorites Horizontal
        if (favoriteSongs.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.tab_favorites))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(favoriteSongs) { song ->
                        SongCard(song, onClick = { onSongClick(song) })
                    }
                }
            }
        }

        // Recently Played
        if (recentSongs.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.recent))
            }
            items(recentSongs) { song ->
                SongListItem(
                    song = song,
                    isSelected = false,
                    isPlaying = false,
                    onClick = { onSongClick(song) },
                    onMoreClick = { onSongMoreClick(song) }
                )
            }
        } else if (favoriteSongs.isEmpty() && smartPlaylists.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PrimaryOrange,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.smart_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.smart_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp).height(18.dp)
                .background(PrimaryOrange, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SongCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.albumArtUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.ic_monkey_head)
        )
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onMenuClick: () -> Unit,
    isSearchActive: Boolean = false
) {
    var text by remember { mutableStateOf(searchQuery) }
    LaunchedEffect(searchQuery) { if (text != searchQuery) text = searchQuery }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_monkey_head),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(50))
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            IconButton(onClick = {
                text = if (text.isEmpty()) "" else ""
                onSearchQueryChanged(text)
            }) {
                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
    // Barra de búsqueda expandible debajo del TopAppBar
    if (text.isNotEmpty() || isSearchActive) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; onSearchQueryChanged(it) },
            placeholder = { Text(stringResource(R.string.search)) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (text.isNotEmpty()) IconButton(onClick = { text = ""; onSearchQueryChanged("") }) {
                    Icon(Icons.Default.Close, null)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(50),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryOrange,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@Composable
fun SongList(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onMoreClick: (Song) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(songs) { song ->
            SongListItem(
                song = song,
                isSelected = song.id == currentSong?.id,
                isPlaying = isPlaying && song.id == currentSong?.id,
                onClick = { onSongClick(song) },
                onMoreClick = { onMoreClick(song) }
            )
        }
    }
}

@Composable
fun SongListItem(
    song: Song,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val bgColor = if (isSelected)
        PrimaryOrange.copy(alpha = 0.08f)
    else Color.Transparent

    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(bgColor),
        headlineContent = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = buildString {
                    append(song.artist)
                    if (song.album.isNotBlank()) append(" • ${song.album}")
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.albumArtUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_monkey_head)
                )
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = PrimaryOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
    HorizontalDivider(
        modifier = Modifier.padding(start = 76.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    )
}

@Composable
fun PlayerBottomBar(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit
) {
    // Mostrar siempre el mini player.
    // Si hay canción activa o última reproducida → mostrar info.
    // Si no hay ninguna → mostrar estado idle con texto indicativo.
    val song = playerState.currentSong ?: playerState.lastPlayedSong

    val progress = if (playerState.duration > 0L)
        (playerState.currentPosition.toFloat() / playerState.duration.toFloat()).coerceIn(0f, 1f)
    else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = song != null, onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
    ) {
        Column {
            // Barra de progreso naranja en la parte superior del mini player
            if (song != null && playerState.duration > 0L) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = PrimaryOrange,
                    trackColor = PrimaryOrange.copy(alpha = 0.15f)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (song == null) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, null,
                            tint = PrimaryOrange, modifier = Modifier.size(22.dp))
                    }
                    Text(
                        text = stringResource(R.string.player_idle_subtitle),
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    return@Row
                }
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_monkey_head)
                )
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = onSkipNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface)
                }
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
    playerState: PlayerState,
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
    onAddToPlaylist: (Song) -> Unit,
    onEditSong: (Song) -> Unit,
    onPlayFromQueue: (Song) -> Unit
) {
    // Pager: 0 = portada+controles, 1 = letras, 2 = cola
    val tabs = listOf(
        stringResource(R.string.tab_songs),
        stringResource(R.string.lyrics_tab),
        stringResource(R.string.queue_tab)
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    // EditTagsDialog
    var showEditDialog by remember { mutableStateOf(false) }
    if (showEditDialog) {
        EditTagsDialog(
            song = song,
            isSaving = false,
            onDismiss = { showEditDialog = false },
            onSave = { title, artist, album, genre ->
                onEditSong(song.copy(title = title, artist = artist, album = album, genre = genre))
                showEditDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (song.isFavorite) PrimaryOrange else LocalContentColor.current
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Tab row: Portada / Letras / Cola
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = PrimaryOrange
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    // ── Página 0: Portada + controles ──────────────────────
                    0 -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(280.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.ic_monkey_head)
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            song.title,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            song.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(24.dp))
                        MediaProgressSlider(
                            playerState = playerState,
                            onSeekTo = onSeekTo
                        )
                        PlayerControls(
                            playerState = playerState,
                            onPlayPause = onPlayPause,
                            onSkipNext = onSkipNext,
                            onSkipPrevious = onSkipPrevious,
                            onSeekBack = onSeekBack,
                            onSeekForward = onSeekForward,
                            onToggleFavorite = onToggleFavorite,
                            onToggleShuffle = onToggleShuffle,
                            onCycleRepeat = onCycleRepeatMode
                        )
                    }

                    // ── Página 1: Letras ────────────────────────────────────
                    1 -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isLoadingLyrics -> CircularProgressIndicator(color = PrimaryOrange)
                            lyrics.isEmpty() -> Text(
                                stringResource(R.string.lyrics_not_found),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            else -> LyricsView(
                                lyrics = lyrics,
                                currentPosition = playerState.currentPosition,
                                accentColor = playerState.accentColor,
                                onLyricClick = onSeekTo
                            )
                        }
                    }

                    // ── Página 2: Cola ──────────────────────────────────────
                    2 -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(queue, key = { it.id }) { queueSong ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        queueSong.title,
                                        fontWeight = if (queueSong.id == song.id) FontWeight.Bold else FontWeight.Normal,
                                        color = if (queueSong.id == song.id) PrimaryOrange else LocalContentColor.current
                                    )
                                },
                                supportingContent = { Text(queueSong.artist) },
                                leadingContent = {
                                    if (queueSong.id == song.id) {
                                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = PrimaryOrange)
                                    } else {
                                        AsyncImage(
                                            model = queueSong.albumArtUri,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop,
                                            error = painterResource(R.drawable.ic_monkey_head)
                                        )
                                    }
                                },
                                modifier = Modifier.clickable { onPlayFromQueue(queueSong) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistGrid(
    playlists: List<PlaylistEntity>,
    smartPlaylists: List<SmartPlaylist>,
    onCreatePlaylist: (String) -> Unit,
    onPlaylistClick: (PlaylistEntity) -> Unit,
    onSmartPlaylistClick: (SmartPlaylist) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    
    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            }
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable { showCreateDialog = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(48.dp))
                        Text(stringResource(R.string.new_playlist), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
        
        items(smartPlaylists) { smart ->
            SmartPlaylistCard(smart, onClick = { onSmartPlaylistClick(smart) })
        }

        items(playlists) { playlist ->
            PlaylistCard(playlist.name, stringResource(R.string.playlist), onPlaylistClick = { onPlaylistClick(playlist) })
        }
    }
}

@Composable
fun SmartPlaylistCard(smart: SmartPlaylist, onClick: () -> Unit) {
    val title = when (smart.type) {
        SmartPlaylistType.DAILY_MIX -> stringResource(R.string.smart_daily_mix_title)
        SmartPlaylistType.REDISCOVER -> stringResource(R.string.smart_rediscover_title)
        SmartPlaylistType.TOP_SONGS -> stringResource(R.string.smart_top_songs_title)
        else -> ""
    }
    val icon = when (smart.type) {
        SmartPlaylistType.DAILY_MIX -> Icons.Default.AutoAwesome
        SmartPlaylistType.REDISCOVER -> Icons.Default.History
        SmartPlaylistType.TOP_SONGS -> Icons.Default.Star
        else -> Icons.AutoMirrored.Filled.PlaylistPlay
    }
    
    val gradients = listOf(
        listOf(Color(0xFFFF8C00), Color(0xFFFF5500)),  // DAILY_MIX
        listOf(Color(0xFF7B2FBE), Color(0xFFFF8C00)),  // REDISCOVER
        listOf(Color(0xFF1DB954), Color(0xFF0D7A38))   // TOP_SONGS
    )
    val gradientColors = when (smart.type) {
        SmartPlaylistType.DAILY_MIX -> gradients[0]
        SmartPlaylistType.REDISCOVER -> gradients[1]
        SmartPlaylistType.TOP_SONGS -> gradients[2]
        else -> gradients[0]
    }

    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(56.dp).alpha(0.2f),
                tint = Color.White
            )
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                Text(stringResource(R.string.stats_artist_songs, smart.songs.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun PlaylistCard(name: String, subtitle: String, onPlaylistClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onPlaylistClick)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(64.dp).alpha(0.1f)
            )
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun GenreList(genres: Map<String, List<Song>>, onGenreClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(genres.keys.sorted()) { genre ->
            ListItem(
                modifier = Modifier.clickable { onGenreClick(genre) },
                headlineContent = { Text(genre) },
                supportingContent = { Text(stringResource(R.string.stats_artist_songs, genres[genre]?.size ?: 0)) },
                leadingContent = {
                    Box(
                        modifier = Modifier.size(48.dp)
                            .background(PrimaryOrange.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = PrimaryOrange)
                    }
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun ArtistList(artists: Map<String, List<Song>>, onArtistClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(artists.keys.sorted()) { artist ->
            ListItem(
                modifier = Modifier.clickable { onArtistClick(artist) },
                headlineContent = { Text(artist) },
                supportingContent = { Text(stringResource(R.string.stats_artist_songs, artists[artist]?.size ?: 0)) },
                leadingContent = {
                    Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                }
            )
        }
    }
}

@Composable
fun AlbumGrid(albums: Map<String, List<Song>>, onAlbumClick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(albums.keys.toList()) { album ->
            Card(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable { onAlbumClick(album) }) {
                Column {
                    AsyncImage(
                        model = albums[album]?.firstOrNull()?.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_monkey_head)
                    )
                    Text(
                        album,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun FolderList(folders: Map<String, List<Song>>, onFolderClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(folders.keys.sorted()) { path ->
            val folderName = path.substringAfterLast("/")
            ListItem(
                modifier = Modifier.clickable { onFolderClick(path) },
                headlineContent = { Text(folderName) },
                supportingContent = { Text(path) },
                leadingContent = {
                    Box(
                        modifier = Modifier.size(48.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                    }
                },
                trailingContent = {
                    Text(
                        "${folders[path]?.size ?: 0}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun SleepTimerDialog(currentMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var minutes by remember { mutableStateOf(if (currentMinutes > 0) currentMinutes.toString() else "30") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_timer)) },
        text = {
            Column {
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { if (it.all { c -> c.isDigit() }) minutes = it },
                    label = { Text("Minutes") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45, 60).forEach { min ->
                        AssistChip(onClick = { minutes = min.toString() }, label = { Text("${min}m") })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minutes.toIntOrNull() ?: 0) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(0) }) { Text("Off") }
        }
    )
}

@Composable
fun LanguageDialog(onDismiss: () -> Unit, onLanguageSelected: (String) -> Unit) {
    val languages = listOf(
        "" to "System Default",
        "en" to "English",
        "es" to "Español",
        "de" to "Deutsch",
        "fr" to "Français",
        "it" to "Italiano",
        "pt" to "Português",
        "ru" to "Русский",
        "uk" to "Українська",
        "ar" to "العربية",
        "fa" to "فارسی",
        "hi" to "हिन्दी",
        "ja" to "日本語",
        "ko" to "한국어",
        "zh" to "中文 (简体)",
        "zh-TW" to "中文 (繁體)",
        "id" to "Bahasa Indonesia",
        "sv" to "Svenska",
        "tr" to "Türkçe"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language)) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                items(languages) { (code, name) ->
                    ListItem(
                        modifier = Modifier.clickable { onLanguageSelected(code) },
                        headlineContent = { Text(name) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTagsDialog(
    song: Song,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, artist: String, album: String, genre: String) -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var genre by remember { mutableStateOf(song.genre) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_tags)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(stringResource(R.string.artist)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text(stringResource(R.string.album)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text(stringResource(R.string.genre)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, artist, album, genre) },
                enabled = !isSaving && title.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.save), color = PrimaryOrange)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
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
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(excludedFolders) { folder ->
                ListItem(
                    headlineContent = { Text(folder) },
                    trailingContent = {
                        IconButton(onClick = { onRemoveExcludedFolder(folder) }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListDetailScreen(
    title: String,
    songs: List<Song>,
    uiState: LibraryUiState,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onMoreClick: (Song) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (songs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_songs))
                }
            } else {
                SongList(
                    songs = songs,
                    currentSong = uiState.playerState.currentSong,
                    isPlaying = uiState.playerState.isPlaying,
                    onSongClick = { onPlaySong(it, songs) },
                    onMoreClick = onMoreClick
                )
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_playlist)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPickerDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (PlaylistEntity) -> Unit,
    onCreatePlaylist: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                item {
                    ListItem(
                        modifier = Modifier.clickable { showCreateDialog = true },
                        headlineContent = { Text(stringResource(R.string.new_playlist), color = PrimaryOrange) },
                        leadingContent = { Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryOrange) }
                    )
                }
                items(playlists) { playlist ->
                    ListItem(
                        modifier = Modifier.clickable { onPlaylistSelected(playlist) },
                        headlineContent = { Text(playlist.name) },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongMenuSheet(
    song: Song,
    onDismiss: () -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onEditTags: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header con info de la canción
            ListItem(
                headlineContent = { Text(song.title, fontWeight = FontWeight.Bold) },
                supportingContent = { Text("${song.artist} • ${song.album}") },
                leadingContent = {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_monkey_head)
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            ListItem(
                modifier = Modifier.clickable { onPlayNext(song) },
                headlineContent = { Text(stringResource(R.string.add_to_queue)) },
                leadingContent = { Icon(Icons.Default.QueueMusic, contentDescription = null) }
            )
            ListItem(
                modifier = Modifier.clickable { onAddToPlaylist(song) },
                headlineContent = { Text(stringResource(R.string.add_to_playlist)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) }
            )
            ListItem(
                modifier = Modifier.clickable { onToggleFavorite(song) },
                headlineContent = { 
                    Text(if (song.isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites)) 
                },
                leadingContent = { 
                    Icon(if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null) 
                }
            )
            ListItem(
                modifier = Modifier.clickable { onEditTags(song) },
                headlineContent = { Text(stringResource(R.string.edit_tags)) },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
        }
    }
}
