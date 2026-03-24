package com.lg.monkeymusicplayer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.ui.MusicViewModel
import com.lg.monkeymusicplayer.ui.theme.PrimaryOrange
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val equalizerData by viewModel.equalizerData.collectAsState(initial = null)
    val accentColor = uiState.playerState.accentColor // Dynamic from song
    val currentSong = uiState.playerState.currentSong
    val coroutineScope = rememberCoroutineScope()
    
    var enabled by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableStateOf(0) }
    val presets = listOf("FLAT", "ROCK", "POP", "JAZZ", "BASS", "TREBLE")
    
    // EQ Data
    val numBands = equalizerData?.getShort("num_bands")?.toInt() ?: 5
    val centerFreqs = equalizerData?.getIntArray("center_freqs") ?: IntArray(numBands) { 0 }
    
    var customLevels by remember(equalizerData) {
        val currentData = equalizerData
        val initialLevels = currentData?.getShortArray("band_levels")?.map { 
            it.toFloat() / (currentData.getShort("max_level").toFloat().takeIf { v -> v > 0f } ?: 1500f).coerceAtLeast(1f) 
        } ?: List(numBands) { 0f }
        mutableStateOf(initialLevels)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred album art background
        currentSong?.let { song ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(song.albumArtUri).build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Dark gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, null, tint = accentColor, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            stringResource(R.string.equalizer),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Song info
            currentSong?.let { song ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(song.albumArtUri).build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                song.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                song.artist,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Toggle & Presets
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.eq_high_fidelity_mode),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accentColor,
                                checkedTrackColor = accentColor.copy(alpha = 0.5f)
                            ),
                            thumbContent = {
                                Icon(
                                    Icons.Default.GraphicEq,
                                    null,
                                    tint = if (enabled) Color.White else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Presets LazyRow
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        items(presets.size) { index ->
                            FilterChip(
                                selected = selectedPreset == index,
                                onClick = {
                                    selectedPreset = index
                                    // ── CORRECCIÓN: sincronizar el hardware EQ ──
                                    // Antes: customLevels se actualizaba visualmente pero
                                    //        setEqualizerBand() nunca se llamaba → el EQ del
                                    //        sistema no cambiaba aunque la UI lo mostrara diferente.
                                    // Ahora: se aplica cada nivel al hardware vía el ViewModel.
                                    val presetLevels = when (presets[index]) {
                                        "FLAT"   -> List(numBands) { 0f }
                                        "ROCK"   -> listOf(-0.2f, 0f, 0.2f, 0.4f, 0.3f)
                                        "POP"    -> listOf(0.2f, 0.2f, 0f, 0f, 0.2f)
                                        "JAZZ"   -> listOf(0.4f, 0.2f, 0f, -0.2f, 0f)
                                        "BASS"   -> listOf(0.8f, 0.4f, 0f, 0f, 0f)
                                        else     -> listOf(0f, 0f, 0.2f, 0.6f, 0.8f) // TREBLE
                                    }
                                    // Asegurar que la lista tenga exactamente numBands elementos
                                    val normalized = List(numBands) { i ->
                                        presetLevels.getOrElse(i) { 0f }
                                    }
                                    customLevels = normalized
                                    // Aplicar al hardware EQ
                                    coroutineScope.launch {
                                        normalized.forEachIndexed { bandIndex, level ->
                                            val rawLevel = (level * 1500f).toInt().toShort()
                                            viewModel.setEqualizerBand(bandIndex.toShort(), rawLevel)
                                        }
                                    }
                                },
                                label = { Text(presets[index], color = Color.White, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = if (selectedPreset == index) accentColor.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.3f),
                                    selectedContainerColor = accentColor.copy(alpha = 0.3f),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // EQ Graph (visual curve)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawEQCurve(customLevels, size)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Band Sliders
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                customLevels.forEachIndexed { index, level ->
                    AnimatedCard(
                        level = level,
                        frequency = centerFreqs.getOrNull(index) ?: 0,
                        onLevelChange = { newLevel ->
                            val newList = customLevels.toMutableList()
                            newList[index] = newLevel
                            customLevels = newList
                            coroutineScope.launch {
                                val rawLevel = (newLevel * 1500f).toInt().toShort()
                                viewModel.setEqualizerBand(index.toShort(), rawLevel)
                            }
                        },
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedCard(
    level: Float,
    frequency: Int,
    onLevelChange: (Float) -> Unit,
    accentColor: Color
) {
    var isDragging by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(80.dp)) {
                Text(
                    if (frequency >= 1000) "${(frequency / 1000f).toInt()}kHz" else "${frequency}Hz",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${(level * 15).toInt()}dB",
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor
                )
            }

            Slider(
                value = level,
                onValueChange = onLevelChange,
                valueRange = -1f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                )
            )
        }
    }
}

private fun DrawScope.drawEQCurve(levels: List<Float>, size: androidx.compose.ui.geometry.Size) {
    if (levels.isEmpty()) return
    
    val path = Path()
    val width = size.width
    val height = size.height
    val centerY = height / 2
    
    val points = mutableListOf<Offset>()
    val stepX = width / (levels.size + 1)
    
    // Add start point
    points.add(Offset(0f, centerY))
    
    levels.forEachIndexed { index, level ->
        val x = stepX * (index + 1)
        val y = centerY - (level * (height / 2.5f))
        points.add(Offset(x, y))
    }
    
    // Add end point
    points.add(Offset(width, centerY))
    
    // Smooth curve using cubic hermite spline approximation or simple quadratic
    path.moveTo(points[0].x, points[0].y)
    
    for (i in 0 until points.size - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val midX = (p0.x + p1.x) / 2
        
        path.quadraticTo(p0.x, p0.y, midX, (p0.y + p1.y) / 2)
    }
    
    path.lineTo(points.last().x, points.last().y)
    
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.3f),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
    
    // Draw points
    points.forEachIndexed { index, point ->
        if (index > 0 && index < points.size - 1) {
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = point
            )
        }
    }
}
