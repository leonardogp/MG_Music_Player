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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.ui.screens.LibraryScreen
import kotlinx.coroutines.delay

@Composable
fun AppRoot(viewModel: MusicViewModel, windowSizeClass: WindowSizeClass) {
    val uiState by viewModel.uiState.collectAsState()

    // Estado para asegurar que el splash sea visible al menos un tiempo
    var showSplashTimeout by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(2500) // 2.5 segundos para apreciar la animación completa
        showSplashTimeout = false
    }

    val showLoading = uiState.isLoading ||
            (uiState.isScanning && uiState.songs.isEmpty()) ||
            showSplashTimeout

    if (showLoading) {
        UltraProSplashScreen(
            isScanning   = uiState.isScanning,
            scanProgress = uiState.scanProgress,
            scanTotal    = uiState.scanTotal
        )
    } else {
        LibraryScreen(
            viewModel       = viewModel,
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
    // ── Estado de entrada ──
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val infiniteTransition = rememberInfiniteTransition(label = "SplashInfinite")

    // Logo: entra desde escala 0 → 1 con rebote (Naranja PrimaryOrange #FF8C00)
    val logoScale by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(800, easing = EaseOutBack),
        label         = "LogoScale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label         = "LogoAlpha"
    )

    // Texto y barras: aparecen con retraso
    val contentAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 500),
        label         = "ContentAlpha"
    )

    // Ripple 1 — Naranja Principal (#FF8C00)
    val rippleScale by infiniteTransition.animateFloat(
        initialValue  = 0.5f,
        targetValue   = 2.2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleScale"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.35f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleAlpha"
    )

    // Ripple 2 — Dorado Secundario (#FFD700) desfasado
    val ripple2Scale by infiniteTransition.animateFloat(
        initialValue  = 0.5f,
        targetValue   = 2.2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = LinearEasing, delayMillis = 1100),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleScale2"
    )
    val ripple2Alpha by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2200
                0f at 0
                0.25f at 300
                0f at 2200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleAlpha2"
    )

    // Barras de ecualizador animadas
    val barHeights = List(7) { index ->
        infiniteTransition.animateFloat(
            initialValue  = 12f,
            targetValue   = 56f,
            animationSpec = infiniteRepeatable(
                animation  = tween(450, delayMillis = index * 90, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Bar_$index"
        )
    }

    // Progreso del escaneo
    val progressFraction = if (scanTotal > 0) scanProgress.toFloat() / scanTotal else 0f
    val animatedProgress by animateFloatAsState(
        targetValue   = progressFraction,
        animationSpec = tween(300),
        label         = "ScanProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF121212), // DarkBackground
                        0.7f to Color(0xFF1E1E1E), // DarkSurface
                        1.0f to Color(0xFF121212)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // Contenedor para alinear el logo y las ondas en el mismo centro exacto
        Box(contentAlignment = Alignment.Center) {
            // ── Ripple 1 (Naranja) ──
            Canvas(modifier = Modifier.size(240.dp)) {
                drawCircle(
                    color  = Color(0xFFFF8C00),
                    radius = size.minDimension / 2 * rippleScale,
                    alpha  = rippleAlpha * logoAlpha
                )
            }

            // ── Ripple 2 (Dorado) ──
            Canvas(modifier = Modifier.size(240.dp)) {
                drawCircle(
                    color  = Color(0xFFFFD700),
                    radius = size.minDimension / 2 * ripple2Scale,
                    alpha  = ripple2Alpha * logoAlpha
                )
            }

            // ── Logo del mono ─────────────────────────────────────────────
            Image(
                painter           = painterResource(id = R.drawable.ic_monkey_head),
                contentDescription = null,
                modifier          = Modifier
                    .size(140.dp)
                    .scale(logoScale)
                    .graphicsLayer(alpha = logoAlpha)
            )
        }

        // El resto del contenido (barras, texto, progreso) debajo
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 80.dp) // Espacio desde el fondo
            ) {
                // ── Barras de ecualizador ─────────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    modifier              = Modifier.graphicsLayer(alpha = contentAlpha)
                ) {
                    barHeights.forEachIndexed { index, bar ->
                        val isCenter = index == barHeights.size / 2
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(bar.value.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isCenter) Color(0xFFFF8C00) // Centro naranja
                                    else Color.White.copy(alpha = 0.75f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Nombre de la app ──────────────────────────────────────────
                Text(
                    text        = stringResource(id = R.string.app_name),
                    color       = Color.White.copy(alpha = 0.85f),
                    fontSize    = 20.sp,
                    fontWeight  = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    modifier    = Modifier.graphicsLayer(alpha = contentAlpha)
                )

                // ── Barra de progreso ──
                if (isScanning) {
                    Spacer(modifier = Modifier.height(44.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(horizontal = 48.dp)
                    ) {
                        if (scanTotal > 0) {
                            LinearProgressIndicator(
                                progress  = { animatedProgress },
                                modifier  = Modifier.fillMaxWidth().height(3.dp),
                                color     = Color(0xFFFF8C00),
                                trackColor = Color.White.copy(alpha = 0.12f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text  = stringResource(R.string.scanning_progress, scanProgress, scanTotal),
                                color = Color.White.copy(alpha = 0.55f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            CircularProgressIndicator(
                                color       = Color(0xFFFF8C00),
                                strokeWidth = 2.dp,
                                modifier    = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
