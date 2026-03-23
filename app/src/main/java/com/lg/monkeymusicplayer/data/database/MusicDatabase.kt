package com.lg.monkeymusicplayer.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SongEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        HistoryEntity::class
    ],
    version = 2,
    exportSchema = true  // exportSchema=true para poder auditar el historial de esquemas
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        // ── Migraciones explícitas ──
        // Añadir una entrada aquí por cada cambio de esquema antes de incrementar `version`.
        // Ejemplo de migración de v1 → v2:
        //   MIGRATION_1_2 añadió la columna `genre` a songs y las tablas history/playlist_songs.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Añadir columna genre a songs (valor por defecto vacío para filas existentes)
                db.execSQL("ALTER TABLE songs ADD COLUMN genre TEXT NOT NULL DEFAULT ''")
                // Crear tabla history si no existía en v1
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        songId INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                // Crear tabla playlist_songs si no existía en v1
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS playlist_songs (
                        playlistId INTEGER NOT NULL,
                        songId INTEGER NOT NULL,
                        PRIMARY KEY(playlistId, songId)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    // fallbackToDestructiveMigration solo en DEBUG como safety net para
                    // migraciones no cubiertas durante desarrollo. En release, Room lanzará
                    // una excepción clara en lugar de borrar silenciosamente los datos del usuario.
                    .apply {
                        if (com.lg.monkeymusicplayer.BuildConfig.DEBUG) {
                            fallbackToDestructiveMigration()
                        }
                    }
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
