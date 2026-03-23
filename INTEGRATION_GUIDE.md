# Integration Guide — Monkey Music Player

## Requisitos

- Android Studio Hedgehog (2023.1.1) o superior
- JDK 11
- Android SDK 35 (compileSdk) / minSdk 24

## Ramas

| Rama        | Propósito                              |
|-------------|----------------------------------------|
| `main`      | Versión estable                        |
| `Developer` | Desarrollo activo, puede ser inestable |

## Setup local

```bash
git clone <repo-url>
git checkout Developer
```

Abrir el proyecto en Android Studio y sincronizar Gradle. No hay dependencias externas de npm.

## Ejecutar tests unitarios

```bash
./gradlew test
```

## Ejecutar tests instrumentados (emulador/dispositivo)

```bash
./gradlew connectedAndroidTest
```

## Build release

```bash
./gradlew assembleRelease
```

El APK firmado se genera en `app/build/outputs/apk/release/`.

> **Nota**: la build de release tiene R8/minificación activa (`isMinifyEnabled = true`).
> Las reglas ProGuard están en `app/proguard-rules.pro`.

## Room — Migraciones

El esquema de la base de datos se exporta automáticamente a `app/schemas/` en cada compilación.
Commitear esos JSONs al repo permite auditar cambios de esquema y escribir migraciones verificables.

Al cambiar el esquema:
1. Incrementar `version` en `MusicDatabase`.
2. Añadir un objeto `Migration(oldVersion, newVersion)` en `MusicDatabase.Companion`.
3. Registrarlo con `.addMigrations(...)` en `getDatabase()`.

En DEBUG, `fallbackToDestructiveMigration()` actúa como safety net. En release, Room lanzará
una excepción clara si falta una migración.

## Permisos

| Permiso                    | Cuándo se solicita                                |
|----------------------------|---------------------------------------------------|
| `READ_MEDIA_AUDIO` (API 33+) | Al arrancar, para escanear la biblioteca        |
| `READ_EXTERNAL_STORAGE`    | Al arrancar (API < 33)                            |
| `MANAGE_EXTERNAL_STORAGE`  | Solo al intentar editar tags ID3 (API 30+)        |
| `POST_NOTIFICATIONS`       | Al arrancar (API 33+), para la MediaSession       |
