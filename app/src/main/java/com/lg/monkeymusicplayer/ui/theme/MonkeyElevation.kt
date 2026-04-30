package com.lg.monkeymusicplayer.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Simple predefined elevations for the app
object MonkeyElevation {
    // Semantic elevations (realistic, reusable across components)
    val Flat: Dp = 0.dp
    val Raised: Dp = 2.dp
    val Floating: Dp = 6.dp
    val Hero: Dp = 12.dp
    val Glow: Dp = 16.dp

    // Backward-compatibility: keep existing numeric levels
    val level0: Dp = 0.dp
    val level1: Dp = 1.dp
    val level2: Dp = 2.dp
    val level3: Dp = 3.dp
    val level4: Dp = 4.dp
}
