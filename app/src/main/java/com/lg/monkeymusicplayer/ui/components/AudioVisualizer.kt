package com.lg.monkeymusicplayer.ui.components

import android.media.audiofx.Visualizer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AudioVisualizer(audioSessionId: Int) {
    var magnitudes by remember { mutableStateOf(FloatArray(64)) }
    
    DisposableEffect(audioSessionId) {
        if (audioSessionId <= 0) return@DisposableEffect onDispose {}
        
        val visualizer = try {
            Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                    
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        val newMagnitudes = FloatArray(64)
                        for (i in 0 until 64) {
                            val r = fft?.get(i * 2)?.toInt() ?: 0
                            val j = fft?.get(i * 2 + 1)?.toInt() ?: 0
                            newMagnitudes[i] = Math.hypot(r.toDouble(), j.toDouble()).toFloat()
                        }
                        magnitudes = newMagnitudes
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

    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        val width = size.width
        val height = size.height
        val barWidth = width / magnitudes.size

        magnitudes.forEachIndexed { index, magnitude ->
            val barHeight = (magnitude / 100f) * height
            drawRect(
                color = Color.Cyan.copy(alpha = 0.7f),
                topLeft = androidx.compose.ui.geometry.Offset(index * barWidth, height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth - 2.dp.toPx(), barHeight)
            )
        }
    }
}
