# Monkey Music Player

Monkey Music Player es un reproductor de música moderno para Android, diseñado con un enfoque en la simplicidad, el rendimiento y una interfaz de usuario atractiva siguiendo las directrices de Material Design 3.

## Visión General

La aplicación permite a los usuarios gestionar y reproducir su biblioteca de música local con una experiencia fluida. Ofrece una navegación intuitiva por categorí­as (canciones, álbumes, artistas, géneros y carpetas), gestión de listas de reproducción y un reproductor a pantalla completa con funciones avanzadas como letras sincronizadas y ecualizador.

## Arquitectura

La aplicación sigue los principios de **Clean Architecture** y los patrones recomendados por Google (**MVVM - Model-View-ViewModel**):

- **UI Layer (Jetpack Compose):** Pantallas declarativas y reactivas que observan el estado del ViewModel.
- **Domain Layer:** Modelos de datos de dominio que representan la lógica de negocio de la aplicación.
- **Data Layer:** 
  - **Room Database:** Persistencia local para canciones, favoritos, historial y listas de reproducción.
  - **MediaStore API:** Fuente de datos para escanear archivos multimedia del dispositivo.
  - **Repositories:** Actúan como mediadores entre las fuentes de datos y la lógica de la aplicación.
- **Core Layer:** Servicios especializados como el motor de reproducción (Media3/ExoPlayer), el escáner de archivos y la integración de letras.
- **Dependency Injection:** Implementada con **Hilt (Dagger)** para una gestión desacoplada de componentes.

## Estructura del Proyecto

```text
MonkeyMusicPlayer/
├── app/
│   ├── src/
│   │├── main/
│   │   │   ├── java/com/lg/monkeymusicplayer/
│   │   │   │   ├── core/                # Lógica central e infraestructura
│   │   │   │   │   ├── player/          # Gestión de Media3 y ExoPlayer
│   │   │   │   │   ├── scanner/         # Escaneo de archivos (MediaStore)
│   │   │   │   │   ├── lyrics/          # Integración de letras (LRCLib)
│   │   │   │   │   └── exception/       # Manejo global de errores
│   │   │   │   ├── data/                # Implementación de datos
│   │   │   │   │   ├── database/        # Room (Entidades, DAOs, DB)
│   │   │   │   │   ├── repository/      # Repositorios (Lógica de datos)
│   │   │   │   │   └── model/           # Modelos de dominio (Song, etc.)
│   │   │   │   ├── di/                  # Inyección de dependencias (Hilt)
│   │   │   │   ├── ui/                  # Capa de presentación (Compose)
│   │   │   │   │   ├── screens/         # Pantallas (Library, Player, Settings)
│   │   │   │   │   ├── components/      # UI reutilizable (Sliders, Controles)
│   │   │   │   │   └── theme/           # Material 3 (Colores, Tipografía)
│   │   │   │   ├── util/                # Helpers (Formateadores, Permisos)
│   │   │   │   └── MonkeyMusicPlayerApp.kt # Clase Application
│   │   │   └── res/                     # Recursos (XML, Layouts, Drawables)
│   └── build.gradle.kts                 # Configuración del módulo app
├── gradle/                              # Archivos del wrapper de Gradle
│   └── libs.versions.toml               # Catálogo de dependencias
├── build.gradle.kts                     # Configuración raíz del proyecto
├── settings.gradle.kts                  # Configuración de módulos
└── README.md                            # Documentación del proyecto
```

## Funcionalidades Implementadas

- **Gestión de Biblioteca:**
  - Escaneo manual de archivos de audio del dispositivo.
  - Organización por Canciones, Artistas, Álbumes, Géneros y Carpetas.
  - Buscador global integrado.
  - Ordenación personalizada (Nombre, Artista, Álbum).
- **Reproducción Avanzada:**
  - Motor basado en **Media3 (ExoPlayer)**.
  - Reproducción en segundo plano y controles en la notificación.
  - Modos de repetición (Una, Todo, Desactivado) y Aleatorio (Shuffle).
  - Temporizador de apagado (Sleep Timer).
  - Ecualizador de sistema integrado.
- **Letras y Metadatos:**
  - Soporte para letras locales (.lrc) y bÃºsqueda online ví­a **LRCLib**.
  - Editor de etiquetas ID3 (Título, Artista, Álbum, Género) con sincronización en MediaStore.
  - Extracción automática de colores de la portada para tematizar la UI.
- **Personalización y Extras:**
  - Gestión de Listas de Reproducción (Crear, Añadir, Eliminar).
  - Sistema de Favoritos e Historial de reproducción reciente.
  - Widgets de escritorio en múltiples tamaños (2x1, 4x1, 4x2, 4x4).
  - Soporte multi-idioma (mÃ¡s de 15 idiomas).

## Próximas Mejoras (Roadmap)

### Fase 1: Experiencia Premium (Core competitivo)
*Objetivo: Igualar o superar apps líderes en experiencia base.*

- [ ] Soporte para reproducción Gapless (sin silencios entre canciones).
- [ ] Implementación de Crossfade configurable entre pistas.
- [ ] Mejoras en el motor de reproducción (Replay Gain básico, manejo de audio focus).
- [ ] Descarga automática de portadas de álbumes faltantes.
- [ ] Edición avanzada de carátulas (embeber imágenes en archivos MP3).
- [ ] Optimización del rendimiento en escaneo y carga de biblioteca.

### Fase 2: Funciones PRO (Monetización)
*Objetivo: Crear valor claro para versión premium.*

- [ ] Ecualizador interno avanzado (5/10 bandas) con presets y perfiles de usuario.
- [ ] Efectos de audio: Bass Boost y Virtualizer.
- [ ] Temas avanzados: Modo AMOLED y personalización de colores manual.
- [ ] Reproducción avanzada por carpetas (exclusión de directorios, selección granular).

### Fase 3: Diferenciación (Nivel TOP)
*Objetivo: Destacar frente a la competencia.*

- [ ] Smart Playlists automáticas (MáS reproducidas, Recientes, Descubiertas).
- [ ] Sistema de múltiples colas de reproducción.
- [ ] Mejoras en letras: sincronización precisa en tiempo real y caché offline.
- [ ] Gestos avanzados y microinteracciones premium.

### Fase 4: Ecosistema e Integraciones
*Objetivo: Competir con apps completas del mercado.*

- [ ] Soporte para Android Auto.
- [ ] Soporte para Google Cast (Chromecast).
- [ ] Integración con dispositivos externos (controles Bluetooth avanzados).
- [ ] Widgets interactivos mejorados.

### Fase 5: Infraestructura y Valor a Largo Plazo
*Objetivo: Retención y fidelización.*

- [ ] Sistema de backup y restauración (Playlists, Favoritos, Configuraciones).
- [ ] Sincronización opcional en la nube.
- [ ] Sistema de feedback integrado dentro de la app. Telemetría básica (respetando privacidad): uso de features, retención

## Librerías Principales

- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose), [Material3](https://developer.android.com/jetpack/androidx/releases/compose-material3).
- **Imagen:** [Coil](https://coil-kt.github.io/coil/) (Carga de portadas y procesamiento de imágenes).
- **Navegación:** [Compose Navigation](https://developer.android.com/jetpack/compose/navigation).
- **Inyección de Dependencias:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android).
- **Persistencia:** [Room](https://developer.android.com/training/data-storage/room).
- **Reproducción:** [Media3 / ExoPlayer](https://developer.android.com/guide/topics/media/media3).
- **Red (Letras):** [Ktor](https://ktor.io/) (Cliente HTTP) y [Kotlinx Serialization](https://kotlinlang.org/docs/serialization.html).
- **Utilidades:** [Palette](https://developer.android.com/training/material/palette-colors), [mp3agic](https://github.com/mpatric/mp3agic), [Timber](https://github.com/JakeWharton/timber).

---
Desarrollado por Leonardo Granados para amantes de la música.
