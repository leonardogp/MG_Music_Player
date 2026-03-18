package com.lg.monkeymusicplayer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.data.database.PlaylistEntity
import com.lg.monkeymusicplayer.data.database.HistoryEntity
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.ui.SortOrder
import com.lg.monkeymusicplayer.ui.PlayerState
import com.lg.monkeymusicplayer.ui.LibraryUiState
import androidx.media3.common.Player
import com.lg.monkeymusicplayer.ui.components.AudioVisualizer
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
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
    onChangeLanguage: (String) -> Unit
) {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            LibraryMainContent(
                uiState, onSearchQueryChanged, onSortOrderChanged,
                onPlayPause, onPlay, onAddToQueue, onScanMusic, onSkipNext, onSkipPrevious,
                onToggleShuffle, onLoadPlaylistSongs,
                onPlayerClick = { navController.navigate("player") },
                onMenuClick = { navController.navigate("settings") },
                onToggleFavorite = onToggleFavorite,
                onCreatePlaylist = onCreatePlaylist,
                onDeletePlaylist = onDeletePlaylist,
                onAddSongToPlaylist = onAddSongToPlaylist,
                onAddSongsToPlaylist = onAddSongsToPlaylist,
                onRemoveSongFromPlaylist = onRemoveSongFromPlaylist,
                onUpdateSongTags = onUpdateSongTags,
                onOpenEqualizer = onOpenEqualizer,
                onSetSleepTimer = onSetSleepTimer,
                onChangeLanguage = onChangeLanguage
            )
        }
        composable("player") {
            if (uiState.playerState.currentSong != null) {
                FullPlayerScreen(
                    song = uiState.playerState.currentSong,
                    queue = uiState.playerState.currentQueue,
                    isPlaying = uiState.playerState.isPlaying,
                    isShuffleMode = uiState.playerState.isShuffleMode,
                    repeatMode = uiState.playerState.repeatMode,
                    currentPosition = uiState.playerState.currentPosition,
                    duration = uiState.playerState.duration,
                    audioSessionId = uiState.playerState.audioSessionId,
                    onClose = { navController.popBackStack() },
                    onPlayPause = onPlayPause,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onSeekTo = onSeekTo,
                    onSeekForward = onSeekForward,
                    onSeekBack = onSeekBack,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeatMode = onCycleRepeatMode,
                    onToggleFavorite = { onToggleFavorite(uiState.playerState.currentSong) },
                    onAddToPlaylist = { /* handle */ },
                    onPlayFromQueue = { onPlay(it, uiState.playerState.currentQueue) }
                )
            }
        }
        composable("settings") {
            SettingsScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onScanMusic = onScanMusic,
                onOpenEqualizer = onOpenEqualizer,
                onSetSleepTimer = onSetSleepTimer,
                onChangeLanguage = onChangeLanguage
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: LibraryUiState,
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
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ListItem(
                modifier = Modifier.clickable { onScanMusic() },
                headlineContent = { Text("Escanear música") },
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
                            Text("Escaneando: ${uiState.scanProgress} / ${uiState.scanTotal}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            )
            ListItem(
                modifier = Modifier.clickable { showSleepTimerDialog = true },
                headlineContent = { 
                    val timerText = if (uiState.playerState.sleepTimerMinutes > 0) {
                        "Temporizador (${formatTime(uiState.playerState.sleepTimerRemainingMillis)})"
                    } else {
                        "Temporizador de apagado"
                    }
                    Text(timerText) 
                },
                leadingContent = { Icon(Icons.Default.Timer, contentDescription = null) }
            )
            ListItem(
                modifier = Modifier.clickable { onOpenEqualizer(); onBack() },
                headlineContent = { Text("Ecualizador") },
                leadingContent = { Icon(Icons.Default.GraphicEq, contentDescription = null) }
            )
            ListItem(
                modifier = Modifier.clickable { showLanguageDialog = true },
                headlineContent = { Text("Idioma") },
                leadingContent = { Icon(Icons.Default.Language, contentDescription = null) }
            )
        }
    }
}

@Composable
fun LibraryMainContent(
    uiState: LibraryUiState,
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

    if (editingSong != null) {
        EditTagsDialog(
            song = editingSong!!,
            onDismiss = { editingSong = null },
            onSave = { title, artist, album, genre ->
                onUpdateSongTags(editingSong!!, title, artist, album, genre)
                editingSong = null
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
        uiState,
        onSearchQueryChanged, onSortOrderChanged,
        onPlayPause, onPlay, onAddToQueue,
        onScanMusic, onSkipNext, onSkipPrevious, onToggleShuffle,
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
        headlineContent = { Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1) },
        supportingContent = { Text("${song.artist} • ${song.album}", style = MaterialTheme.typography.bodyMedium, maxLines = 1) },
        leadingContent = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                error = painterResource(R.drawable.ic_mg_logo),
                placeholder = painterResource(R.drawable.ic_mg_logo),
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (song.isFavorite) "Quitar de favoritos" else "Agregar a favoritos") },
                        leadingIcon = { Icon(if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (song.isFavorite) Color.Red else LocalContentColor.current) },
                        onClick = { onToggleFavorite(song); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Agregar a la cola") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                        onClick = { onAddToQueue(song); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Agregar a playlist") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                        onClick = { onAddSongToPlaylist(song); showMenu = false }
                    )
                    if (onRemoveFromPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("Eliminar de la playlist") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = { onRemoveFromPlaylist(); showMenu = false }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Editar etiquetas") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { onEditSong(song); showMenu = false }
                    )
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
    onPlayFromQueue: (Song) -> Unit
) {
    BackHandler { onClose() }
    var showQueue by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = sheetState
        ) {
            Text(
                "Siguiente en la cola", 
                modifier = Modifier.padding(16.dp), 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                items(queue) { queueSong ->
                    ListItem(
                        modifier = Modifier.clickable { onPlayFromQueue(queueSong) },
                        headlineContent = { 
                            Text(
                                queueSong.title, 
                                color = if (queueSong.id == song.id) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                fontWeight = if (queueSong.id == song.id) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        supportingContent = { Text(queueSong.artist) },
                        leadingContent = {
                            AsyncImage(
                                model = queueSong.albumArtUri,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                        },
                        trailingContent = {
                            if (queueSong.id == song.id) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.albumArtUri)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().blur(60.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Cerrar", tint = Color.White)
                }
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Ver Cola", tint = Color.White)
                }
            }
            
            Card(
                modifier = Modifier.size(300.dp).padding(8.dp),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.albumArtUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    error = painterResource(R.drawable.ic_mg_logo),
                    placeholder = painterResource(R.drawable.ic_mg_logo),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Box(modifier = Modifier.height(80.dp).fillMaxWidth()) {
                AudioVisualizer(audioSessionId)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { onSeekTo(it.toLong()) },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatTime(currentPosition), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                    Text(text = formatTime(duration), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            Icons.Default.Shuffle, 
                            contentDescription = "Shuffle",
                            tint = if (isShuffleMode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = onCycleRepeatMode) {
                        Icon(
                            imageVector = when(repeatMode) {
                                Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (song.isFavorite) Color.Red else Color.White.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = onAddToPlaylist) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add Playlist", tint = Color.White.copy(alpha = 0.7f))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSkipPrevious) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(36.dp), tint = Color.White)
                    }
                    
                    IconButton(onClick = onSeekBack) {
                        Icon(Icons.Default.Replay10, contentDescription = "Atrasar 10s", modifier = Modifier.size(30.dp), tint = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    FloatingActionButton(
                        onClick = onPlayPause,
                        shape = RoundedCornerShape(100.dp),
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onSeekForward) {
                        Icon(Icons.Default.Forward10, contentDescription = "Adelantar 10s", modifier = Modifier.size(30.dp), tint = Color.White)
                    }

                    IconButton(onClick = onSkipNext) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", modifier = Modifier.size(36.dp), tint = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileLayout(
    uiState: LibraryUiState,
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
    val tabs = listOf("Principal", "Canciones", "Géneros", "Artistas", "Álbumes", "Carpetas", "Playlists", "Favoritos", "Historial")
    var selectedCategoryItem by remember { mutableStateOf<String?>(null) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
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
                                        val hint = when(currentTab) {
                                            2 -> "Buscar géneros..."
                                            3 -> "Buscar artistas..."
                                            4 -> "Buscar álbumes..."
                                            5 -> "Buscar carpetas..."
                                            6 -> "Buscar playlists..."
                                            else -> "Buscar canciones..."
                                        }
                                        Text(hint) 
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
                                            Icon(Icons.Default.Close, contentDescription = "Cerrar búsqueda")
                                        }
                                    }
                                )
                            } else {
                                if (currentTab in 2..5 && selectedCategoryItem != null) {
                                    Text(text = selectedCategoryItem!!)
                                } else if (currentTab == 6 && selectedPlaylistId != null) {
                                    val title = uiState.playlists.find { it.id.toString() == selectedPlaylistId }?.name ?: "Playlist"
                                    Text(text = title)
                                } else {
                                    Text(text = tabs[currentTab])
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
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                            }
                        } else if (!isSearchActive) {
                            IconButton(onClick = onMenuClick) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_monkey_head),
                                    contentDescription = "Ajustes",
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.Unspecified
                                )
                            }
                        }
                    },
                    actions = {
                        if (!isSearchActive) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar")
                            }
                            if (pagerState.currentPage == 1 && selectedCategoryItem == null) {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
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
                ScrollableTabRow(selectedTabIndex = pagerState.currentPage, edgePadding = 16.dp, divider = {}) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { 
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                                selectedCategoryItem = null 
                                selectedPlaylistId = null
                                isSearchActive = false
                                onSearchQueryChanged("")
                            },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        bottomBar = { 
            PlayerBottomBar(uiState.playerState.currentSong, uiState.playerState.isPlaying, uiState.playerState.currentPosition, uiState.playerState.duration, onPlayPause, onSkipNext, onSkipPrevious, onPlayerClick)
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding),
            beyondViewportPageCount = 1,
            userScrollEnabled = (selectedCategoryItem == null && selectedPlaylistId == null)
        ) { page ->
            // Filtrar datos según la búsqueda
            val filteredSongs = uiState.songs.filter { it.title.contains(uiState.searchQuery, ignoreCase = true) || it.artist.contains(uiState.searchQuery, ignoreCase = true) }
            val filteredGenres = uiState.genres.filterKeys { it.contains(uiState.searchQuery, ignoreCase = true) }
            val filteredArtists = uiState.artists.filterKeys { it.contains(uiState.searchQuery, ignoreCase = true) }
            val filteredAlbums = uiState.albums.filterKeys { it.contains(uiState.searchQuery, ignoreCase = true) }
            val filteredFolders = uiState.folders.filterKeys { it.contains(uiState.searchQuery, ignoreCase = true) }
            val filteredPlaylists = uiState.playlists.filter { it.name.contains(uiState.searchQuery, ignoreCase = true) }

            when (page) {
                0 -> MainTab(uiState.songs, onPlay, onAddToQueue, onAddSongToPlaylist, onEditSong, onToggleFavorite)
                1 -> SongList(filteredSongs, filteredSongs, onPlay, onAddToQueue, onAddSongToPlaylist, null, onEditSong, onToggleFavorite)
                2 -> CategoryNavigation(filteredGenres, selectedCategoryItem, onPlay, onAddToQueue, { selectedCategoryItem = it }, Icons.Default.LibraryMusic, onAddSongToPlaylist, onEditSong, onToggleFavorite)
                3 -> CategoryNavigation(filteredArtists, selectedCategoryItem, onPlay, onAddToQueue, { selectedCategoryItem = it }, Icons.Default.Person, onAddSongToPlaylist, onEditSong, onToggleFavorite)
                4 -> CategoryNavigation(filteredAlbums, selectedCategoryItem, onPlay, onAddToQueue, { selectedCategoryItem = it }, Icons.Default.Album, onAddSongToPlaylist, onEditSong, onToggleFavorite)
                5 -> CategoryNavigation(filteredFolders, selectedCategoryItem, onPlay, onAddToQueue, { selectedCategoryItem = it }, Icons.Default.Folder, onAddSongToPlaylist, onEditSong, onToggleFavorite)
                6 -> {
                    if (selectedPlaylistId != null) {
                        LaunchedEffect(selectedPlaylistId) { onLoadPlaylistSongs(selectedPlaylistId!!) }
                        val playlistSongs = uiState.currentPlaylistSongs.filter { it.title.contains(uiState.searchQuery, ignoreCase = true) }
                        SongList(
                            playlistSongs, 
                            playlistSongs, 
                            onPlay, 
                            onAddToQueue, 
                            onAddSongToPlaylist, 
                            { songId -> onRemoveSongFromPlaylist(selectedPlaylistId!!, songId) }, 
                            onEditSong, 
                            onToggleFavorite
                        )
                    } else {
                        PlaylistSummaryList(filteredPlaylists, onItemClick = { selectedPlaylistId = it }, onCreatePlaylist, onDeletePlaylist)
                    }
                }
                7 -> {
                    val favSongs = uiState.songs.filter { it.isFavorite && it.title.contains(uiState.searchQuery, ignoreCase = true) }
                    SongList(favSongs, favSongs, onPlay, onAddToQueue, onAddSongToPlaylist, null, onEditSong, onToggleFavorite)
                }
                8 -> {
                    val historySongs = uiState.history.mapNotNull { h -> uiState.songs.find { it.id == h.songId } }.distinctBy { it.id }.filter { it.title.contains(uiState.searchQuery, ignoreCase = true) }
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
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Favoritos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (favoriteSongs.isEmpty()) {
            Box(modifier = Modifier.height(200.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Text("No tienes favoritos aún", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(favoriteSongs) { song ->
                    FavoriteCarouselItem(song, onClick = { onPlay(song, favoriteSongs) })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Recientes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(songs.take(10)) { song ->
                SongItem(song, songs, onPlay, onAddToQueue, onAddSongToPlaylist, null, onEditSong, onToggleFavorite)
            }
        }
    }
}

@Composable
fun FavoriteCarouselItem(song: Song, onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(160.dp, 220.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.height(150.dp).fillMaxWidth(),
                error = painterResource(R.drawable.ic_mg_logo)
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
                .clip(RoundedCornerShape(16.dp))
                .clickable { onPlayerClick() }
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentSong.albumArtUri)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(64.dp).blur(20.dp),
                        contentScale = ContentScale.Crop,
                        alpha = 0.2f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(currentSong.albumArtUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            error = painterResource(R.drawable.ic_mg_logo),
                            placeholder = painterResource(R.drawable.ic_mg_logo),
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = currentSong.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, fontWeight = FontWeight.Bold)
                            Text(text = currentSong.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onSkipPrevious) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior")
                            }
                            IconButton(onClick = onPlayPause) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                    contentDescription = "Play/Pause",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            IconButton(onClick = onSkipNext) { 
                                Icon(Icons.Default.SkipNext, contentDescription = "Próxima") 
                            }
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = { if (duration > 0) currentPosition.toFloat() / duration else 0f },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
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
fun PlaylistSummaryList(
    playlists: List<PlaylistEntity>, 
    onItemClick: (String) -> Unit, 
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ListItem(
            modifier = Modifier.clickable { onCreatePlaylist() },
            headlineContent = { Text("Crear nueva playlist", fontWeight = FontWeight.Bold) },
            leadingContent = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        )
        HorizontalDivider()
        
        if (playlists.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No hay playlists creadas")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(playlists) { playlist ->
                    var showMenu by remember { mutableStateOf(false) }
                    
                    ListItem(
                        modifier = Modifier.clickable { onItemClick(playlist.id.toString()) },
                        headlineContent = { Text(playlist.name) },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                        trailingContent = {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones de playlist")
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Eliminar Playlist") },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                        onClick = { 
                                            onDeletePlaylist(playlist)
                                            showMenu = false 
                                        }
                                    )
                                }
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
fun CategorySummaryList(data: Map<String, List<Song>>, onItemClick: (String) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    if (data.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay elementos") }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(data.keys.toList().sorted()) { key ->
                val songCount = data[key]?.size ?: 0
                val firstSong = data[key]?.firstOrNull()
                
                ListItem(
                    modifier = Modifier.clickable { onItemClick(key) },
                    headlineContent = { Text(key) },
                    supportingContent = { Text("$songCount canciones") },
                    leadingContent = { 
                        if (firstSong != null && icon == Icons.Default.Album) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(firstSong.albumArtUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                error = painterResource(R.drawable.ic_mg_logo),
                                placeholder = painterResource(R.drawable.ic_mg_logo),
                                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp)) 
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay canciones") }
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
fun AddToPlaylistDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar a playlist") },
        text = {
            Column {
                if (playlists.isEmpty()) {
                    Text("No tienes playlists creadas.")
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
                TextButton(
                    onClick = onCreateNew,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear nueva playlist")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
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
        title = { Text("Nueva playlist") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Nombre de la playlist") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun EditTagsDialog(
    song: Song,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var genre by remember { mutableStateOf(song.genre) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar etiquetas") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") })
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artista") })
                OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text("Álbum") })
                OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Género") })
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, artist, album, genre) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
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
        title = { Text("Temporizador de apagado") },
        text = {
            Column {
                Text("Selecciona en cuántos minutos se detendrá la música:")
                Spacer(modifier = Modifier.height(8.dp))
                options.forEach { minutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(minutes) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentMinutes == minutes, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (minutes == 0) "Desactivado" else "$minutes minutos")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun LanguageDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val languages = listOf("es" to "Español", "en" to "English", "pt" to "Português")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Idioma") },
        text = {
            Column {
                languages.forEach { (code, name) ->
                    ListItem(
                        modifier = Modifier.clickable { onLanguageSelected(code) },
                        headlineContent = { Text(name) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
