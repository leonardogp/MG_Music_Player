package com.lg.monkeymusicplayer.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

private val monkeyPalette = listOf(
    Color(0xFFFF9800),
    Color(0xFFFF5722),
    Color(0xFFE91E63),
    Color(0xFF9C27B0),
    Color(0xFF3F51B5),
    Color(0xFF2196F3),
    Color(0xFF009688),
    Color(0xFF4CAF50),
    Color(0xFFFFC107)
)

fun dynamicAccent(seed: String): Color {
    val index = seed.hashCode().absoluteValue % monkeyPalette.size
    return monkeyPalette[index]
}