package com.lg.monkeymusicplayer.ui

import androidx.annotation.StringRes
import com.lg.monkeymusicplayer.R

enum class SortOrder(@StringRes val labelResId: Int) {
    NAME(R.string.sort_name),
    ARTIST(R.string.sort_artist),
    ALBUM(R.string.sort_album),
    DATE_ADDED(R.string.sort_date_added)
}
