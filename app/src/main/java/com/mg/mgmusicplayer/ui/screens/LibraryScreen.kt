package com.mg.mgmusicplayer.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mg.mgmusicplayer.R
import com.mg.mgmusicplayer.data.database.PlaylistEntity
import com.mg.mgmusicplayer.data.model.Song
import com.mg.mgmusicplayer.ui.SortOrder
import androidx.media3.common.Player
import com.mg.mgmusicplayer.core.utils.CoverUtils
import com.mg.mgmusicplayer.ui.components.AudioVisualizer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LibraryScreen(
    songs: List<Song>,
    genres: Map<String, List<Song>>,
    artists: Map<String, List<Song>>,
    albums: Map<String, List<Song>>,
    folders: Map<String, List<Song>>,
    playlists: List<PlaylistEntity>,
    currentPlaylistSongs: List<Song>,
    searchQuery: String,
    sortOrder: SortOrder,
    onSearchQueryChanged: (String) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    currentSong: Song?,
    isPlaying: Boolean,
    isShuffleMode: Boolean,
    repeatMode: Int,
    audioSessionId: Int,
    onPlayPause: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onScanMusic: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBack: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onAddSongToPlaylist: (String, Song) -> Unit,
    onAddSongsToPlaylist: (String, List<Song>) -> Unit,
    onLoadPlaylistSongs: (String) -> Unit,
    onUpdateSongTags: (Song, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as ComponentActivity)
    val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
    
    var isPlayerFullOpen by remember { mutableStateOf(false) }
    var songsToAddToPlaylist by remember { mutableStateOf<List<Song>?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var editingSong by remember { mutableStateOf<Song?>(null) }

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
            playlists = playlists,
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
            onCreateNew = {
                showCreatePlaylistDialog = true
            }
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

    if (isPlayerFullOpen && currentSong != null) {
        FullPlayerScreen(
            song = currentSong,
            isPlaying = isPlaying,
            isShuffleMode = isShuffleMode,
            repeatMode = repeatMode,
            audioSessionId = audioSessionId,
            onClose = { isPlayerFullOpen = false },
            onPlayPause = onPlayPause,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onSeekForward = onSeekForward,
            onSeekBack = onSeekBack,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeatMode = onCycleRepeatMode,
            onToggleFavorite = { onToggleFavorite(currentSong) },
            onAddToPlaylist = { songsToAddToPlaylist = listOf(currentSong) }
        )
    } else {
        MobileLayout(
            songs, genres, artists, albums, folders, playlists, currentPlaylistSongs, searchQuery, sortOrder,
            onSearchQueryChanged, onSortOrderChanged,
            currentSong, isPlaying, isShuffleMode, onPlayPause, onPlay,
            onScanMusic, onSkipNext, onSkipPrevious, onToggleShuffle,
            onLoadPlaylistSongs = onLoadPlaylistSongs,
            onPlayerClick = { isPlayerFullOpen = true },
            onAddSongToPlaylist = { songsToAddToPlaylist = listOf(it) },
            onAddSongsToPlaylist = { songsToAddToPlaylist = it },
            onCreatePlaylist = { showCreatePlaylistDialog = true },
            onEditSong = { editingSong = it }
        )
    }
}

@Composable
fun SongItem(song: Song, contextPlaylist: List<Song>, onPlay: (Song, List<Song>) -> Unit, onAddSongToPlaylist: (Song) -> Unit, onEditSong: (Song) -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(song.id) { CoverUtils.getEmbeddedCover(context, song.id, song.path) }

    ListItem(
        modifier = Modifier.clickable { onPlay(song, contextPlaylist) },
        headlineContent = { Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1) },
        supportingContent = { Text("${song.artist} • ${song.album}", style = MaterialTheme.typography.bodyMedium, maxLines = 1) },
        leadingContent = {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(50.dp))
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = { onEditSong(song) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar etiquetas")
                }
                IconButton(onClick = { onAddSongToPlaylist(song) }) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Agregar a playlist")
                }
            }
        }
    )
}

@Composable
fun FullPlayerScreen(
    song: Song,
    isPlaying: Boolean,
    isShuffleMode: Boolean,
    repeatMode: Int,
    audioSessionId: Int,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBack: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    BackHandler { onClose() }
    val context = LocalContext.current
    val bitmap = remember(song.id) { CoverUtils.getEmbeddedCover(context, song.id, song.path) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo con Blur Intenso
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(60.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.5f
            )
        }
        
        // Gradiente Oscuro Superpuesto
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
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Cerrar", tint = Color.White)
            }
            
            // Carátula principal con diseño de tarjeta
            Card(
                modifier = Modifier.size(300.dp).padding(8.dp),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_mg_logo),
                            contentDescription = null,
                            modifier = Modifier.size(150.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Visualizador estilizado
            Box(modifier = Modifier.height(120.dp).fillMaxWidth()) {
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Controles de reproducción con estilo flotante
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
    songs: List<Song>,
    genres: Map<String, List<Song>>,
    artists: Map<String, List<Song>>,
    albums: Map<String, List<Song>>,
    folders: Map<String, List<Song>>,
    playlists: List<PlaylistEntity>,
    currentPlaylistSongs: List<Song>,
    searchQuery: String,
    sortOrder: SortOrder,
    onSearchQueryChanged: (String) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    currentSong: Song?,
    isPlaying: Boolean,
    isShuffleMode: Boolean,
    onPlayPause: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onScanMusic: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onLoadPlaylistSongs: (String) -> Unit,
    onPlayerClick: () -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onAddSongsToPlaylist: (List<Song>) -> Unit,
    onCreatePlaylist: () -> Unit,
    onEditSong: (Song) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Temas", "Géneros", "Artistas", "Álbumes", "Carpetas", "Playlists")
    var selectedCategoryItem by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (selectedCategoryItem != null && selectedTab != 0) {
                            val title = if (selectedTab == 5) {
                                playlists.find { it.id.toString() == selectedCategoryItem }?.name ?: selectedCategoryItem!!
                            } else {
                                selectedCategoryItem!!
                            }
                            Text(text = title)
                        } else {
                            TextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChanged,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Buscar...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        if (selectedCategoryItem != null && selectedTab != 0) {
                            IconButton(onClick = { selectedCategoryItem = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                            }
                        }
                    },
                    actions = {
                        if (selectedCategoryItem != null && selectedTab != 0) {
                            if (selectedTab == 5) {
                                // Playlist Actions: Shuffle and Play All
                                IconButton(onClick = { 
                                    if (currentPlaylistSongs.isNotEmpty()) {
                                        if (!isShuffleMode) onToggleShuffle()
                                        onPlay(currentPlaylistSongs.shuffled().first(), currentPlaylistSongs)
                                    }
                                }) {
                                    Icon(Icons.Default.Shuffle, contentDescription = "Reproducción Aleatoria")
                                }
                                IconButton(onClick = {
                                    if (currentPlaylistSongs.isNotEmpty()) {
                                        onPlay(currentPlaylistSongs.first(), currentPlaylistSongs)
                                    }
                                }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir Todo")
                                }
                            } else {
                                // Category Actions: Add All
                                IconButton(onClick = {
                                    val categorySongs = when (selectedTab) {
                                        1 -> genres[selectedCategoryItem]
                                        2 -> artists[selectedCategoryItem]
                                        3 -> albums[selectedCategoryItem]
                                        4 -> folders[selectedCategoryItem]
                                        else -> null
                                    }
                                    categorySongs?.let { onAddSongsToPlaylist(it) }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Agregar todo a playlist")
                                }
                            }
                        }

                        if (selectedTab == 0 && selectedCategoryItem == null) {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
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
                        if (selectedTab == 5 && selectedCategoryItem == null) {
                            IconButton(onClick = onCreatePlaylist) {
                                Icon(Icons.Default.Add, contentDescription = "Nueva Playlist")
                            }
                        }
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Settings")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Escanear música") },
                                onClick = { showMenu = false; onScanMusic() },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                        }
                    }
                )
                if (selectedCategoryItem == null || selectedTab == 0) {
                    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp, divider = {}) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index; selectedCategoryItem = null },
                                text = { Text(title) }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = { 
            PlayerBottomBar(currentSong, isPlaying, isShuffleMode, onPlayPause, onSkipNext, onSkipPrevious, onToggleShuffle, onPlayerClick)
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> SongList(songs, songs, onPlay, onAddSongToPlaylist, onEditSong)
                1 -> CategoryNavigation(genres, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, Icons.Default.LibraryMusic, onAddSongToPlaylist, onEditSong)
                2 -> CategoryNavigation(artists, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, Icons.Default.Person, onAddSongToPlaylist, onEditSong)
                3 -> CategoryNavigation(albums, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, Icons.Default.Album, onAddSongToPlaylist, onEditSong)
                4 -> CategoryNavigation(folders, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, Icons.Default.Folder, onAddSongToPlaylist, onEditSong)
                5 -> {
                    if (selectedCategoryItem != null) {
                        LaunchedEffect(selectedCategoryItem) {
                            onLoadPlaylistSongs(selectedCategoryItem!!)
                        }
                        SongList(currentPlaylistSongs, currentPlaylistSongs, onPlay, onAddSongToPlaylist, onEditSong)
                    } else {
                        PlaylistSummaryList(playlists, onItemClick = { selectedCategoryItem = it })
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerBottomBar(
    currentSong: Song?,
    isPlaying: Boolean,
    isShuffleMode: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onPlayerClick: () -> Unit
) {
    if (currentSong != null) {
        val context = LocalContext.current
        val bitmap = remember(currentSong.id) { CoverUtils.getEmbeddedCover(context, currentSong.id, currentSong.path) }
        
        Surface(
            tonalElevation = 8.dp,
            modifier = Modifier
                .padding(8.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onPlayerClick() }
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Blur en la barra inferior (Efecto Glassmorphism)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(64.dp).blur(20.dp),
                        contentScale = ContentScale.Crop,
                        alpha = 0.2f
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(48.dp))
                    }
                    
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
        }
    }
}

@Composable
fun CategoryNavigation(
    data: Map<String, List<Song>>,
    selectedItem: String?,
    onPlay: (Song, List<Song>) -> Unit,
    onItemClick: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onAddSongToPlaylist: (Song) -> Unit,
    onEditSong: (Song) -> Unit
) {
    if (selectedItem == null) {
        CategorySummaryList(data, onItemClick, icon)
    } else {
        val categorySongs = data[selectedItem] ?: emptyList()
        SongList(categorySongs, categorySongs, onPlay, onAddSongToPlaylist, onEditSong)
    }
}

// Map specialized overloads to avoid ambiguous calls
@Composable
fun CategoryNavigation(
    data: Map<String, List<Song>>,
    placeholder: Map<String, List<Song>>,
    selectedItem: String?,
    onPlay: (Song, List<Song>) -> Unit,
    onItemClick: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onAddSongToPlaylist: (Song) -> Unit,
    onEditSong: (Song) -> Unit
) {
    CategoryNavigation(data, selectedItem, onPlay, onItemClick, icon, onAddSongToPlaylist, onEditSong)
}

@Composable
fun PlaylistNavigation(
    playlists: List<PlaylistEntity>,
    selectedItem: String?,
    onPlay: (Song, List<Song>) -> Unit,
    onItemClick: (String) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onEditSong: (Song) -> Unit
) {
    if (selectedItem == null) {
        PlaylistSummaryList(playlists, onItemClick)
    } else {
        // Need a way to get songs for a playlist by ID from DB, simplified here
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Canciones de playlist") }
    }
}

@Composable
fun PlaylistSummaryList(playlists: List<PlaylistEntity>, onItemClick: (String) -> Unit) {
    if (playlists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay playlists creadas")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(playlists) { playlist ->
                ListItem(
                    modifier = Modifier.clickable { onItemClick(playlist.id.toString()) },
                    headlineContent = { Text(playlist.name) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
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
                ListItem(
                    modifier = Modifier.clickable { onItemClick(key) },
                    headlineContent = { Text(key) },
                    supportingContent = { Text("$songCount canciones") },
                    leadingContent = { Icon(icon, contentDescription = null) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun SongList(songs: List<Song>, contextPlaylist: List<Song>, onPlay: (Song, List<Song>) -> Unit, onAddSongToPlaylist: (Song) -> Unit, onEditSong: (Song) -> Unit) {
    if (songs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay canciones") }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(songs) { song -> SongItem(song, contextPlaylist, onPlay, onAddSongToPlaylist, onEditSong) }
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
