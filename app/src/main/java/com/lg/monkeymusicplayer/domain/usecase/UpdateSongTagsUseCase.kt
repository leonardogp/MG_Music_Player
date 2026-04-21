package com.lg.monkeymusicplayer.domain.usecase

import com.lg.monkeymusicplayer.core.result.Result
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.data.repository.MusicRepository
import javax.inject.Inject

/**
 * Actualiza los tags ID3 (título, artista, álbum, género) de una canción.
 * Devuelve [Result] para que el ViewModel mapee éxito/error al UiState correspondiente.
 */
class UpdateSongTagsUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(
        song: Song,
        title: String,
        artist: String,
        album: String,
        genre: String
    ): Result<Unit> = repository.updateSongTags(song, title, artist, album, genre)
}
