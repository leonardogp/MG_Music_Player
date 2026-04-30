package com.lg.monkeymusicplayer.ui.components.core

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lg.monkeymusicplayer.ui.theme.MonkeyElevation
import com.lg.monkeymusicplayer.ui.theme.MonkeyShapes

/**
 * Simple card wrapper with optional click handler.
 * This is a lightweight abstraction over a Surface to avoid tight coupling
 * to Material3 Card API surface changes.
 */
@Composable
fun MonkeyCard(
    modifier: Modifier = Modifier,
    elevation: Dp = MonkeyElevation.Flat,
    border: Boolean = false,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    glow: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val effectiveElevation = if (glow) elevation + 4.dp else elevation
    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Surface(
        modifier = modifier.then(clickModifier),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = effectiveElevation,
        border = if (border) BorderStroke(1.dp, borderColor) else null,
        shape = MonkeyShapes.shapes.small
    ) {
        content()
    }
}
