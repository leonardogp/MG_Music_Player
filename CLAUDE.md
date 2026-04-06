# CLAUDE.md — Monkey Music Player

Rama activa: `Developer` · Repositorio: `leonardogp/Monkey_Music_Player`

---

## Datos generales

| | |
|---|---|
| Lenguaje | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material3 (BOM 2024.11.00) |
| Reproducción | Media3 / ExoPlayer 1.5.0 |
| Base de datos | Room 2.6.1 |
| DI | Hilt 2.52 |
| Imágenes | Coil 2.7.0 |
| Tags ID3 | mp3agic 0.9.1 |
| HTTP / Letras | Ktor 3.0.1 |
| Logging | Timber 5.0.1 |
| SDK | minSdk 24 · targetSdk 35 · compileSdk 35 |
| Java | VERSION_11 |
| Build | AGP 8.13.2 · KSP 2.0.21-1.0.28 |

---

## Arquitectura

Patrón MVVM estricto. Una sola fuente de verdad por capa.

```
UI (Compose)
  └── MusicViewModel  (HiltViewModel)
        ├── MusicRepository          (datos + IO)
        ├── MusicPlayerManager       (MediaController)
        └── ExcludedFoldersRepository
```

- La UI solo lee `StateFlow` y llama funciones del ViewModel; nunca accede al repositorio directamente.
- `MusicRepository` maneja Room, MediaStore, mp3agic y LRCLib. Todo IO en `withContext(Dispatchers.IO)`.
- `MusicPlayerManager` envuelve `MediaController` (Media3) y gestiona la conexión al `MusicService`.
- `MusicService` extiende `MediaSessionService`. Contiene `ExoPlayer`, el ecualizador de hardware y el widget.

---

## Estilo de codificación

- **Coroutines:** IO en `Dispatchers.IO`, actualizaciones de UI en `Dispatchers.Main`. Jobs cancelables para operaciones que pueden llegar desordenadas.
- **Lookups de lista:** `associateBy` Maps para O(1) en lugar de `find` O(N²).
- **Logging:** Timber en lugar de `Log.*` en todo el proyecto.
- **Room:** migraciones siempre explícitas con `addMigrations(...)`. Nunca `fallbackToDestructiveMigration` en release.
- **Compose:** cualquier función que use APIs experimentales de Material3 requiere `@OptIn(ExperimentalMaterial3Api::class)`, incluso si el uso está en un lambda interno.
- **Kotlin:** data classes tipadas sobre `Array<Any?>`, referencias nombradas para listeners para evitar memory leaks.

---

## Convenciones del proyecto

- Los outputs de Claude son ZIPs con solo los archivos modificados, salvo solicitud explícita del paquete completo.
- `exportSchema = true` en Room → JSONs en `app/schemas/` deben commitearse al repo.
- El código muerto se elimina con `git rm`, no se deja comentado ni sin usar.
- Las respuestas de Claude durante implementación activa son técnicas y concisas; sin explicaciones extensas salvo solicitud.
- Al iniciar una sesión con cambios en el código, Leo sube un ZIP actualizado como nueva línea base. Los outputs de sesiones anteriores no se asumen vigentes si hay un ZIP nuevo.
- Los bugs se verifican en múltiples iteraciones antes de cerrarse.
