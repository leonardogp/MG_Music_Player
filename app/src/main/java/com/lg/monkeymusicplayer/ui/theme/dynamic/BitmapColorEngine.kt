package com.lg.monkeymusicplayer.ui.theme.dynamic

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

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

        val dominant = calculateDominantColor(pixels)

        return DynamicPlayerColors(

            dominant = dominant,

            accent = dominant,

            background = dominant.darken(0.72f),

            backgroundDark = dominant.darken(0.88f),

            textPrimary = Color.White,

            textSecondary = Color.White.copy(alpha = 0.70f),

            waveform = dominant,

            controls = Color.White

        )
    }

    private fun calculateDominantColor(
        pixels: IntArray
    ): Color {

        val histogram = HashMap<Int, Int>()

        pixels.forEach {

            val r = (it shr 16) and 0xff
            val g = (it shr 8) and 0xff
            val b = it and 0xff

            if (isIgnoredColor(r, g, b))
                return@forEach

            val key =
                ((r / 16) shl 8) +
                        ((g / 16) shl 4) +
                        (b / 16)

            histogram[key] =
                (histogram[key] ?: 0) + 1
        }

        val dominant =
            histogram.maxByOrNull { it.value }?.key ?: 0

        val r = ((dominant shr 8) and 0xF) * 16
        val g = ((dominant shr 4) and 0xF) * 16
        val b = (dominant and 0xF) * 16

        return Color(
            android.graphics.Color.rgb(r, g, b)
        )
    }

    private fun isIgnoredColor(
        r: Int,
        g: Int,
        b: Int
    ): Boolean {

        if (r > 245 && g > 245 && b > 245)
            return true

        if (r < 12 && g < 12 && b < 12)
            return true

        if (abs(r - g) < 6 &&
            abs(g - b) < 6)
            return true

        return false
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