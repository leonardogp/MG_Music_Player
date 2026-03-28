package com.lg.monkeymusicplayer.core.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Cliente para la API pública de LRCLib (https://lrclib.net).
 * No requiere API key. Devuelve letras sincronizadas en formato LRC cuando están disponibles,
 * o letras planas como fallback.
 *
 * Endpoint: GET https://lrclib.net/api/get
 *   ?track_name=<title>
 *   &artist_name=<artist>
 *
 * La respuesta incluye `syncedLyrics` (formato LRC con timestamps) y `plainLyrics`.
 * Priorizamos siempre `syncedLyrics`.
 */
object LrcLibService {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 8_000
            connectTimeoutMillis = 5_000
        }
    }

    @Serializable
    private data class LrcLibResponse(
        @SerialName("syncedLyrics") val syncedLyrics: String? = null,
        @SerialName("plainLyrics") val plainLyrics: String? = null
    )

    /**
     * Busca letras para [title] y [artist].
     *
     * @return El contenido LRC sincronizado si existe, las letras planas si no,
     *         o null si no se encontraron letras.
     */
    suspend fun fetchLyrics(title: String, artist: String): String? {
        return try {
            val response = client.get("https://lrclib.net/api/get") {
                // Limpiar el título de sufijos comunes que reducen la tasa de aciertos
                // Ej: "Song (feat. X)" → "Song", "Song - Remastered" → "Song"
                val cleanTitle = title
                    .replace(Regex("\\s*\\(feat\\..*?\\)", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\s*-\\s*(remastered|live|remix|radio edit).*", RegexOption.IGNORE_CASE), "")
                    .trim()

                parameter("track_name", cleanTitle)
                parameter("artist_name", artist)
            }

            if (!response.status.isSuccess()) {
                Timber.d("LRCLib: no lyrics for '$title' by '$artist' (HTTP ${response.status.value})")
                return null
            }

            val body = response.body<LrcLibResponse>()

            // Preferir letras sincronizadas; caer a planas si no hay timestamps
            val lrcContent = body.syncedLyrics?.takeIf { it.isNotBlank() }
                ?: body.plainLyrics?.takeIf { it.isNotBlank() }?.let { plain ->
                    // Convertir letras planas a formato LRC básico (sin timestamps)
                    // para reutilizar el parseLrc existente con [00:00.00] como placeholder
                    plain.lines().joinToString("\n") { "[00:00.00]$it" }
                }

            lrcContent
        } catch (e: Exception) {
            Timber.w(e, "LRCLib fetch failed for '$title' by '$artist'")
            null
        }
    }
}
