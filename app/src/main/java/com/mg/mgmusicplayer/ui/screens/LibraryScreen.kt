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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mg.mgmusicplayer.R
import com.mg.mgmusicplayer.data.model.Playlist
import com.mg.mgmusicplayer.data.model.Song
import com.mg.mgmusicplayer.ui.SortOrder
import androidx.media3.common.Player

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LibraryScreen(
    songs: List<Song>,
    genres: Map<String, List<Song>>,
    artists: Map<String, List<Song>>,
    albums: Map<String, List<Song>>,
    folders: Map<String, List<Song>>,
    playlists: List<Playlist>,
    recentSongs: List<Song>,
    searchQuery: String,
    sortOrder: SortOrder,
    onSearchQueryChanged: (String) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    currentSong: Song?,
    isPlaying: Boolean,
    isShuffleMode: Boolean,
    repeatMode: Int,
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
    onUpdateSongTags: (Song, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as ComponentActivity)
    val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
    
    var isPlayerFullOpen by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf<Song?>(null) }
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

    if (showPlaylistDialog != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { showPlaylistDialog = null },
            onPlaylistSelected = { playlistId ->
                onAddSongToPlaylist(playlistId, showPlaylistDialog!!)
                showPlaylistDialog = null
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
            onClose = { isPlayerFullOpen = false },
            onPlayPause = onPlayPause,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onSeekForward = onSeekForward,
            onSeekBack = onSeekBack,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeatMode = onCycleRepeatMode,
            onToggleFavorite = { onToggleFavorite(currentSong) },
            onAddToPlaylist = { showPlaylistDialog = currentSong }
        )
    } else {
        if (isTablet) {
            TabletLayout(
                songs, genres, artists, albums, folders, playlists, recentSongs, searchQuery, sortOrder,
                onSearchQueryChanged, onSortOrderChanged,
                currentSong, isPlaying, isShuffleMode, onPlayPause, onPlay,
                onScanMusic, onSkipNext, onSkipPrevious, onToggleShuffle,
                onPlayerClick = { isPlayerFullOpen = true },
                onAddSongToPlaylist = { showPlaylistDialog = it },
                onCreatePlaylist = { showCreatePlaylistDialog = true },
                onEditSong = { editingSong = it }
            )
        } else {
            MobileLayout(
                songs, genres, artists, albums, folders, playlists, recentSongs, searchQuery, sortOrder,
                onSearchQueryChanged, onSortOrderChanged,
                currentSong, isPlaying, isShuffleMode, onPlayPause, onPlay,
                onScanMusic, onSkipNext, onSkipPrevious, onToggleShuffle,
                onPlayerClick = { isPlayerFullOpen = true },
                onAddSongToPlaylist = { showPlaylistDialog = it },
                onCreatePlaylist = { showCreatePlaylistDialog = true },
                onEditSong = { editingSong = it }
            )
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
    playlists: List<Playlist>,
    recentSongs: List<Song>,
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
    onPlayerClick: () -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onCreatePlaylist: () -> Unit,
    onEditSong: (Song) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Temas", "Géneros", "Artistas", "Álbumes", "Carpetas", "Playlists", "Recientes")
    var selectedCategoryItem by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (selectedCategoryItem != null && selectedTab != 0) {
                            Text(text = selectedCategoryItem!!)
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
                        if (selectedTab == 0 && selectedCategoryItem == null) {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                SortOrder.values().forEach { order ->
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
                5 -> PlaylistNavigation(playlists, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, onAddSongToPlaylist, onEditSong)
                6 -> SongList(recentSongs, recentSongs, onPlay, onAddSongToPlaylist, onEditSong)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabletLayout(
    songs: List<Song>,
    genres: Map<String, List<Song>>,
    artists: Map<String, List<Song>>,
    albums: Map<String, List<Song>>,
    folders: Map<String, List<Song>>,
    playlists: List<Playlist>,
    recentSongs: List<Song>,
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
    onPlayerClick: () -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onCreatePlaylist: () -> Unit,
    onEditSong: (Song) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategoryItem by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            header = {
                IconButton(onClick = onScanMusic) { Icon(Icons.Default.Refresh, contentDescription = "Scan") }
            }
        ) {
            val railItems = listOf(
                "Temas" to Icons.Default.MusicNote,
                "Géneros" to Icons.Default.LibraryMusic,
                "Artistas" to Icons.Default.Person,
                "Álbumes" to Icons.Default.Album,
                "Carpetas" to Icons.Default.Folder,
                "Playlists" to Icons.AutoMirrored.Filled.PlaylistPlay,
                "Recientes" to Icons.Default.History
            )
            railItems.forEachIndexed { index, item ->
                NavigationRailItem(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index; selectedCategoryItem = null },
                    icon = { Icon(item.second, contentDescription = item.first) },
                    label = { Text(item.first) }
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar en la biblioteca...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            SortOrder.values().forEach { order ->
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
                }
            )
            
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> SongList(songs, songs, onPlay, onAddSongToPlaylist, onEditSong)
                    1 -> CategoryNavigation(genres, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, Icons.Default.LibraryMusic, onAddSongToPlaylist, onEditSong)
                    2 -> CategoryNavigation(artists, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, Icons.Default.Person, onAddSongToPlaylist, onEditSong)
                    3 -> CategoryNavigation(albums, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, Icons.Default.Album, onAddSongToPlaylist, onEditSong)
                    4 -> CategoryNavigation(folders, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, Icons.Default.Folder, onAddSongToPlaylist, onEditSong)
                    5 -> PlaylistNavigation(playlists, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, onAddSongToPlaylist, onEditSong)
                    6 -> SongList(recentSongs, recentSongs, onPlay, onAddSongToPlaylist, onEditSong)
                }
            }
            
            PlayerBottomBar(currentSong, isPlaying, isShuffleMode, onPlayPause, onSkipNext, onSkipPrevious, onToggleShuffle, onPlayerClick)
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
        Surface(
            tonalElevation = 8.dp,
            modifier = Modifier.clickable { onPlayerClick() }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = currentSong.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    Text(text = currentSong.artist, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPlayPause) {
                        Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause")
                    }
                    IconButton(onClick = onSkipNext) { Icon(Icons.Default.SkipNext, contentDescription = "Próxima") }
                }
            }
        }
    }
}

@Composable
fun FullPlayerScreen(
    song: Song,
    isPlaying: Boolean,
    isShuffleMode: Boolean,
    repeatMode: Int,
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
    
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Cerrar")
            }
            
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_mg_logo),
                    contentDescription = null,
                    modifier = Modifier.size(150.dp),
                    contentScale = ContentScale.Fit
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
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
                            tint = if (isShuffleMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                            tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (song.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onAddToPlaylist) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add Playlist")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSkipPrevious) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(40.dp))
                    }
                    IconButton(onClick = onSeekBack) {
                        Icon(Icons.Default.Replay10, contentDescription = "Atrás 10s", modifier = Modifier.size(32.dp))
                    }
                    
                    FloatingActionButton(
                        onClick = onPlayPause,
                        shape = RoundedCornerShape(100.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(72.dp).padding(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    IconButton(onClick = onSeekForward) {
                        Icon(Icons.Default.Forward10, contentDescription = "Adelante 10s", modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = onSkipNext) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", modifier = Modifier.size(40.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
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

@Composable
fun PlaylistNavigation(
    playlists: List<Playlist>,
    selectedItem: String?,
    onPlay: (Song, List<Song>) -> Unit,
    onItemClick: (String) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onEditSong: (Song) -> Unit
) {
    if (selectedItem == null) {
        PlaylistSummaryList(playlists, onItemClick)
    } else {
        val playlist = playlists.find { it.id == selectedItem }
        val playlistSongs = playlist?.songs ?: emptyList()
        SongList(playlistSongs, playlistSongs, onPlay, onAddSongToPlaylist, onEditSong)
    }
}

@Composable
fun PlaylistSummaryList(playlists: List<Playlist>, onItemClick: (String) -> Unit) {
    if (playlists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay playlists creadas")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(playlists) { playlist ->
                ListItem(
                    modifier = Modifier.clickable { onItemClick(playlist.id) },
                    headlineContent = { Text(playlist.name) },
                    supportingContent = { Text("${playlist.songs.size} canciones") },
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
fun SongItem(song: Song, contextPlaylist: List<Song>, onPlay: (Song, List<Song>) -> Unit, onAddSongToPlaylist: (Song) -> Unit, onEditSong: (Song) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onPlay(song, contextPlaylist) },
        headlineContent = { Text(song.title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text("${song.artist} • ${song.album}", style = MaterialTheme.typography.bodyMedium) },
        leadingContent = { Icon(Icons.Default.MusicNote, contentDescription = null) },
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
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
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
                                modifier = Modifier.clickable { onPlaylistSelected(playlist.id) },
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
