package com.lg.monkeymusicplayer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass

@Composable
fun ScaffoldWithInsets(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize()
    ) {
        content()
    }
}

