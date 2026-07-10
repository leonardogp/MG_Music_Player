package com.lg.monkeymusicplayer.ui.theme.dynamic

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

class BitmapAnalyzer {

    fun analyze(
        pixels: IntArray
    ): ColorPalette {

        val histogram = HashMap<Int, Int>()

        pixels.forEach {

            val r = (it shr 16) and 0xff
            val g = (it shr 8) and 0xff
            val b = it and 0xff

            if (isIgnoredColor(r, g, b))
                return@forEach

            val key =
                quantizeColor(
                    r,
                    g,
                    b
                )

            histogram[key] =
                (histogram[key] ?: 0) + 1
        }

        val dominantColors =
            findDominantColors(histogram)

        return ColorPalette(

            dominant = dominantColors.primary,

            vibrant = dominantColors.secondary,

            muted = dominantColors.primary.desaturate(0.45f),

            dark = dominantColors.primary.darken(0.45f),

            light = dominantColors.primary.lighten(0.75f)

        )
    }

    private fun histogramKeyToColor(
        key: Int
    ): Color {

        val r = ((key shr 8) and 0xF) * 16
        val g = ((key shr 4) and 0xF) * 16
        val b = (key and 0xF) * 16

        return Color(
            android.graphics.Color.rgb(r, g, b)
        )
    }

    private fun colorDistance(
        c1: Color,
        c2: Color
    ): Float {

        val dr = c1.red - c2.red
        val dg = c1.green - c2.green
        val db = c1.blue - c2.blue

        return dr * dr +
                dg * dg +
                db * db
    }

    private fun findDominantColors(
        histogram: HashMap<Int, Int>
    ): DominantColors {

        val sorted =
            histogram.entries
                .sortedByDescending { it.value }

        val primaryKey =
            sorted.firstOrNull()?.key ?: 0

        val primaryColor =
            histogramKeyToColor(primaryKey)

        val secondaryKey =

            sorted.firstOrNull {

                val candidate =
                    histogramKeyToColor(it.key)

                colorDistance(
                    primaryColor,
                    candidate
                ) > 0.08f

            }?.key ?: primaryKey

        return DominantColors(

            primary = primaryColor,

            secondary = histogramKeyToColor(secondaryKey)

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

    private fun Color.lighten(
        amount: Float
    ): Color {

        return Color(

            red = red + (1f - red) * amount,

            green = green + (1f - green) * amount,

            blue = blue + (1f - blue) * amount,

            alpha = alpha

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

    private fun Color.desaturate(
        amount: Float
    ): Color {

        val gray = (red + green + blue) / 3f

        return Color(

            red = red + (gray - red) * amount,

            green = green + (gray - green) * amount,

            blue = blue + (gray - blue) * amount,

            alpha = alpha

        )
    }

    private fun quantizeColor(
        r: Int,
        g: Int,
        b: Int
    ): Int {

        return ((r / 16) shl 8) +
                ((g / 16) shl 4) +
                (b / 16)
    }
}