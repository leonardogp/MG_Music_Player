package com.lg.monkeymusicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.data.model.ArtistStat
import com.lg.monkeymusicplayer.data.model.SongWithStat
import com.lg.monkeymusicplayer.data.model.UserStats
import com.lg.monkeymusicplayer.data.repository.StatsRepository
import com.lg.monkeymusicplayer.ui.theme.PrimaryOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    statsRepository: StatsRepository,
    onBack: () -> Unit
) {
    val stats by statsRepository.userStats.collectAsState(initial = UserStats())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.size(32.dp)
                            .background(PrimaryOrange.copy(0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.BarChart, null, tint = PrimaryOrange,
                                modifier = Modifier.size(18.dp))
                        }
                        Text(stringResource(R.string.stats_title),
                            fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (stats.totalPlays == 0) {
            StatsEmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // ── Resumen global ───────────────────────────────────────
                item { GlobalSummaryCard(stats) }

                // ── Top canciones ────────────────────────────────────────
                item {
                    StatsSectionHeader(
                        icon  = Icons.Default.MusicNote,
                        title = stringResource(R.string.stats_top_songs)
                    )
                }
                itemsIndexed(stats.topSongs) { index, item ->
                    TopSongRow(rank = index + 1, item = item)
                }

                // ── Top artistas ─────────────────────────────────────────
                item {
                    StatsSectionHeader(
                        icon  = Icons.Default.Person,
                        title = stringResource(R.string.stats_top_artists)
                    )
                }
                itemsIndexed(stats.topArtists) { index, item ->
                    TopArtistRow(rank = index + 1, item = item)
                }
            }
        }
    }
}

// ── Componentes ──────────────────────────────────────────────────────────────

@Composable
private fun StatsEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.BarChart, null, modifier = Modifier.size(64.dp),
                tint = PrimaryOrange.copy(0.4f))
            Text(stringResource(R.string.stats_empty_title),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.stats_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 48.dp))
        }
    }
}

@Composable
private fun GlobalSummaryCard(stats: UserStats) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                Brush.horizontalGradient(listOf(PrimaryOrange.copy(0.12f), Color.Transparent))
            )
        ) {
            Column(modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround) {
                    SummaryMetric(
                        icon  = Icons.Default.AccessTime,
                        value = formatListenTime(stats.totalListenedMs),
                        label = stringResource(R.string.stats_listened)
                    )
                    SummaryMetric(
                        icon  = Icons.Default.PlayArrow,
                        value = stats.totalPlays.toString(),
                        label = stringResource(R.string.stats_plays)
                    )
                    SummaryMetric(
                        icon  = Icons.Default.LibraryMusic,
                        value = stats.uniqueSongsPlayed.toString(),
                        label = stringResource(R.string.stats_songs_played)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(44.dp)
            .background(PrimaryOrange.copy(0.15f), CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = PrimaryOrange, modifier = Modifier.size(22.dp))
        }
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatsSectionHeader(icon: ImageVector, title: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.size(32.dp).background(PrimaryOrange.copy(0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
        }
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outline.copy(0.3f))
}

@Composable
private fun TopSongRow(rank: Int, item: SongWithStat) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = item.song.albumArtUri,
                    contentDescription = null,
                    error = painterResource(R.drawable.ic_monkey_head),
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                // Rank badge — solo top 3
                if (rank <= 3) {
                    Box(modifier = Modifier.align(Alignment.TopEnd)
                        .size(18.dp)
                        .background(rankColor(rank), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text(rank.toString(), fontSize = 9.sp,
                            fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        },
        headlineContent = {
            Text(item.song.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Text(item.song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.stats_plays_count, item.playCount),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, color = PrimaryOrange)
                Text(stringResource(R.string.stats_completed_percent, (item.completionRate * 100).toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun TopArtistRow(rank: Int, item: ArtistStat) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(modifier = Modifier.size(48.dp)
                .background(PrimaryOrange.copy(0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center) {
                Text(
                    text = item.artist.take(1).uppercase(),
                    fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PrimaryOrange
                )
            }
        },
        headlineContent = {
            Text(item.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Text(stringResource(R.string.stats_artist_songs, item.songCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.stats_plays_count, item.totalPlays),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, color = PrimaryOrange)
                Text(formatListenTime(item.totalPlayTimeMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun rankColor(rank: Int): Color = when (rank) {
    1 -> Color(0xFFFFD700)  // Oro
    2 -> Color(0xFFC0C0C0)  // Plata
    3 -> Color(0xFFCD7F32)  // Bronce
    else -> Color.Transparent
}

@Composable
private fun formatListenTime(ms: Long): String {
    val totalMinutes = ms / 60_000
    return when {
        totalMinutes < 60   -> stringResource(R.string.time_unit_m, totalMinutes)
        totalMinutes < 1440 -> stringResource(R.string.time_unit_h_m, totalMinutes / 60, totalMinutes % 60)
        else                -> stringResource(R.string.time_unit_d_h, totalMinutes / 1440, (totalMinutes % 1440) / 60)
    }
}
