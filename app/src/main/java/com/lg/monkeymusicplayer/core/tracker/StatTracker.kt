package com.lg.monkeymusicplayer.core.tracker

import com.lg.monkeymusicplayer.data.database.SongStatDao
import com.lg.monkeymusicplayer.data.database.SongStatEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * StatTracker — registra eventos de reproducción y los persiste en [SongStatDao].
 *
 * Es el único responsable de escribir en `song_stats`. Ni el ViewModel ni el Service
 * tocan esa tabla directamente.
 *
 * Ciclo de vida de una reproducción:
 *  1. [onPlayStarted]  → cuando el usuario inicia o el auto-advance cambia de canción.
 *  2. [onPause]        → cuando se pausa. Acumula tiempo de sesión.
 *  3. [onSkip]         → cuando se salta antes del umbral. Registra skip + tiempo parcial.
 *  4. [onSongComplete] → cuando supera [COMPLETE_THRESHOLD_PERCENT]. Registra complete.
 *
 * Detección de skip vs complete:
 *  - Skip:     posición < umbral al momento del salto manual.
 *  - Complete: posición / duración >= [COMPLETE_THRESHOLD_PERCENT].
 *
 * Escrituras fire-and-forget en Dispatchers.IO con SupervisorJob propio.
 */
@Singleton
class StatTracker @Inject constructor(
    private val dao: SongStatDao
) {

    companion object {
        const val COMPLETE_THRESHOLD_PERCENT = 0.80f
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var activeSongId: Long = -1L
    private var sessionStartMs: Long = 0L

    // ── API pública ──────────────────────────────────────────────────────────

    fun onPlayStarted(songId: Long) {
        activeSongId = songId
        sessionStartMs = System.currentTimeMillis()
        scope.launch {
            ensureStatExists(songId)
            dao.incrementPlay(songId, sessionStartMs)
            Timber.d("StatTracker: play started — songId=$songId")
        }
    }

    fun onSkip(songId: Long, currentPositionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        val ratio = currentPositionMs.toFloat() / durationMs
        if (ratio >= COMPLETE_THRESHOLD_PERCENT) return

        scope.launch {
            ensureStatExists(songId)
            dao.incrementSkip(songId)
            if (currentPositionMs > 0L) dao.addPlayTime(songId, currentPositionMs)
            Timber.d("StatTracker: skip — songId=$songId ratio=%.2f".format(ratio))
        }
        activeSongId = -1L
    }

    fun onSongComplete(songId: Long, playedMs: Long) {
        scope.launch {
            ensureStatExists(songId)
            dao.incrementComplete(songId, playedMs)
            Timber.d("StatTracker: complete — songId=$songId playedMs=$playedMs")
        }
        activeSongId = -1L
    }

    fun onPause(songId: Long, currentPositionMs: Long) {
        if (currentPositionMs <= 0L) return
        val elapsed = (currentPositionMs - sessionStartMs).coerceAtLeast(0L)
        scope.launch {
            ensureStatExists(songId)
            dao.addPlayTime(songId, elapsed)
            Timber.d("StatTracker: pause — songId=$songId elapsed=$elapsed")
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private suspend fun ensureStatExists(songId: Long) {
        dao.insertIfAbsent(SongStatEntity(songId = songId))
    }
}
