package com.lg.monkeymusicplayer.ui.theme.dynamic

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

private val analyzer = BitmapAnalyzer()

class BitmapColorEngine : DynamicColorEngine {

    override suspend fun extract(
        bitmap: Bitmap
    ): DynamicPlayerColors {

        val smallBitmap = Bitmap.createScaledBitmap(
            bitmap,
            48,
            48,
            true
        )

        val pixels = IntArray(48 * 48)

        smallBitmap.getPixels(
            pixels,
            0,
            48,
            0,
            0,
            48,
            48
        )

        val palette = analyzer.analyze(pixels)

        return ColorPaletteMapper.map(palette)
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