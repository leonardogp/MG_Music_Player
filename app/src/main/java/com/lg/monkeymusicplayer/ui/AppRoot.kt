package com.lg.monkeymusicplayer.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.ui.screens.LibraryScreen

@Composable
fun AppRoot(viewModel: MusicViewModel, windowSizeClass: WindowSizeClass) {
    val uiState by viewModel.uiState.collectAsState()

    // Mostrar la pantalla de bienvenida cuando:
    //   • isLoading == true  (Room aún no emitió el primer valor)
    //   • isScanning == true && no hay canciones todavía (escaneo inicial en curso)
    // En cuanto haya canciones en la librería la app es usable aunque siga escaneando.
    val showLoading = uiState.isLoading ||
            (uiState.isScanning && uiState.songs.isEmpty())

    if (showLoading) {
        UltraProSplashScreen(
            isScanning   = uiState.isScanning,
            scanProgress = uiState.scanProgress,
            scanTotal    = uiState.scanTotal
        )
    } else {
        LibraryScreen(
            viewModel     = viewModel,
            windowSizeClass = windowSizeClass
        )
    }
}

@Composable
fun UltraProSplashScreen(
    isScanning: Boolean = false,
    scanProgress: Int = 0,
    scanTotal: Int = 0
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SplashInfinite")

    // Animación de escala del mono al aparecer
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(900, easing = EaseOutBack),
        label = "MonkeyScale"
    )

    // Ripple (onda circular)
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleScale"
    )

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleAlpha"
    )

    // Barras tipo audio
    val bars = List(5) { index ->
        infiniteTransition.animateFloat(
            initialValue = 20f,
            targetValue = 80f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 500,
                    delayMillis = index * 120
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Bar_$index"
        )
    }

    // Progreso de escaneo (si aplica)
    val progressFraction = if (scanTotal > 0) scanProgress.toFloat() / scanTotal else 0f
    val animatedProgress by animateFloatAsState(
        targetValue  = progressFraction,
        animationSpec = tween(durationMillis = 300),
        label        = "ScanProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // Ripple detrás
        Canvas(modifier = Modifier.size(260.dp)) {
            drawCircle(
                color = Color(0xFF22C55E),
                radius = size.minDimension / 2 * rippleScale,
                alpha = rippleAlpha
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // 🐵 Mono (icono)
            Image(
                painter = painterResource(id = R.drawable.ic_monkey_head),
                contentDescription = null,
                modifier = Modifier
                    .size((140 * scale).dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 🎧 Barras ecualizador
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bars.forEach { bar ->
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(bar.value.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 📝 Nombre app
            Text(
                text = stringResource(id = R.string.app_name),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            if (isScanning && scanTotal > 0) {
                Spacer(modifier = Modifier.height(40.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 48.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = Color(0xFF22C55E),
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.scanning_progress, scanProgress, scanTotal),
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
