package com.lg.monkeymusicplayer.core.player

import com.mpatric.mp3agic.Mp3File
import timber.log.Timber

/**
 * ReplayGainReader — extrae el tag REPLAYGAIN_TRACK_GAIN de archivos MP3.
 *
 * El tag se almacena en ID3v2 como TXXX con descripción "REPLAYGAIN_TRACK_GAIN"
 * y valor de la forma "+2.50 dB" o "-3.10 dB".
 *
 * Prioridad de lectura:
 *  1. TXXX:REPLAYGAIN_TRACK_GAIN (track-level, preferido)
 *  2. TXXX:REPLAYGAIN_ALBUM_GAIN (fallback si no hay track gain)
 *
 * Uso:
 * ```kotlin
 * val gainDb = ReplayGainReader.readTrackGain(song.path)
 * // gainDb es Float? — null si el archivo no tiene el tag o no es MP3 válido
 * ```
 */
object ReplayGainReader {

    private val GAIN_REGEX = Regex("""([+-]?\d+(?:\.\d+)?)\s*dB""", RegexOption.IGNORE_CASE)

    /**
     * Lee el gain de un archivo de audio.
     * @return Ganancia en dB como Float, o null si no disponible.
     */
    fun readTrackGain(path: String): Float? {
        if (!path.endsWith(".mp3", ignoreCase = true)) return null
        return try {
            val mp3 = Mp3File(path)
            if (!mp3.hasId3v2Tag()) return null
            val tag = mp3.id3v2Tag

            // Buscar TXXX frames: mp3agic no tiene un helper directo para TXXX, los extraemos manualmente
            val xxxFrames = getTxxxFrames(tag)
            val trackGain = xxxFrames["REPLAYGAIN_TRACK_GAIN"]
                ?: xxxFrames["replaygain_track_gain"]
                ?: xxxFrames["REPLAYGAIN_ALBUM_GAIN"]
                ?: xxxFrames["replaygain_album_gain"]
                ?: return null

            parseGain(trackGain)
        } catch (e: Exception) {
            Timber.v("ReplayGainReader: no se pudo leer gain de $path — ${e.message}")
            null
        }
    }

    /**
     * Extrae los frames TXXX (User defined text information) de un tag ID3v2.
     * mp3agic no tiene un método directo para esto en la versión 0.9.1.
     */
    private fun getTxxxFrames(tag: com.mpatric.mp3agic.ID3v2): Map<String, String> {
        val txxxMap = mutableMapOf<String, String>()
        val frameSet = tag.frameSets["TXXX"] ?: return txxxMap

        for (frame in frameSet.frames) {
            try {
                val data = frame.data ?: continue
                if (data.size < 2) continue

                // Formato TXXX: [encoding (1 byte)] [description (N bytes)] [terminator] [value (M bytes)]
                // El terminador depende del encoding (1 byte para ISO/UTF-8, 2 bytes para UTF-16)
                val encoding = data[0].toInt()
                val charset = when (encoding) {
                    1 -> Charsets.UTF_16    // UTF-16 con BOM
                    2 -> Charsets.UTF_16BE  // UTF-16BE sin BOM
                    3 -> Charsets.UTF_8     // UTF-8
                    else -> Charsets.ISO_8859_1
                }

                // Salto del byte de encoding
                val content = data.sliceArray(1 until data.size)
                
                // Encontrar el separador nulo entre descripción y valor
                // Para simplificar, buscamos el primer byte 0 (funciona bien para ISO y UTF-8).
                // UTF-16 es más complejo pero los tags ReplayGain suelen ser ASCII.
                var splitIndex = -1
                for (i in content.indices) {
                    if (content[i] == 0.toByte()) {
                        splitIndex = i
                        break
                    }
                }

                if (splitIndex != -1) {
                    val description = String(content, 0, splitIndex, charset).trim()
                    val value = String(content, splitIndex + 1, content.size - splitIndex - 1, charset).trim()
                    txxxMap[description] = value
                }
            } catch (e: Exception) {
                // Silencioso, si un frame falla seguimos con los demás
            }
        }
        return txxxMap
    }

    /** Parsea strings como "+2.50 dB", "-3.10 dB", "2.50" → Float en dB. */
    private fun parseGain(raw: String): Float? {
        return GAIN_REGEX.find(raw)?.groupValues?.getOrNull(1)?.toFloatOrNull()
    }
}
