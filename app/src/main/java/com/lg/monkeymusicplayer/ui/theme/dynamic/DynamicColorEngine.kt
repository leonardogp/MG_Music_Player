package com.lg.monkeymusicplayer.ui.theme.dynamic

import android.graphics.Bitmap

interface DynamicColorEngine {

    suspend fun extract(bitmap: Bitmap): DynamicPlayerColors

}