package com.mg.mgmusicplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mg.mgmusicplayer.data.model.Song
import androidx.activity.ComponentActivity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LibraryScreen(
    songs: List<Song>,
    genres: Map<String, List<Song>>,
    artists: Map<String, List<Song>>,
    albums: Map<String, List<Song>>,
    folders: Map<String, List<Song>>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    currentSong: Song?,
    isPlaying: Boolean,
    isShuffleMode: Boolean,
    onPlayPause: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onScanMusic: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit
) {
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as ComponentActivity)
    val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    if (isTablet) {
        TabletLayout(
            songs, genres, artists, albums, folders, searchQuery, onSearchQueryChanged,
            currentSong, isPlaying, isShuffleMode, onPlayPause, onPlay,
            onScanMusic, onSkipNext, onSkipPrevious, onToggleShuffle
        )
    } else {
        MobileLayout(
            songs, genres, artists, albums, folders, searchQuery, onSearchQueryChanged,
            currentSong, isPlaying, isShuffleMode, onPlayPause, onPlay,
            onScanMusic, onSkipNext, onSkipPrevious, onToggleShuffle
        )
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
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    currentSong: Song?,
    isPlaying: Boolean,
    isShuffleMode: Boolean,
    onPlayPause: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onScanMusic: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Temas", "Géneros", "Artistas", "Álbumes", "Carpetas")
    var selectedCategoryItem by remember { mutableStateOf<String?>(null) }

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
        bottomBar = { PlayerBottomBar(currentSong, isPlaying, isShuffleMode, onPlayPause, onSkipNext, onSkipPrevious, onToggleShuffle) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> SongList(songs, songs, onPlay)
                1 -> CategoryNavigation(genres, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, Icons.Default.LibraryMusic)
                2 -> CategoryNavigation(artists, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, Icons.Default.Person)
                3 -> CategoryNavigation(albums, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, Icons.Default.Album)
                4 -> CategoryNavigation(folders, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, Icons.Default.Folder)
            }
        }
    }
}

@Composable
fun TabletLayout(
    songs: List<Song>,
    genres: Map<String, List<Song>>,
    artists: Map<String, List<Song>>,
    albums: Map<String, List<Song>>,
    folders: Map<String, List<Song>>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    currentSong: Song?,
    isPlaying: Boolean,
    isShuffleMode: Boolean,
    onPlayPause: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onScanMusic: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategoryItem by remember { mutableStateOf<String?>(null) }

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
                "Carpetas" to Icons.Default.Folder
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
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Buscar en la biblioteca...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> SongList(songs, songs, onPlay)
                    1 -> CategoryNavigation(genres, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, Icons.Default.LibraryMusic)
                    2 -> CategoryNavigation(artists, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, Icons.Default.Person)
                    3 -> CategoryNavigation(albums, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, Icons.Default.Album)
                    4 -> CategoryNavigation(folders, selectedCategoryItem, onPlay, { selectedCategoryItem = it }, Icons.Default.Folder)
                }
            }
            
            PlayerBottomBar(currentSong, isPlaying, isShuffleMode, onPlayPause, onSkipNext, onSkipPrevious, onToggleShuffle)
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
    onToggleShuffle: () -> Unit
) {
    if (currentSong != null) {
        Surface(tonalElevation = 8.dp) {
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
                    IconButton(onClick = onToggleShuffle) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle",
                            tint = if (isShuffleMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onSkipPrevious) { Icon(Icons.Default.SkipPrevious, contentDescription = "Atrás") }
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
fun CategoryNavigation(
    data: Map<String, List<Song>>,
    selectedItem: String?,
    onPlay: (Song, List<Song>) -> Unit,
    onItemClick: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    if (selectedItem == null) {
        CategorySummaryList(data, onItemClick, icon)
    } else {
        val categorySongs = data[selectedItem] ?: emptyList()
        SongList(categorySongs, categorySongs, onPlay)
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
fun SongList(songs: List<Song>, contextPlaylist: List<Song>, onPlay: (Song, List<Song>) -> Unit) {
    if (songs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay canciones") }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(songs) { song -> SongItem(song, contextPlaylist, onPlay) }
        }
    }
}

@Composable
fun SongItem(song: Song, contextPlaylist: List<Song>, onPlay: (Song, List<Song>) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onPlay(song, contextPlaylist) },
        headlineContent = { Text(song.title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text("${song.artist} • ${song.album}", style = MaterialTheme.typography.bodyMedium) },
        leadingContent = { Icon(Icons.Default.MusicNote, contentDescription = null) }
    )
}
