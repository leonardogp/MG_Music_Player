package com.lg.monkeymusicplayer.ui.theme.dynamic

import androidx.compose.runtime.staticCompositionLocalOf

val LocalDynamicPlayerColors =
    staticCompositionLocalOf {

        DynamicPlayerColors(

            dominant = androidx.compose.ui.graphics.Color(0xFFFF9800),

            accent = androidx.compose.ui.graphics.Color(0xFFFF9800),

            background = androidx.compose.ui.graphics.Color.Black,

            backgroundDark = androidx.compose.ui.graphics.Color.Black,

            textPrimary = androidx.compose.ui.graphics.Color.White,

            textSecondary = androidx.compose.ui.graphics.Color.LightGray,

            waveform = androidx.compose.ui.graphics.Color(0xFFFF9800),

            controls = androidx.compose.ui.graphics.Color.White

        )
    }