package com.lg.monkeymusicplayer.di

import android.content.Context
import com.lg.monkeymusicplayer.core.player.MusicPlayerManager
import com.lg.monkeymusicplayer.data.database.MusicDao
import com.lg.monkeymusicplayer.data.database.MusicDatabase
import com.lg.monkeymusicplayer.data.repository.ExcludedFoldersRepository
import com.lg.monkeymusicplayer.data.repository.MusicRepository
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
    fun provideMusicRepository(
        @ApplicationContext context: Context,
        musicDao: MusicDao,
        excludedFoldersRepository: ExcludedFoldersRepository
    ): MusicRepository {
        return MusicRepository(context, musicDao, excludedFoldersRepository)
    }

    @Provides
    @Singleton
    fun provideMusicPlayerManager(@ApplicationContext context: Context): MusicPlayerManager {
        return MusicPlayerManager(context)
    }

    @Provides
    @Singleton
    fun provideExcludedFoldersRepository(@ApplicationContext context: Context): ExcludedFoldersRepository {
        return ExcludedFoldersRepository(context)
    }
}
