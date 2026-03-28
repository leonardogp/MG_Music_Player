package com.lg.monkeymusicplayer.ui.widget

import androidx.media3.common.util.UnstableApi
import com.lg.monkeymusicplayer.R

/** 2x1 — portada + título/artista + play/pause */
@UnstableApi
class MusicWidget2x1 : MusicWidget() {
    override val widgetLayout = R.layout.music_widget_small
}

/** 2x2 — portada dominante full-bleed + controles superpuestos */
@UnstableApi
class MusicWidget2x2 : MusicWidget() {
    override val widgetLayout = R.layout.music_widget_square
}

/** 4x1 — portada + título/artista + prev/play/next */
@UnstableApi
class MusicWidget4x1 : MusicWidget() {
    override val widgetLayout = R.layout.music_widget
}

/** 4x2 — portada grande + texto completo + progreso + controles + favorito */
@UnstableApi
class MusicWidget4x2 : MusicWidget() {
    override val widgetLayout = R.layout.music_widget_medium
}

/** 4x4 — portada dominante + progreso + shuffle/repeat + favorito */
@UnstableApi
class MusicWidget4x4 : MusicWidget() {
    override val widgetLayout = R.layout.music_widget_large
}
