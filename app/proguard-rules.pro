# ── Reglas ProGuard / R8 para Monkey Music Player ──

# ── AndroidX / Jetpack ──
-keep class androidx.** { *; }
-dontwarn androidx.**

# ── Media3 / ExoPlayer ──
# Media3 incluye sus propias consumer proguard rules en el AAR,
# pero se añaden explícitamente por claridad y por si se usan APIs @UnstableApi.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Room ──
# Room genera código en tiempo de compilación con KSP; los DAOs y Entities deben conservarse.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** getDatabase(...);
}

# ── Kotlin Coroutines ──
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ── Coil ──
-keep class coil.** { *; }
-dontwarn coil.**

# ── mp3agic (edición de tags ID3) ──
# mp3agic usa reflexión para los tags — conservar todas sus clases.
-keep class com.mpatric.mp3agic.** { *; }
-dontwarn com.mpatric.mp3agic.**

# ── Timber ──
-dontwarn org.slf4j.**
-keep class timber.log.** { *; }

# ── Guava (usada por Media3 internamente) ──
-dontwarn com.google.common.**
-keep class com.google.common.util.concurrent.** { *; }

# ── Modelos de datos del dominio ──
# Song, SongEntity, etc. se serializan/deserializan en Room y pueden accederse
# por reflexión; conservar sus miembros.
-keep class com.lg.monkeymusicplayer.data.model.** { *; }
-keep class com.lg.monkeymusicplayer.data.database.** { *; }

# ── Firebase Crashlytics ──
# Crashlytics necesita los stack traces sin ofuscar para mostrarlos correctamente.
# Las reglas de desofuscación (mapping.txt) se suben automáticamente al build si
# el plugin de Crashlytics está activo — estas reglas protegen la integración en runtime.
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-keep class com.crashlytics.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.crashlytics.**
# Mantener los nombres de las clases de excepción propias para legibilidad en el dashboard
-keep class com.lg.monkeymusicplayer.core.exception.** { *; }

# ── Preservar stack traces en crash reports (obligatorio con Crashlytics) ──
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
