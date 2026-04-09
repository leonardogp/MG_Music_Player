package com.lg.monkeymusicplayer.di

import android.content.Context
import com.lg.monkeymusicplayer.core.player.MusicPlayerManager
import com.lg.monkeymusicplayer.core.tracker.StatTracker
import com.lg.monkeymusicplayer.data.database.MusicDao
import com.lg.monkeymusicplayer.data.database.SongStatDao
import com.lg.monkeymusicplayer.data.database.MusicDatabase
import com.lg.monkeymusicplayer.data.repository.ExcludedFoldersRepository
import com.lg.monkeymusicplayer.core.smart.SmartEngine
import com.lg.monkeymusicplayer.data.repository.BackupRepository
import com.lg.monkeymusicplayer.data.repository.MusicRepository
import com.lg.monkeymusicplayer.data.repository.SmartRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMusicDatabase(@ApplicationContext context: Context): MusicDatabase {
        return MusicDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideMusicDao(database: MusicDatabase): MusicDao {
        return database.musicDao()
    }

    @Provides
    @Singleton
    fun provideSongStatDao(database: MusicDatabase): SongStatDao {
        return database.songStatDao()
    }

    @Provides
    @Singleton
    fun provideStatTracker(dao: SongStatDao): StatTracker {
        return StatTracker(dao)
    }

    @Provides
    @Singleton
    fun provideMusicRepository(
        @ApplicationContext context: Context,
        musicDao: MusicDao,
        excludedFoldersRepository: ExcludedFoldersRepository
    ): MusicRepository {
        return MusicRepository(context, musicDao, excludedFoldersRepository)
    }

    @Provides
    @Singleton
    fun provideMusicPlayerManager(
        @ApplicationContext context: Context,
        statTracker: StatTracker
    ): MusicPlayerManager {
        return MusicPlayerManager(context, statTracker)
    }

    @Provides
    @Singleton
    fun provideExcludedFoldersRepository(@ApplicationContext context: Context): ExcludedFoldersRepository {
        return ExcludedFoldersRepository(context)
    }
}
