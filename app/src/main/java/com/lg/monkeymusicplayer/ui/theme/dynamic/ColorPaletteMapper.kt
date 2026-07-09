package com.lg.monkeymusicplayer.ui.theme.dynamic

import androidx.compose.ui.graphics.Color

object ColorPaletteMapper {

    fun map(
        palette: ColorPalette
    ): DynamicPlayerColors {

        return DynamicPlayerColors(

            dominant = palette.dominant,

            vibrant = palette.vibrant,

            muted = palette.muted,

            dark = palette.dark,

            light = palette.light,

            accent = palette.vibrant,

            background = palette.dark,

            backgroundDark = palette.dark.darken(0.35f),

            textPrimary = Color.White,

            textSecondary = Color.White.copy(alpha = 0.70f),

            waveform = palette.vibrant,

            controls = Color.White

        )
    }

    private fun Color.darken(
        amount: Float
    ): Color {

        return Color(

            red = red * (1f - amount),

            green = green * (1f - amount),

            blue = blue * (1f - amount),

            alpha = alpha

        )
    }
}