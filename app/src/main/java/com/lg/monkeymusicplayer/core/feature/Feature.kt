package com.lg.monkeymusicplayer.core.feature

/**
 * Catálogo de features que pueden estar bloqueadas en la versión gratuita.
 *
 * Uso en Composables:
 * ```kotlin
 * if (FeatureGate.isUnlocked(Feature.VISUALIZER)) {
 *     AudioVisualizer(...)
 * } else {
 *     LockedFeatureBadge(Feature.VISUALIZER)
 * }
 * ```
 *
 * Uso en ViewModels:
 * ```kotlin
 * if (!FeatureGate.isUnlocked(Feature.CROSSFADE)) return
 * ```
 */
enum class Feature(
    /** Identificador estable para persistencia (SharedPreferences / Billing). */
    val key: String,
    /** Nombre legible para mostrar en UI de paywall. */
    val displayName: String,
    /** True si la feature está habilitada en la versión gratuita. */
    val freeDefault: Boolean = false
) {
    // ── Core (gratuitas) ─────────────────────────────────────────────────────
    LIBRARY(
        key = "library",
        displayName = "Library",
        freeDefault = true
    ),
    PLAYLISTS(
        key = "playlists",
        displayName = "Playlists",
        freeDefault = true
    ),
    FAVORITES(
        key = "favorites",
        displayName = "Favorites",
        freeDefault = true
    ),
    LYRICS(
        key = "lyrics",
        displayName = "Lyrics",
        freeDefault = true
    ),
    SLEEP_TIMER(
        key = "sleep_timer",
        displayName = "Sleep Timer",
        freeDefault = true
    ),

    // ── Premium ──────────────────────────────────────────────────────────────
    VISUALIZER(
        key = "visualizer",
        displayName = "Audio Visualizer"
    ),
    CROSSFADE(
        key = "crossfade",
        displayName = "Crossfade"
    ),
    EQUALIZER_PRESETS(
        key = "eq_presets",
        displayName = "EQ Presets"
    ),
    SMART_PLAYLISTS(
        key = "smart_playlists",
        displayName = "Smart Playlists"
    ),
    STATS(
        key = "stats",
        displayName = "Listening Stats"
    ),
    BACKUP(
        key = "backup",
        displayName = "Backup & Restore"
    ),
    REPLAY_GAIN(
        key = "replay_gain",
        displayName = "ReplayGain"
    )
}
