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
        HistoryEntity::class,
        EqPresetEntity::class,
        SongStatEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao
    abstract fun songStatDao(): SongStatDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        // ── Migraciones explícitas ──
        // Añadir una entrada aquí por cada cambio de esquema antes de incrementar `version`.
        // Ejemplo de migración de v1 → v2:
        //   MIGRATION_1_2 añadió la columna `genre` a songs y las tablas history/playlist_songs.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN genre TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        songId INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
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

        // v2 → v3: tabla de EQ presets guardables por usuario
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS eq_presets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        bandLevels TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // v3 → v4: tabla de estadísticas de reproducción por canción (Smart Engine)
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS song_stats (
                        songId INTEGER PRIMARY KEY NOT NULL,
                        playCount INTEGER NOT NULL DEFAULT 0,
                        skipCount INTEGER NOT NULL DEFAULT 0,
                        completeCount INTEGER NOT NULL DEFAULT 0,
                        totalPlayTimeMs INTEGER NOT NULL DEFAULT 0,
                        lastPlayedAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        // v4 → v5: columna replayGain para normalización de volumen (ReplayGain)
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN replayGain REAL")
            }
        }

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
