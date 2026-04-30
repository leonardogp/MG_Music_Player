package com.lg.monkeymusicplayer.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Consistent radii definitions for the app.
 * Exposes individual radii and a Shapes instance mapped to Material3 slots.
 */
object MonkeyShapes {
    // Individual radii (consistent taxonomy)
    val xs = RoundedCornerShape(2.dp)
    val sm = RoundedCornerShape(4.dp)
    val md = RoundedCornerShape(8.dp)
    val lg = RoundedCornerShape(12.dp)
    val xl = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(999.dp)
    val circle = CircleShape

    // Shapes mapping for Material3 theme (extraSmall -> xs, small -> sm, etc.)
    val shapes: Shapes = Shapes(
        extraSmall = xs,
        small = sm,
        medium = md,
        large = lg
    )
}
