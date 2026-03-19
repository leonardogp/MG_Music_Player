package com.lg.monkeymusicplayer.ui.components

import android.media.audiofx.Visualizer
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AudioVisualizer(audioSessionId: Int, accentColor: Color = Color.Cyan) {
    var rawMagnitudes by remember { mutableStateOf(FloatArray(32)) }
    
    // Suavizado de las barras (interpolación)
    val smoothedMagnitudes = remember { FloatArray(32) }
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow"
    )

    DisposableEffect(audioSessionId) {
        if (audioSessionId <= 0) return@DisposableEffect onDispose {}
        
        val visualizer = try {
            Visualizer(audioSessionId).apply {
                captureSize = 128 // Tamaño reducido para mayor velocidad
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                    
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        val newMagnitudes = FloatArray(32)
                        if (fft != null) {
                            for (i in 0 until 32) {
                                val r = fft[i * 2].toInt()
                                val j = fft[i * 2 + 1].toInt()
                                val magnitude = Math.hypot(r.toDouble(), j.toDouble()).toFloat()
                                newMagnitudes[i] = magnitude
                            }
                        }
                        rawMagnitudes = newMagnitudes
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        } catch (e: Exception) {
            null
        }

        onDispose {
            visualizer?.release()
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val barCount = 32
        val barWidth = width / barCount
        val gap = 4.dp.toPx()

        for (i in 0 until barCount) {
            // Aplicar inercia (caída lenta)
            val target = (rawMagnitudes[i] * 2f).coerceAtMost(height)
            smoothedMagnitudes[i] = smoothedMagnitudes[i] * 0.8f + target * 0.2f
            
            val currentBarHeight = smoothedMagnitudes[i].coerceAtLeast(5.dp.toPx())
            
            // Dibujar barra con gradiente
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = glowAlpha),
                        accentColor.copy(alpha = 0.2f)
                    ),
                    startY = height - currentBarHeight,
                    endY = height
                ),
                topLeft = Offset(i * barWidth + gap / 2, height - currentBarHeight),
                size = Size(barWidth - gap, currentBarHeight)
            )

            // Dibujar "capa de brillo" superior
            drawRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = Offset(i * barWidth + gap / 2, height - currentBarHeight),
                size = Size(barWidth - gap, 2.dp.toPx())
            )
        }
    }
}
