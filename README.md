# Monkey Music Player

Monkey Music Player es un reproductor de música moderno para Android, diseñado con un enfoque en la simplicidad, el rendimiento y una interfaz de usuario atractiva siguiendo las directrices de Material Design 3.

## Visión General

La aplicación permite a los usuarios gestionar y reproducir su biblioteca de música local con una experiencia fluida. Ofrece una navegación intuitiva por categorías (canciones, álbumes, artistas, géneros y carpetas), gestión de listas de reproducción, un reproductor a pantalla completa con funciones avanzadas como letras sincronizadas, ecualizador, visualizador de audio y Smart Playlists generadas por un motor de scoring local.

## Arquitectura

La aplicación sigue los principios de **Clean Architecture** y los patrones recomendados por Google (**MVVM - Model-View-ViewModel**):

- **UI Layer (Jetpack Compose):** Pantallas declarativas y reactivas que observan el estado del ViewModel a través de `StateFlow<UiState<T>>`.
- **Domain Layer:** Use Cases que encapsulan la lógica de negocio y actúan como única interfaz entre la UI y los repositorios.
- **Data Layer:**
  - **Room Database:** Persistencia local para canciones, favoritos, historial, listas de reproducción, estadísticas de reproducción y presets de EQ.
  - **MediaStore API:** Fuente de datos para escanear archivos multimedia del dispositivo.
  - **Repositories:** Mediadores entre las fuentes de datos y los Use Cases.
- **Core Layer:** Servicios especializados — motor de reproducción (Media3/ExoPlayer), SmartEngine (scoring local), StatTracker (eventos de reproducción), workers de background (WorkManager).
- **Dependency Injection:** Implementada con **Hilt (Dagger)** incluyendo integración con WorkManager vía `HiltWorkerFactory`.

## Estructura del Proyecto

```text
MonkeyMusicPlayer/
├── app/
│   └── src/main/java/com/lg/monkeymusicplayer/
│       ├── core/
│       │   ├── player/          # Media3/ExoPlayer (crossfade, gapless, EQ)
│       │   ├── scanner/         # Escaneo MediaStore
│       │   ├── smart/           # SmartEngine (scoring + smart playlists)
│       │   ├── tracker/         # StatTracker (eventos play/skip/complete)
│       │   ├── worker/          # WorkManager workers (SmartPlaylistWorker)
│       │   ├── lyrics/          # LRCLib integration
│       │   └── exception/       # GlobalExceptionHandler
│       ├── data/
│       │   ├── database/        # Room (SongEntity, SongStatEntity, DAOs)
│       │   ├── repository/      # MusicRepository, SmartRepository, StatsRepository, BackupRepository
│       │   └── model/           # Modelos de dominio
│       ├── domain/
│       │   └── usecase/         # GetSongsUseCase, GetSmartPlaylistsUseCase, GetUserStatsUseCase,
│       │                        # RefreshMusicLibraryUseCase, ToggleFavoriteUseCase
│       ├── di/                  # Hilt modules
│       ├── ui/
│       │   ├── screens/         # Library, Player, Stats, Backup, Equalizer
│       │   ├── components/      # AudioVisualizer, PlayerControls, LyricsView, MediaProgressSlider
│       │   ├── widget/          # Widgets 2x1, 2x2, 4x1, 4x2, 4x4
│       │   └── theme/           # Material 3 + PrimaryOrange accent
│       └── util/
└── app/src/main/res/
    └── values-{locale}/         # 17 idiomas, todos sincronizados al 100% (169 strings)
```

## Funcionalidades Implementadas

### Reproducción y Audio
- Motor basado en **Media3 (ExoPlayer)** con reproducción en background y notificación.
- **Gapless playback** — pre-carga del siguiente MediaItem para eliminar silencios entre pistas.
- **Crossfade configurable** entre pistas mediante fade de volumen por pasos.
- Modos de repetición (Una, Todo, Desactivado) y Shuffle.
- Temporizador de apagado (Sleep Timer).
- Ecualizador de sistema con presets guardables por el usuario.
- **Visualizador de audio** — FFT en tiempo real renderizado con Compose Canvas + glow animado.

### Biblioteca y Metadatos
- Escaneo de archivos de audio del dispositivo con exclusión de carpetas configurable.
- Organización por Canciones, Artistas, Álbumes, Géneros y Carpetas.
- Buscador global y ordenación personalizada (Nombre, Artista, Álbum, Fecha).
- Editor de etiquetas ID3 (Título, Artista, Álbum, Género) sincronizado con MediaStore.
- Soporte para letras locales (.lrc) y búsqueda online vía **LRCLib** con caché.

### Smart Playlists (IA Local)
- **SmartEngine** con fórmula de scoring inspirada en Hacker News + Spotify decay:
  ```
  score = playCount×2 + completeCount×3 - skipCount×1.5 + completionRate - ln(1 + days/14)
  ```
- **Daily Mix** — Top picks ponderados por recencia.
- **Rediscover** — Alto score histórico + sin reproducción reciente (>21 días).
- **Top Songs** — Ranking global de todos los tiempos.
- Recálculo reactivo ante cada evento de reproducción (Flow de Room).
- **Scheduler periódico** (WorkManager, cada 24h) para mantener scores actualizados en background.

### Estadísticas
- Tracking de eventos: play, skip, complete (≥80%), tiempo acumulado por canción.
- Pantalla de estadísticas: tiempo total escuchado, top canciones, top artistas, tasa de completado.
- Historial de reproducción reciente.

### Personalización y Extras
- Gestión de Playlists (Crear, Añadir, Eliminar).
- Sistema de Favoritos.
- Widgets de escritorio en múltiples tamaños (2x1, 2x2, 4x1, 4x2, 4x4).
- **Dynamic Theming** — extracción de color dominante de portada vía Palette API + Material You (Android 12+).
- **Backup & Restore** — Export/import de favoritos, playlists, presets EQ y estadísticas a `.monkeybackup`.
- **Multi-idioma** — 17 idiomas al 100%: ar, de, es, fa, fr, hi, in, it, ja, ko, pt, ru, sv, tr, uk, zh-rCN, zh-rTW.

## Estado del Roadmap

### ✅ Completado

| Área | Detalle |
|---|---|
| Arquitectura base | MVVM + Clean Architecture + `UiState<T>` unificado |
| Use Cases | `GetSongs`, `GetSmartPlaylists`, `GetUserStats`, `RefreshMusicLibrary`, `ToggleFavorite`, `PlaySong`, `UpdateSongTags` — ViewModel migrado |
| Tracking de reproducción | `StatTracker` + `SongStatEntity` + DAO con operaciones atómicas |
| Smart Engine | Scoring + 3 Smart Playlists reactivas |
| Smart Scheduler | `SmartPlaylistWorker` (WorkManager, 24h, HiltWorkerFactory) |
| Visualizador de audio | FFT real-time, Compose Canvas, glow animado |
| Crossfade | Configurable, fade por steps en `MusicService` |
| Gapless playback | Pre-carga de siguiente MediaItem en ExoPlayer |
| ReplayGain | Lectura tag TXXX vía mp3agic · Room v5 con columna `replayGain` · Ajuste de volumen lineal en `MusicPlayerManager` |
| Feature gating | `Feature` enum + `FeatureGate` singleton (SharedPrefs backend, listo para Google Billing) |
| Dynamic Theming | Palette API + Material You |
| Backup & Restore | Export/import JSON (`.monkeybackup`) |
| Estadísticas | Pantalla completa con top songs, top artists, tiempo total |
| Android Auto | Árbol de navegación localizado (Songs/Favorites/Playlists via `getString`) |
| Localización | 17 idiomas — 169/169 strings cada uno |
| Bug: genre duplication | Fix `ID3v24Tag()` fresco + `IGNORE` + `updateSongMetadata` |
| Bug: widget sync | `widgetUpdateJob` en `MusicService` |

### ✅ Completado (actualización)

| Área | Detalle |
|---|---|
| Múltiples colas | `QueueManager` singleton — N colas nombradas, switch activo, reorden drag&drop |
| Chromecast | `CastManager` + `CastOptionsProvider` + Cast SDK (Default Media Receiver) + integración Manifest |
| Sincronización en nube | `CloudSyncRepository` + `CloudSyncWorker` (WorkManager, 12h, requires network) |

### ✅ Completado (actualización final)

| Área | Detalle |
|---|---|
| UI de múltiples colas | `QueuesScreen` — crear/cambiar/eliminar colas, badge activo, switch automático al player |
| Cast integrado en UI | Botón Cast en TopAppBar (visible solo con dispositivos) · `CastManager.initialize/release` en `MainActivity` |
| Cloud Sync UI | `CloudSyncScreen` — upload/download manual + nota de auto-sync cada 12h |
| ViewModel completo | `castState`, `castCurrentSong`, `castActiveQueue`, `cloudSyncUpload`, `cloudSyncDownload` |

### ✅ Completado (sesión final)

| Área | Detalle |
|---|---|
| Localización completa | 17 locales × 189 strings — 100% sincronizados |
| Paywall / Google Billing | `BillingManager` + `BillingState` + `PaywallScreen` con precio real de Play Console |
| PRO CTA en Settings | Ítem destacado visible solo para usuarios no-PRO |
| Billing en lifecycle | `connect()` en `onCreate`, `disconnect()` en `onDestroy` de MainActivity |

### ✅ Completado (actualización final)

| Área | Detalle |
|---|---|
| Localización completa | 17 idiomas — 204/204 strings (incluye PRO, Cast, Cloud, Queues) |
| Backend de nube | `CloudBackend` interfaz + `LocalFileBackend` activo + `CloudSyncRepository` refactorizado |
| Paywall | `BillingManager` + `PaywallScreen` + `BillingState` — completo |

### 🔲 Pendiente (no implementable sin cuentas externas)

| Área | Detalle |
|---|---|
| Modularización Gradle | Separar en `:core`, `:domain`, `:data`, `:feature-*` |
| Registro SKU Play Console | Crear producto `monkey_music_pro_lifetime` en Google Play Console |
| Backend de nube real | Implementar `CloudBackend` con Firebase Storage o Google Drive REST API |

## Librerías Principales

- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose), [Material3](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- **Imagen:** [Coil](https://coil-kt.github.io/coil/)
- **Navegación:** [Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- **Inyección de Dependencias:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Persistencia:** [Room](https://developer.android.com/training/data-storage/room)
- **Reproducción:** [Media3 / ExoPlayer](https://developer.android.com/guide/topics/media/media3)
- **Background:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) + Hilt Work
- **Red (Letras):** [Ktor](https://ktor.io/) + [Kotlinx Serialization](https://kotlinlang.org/docs/serialization.html)
- **Utilidades:** [Palette](https://developer.android.com/training/material/palette-colors), [mp3agic](https://github.com/mpatric/mp3agic), [Timber](https://github.com/JakeWharton/timber), [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics)

---
Desarrollado por Leonardo Granados para amantes de la música.
