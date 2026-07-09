# Monkey Music Player

## Arquitectura del Proyecto

Última actualización: Julio 2026

---

# Objetivo

Monkey Music Player es un reproductor musical para Android desarrollado con Kotlin + Jetpack Compose cuyo objetivo es ofrecer una experiencia visual moderna, reactiva y altamente personalizable, inspirada en aplicaciones como Spotify, PowerAmp y Oto Music, pero con una identidad propia.

El proyecto sigue una arquitectura MVVM y un desarrollo iterativo basado en sprints.

---

# Stack tecnológico

- Kotlin
- Jetpack Compose
- Material 3
- Media3
- Room
- Navigation Compose
- Coroutines
- Coil

Arquitectura:

MVVM

---

# Organización del proyecto

```
ui
│
├── components
│   ├── core
│   ├── dialogs
│   ├── home
│   └── library
│
├── screens
│   └── library
│
├── theme
│   └── dynamic
│
└── player (Sprint 2)
```

---

# Components

## core

Componentes reutilizables.

Ejemplos:

- MonkeyButton
- MonkeyPlayerBottomBar
- MonkeySearchBar
- PlaybackWaveform

---

## dialogs

Todos los diálogos reutilizables.

- SleepTimerDialog
- LanguageDialog
- EditTagsDialog
- CreatePlaylistDialog
- PlaylistPickerDialog
- SongMenuSheet

---

## home

Componentes exclusivos de Home.

- HomeContent
- HomeSection
- SmartPlaylistCardSmall

---

## library

Componentes exclusivos de Library.

- SongList
- SongItem
- PlaylistGrid
- PlaylistCard
- SmartPlaylistCard
- GenreList
- ArtistList
- AlbumGrid
- FolderList
- EmptyLibraryState

---

## screens

Pantallas completas.

Actualmente:

- ExcludedFoldersScreen
- SongListDetailScreen

---

# LibraryScreen

Debe contener únicamente:

- LibraryScreen()
- LibraryMainContent()
- LibraryTopBar()
- FullPlayerScreen()

No agregar nuevos componentes reutilizables aquí.

---

# Player

En Sprint 2 se moverá a:

```
ui/player
```

Estructura planeada:

```
player

FullPlayerScreen.kt

MiniPlayer.kt

PlayerControls.kt

LyricsPage.kt

QueuePage.kt

PlaylistPage.kt
```

---

# Dynamic Theme

No se utilizará Android Palette.

Monkey implementará un motor propio de extracción de colores.

Ubicación:

```
ui/theme/dynamic
```

Archivos:

- DynamicPlayerColors.kt
- DynamicPlayerTheme.kt
- DynamicColorEngine.kt
- BitmapColorEngine.kt
- BitmapAnalyzer.kt
- ColorPalette.kt

---

# DynamicPlayerColors

Modelo oficial:

```kotlin
data class DynamicPlayerColors(

    val dominant: Color,

    val vibrant: Color,

    val muted: Color,

    val dark: Color,

    val light: Color,

    val accent: Color,

    val background: Color,

    val backgroundDark: Color,

    val textPrimary: Color,

    val textSecondary: Color,

    val waveform: Color,

    val controls: Color
)
```

---

# Principios de desarrollo

Siempre trabajar:

- Paso a paso.
- Cambios pequeños.
- Proyecto compilando en cada paso.
- Sin refactors masivos.
- No romper funcionalidades existentes.

---

# Convenciones

No usar colores fijos dentro de la UI.

Incorrecto:

```
PrimaryOrange
```

Correcto:

```
LocalDynamicPlayerColors.current.accent
```

---

# Objetivo visual

La aplicación debe sentirse viva.

Toda la interfaz reaccionará a:

- portada del álbum
- reproducción
- gestos
- scroll
- animaciones
- colores dinámicos