ROADMAP MONKEY PLAYER APP

1. REFACTOR BASE (SPRINT 0)
Objetivo

Preparar la app para escalar sin romperse.

1.1 Modularización
Acción:

Separar el proyecto en módulos Gradle:
:core
:domain
:data
:player
:feature-library
:feature-nowplaying
:feature-smart
:feature-stats
:feature-settings

Reglas:
* domain → modelos + use cases
* data → repositorios + sources
* player → ExoPlayer wrapper
* feature-* → UI + ViewModel

1.2 Estandarizar estados UI
Crear:
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

Aplicar en:
* Library
* NowPlaying
* Playlists

1.3 Use Cases (obligatorio)

Ejemplo:
class GetSongsUseCase(private val repo: MusicRepository) {
    suspend operator fun invoke(): List<Song>
}

1.4 Tracking base de eventos (CRÍTICO)

Crear tabla Room:
(Ejemplo)
@Entity
data class SongStat(
    val songId: Long,
    val playCount: Int,
    val skipCount: Int,
    val lastPlayed: Long,
    val totalPlayTime: Long
)

2. SMART ENGINE (IA LOCAL) — CORE DEL PRODUCTO
Objetivo

Diferenciación real.

2.1 Motor de scoring
Implementar:
fun calculateScore(stat: SongStat): Double {
    val recency = (System.currentTimeMillis() - stat.lastPlayed) / TIME_DECAY
    return (stat.playCount * 2) - stat.skipCount + (1 / (1 + recency))
}

2.2 Smart Playlists
Crear UseCases:
* GetDailyMixUseCase
* GetRediscoverUseCase
* GetTopSongsUseCase

Lógica:
🔥 Daily Mix
 * top score
 * mezcla artistas
🔁 Rediscover
 * alto score histórico
 * bajo “recency”
💔 Forgotten
 * bajo uso reciente

2.3 Eventos a capturar

En el player:

 * play
 * pause
 * skip
 * complete (>80%)
 
2.4 Scheduler local
 * recalcular playlists cada 24h
 * usar WorkManager

3. EXPERIENCIA VISUAL (WOW FACTOR)
Objetivo

Que el usuario diga “esta app se ve diferente”.

3.1 Visualizador de audio
Implementación:
 * usar AudioProcessor de Media3 o FFT
 * renderizar con Compose Canvas
 
Tipos:
 * barras (FFT)
 * onda (waveform)
 * partículas (opcional v2)
 
3.2 UI reactiva al audio
Crear:
data class AudioFrame(
    val amplitude: Float,
    val frequencies: List<Float>
)
UI se actualiza con Flow.

3.3 Dynamic Theming
Usar:
Palette API o Material You
Aplicar:
 * fondo NowPlaying
 * controles
 * visualizador
 
3.4 Animaciones clave
 * transición de canciones
 * carátula con escala/blur
 * mini-player expandible
 
4. AUDIO PRO FEATURES
Objetivo:
Competir con app de alto nivel

4.1 Crossfade
Implementar:
 * 2 instancias de ExoPlayer
 * fade in/out con volumen
 
4.2 Gapless playback
 * usar Media3 correctamente
 * evitar recarga de source
 
4.3 ReplayGain (opcional)

5. ENGAGEMENT & RETENCIÓN

5.1 Estadísticas usuario
Crear pantalla:
 * top canciones
 * top artistas
 * minutos escuchados
 
5.2 Historial
 * últimas canciones reproducidas
 
5.3 Widgets (mejoras)

6. BACKUP / PORTABILIDAD
6.1 Export
(Ejemplo):
{
  "playlists": [],
  "favorites": [],
  "stats": []
}

7. MONETIZACIÓN READY
7.1 Feature gating

Crear:
(Ejemplo):
enum class Feature {
    VISUALIZER,
    CROSSFADE,
    STATS,
    SMART_PLAYLISTS
}

7.2 Paywall (Implementar solo cuando se hayan completado los pasos anteriores)
 * bloquear features premium
 * usar Google Billing
 
