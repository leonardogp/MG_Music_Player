package com.lg.monkeymusicplayer.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween

// Lightweight motion specs for the app
object MonkeyMotion {
    val fast: AnimationSpec<Float> = tween<Float>(durationMillis = 150)
    val medium: AnimationSpec<Float> = tween<Float>(durationMillis = 300)
    val slow: AnimationSpec<Float> = tween<Float>(durationMillis = 600)
}
