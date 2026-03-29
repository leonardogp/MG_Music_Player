package com.lg.monkeymusicplayer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        LoadingScreen(
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
fun LoadingScreen(
    isScanning: Boolean = false,
    scanProgress: Int = 0,
    scanTotal: Int = 0
) {
    // Progreso animado para que la barra no salte bruscamente
    val progressFraction = if (scanTotal > 0) scanProgress.toFloat() / scanTotal else 0f
    val animatedProgress by animateFloatAsState(
        targetValue  = progressFraction,
        animationSpec = tween(durationMillis = 300),
        label        = "scan_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter           = painterResource(id = R.drawable.ic_monkey_head),
                contentDescription = null,
                modifier          = Modifier.size(140.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text  = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color       = Color.White,
                    fontWeight  = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (isScanning && scanTotal > 0) {
                // Escaneo inicial en curso: barra de progreso determinada con contador
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 48.dp)
                ) {
                    LinearProgressIndicator(
                        progress     = { animatedProgress },
                        modifier     = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color        = MaterialTheme.colorScheme.primary,
                        trackColor   = Color.White.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text  = stringResource(R.string.scanning_progress, scanProgress, scanTotal),
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                // Cargando desde DB o inicio del escaneo (total aún desconocido)
                CircularProgressIndicator(
                    color       = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier    = Modifier.size(32.dp)
                )
            }
        }
    }
}
