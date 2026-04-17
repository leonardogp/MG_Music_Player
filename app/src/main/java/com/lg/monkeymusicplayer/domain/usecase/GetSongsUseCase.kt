package com.lg.monkeymusicplayer.domain.usecase

import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.data.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Expone el catálogo completo como Flow reactivo.
 * El ViewModel no debe depender directamente de MusicRepository para lecturas de canciones.
 */
class GetSongsUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(): Flow<List<Song>> = repository.allSongsFlow
}
