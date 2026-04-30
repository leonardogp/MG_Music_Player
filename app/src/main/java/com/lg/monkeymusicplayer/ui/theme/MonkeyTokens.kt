package com.lg.monkeymusicplayer.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

/** Real design tokens organized by category. */
object MonkeyColors {
    // Surface levels (backgrounds and cards)
    val Surface0: Color = Color(0xFFFFFFFF)       // fondo absoluto
    val Surface1: Color = Color(0xFFF5F5F5)       // cards
    val Surface2: Color = Color(0xFFE0E0E0)       // elevated
    val Surface3: Color = Color(0xFFCCCCCC)       // interactive

    // Accent system
    val Accent: Color = Color(0xFFFF6D00)
    val AccentGlow: Color = Color(0xFFFFA726)
    val AccentMuted: Color = Color(0x66FF6D00)

    // Text colors
    val TextPrimary: Color = Color(0xFF111111)
    val TextSecondary: Color = Color(0xFF555555)
    val TextTertiary: Color = Color(0xFF888888)
}

object MonkeySpacing {
    val xs: Dp = 4.dp
    val s: Dp = 8.dp
    val m: Dp = 12.dp
    val l: Dp = 16.dp
    val xl: Dp = 24.dp
}

object MonkeyRadius {
    val sm: Dp = 4.dp
    val md: Dp = 8.dp
    val lg: Dp = 12.dp
    val xl: Dp = 16.dp
}

object MonkeyStroke {
    val hairline: Dp = 1.dp
    val thin: Dp = 2.dp
    val thick: Dp = 3.dp
}
