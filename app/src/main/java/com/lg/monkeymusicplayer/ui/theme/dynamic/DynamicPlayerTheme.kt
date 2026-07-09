package com.lg.monkeymusicplayer.ui.theme.dynamic

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalDynamicPlayerColors =
    staticCompositionLocalOf {

        DynamicPlayerColors(

            dominant = Color(0xFFFF9800),

            vibrant = Color(0xFFFFB74D),

            muted = Color(0xFF8D6E63),

            dark = Color(0xFF212121),

            light = Color(0xFFFFF3E0),

            accent = Color(0xFFFF9800),

            background = Color.Black,

            backgroundDark = Color.Black,

            textPrimary = Color.White,

            textSecondary = Color.LightGray,

            waveform = Color(0xFFFF9800),

            controls = Color.White
        )
    }