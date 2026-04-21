package com.lg.monkeymusicplayer.domain.usecase

import com.lg.monkeymusicplayer.core.player.MusicPlayerManager
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Orquesta la reproducción de una canción:
 * 1. Asigna la playlist al player.
 * 2. Lanza la reproducción de la canción seleccionada.
 * 3. Registra el evento en el historial (fire-and-forget en IO).
 *
 * El ViewModel delega aquí en lugar de coordinar las 3 operaciones directamente.
 */
class PlaySongUseCase @Inject constructor(
    private val playerManager: MusicPlayerManager,
    private val repository: MusicRepository
) {
    suspend operator fun invoke(song: Song, playlist: List<Song>) {
        playerManager.setPlaylist(playlist)
        playerManager.play(song)
        withContext(Dispatchers.IO) {
            repository.addToHistory(song.id)
        }
    }
}
