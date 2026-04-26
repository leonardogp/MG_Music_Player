package com.lg.monkeymusicplayer.core.feature

enum class Feature(
    val key: String,
    val displayName: String,
    val freeDefault: Boolean = false
) {
    // ── Gratuitas ────────────────────────────────────────────────────────────
    LIBRARY(key = "library", displayName = "Library", freeDefault = true),
    PLAYLISTS(key = "playlists", displayName = "Playlists", freeDefault = true),
    FAVORITES(key = "favorites", displayName = "Favorites", freeDefault = true),
    LYRICS(key = "lyrics", displayName = "Lyrics", freeDefault = true),
    SLEEP_TIMER(key = "sleep_timer", displayName = "Sleep Timer", freeDefault = true),
    WIDGETS(key = "widgets", displayName = "Widgets", freeDefault = true),
    SEARCH(key = "search", displayName = "Search", freeDefault = true),
    EDIT_TAGS(key = "edit_tags", displayName = "Edit Tags", freeDefault = true),
    FOLDERS(key = "folders", displayName = "Folder Browser", freeDefault = true),

    // ── PRO ──────────────────────────────────────────────────────────────────
    SMART_PLAYLISTS(key = "smart_playlists", displayName = "Smart Playlists"),
    STATS(key = "stats", displayName = "Listening Stats"),
    VISUALIZER(key = "visualizer", displayName = "Audio Visualizer"),
    CROSSFADE(key = "crossfade", displayName = "Crossfade"),
    REPLAY_GAIN(key = "replay_gain", displayName = "ReplayGain"),
    EQUALIZER_PRESETS(key = "eq_presets", displayName = "EQ Presets"),
    BACKUP(key = "backup", displayName = "Backup & Restore"),
    CLOUD_SYNC(key = "cloud_sync", displayName = "Cloud Sync"),
    CHROMECAST(key = "chromecast", displayName = "Chromecast"),
}
