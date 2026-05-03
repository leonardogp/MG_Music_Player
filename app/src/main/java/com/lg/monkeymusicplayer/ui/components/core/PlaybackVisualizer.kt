package com.lg.monkeymusicplayer.ui.components.core

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lg.monkeymusicplayer.ui.theme.PrimaryOrange

@Composable
fun PlaybackVisualizer(
    isPlaying: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")

    val heights = List(4) { index ->
        infiniteTransition.animateFloat(
            initialValue = 6f,
            targetValue = if (isPlaying) 18f else 6f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 350 + index * 90,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = ""
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.height(20.dp)
    ) {
        heights.forEach {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(it.value.dp)
                    .background(
                        PrimaryOrange,
                        RoundedCornerShape(8.dp)
                    )
            )
        }
    }
}