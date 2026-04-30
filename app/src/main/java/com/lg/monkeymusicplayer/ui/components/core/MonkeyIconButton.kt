package com.lg.monkeymusicplayer.ui.components.core

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min

enum class MonkeyIconButtonVariant {
    Ghost, Filled, Reactive, Hero
}

@Composable
fun MonkeyIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String? = null,
    variant: MonkeyIconButtonVariant = MonkeyIconButtonVariant.Ghost
) {
    when (variant) {
        MonkeyIconButtonVariant.Ghost -> {
            IconButton(onClick = onClick, modifier = modifier) {
                Icon(imageVector = icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        MonkeyIconButtonVariant.Filled -> {
            Surface(
                modifier = modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
                    Icon(imageVector = icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        MonkeyIconButtonVariant.Reactive -> {
            val interactionSource = remember { MutableInteractionSource() }
            IconButton(
                onClick = onClick,
                modifier = modifier,
                interactionSource = interactionSource
            ) {
                val isPressed = interactionSource.collectIsPressedAsState().value
                val tint = if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
            }
        }
        MonkeyIconButtonVariant.Hero -> {
            Surface(
                modifier = modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 6.dp
            ) {
                IconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
                    Icon(imageVector = icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
