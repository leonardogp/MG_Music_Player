package com.lg.monkeymusicplayer.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import androidx.navigation.NavController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.data.database.PlaylistEntity
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.ui.SortOrder
import com.lg.monkeymusicplayer.ui.PlayerState
import com.lg.monkeymusicplayer.ui.LibraryUiState
import com.lg.monkeymusicplayer.ui.MusicViewModel
import com.lg.monkeymusicplayer.core.result.Result
import com.lg.monkeymusicplayer.ui.components.core.MonkeyPlayerBottomBar
import com.lg.monkeymusicplayer.ui.components.MediaProgressSlider
import com.lg.monkeymusicplayer.ui.theme.PrimaryOrange
import kotlinx.coroutines.launch
import com.lg.monkeymusicplayer.util.TimeFormatter
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.lg.monkeymusicplayer.ui.components.LyricsView
import com.lg.monkeymusicplayer.data.model.SmartPlaylist
import com.lg.monkeymusicplayer.data.model.SmartPlaylistType
import android.net.Uri
import com.lg.monkeymusicplayer.ui.components.core.MonkeySearchBar
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.lg.monkeymusicplayer.ui.components.core.PlaybackWaveform

@SuppressLint("LocalContextGetResourceValueCall")
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
                navController = navController,
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
                onRemoveSongFromPlaylist = { playlistId, songId -> viewModel.removeSongFromPlaylist(playlistId, songId)},
                onLoadPlaylistSongs = viewModel::loadPlaylistSongs,
                onUpdateSongTags = { song, t, a, al, g -> viewModel.updateSongTags(song, t, a, al, g) },
                onOpenEqualizer = { navController.navigate("equalizer") },
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
                onOpenEqualizer = { navController.navigate("equalizer") },
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
        composable("paywall") {
            PaywallScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun LibraryMainContent(
    uiState: LibraryUiState,
    viewModel: MusicViewModel,
    navController: NavController,
    onSearchQueryChanged: (String) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    onPlayPause: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onScanMusic: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSearchOpen: () -> Unit = {},
    onSeekForward: () -> Unit,
    onSeekBack: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onReorderPlaylistSongs: (String, List<Song>) -> Unit = { _, _ -> },
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onAddSongToPlaylist: (String, Song) -> Unit,
    onAddSongsToPlaylist: (String, List<Song>) -> Unit,
    onRemoveSongFromPlaylist: (String, Song) -> Unit,
    onRemovePlaylist: (PlaylistEntity) -> Unit = {},
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
) {
    val favoriteSongs = remember(uiState.songs) {
        uiState.songs.filter { it.isFavorite }
    }

    val recentSongs = remember(uiState.history, uiState.songs) {
        val songMap = uiState.songs.associateBy { it.id }
        uiState.history.mapNotNull { songMap[it.songId] }
            .distinctBy { it.id }
            .take(20)
    }

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
    val tabsListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        tabsListState.animateScrollToItem(pagerState.currentPage)
    }

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = "Monkey Music",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Reactive audio jungle",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(16.dp))
                    }

                    LibraryTopBar(
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onMenuClick = onMenuClick
                    )

                    LazyRow(
                        state = tabsListState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(tabs) { index, title ->
                            val selected = pagerState.currentPage == index

                            Surface(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                shape = RoundedCornerShape(22.dp),
                                color = if (selected)
                                    PrimaryOrange
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = if (selected) 8.dp else 2.dp,
                                shadowElevation = if (selected) 12.dp else 0.dp
                            ) {
                                Text(
                                    text = title,
                                    modifier = Modifier.padding(
                                        horizontal = 18.dp,
                                        vertical = 10.dp
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected)
                                        FontWeight.Bold
                                    else
                                        FontWeight.Medium,
                                    color = if (selected)
                                        Color.White
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },

        bottomBar = {
            MonkeyPlayerBottomBar(
                playerState = uiState.playerState,
                onPlayPause = onPlayPause,
                onSkipNext = onSkipNext,
                onClick = onPlayerClick
            )
        }
    ) { padding ->

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            beyondViewportPageCount = 1
        ) { page ->

            when (page) {
                0 -> HomeContent(
                    favoriteSongs = favoriteSongs,
                    recentSongs = recentSongs,
                    smartPlaylists = uiState.smartPlaylists,
                    onSongClick = { onPlay(it, uiState.songs) },
                    onSongMoreClick = onSongMoreClick,
                    onSmartPlaylistClick = onSmartPlaylistClick
                )

                1 -> SongList(
                    songs = uiState.songs,
                    currentSong = uiState.playerState.currentSong,
                    isPlaying = uiState.playerState.isPlaying,
                    onSongClick = { onPlay(it, uiState.songs) },
                    onMoreClick = onSongMoreClick
                )

                2 -> PlaylistGrid(
                    playlists = uiState.playlists,
                    smartPlaylists = uiState.smartPlaylists,
                    onCreatePlaylist = onCreatePlaylist,
                    onPlaylistClick = onPlaylistClick,
                    onSmartPlaylistClick = onSmartPlaylistClick
                )

                3 -> GenreList(uiState.genres, onGenreClick)
                4 -> ArtistList(uiState.artists, onArtistClick)
                5 -> AlbumGrid(uiState.albums, onAlbumClick)
                6 -> FolderList(uiState.folders, onFolderClick)
            }
        }
    }
}

@Composable
fun LibraryTopBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MonkeySearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChanged,
            placeholder = stringResource(R.string.search_placeholder),
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Smart Playlists (Daily Mix, etc.)
        item {
            Column {
                Text(
                    text = stringResource(R.string.smart_playlists_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(smartPlaylists) { smart ->
                        SmartPlaylistCardSmall(smart, onClick = { onSmartPlaylistClick(smart) })
                    }
                }
            }
        }

        // Favoritos
        if (favoriteSongs.isNotEmpty()) {
            item {
                HomeSection(
                    title = stringResource(R.string.favorites),
                    songs = favoriteSongs.take(5),
                    onSongClick = onSongClick,
                    onSongMoreClick = onSongMoreClick
                )
            }
        }

        // Recientes
        if (recentSongs.isNotEmpty()) {
            item {
                HomeSection(
                    title = stringResource(R.string.recently_added),
                    songs = recentSongs,
                    onSongClick = onSongClick,
                    onSongMoreClick = onSongMoreClick
                )
            }
        }
    }
}

@Composable
fun SmartPlaylistCardSmall(smart: SmartPlaylist, onClick: () -> Unit) {
    val title = when (smart.type) {
        SmartPlaylistType.DAILY_MIX -> stringResource(R.string.smart_daily_mix_title)
        SmartPlaylistType.REDISCOVER -> stringResource(R.string.smart_rediscover_title)
        SmartPlaylistType.TOP_SONGS -> stringResource(R.string.smart_top_songs_title)
    }
    val icon = when (smart.type) {
        SmartPlaylistType.DAILY_MIX -> Icons.Default.AutoAwesome
        SmartPlaylistType.REDISCOVER -> Icons.Default.History
        SmartPlaylistType.TOP_SONGS -> Icons.Default.Star
    }
    
    val gradients = listOf(
        listOf(Color(0xFFFF8C00), Color(0xFFFF5500)),
        listOf(Color(0xFF7B2FBE), Color(0xFFFF8C00)),
        listOf(Color(0xFF1DB954), Color(0xFF0D7A38))
    )
    val colors = when (smart.type) {
        SmartPlaylistType.DAILY_MIX -> gradients[0]
        SmartPlaylistType.REDISCOVER -> gradients[1]
        SmartPlaylistType.TOP_SONGS -> gradients[2]
    }

    Card(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors))) {
            Icon(
                icon, null, 
                modifier = Modifier.align(Alignment.Center).size(48.dp).alpha(0.15f),
                tint = Color.White
            )
            Text(
                title,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun HomeSection(
    title: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        songs.forEach { song ->
            SongItem(
                song = song,
                isSelected = false,
                isPlaying = false,
                onClick = { onSongClick(song) },
                onMoreClick = { onSongMoreClick(song) }
            )
        }
    }
}

@Composable
fun EmptyLibraryState(onScan: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.MusicOff,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.no_songs_found),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.scan_music_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onScan,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.scan_now))
            }
        }
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
        items(songs, key = { it.id }) { song ->
            SongItem(
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
fun SongItem(
    song: Song,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val bgColor = if (isSelected)
        PrimaryOrange.copy(alpha = 0.14f)
    else
        MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)

    ListItem(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        headlineContent = {
            Text(
                song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) PrimaryOrange else Color.Unspecified,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        supportingContent = {
            Text(
                "${song.artist} • ${song.album}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_monkey_head)
                )
                if (isSelected && isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Aquí se podría poner una pequeña animación de barras de sonido
                        Icon(Icons.Default.VolumeUp, null, tint = PrimaryOrange)
                    }
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    TimeFormatter.formatDuration(song.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onMoreClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
            }
        }
    )
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
    val tabLabels = listOf(
        stringResource(R.string.tab_playlist),
        stringResource(R.string.lyrics_tab),
        stringResource(R.string.queue_tab)
    )
    val pagerState = rememberPagerState(pageCount = { tabLabels.size })
    val scope = rememberCoroutineScope()
    var playerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        playerVisible = true
    }

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

    AnimatedVisibility(
        visible = playerVisible,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight }
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight }
        ) + fadeOut()
    ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .systemBarsPadding()
            ) {
        // ── TopBar: flecha abajo | PLAYLIST / nombre | (vacío) ────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(30.dp)
                )
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.tab_playlist).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 2.sp
                )
                Text(
                    text = song.album.ifBlank { song.artist },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── Tabs: Playlist / Letras / Cola ─────────────────────────────────
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Black,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = PrimaryOrange,
                    height = 2.dp
                )
            },
            divider = {}
        ) {
            tabLabels.forEachIndexed { index, label ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            label,
                            color = if (pagerState.currentPage == index) Color.White
                                    else Color.White.copy(alpha = 0.45f),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                )
            }
        }

        // ── Pager ──────────────────────────────────────────────────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {

                // ── Página 0: Portada + info + controles ───────────────────
                0 -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(12.dp))

                    val artworkScale by animateFloatAsState(
                        targetValue = if (playerVisible) 1f else 0.85f,
                        animationSpec = tween(500),
                        label = ""
                    )

                    // Artwork grande
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .graphicsLayer {
                                scaleX = artworkScale
                                scaleY = artworkScale
                            }
                            .clip(RoundedCornerShape(22.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(com.lg.monkeymusicplayer.R.drawable.ic_monkey_head)
                    )

                    Spacer(Modifier.height(20.dp))

                    // Título + íconos edit/fav en la misma fila
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                song.title,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                song.artist,
                                style = MaterialTheme.typography.titleMedium,
                                color = PrimaryOrange,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, null,
                                tint = Color.White.copy(alpha = 0.7f))
                        }
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                if (song.isFavorite) Icons.Default.Favorite
                                else Icons.Default.FavoriteBorder,
                                null,
                                tint = if (song.isFavorite) PrimaryOrange
                                       else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Slider de progreso
                    MediaProgressSlider(
                        playerState = playerState,
                        onSeekTo = onSeekTo
                    )

                    Spacer(Modifier.height(18.dp))

                    PlaybackWaveform(
                        isPlaying = playerState.isPlaying,
                        accent = PrimaryOrange
                    )

                    Spacer(Modifier.height(18.dp))

                    // Fila principal: prev | play | next
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onSkipPrevious, modifier = Modifier.size(52.dp)) {
                            Icon(Icons.Default.SkipPrevious, null,
                                tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(50))
                                .background(PrimaryOrange)
                                .clickable { onPlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (playerState.isPlaying) Icons.Default.Pause
                                else Icons.Default.PlayArrow,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        IconButton(onClick = onSkipNext, modifier = Modifier.size(52.dp)) {
                            Icon(Icons.Default.SkipNext, null,
                                tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Fila inferior: shuffle | seek-back | seek-forward | add-to-queue | repeat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onToggleShuffle) {
                            Icon(
                                Icons.Default.Shuffle, null,
                                tint = if (playerState.isShuffleMode) PrimaryOrange
                                       else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = onSeekBack) {
                            Icon(Icons.Default.Replay10, null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp))
                        }
                        IconButton(onClick = { onAddToPlaylist(song) }) {
                            Icon(Icons.Default.PlaylistAdd, null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp))
                        }
                        IconButton(onClick = onSeekForward) {
                            Icon(Icons.Default.Forward10, null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp))
                        }
                        IconButton(onClick = onCycleRepeatMode) {
                            Icon(
                                when (playerState.repeatMode) {
                                    androidx.media3.common.Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                    else -> Icons.Default.Repeat
                                },
                                null,
                                tint = if (playerState.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF)
                                           PrimaryOrange
                                       else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // ── Página 1: Letras ───────────────────────────────────────
                1 -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoadingLyrics -> CircularProgressIndicator(color = PrimaryOrange)
                        lyrics.isEmpty() -> Text(
                            stringResource(com.lg.monkeymusicplayer.R.string.lyrics_not_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        else -> LyricsView(
                            lyrics = lyrics,
                            currentPosition = playerState.currentPosition,
                            accentColor = playerState.accentColor,
                            onLyricClick = onSeekTo
                        )
                    }
                }

                // ── Página 2: Cola ─────────────────────────────────────────
                2 -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    items(queue, key = { it.id }) { queueSong ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    queueSong.title,
                                    fontWeight = if (queueSong.id == song.id) FontWeight.Bold else FontWeight.Normal,
                                    color = if (queueSong.id == song.id) PrimaryOrange else Color.White
                                )
                            },
                            supportingContent = {
                                Text(queueSong.artist, color = Color.White.copy(alpha = 0.55f))
                            },
                            leadingContent = {
                                if (queueSong.id == song.id) {
                                    Icon(Icons.Default.VolumeUp, null, tint = PrimaryOrange)
                                } else {
                                    AsyncImage(
                                        model = queueSong.albumArtUri,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(com.lg.monkeymusicplayer.R.drawable.ic_monkey_head)
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Black),
                            modifier = Modifier.clickable { onPlayFromQueue(queueSong) }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
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
                        Text(stringResource(com.lg.monkeymusicplayer.R.string.new_playlist), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
        
        items(smartPlaylists) { smart ->
            SmartPlaylistCard(smart, onClick = { onSmartPlaylistClick(smart) })
        }

        items(playlists) { playlist ->
            PlaylistCard(playlist.name, stringResource(com.lg.monkeymusicplayer.R.string.playlist), onPlaylistClick = { onPlaylistClick(playlist) })
        }
    }
}

@Composable
fun SmartPlaylistCard(smart: SmartPlaylist, onClick: () -> Unit) {
    val title = when (smart.type) {
        SmartPlaylistType.DAILY_MIX -> stringResource(com.lg.monkeymusicplayer.R.string.smart_daily_mix_title)
        SmartPlaylistType.REDISCOVER -> stringResource(com.lg.monkeymusicplayer.R.string.smart_rediscover_title)
        SmartPlaylistType.TOP_SONGS -> stringResource(com.lg.monkeymusicplayer.R.string.smart_top_songs_title)
    }
    val icon = when (smart.type) {
        SmartPlaylistType.DAILY_MIX -> Icons.Default.AutoAwesome
        SmartPlaylistType.REDISCOVER -> Icons.Default.History
        SmartPlaylistType.TOP_SONGS -> Icons.Default.Star
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
                Text(stringResource(com.lg.monkeymusicplayer.R.string.stats_artist_songs, smart.songs.size),
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
                Icons.Default.QueueMusic,
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
                supportingContent = { Text(stringResource(com.lg.monkeymusicplayer.R.string.stats_artist_songs, genres[genre]?.size ?: 0)) },
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
                supportingContent = { Text(stringResource(com.lg.monkeymusicplayer.R.string.stats_artist_songs, artists[artist]?.size ?: 0)) },
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
                        error = painterResource(com.lg.monkeymusicplayer.R.drawable.ic_monkey_head)
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
        title = { Text(stringResource(com.lg.monkeymusicplayer.R.string.sleep_timer)) },
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
        title = { Text(stringResource(com.lg.monkeymusicplayer.R.string.language)) },
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
            TextButton(onClick = onDismiss) { Text(stringResource(com.lg.monkeymusicplayer.R.string.cancel)) }
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
        title = { Text(stringResource(com.lg.monkeymusicplayer.R.string.edit_tags)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(com.lg.monkeymusicplayer.R.string.title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(stringResource(com.lg.monkeymusicplayer.R.string.artist)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text(stringResource(com.lg.monkeymusicplayer.R.string.album)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text(stringResource(com.lg.monkeymusicplayer.R.string.genre)) },
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
                    Text(stringResource(com.lg.monkeymusicplayer.R.string.save), color = PrimaryOrange)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(com.lg.monkeymusicplayer.R.string.cancel)) }
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
                title = { Text(stringResource(com.lg.monkeymusicplayer.R.string.excluded_folders)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(com.lg.monkeymusicplayer.R.string.back))
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
                    Text(stringResource(com.lg.monkeymusicplayer.R.string.no_songs))
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
        title = { Text(stringResource(com.lg.monkeymusicplayer.R.string.new_playlist)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(com.lg.monkeymusicplayer.R.string.playlist_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text(stringResource(com.lg.monkeymusicplayer.R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(com.lg.monkeymusicplayer.R.string.cancel))
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
        title = { Text(stringResource(com.lg.monkeymusicplayer.R.string.add_to_playlist)) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                item {
                    ListItem(
                        modifier = Modifier.clickable { showCreateDialog = true },
                        headlineContent = { Text(stringResource(com.lg.monkeymusicplayer.R.string.new_playlist), color = PrimaryOrange) },
                        leadingContent = { Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryOrange) }
                    )
                }
                items(playlists) { playlist ->
                    ListItem(
                        modifier = Modifier.clickable { onPlaylistSelected(playlist) },
                        headlineContent = { Text(playlist.name) },
                        leadingContent = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(com.lg.monkeymusicplayer.R.string.cancel)) }
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
                        error = painterResource(com.lg.monkeymusicplayer.R.drawable.ic_monkey_head)
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            ListItem(
                modifier = Modifier.clickable { onPlayNext(song) },
                headlineContent = { Text(stringResource(com.lg.monkeymusicplayer.R.string.add_to_queue)) },
                leadingContent = { Icon(Icons.Default.QueueMusic, contentDescription = null) }
            )
            ListItem(
                modifier = Modifier.clickable { onAddToPlaylist(song) },
                headlineContent = { Text(stringResource(com.lg.monkeymusicplayer.R.string.add_to_playlist)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) }
            )
            ListItem(
                modifier = Modifier.clickable { onToggleFavorite(song) },
                headlineContent = { 
                    Text(if (song.isFavorite) stringResource(com.lg.monkeymusicplayer.R.string.remove_from_favorites) else stringResource(com.lg.monkeymusicplayer.R.string.add_to_favorites)) 
                },
                leadingContent = { 
                    Icon(if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null) 
                }
            )
            ListItem(
                modifier = Modifier.clickable { onEditTags(song) },
                headlineContent = { Text(stringResource(com.lg.monkeymusicplayer.R.string.edit_tags)) },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
        }
    }
}
