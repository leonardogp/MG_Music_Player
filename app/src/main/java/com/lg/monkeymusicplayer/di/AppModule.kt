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
import com.lg.monkeymusicplayer.core.billing.BillingManager
import com.lg.monkeymusicplayer.core.cast.CastManager
import com.lg.monkeymusicplayer.data.repository.CloudSyncRepository
import com.lg.monkeymusicplayer.data.repository.CloudBackend
import com.lg.monkeymusicplayer.data.repository.LocalFileBackend
import com.lg.monkeymusicplayer.core.feature.FeatureGate
import com.lg.monkeymusicplayer.core.queue.QueueManager
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
        statTracker: StatTracker,
        featureGate: com.lg.monkeymusicplayer.core.feature.FeatureGate
    ): MusicPlayerManager {
        return MusicPlayerManager(context, statTracker, featureGate)
    }

    @Provides
    @Singleton
    fun provideExcludedFoldersRepository(@ApplicationContext context: Context): ExcludedFoldersRepository {
        return ExcludedFoldersRepository(context)
    }

    @Provides
    @Singleton
    fun provideCastManager(@ApplicationContext context: Context): CastManager {
        return CastManager(context)
    }

    @Provides
    @Singleton
    fun provideFeatureGate(@ApplicationContext context: Context): FeatureGate {
        return FeatureGate(context)
    }

    @Provides
    @Singleton
    fun provideQueueManager(): QueueManager {
        return QueueManager()
    }

    @Provides
    @Singleton
    fun provideBillingManager(
        @ApplicationContext context: Context,
        featureGate: com.lg.monkeymusicplayer.core.feature.FeatureGate
    ): BillingManager {
        return BillingManager(context, featureGate)
    }

    /**
     * Backend de nube activo. Cambiar [LocalFileBackend] por Firebase/Drive
     * sin tocar [CloudSyncRepository].
     */
    @Provides
    @Singleton
    fun provideCloudBackend(localFileBackend: LocalFileBackend): CloudBackend {
        return localFileBackend
    }
}
