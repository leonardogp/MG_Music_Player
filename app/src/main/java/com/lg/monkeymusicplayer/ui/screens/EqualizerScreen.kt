package com.lg.monkeymusicplayer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.data.database.EqPresetEntity
import com.lg.monkeymusicplayer.ui.MusicViewModel
import kotlinx.coroutines.launch

private data class BuiltInPreset(val label: String, val levels5: List<Float>)

private val BUILT_IN_PRESETS = listOf(
    BuiltInPreset("FLAT",   listOf( 0.0f,  0.0f,  0.0f,  0.0f,  0.0f)),
    BuiltInPreset("ROCK",   listOf(-0.2f,  0.0f,  0.2f,  0.4f,  0.3f)),
    BuiltInPreset("POP",    listOf( 0.2f,  0.2f,  0.0f,  0.0f,  0.2f)),
    BuiltInPreset("JAZZ",   listOf( 0.4f,  0.2f,  0.0f, -0.2f,  0.0f)),
    BuiltInPreset("BASS",   listOf( 0.8f,  0.4f,  0.0f,  0.0f,  0.0f)),
    BuiltInPreset("TREBLE", listOf( 0.0f,  0.0f,  0.2f,  0.6f,  0.8f)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val equalizerData by viewModel.equalizerData.collectAsState(initial = null)
    val userPresets by viewModel.eqPresets.collectAsState()

    val accentColor = uiState.playerState.accentColor
    val currentSong = uiState.playerState.currentSong
    val coroutineScope = rememberCoroutineScope()

    val numBands = equalizerData?.getShort("num_bands")?.toInt() ?: 5
    val maxLevel = equalizerData?.getShort("max_level")?.toFloat()?.takeIf { it > 0f } ?: 1500f
    val centerFreqs = equalizerData?.getIntArray("center_freqs") ?: IntArray(numBands) { 0 }

    // ── ÚNICO SOURCE OF TRUTH ─────────────────────────────────────────────────
    // customLevels es la única variable que alimenta la curva Y los sliders.
    // Se inicializa con ceros; se sincroniza con el hardware la primera vez.
    // Después solo cambia cuando el usuario mueve un slider o aplica un preset.
    var customLevels by remember { mutableStateOf(List(numBands) { 0f }) }
    var hardwareSynced by remember { mutableStateOf(false) }
    LaunchedEffect(equalizerData) {
        if (equalizerData != null && !hardwareSynced) {
            val levels = equalizerData!!.getShortArray("band_levels")
                ?.map { it.toFloat() / maxLevel }
                ?: List(numBands) { 0f }
            customLevels = levels
            hardwareSynced = true
        }
    }

    var enabled by remember { mutableStateOf(true) }
    var selectedBuiltIn by remember { mutableStateOf(0) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetToDelete by remember { mutableStateOf<EqPresetEntity?>(null) }

    // Helper: aplica niveles al hardware y actualiza la UI
    fun applyLevels(levels: List<Float>) {
        val normalized = List(numBands) { i -> levels.getOrElse(i) { 0f } }
        customLevels = normalized
        coroutineScope.launch {
            normalized.forEachIndexed { idx, level ->
                viewModel.setEqualizerBand(idx.toShort(), (level * maxLevel).toInt().toShort())
            }
        }
    }

    // Diálogos
    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { name -> viewModel.saveEqPreset(name, customLevels); showSaveDialog = false }
        )
    }
    presetToDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            title = { Text(stringResource(R.string.eq_delete_preset)) },
            text = { Text(preset.name) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteEqPreset(preset); presetToDelete = null }) {
                    Text(stringResource(R.string.eq_delete_preset), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { presetToDelete = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        currentSong?.let { song ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(song.albumArtUri).build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(50.dp),
                contentScale = ContentScale.Crop
            )
        }
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Black.copy(0.3f), Color.Black.copy(0.7f), Color.Black.copy(0.9f)))
        ))

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, null, tint = accentColor, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.equalizer), color = Color.White,
                            fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                actions = {
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(R.string.eq_save_preset), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Spacer(Modifier.height(8.dp))

            // Canción actual
            currentSong?.let { song ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.4f))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(song.albumArtUri).build(),
                            contentDescription = null,
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(song.title, color = Color.White, fontWeight = FontWeight.Bold,
                                maxLines = 1, style = MaterialTheme.typography.titleMedium)
                            Text(song.artist, color = Color.White.copy(0.7f),
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Built-in presets + toggle
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.4f))) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.eq_high_fidelity_mode), color = Color.White,
                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Switch(checked = enabled, onCheckedChange = { enabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor,
                                checkedTrackColor = accentColor.copy(0.5f)))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.eq_built_in), color = Color.White.copy(0.6f),
                        style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BUILT_IN_PRESETS) { preset ->
                            val idx = BUILT_IN_PRESETS.indexOf(preset)
                            FilterChip(
                                selected = idx == selectedBuiltIn,
                                onClick = { selectedBuiltIn = idx; applyLevels(preset.levels5) },
                                label = { Text(preset.label, color = Color.White, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.Gray.copy(0.3f),
                                    selectedContainerColor = accentColor.copy(0.35f))
                            )
                        }
                    }
                }
            }

            // User presets
            if (userPresets.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.4f))) {
                    Column(Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.eq_my_presets), color = Color.White.copy(0.6f),
                            style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(userPresets, key = { it.id }) { preset ->
                                InputChip(
                                    selected = false,
                                    onClick = { selectedBuiltIn = -1; applyLevels(preset.toLevels()) },
                                    label = { Text(preset.name, color = Color.White) },
                                    trailingIcon = {
                                        IconButton(onClick = { presetToDelete = preset },
                                            modifier = Modifier.size(18.dp)) {
                                            Icon(Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.eq_delete_preset),
                                                tint = Color.White.copy(0.6f), modifier = Modifier.size(14.dp))
                                        }
                                    },
                                    colors = InputChipDefaults.inputChipColors(containerColor = accentColor.copy(0.2f))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Curva EQ — lee customLevels (source of truth único)
            Card(modifier = Modifier.fillMaxWidth().height(170.dp), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.3f))) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    drawEQCurve(customLevels, size, accentColor)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Sliders — leen y escriben en customLevels
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                customLevels.forEachIndexed { index, level ->
                    EqBandSlider(
                        level = level,
                        frequency = centerFreqs.getOrNull(index) ?: 0,
                        accentColor = accentColor,
                        onLevelChange = { newLevel ->
                            val updated = customLevels.toMutableList()
                            updated[index] = newLevel
                            customLevels = updated
                            selectedBuiltIn = -1
                            coroutineScope.launch {
                                viewModel.setEqualizerBand(index.toShort(), (newLevel * maxLevel).toInt().toShort())
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SavePresetDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.eq_save_preset)) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.eq_preset_name_hint)) },
                label = { Text(stringResource(R.string.eq_preset_name)) },
                singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun EqBandSlider(level: Float, frequency: Int, accentColor: Color, onLevelChange: (Float) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().height(84.dp), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.3f))) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(70.dp)) {
                Text(if (frequency >= 1000) "${frequency / 1000}kHz" else "${frequency}Hz",
                    color = Color.White, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium)
                Text("${(level * 15).toInt()}dB", color = accentColor,
                    style = MaterialTheme.typography.bodySmall)
            }
            Slider(value = level, onValueChange = onLevelChange, valueRange = -1f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Color.White,
                    activeTrackColor = accentColor, inactiveTrackColor = Color.Gray.copy(0.3f)))
        }
    }
}

private fun DrawScope.drawEQCurve(levels: List<Float>, size: androidx.compose.ui.geometry.Size, accentColor: Color) {
    if (levels.isEmpty()) return
    val w = size.width; val h = size.height; val cy = h / 2f
    val stepX = w / (levels.size + 1)
    drawLine(Color.White.copy(0.15f), Offset(0f, cy), Offset(w, cy), strokeWidth = 1.dp.toPx())
    val points = buildList {
        add(Offset(0f, cy))
        levels.forEachIndexed { i, lv -> add(Offset(stepX * (i + 1), cy - lv * (h / 2.5f))) }
        add(Offset(w, cy))
    }
    val linePath = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 0 until points.size - 1) {
            val p0 = points[i]; val p1 = points[i + 1]
            quadraticTo(p0.x, p0.y, (p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
        }
        lineTo(points.last().x, points.last().y)
    }
    val fillPath = Path().apply {
        addPath(linePath)
        lineTo(points.last().x, cy); lineTo(0f, cy); close()
    }
    drawPath(fillPath, Brush.verticalGradient(listOf(accentColor.copy(0.2f), Color.Transparent), 0f, h))
    drawPath(linePath, accentColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    points.drop(1).dropLast(1).forEach { pt ->
        drawCircle(Color.White, 4.dp.toPx(), pt)
        drawCircle(accentColor, 2.5f.dp.toPx(), pt)
    }
}
