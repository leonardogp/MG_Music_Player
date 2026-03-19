package com.lg.monkeymusicplayer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lg.monkeymusicplayer.data.model.LyricLine
import kotlinx.coroutines.launch

@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    currentPosition: Long,
    accentColor: Color,
    onLyricClick: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Encontrar la línea actual basada en el tiempo
    val currentIndex = lyrics.indexOfLast { it.timeMs <= currentPosition }.coerceAtLeast(0)

    // Auto-scroll a la línea actual
    LaunchedEffect(currentIndex) {
        if (lyrics.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(currentIndex, scrollOffset = -200)
            }
        }
    }

    if (lyrics.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No hay letras disponibles",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 200.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(lyrics) { index, lyric ->
                val isCurrent = index == currentIndex
                val color by animateColorAsState(
                    targetValue = if (isCurrent) Color.White else Color.White.copy(alpha = 0.3f),
                    animationSpec = tween(400), label = "lyric_color"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isCurrent) 1.1f else 1f,
                    animationSpec = tween(400), label = "lyric_scale"
                )

                Text(
                    text = lyric.text,
                    color = color,
                    fontSize = if (isCurrent) 26.sp else 22.sp,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 24.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .clickable { onLyricClick(lyric.timeMs) }
                )
            }
        }
    }
}
