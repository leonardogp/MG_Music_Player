package com.lg.monkeymusicplayer.core.player

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaMetadata
import com.google.common.collect.ImmutableList
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.lg.monkeymusicplayer.data.database.MusicDao
import com.lg.monkeymusicplayer.data.database.SongEntity
import com.lg.monkeymusicplayer.data.model.Song
import com.lg.monkeymusicplayer.ui.MainActivity
import com.lg.monkeymusicplayer.ui.widget.MusicWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MusicService : MediaLibraryService() {

    @Inject
    lateinit var musicDao: MusicDao

    private var mediaSession: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer
    private var equalizer: Equalizer? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var crossfadeDurationMs = 5000L
    private var isFading = false
    private var widgetUpdateJob: Job? = null

    private val crossfadeCheckRunnable = object : Runnable {
        override fun run() {
            if (player.isPlaying && !isFading && crossfadeDurationMs > 0) {
                val remaining = player.duration - player.currentPosition
                if (remaining in 1..crossfadeDurationMs) {
                    performFadeOut()
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    companion object {
        const val COMMAND_GET_AUDIO_SESSION_ID = "COMMAND_GET_AUDIO_SESSION_ID"
        const val COMMAND_SET_CROSSFADE_DURATION = "COMMAND_SET_CROSSFADE_DURATION"
        const val COMMAND_SET_EQUALIZER_BAND = "COMMAND_SET_EQUALIZER_BAND"
        const val COMMAND_GET_EQUALIZER_DATA = "COMMAND_GET_EQUALIZER_DATA"

        const val ACTION_WIDGET_PLAY_PAUSE = "com.lg.monkeymusicplayer.ACTION_WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.lg.monkeymusicplayer.ACTION_WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "com.lg.monkeymusicplayer.ACTION_WIDGET_PREV"
        const val ACTION_WIDGET_UPDATE_REQUEST = "com.lg.monkeymusicplayer.ACTION_WIDGET_UPDATE_REQUEST"
        const val ACTION_WIDGET_FAVORITE = "com.lg.monkeymusicplayer.ACTION_WIDGET_FAVORITE"
        const val ACTION_WIDGET_SHUFFLE  = "com.lg.monkeymusicplayer.ACTION_WIDGET_SHUFFLE"
        const val ACTION_WIDGET_REPEAT   = "com.lg.monkeymusicplayer.ACTION_WIDGET_REPEAT"

        // IDs de los nodos raíz del árbol
        const val ROOT_ID       = "ROOT"
        const val SONGS_ID      = "SONGS"
        const val FAVORITES_ID  = "FAVORITES"
        const val PLAYLISTS_ID  = "PLAYLISTS"
        const val PLAYLIST_PREFIX = "playlist_"
    }

    override fun onCreate() {
        super.onCreate()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(32 * 1024, 64 * 1024, 1024, 1024)
            .setBackBuffer(10 * 1024, true)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateWidget()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateWidget()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    performFadeIn()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && equalizer == null) {
                    setupEqualizer()
                }
                updateWidget()
            }
        })

        handler.post(crossfadeCheckRunnable)

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaLibrarySession.Builder(this, player, AutoMediaLibraryCallback())
            .setSessionActivity(pendingIntent)
            .build()

        restoreLastSession()
    }

    private fun restoreLastSession() {
        serviceScope.launch {
            val history = withContext(Dispatchers.IO) {
                musicDao.getHistory().firstOrNull() ?: emptyList()
            }
            if (history.isEmpty()) return@launch

            val songIds = history.map { it.songId }
            val songEntities = withContext(Dispatchers.IO) {
                musicDao.getSongsByIds(songIds)
            }
            val songs = songIds.mapNotNull { id ->
                songEntities.find { it.id == id }?.toDomainModel()
            }
            if (songs.isEmpty()) return@launch

            if (player.mediaItemCount == 0) {
                val mediaItems = songs.map { it.toMediaItem() }
                player.setMediaItems(mediaItems)
                player.prepare()
            }
        }
    }

    private fun updateWidget() {
        val song = player.currentMediaItem?.localConfiguration?.tag as? Song

        widgetUpdateJob?.cancel()
        widgetUpdateJob = serviceScope.launch {
            if (song != null) {
                val isFav = withContext(Dispatchers.IO) {
                    musicDao.getFavorites().firstOrNull()?.contains(song.id) ?: false
                }

                val stillCurrent = player.currentMediaItem?.localConfiguration?.tag as? Song
                if (stillCurrent?.id != song.id) return@launch

                MusicWidget.updateAllWidgets(
                    context        = this@MusicService,
                    songTitle      = song.title,
                    artistName     = song.artist,
                    albumName      = song.album,
                    isPlaying      = player.isPlaying,
                    albumArtUri    = song.albumArtUri,
                    progressMs     = player.currentPosition,
                    durationMs     = player.duration.coerceAtLeast(0L),
                    isFavorite     = isFav,
                    isShuffleOn    = player.shuffleModeEnabled,
                    isRepeatOn     = player.repeatMode != Player.REPEAT_MODE_OFF
                )
            } else {
                val lastEntity = withContext(Dispatchers.IO) {
                    musicDao.getHistory().firstOrNull()?.firstOrNull()
                        ?.let { hist -> musicDao.getSongsByIds(listOf(hist.songId)).firstOrNull() }
                }
                val last = lastEntity?.toDomainModel()
                MusicWidget.updateAllWidgets(
                    context     = this@MusicService,
                    songTitle   = last?.title,
                    artistName  = last?.artist,
                    albumName   = last?.album,
                    isPlaying   = false,
                    albumArtUri = last?.albumArtUri
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_WIDGET_PLAY_PAUSE -> {
                if (player.mediaItemCount == 0) {
                    restoreLastSessionAndPlay()
                } else {
                    if (player.isPlaying) player.pause() else player.play()
                }
            }
            ACTION_WIDGET_NEXT    -> player.seekToNext()
            ACTION_WIDGET_PREV    -> player.seekToPrevious()
            ACTION_WIDGET_SHUFFLE -> {
                player.shuffleModeEnabled = !player.shuffleModeEnabled
                updateWidget()
            }
            ACTION_WIDGET_REPEAT -> {
                player.repeatMode = when (player.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                updateWidget()
            }
            ACTION_WIDGET_FAVORITE -> {
                val song = player.currentMediaItem?.localConfiguration?.tag as? Song
                if (song != null) {
                    serviceScope.launch {
                        val isFav = withContext(Dispatchers.IO) {
                            musicDao.getFavorites().firstOrNull()?.contains(song.id) ?: false
                        }
                        withContext(Dispatchers.IO) {
                            if (isFav) musicDao.deleteFavorite(
                                com.lg.monkeymusicplayer.data.database.FavoriteEntity(song.id)
                            ) else musicDao.insertFavorite(
                                com.lg.monkeymusicplayer.data.database.FavoriteEntity(song.id)
                            )
                        }
                        updateWidget()
                    }
                }
            }
            ACTION_WIDGET_UPDATE_REQUEST -> updateWidget()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun restoreLastSessionAndPlay() {
        serviceScope.launch {
            if (player.mediaItemCount == 0) {
                val history = withContext(Dispatchers.IO) {
                    musicDao.getHistory().firstOrNull() ?: emptyList()
                }
                if (history.isNotEmpty()) {
                    val songIds = history.map { it.songId }
                    val songEntities = withContext(Dispatchers.IO) {
                        musicDao.getSongsByIds(songIds)
                    }
                    val songs = songIds.mapNotNull { id ->
                        songEntities.find { it.id == id }?.toDomainModel()
                    }
                    if (songs.isNotEmpty()) {
                        val mediaItems = songs.map { it.toMediaItem() }
                        player.setMediaItems(mediaItems)
                        player.prepare()
                    }
                }
            }
            if (player.mediaItemCount > 0) {
                player.play()
            }
        }
    }

    private fun setupEqualizer() {
        try {
            equalizer = Equalizer(0, player.audioSessionId)
            equalizer?.enabled = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun performFadeOut() {
        isFading = true
        val startVolume = player.volume
        val steps = 20
        val interval = crossfadeDurationMs / steps

        for (i in 0..steps) {
            handler.postDelayed({
                if (isFading) {
                    player.volume = startVolume * (1.0f - i.toFloat() / steps)
                }
            }, i * interval)
        }
    }

    private fun performFadeIn() {
        isFading = true
        player.volume = 0f
        val steps = 20
        val interval = 100L

        for (i in 0..steps) {
            handler.postDelayed({
                player.volume = (i.toFloat() / steps)
                if (i == steps) isFading = false
            }, i * interval)
        }
    }

    private inner class AutoMediaLibraryCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: ControllerInfo,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Monkey Music")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val items = when {
                parentId == ROOT_ID -> buildRootChildren()
                parentId == SONGS_ID -> buildSongsChildren()
                parentId == FAVORITES_ID -> buildFavoritesChildren()
                parentId == PLAYLISTS_ID -> buildPlaylistsChildren()
                parentId.startsWith(PLAYLIST_PREFIX) -> {
                    val playlistId = parentId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
                    if (playlistId != null) buildPlaylistSongsChildren(playlistId)
                    else emptyList()
                }
                else -> emptyList()
            }
            return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val song = runCatching {
                runBlocking(Dispatchers.IO) {
                    musicDao.getSongsByIds(listOf(mediaId.toLong())).firstOrNull()?.toDomainModel()
                }
            }.getOrNull()

            return if (song != null) {
                Futures.immediateFuture(LibraryResult.ofItem(song.toMediaItem(), null))
            } else {
                Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            }
        }

        private fun buildRootChildren(): List<MediaItem> = listOf(
            buildBrowsableItem(SONGS_ID, "Canciones", MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
            buildBrowsableItem(FAVORITES_ID, "Favoritos", MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
            buildBrowsableItem(PLAYLISTS_ID, "Playlists", MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
        )

        private fun buildSongsChildren(): List<MediaItem> =
            runBlocking(Dispatchers.IO) {
                musicDao.getAllSongs().map { it.toDomainModel().toMediaItem() }
            }

        private fun buildFavoritesChildren(): List<MediaItem> =
            runBlocking(Dispatchers.IO) {
                val favIds = musicDao.getAllFavoriteIds().toSet()
                musicDao.getAllSongs()
                    .filter { it.id in favIds }
                    .map { it.toDomainModel().toMediaItem() }
            }

        private fun buildPlaylistsChildren(): List<MediaItem> =
            runBlocking(Dispatchers.IO) {
                musicDao.getAllPlaylists().map { playlist ->
                    buildBrowsableItem(
                        "$PLAYLIST_PREFIX${playlist.id}",
                        playlist.name,
                        MediaMetadata.MEDIA_TYPE_PLAYLIST
                    )
                }
            }

        private fun buildPlaylistSongsChildren(playlistId: Long): List<MediaItem> =
            runBlocking(Dispatchers.IO) {
                val songIds = musicDao.getSongsInPlaylist(playlistId)
                if (songIds.isEmpty()) return@runBlocking emptyList<MediaItem>()
                musicDao.getSongsByIds(songIds).map { it.toDomainModel().toMediaItem() }
            }

        private fun buildBrowsableItem(id: String, title: String, mediaType: Int): MediaItem =
            MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(mediaType)
                        .build()
                )
                .build()

        override fun onConnect(
            session: MediaSession,
            controller: ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
            availableSessionCommands.add(SessionCommand(COMMAND_GET_AUDIO_SESSION_ID, Bundle.EMPTY))
            availableSessionCommands.add(SessionCommand(COMMAND_SET_CROSSFADE_DURATION, Bundle.EMPTY))
            availableSessionCommands.add(SessionCommand(COMMAND_SET_EQUALIZER_BAND, Bundle.EMPTY))
            availableSessionCommands.add(SessionCommand(COMMAND_GET_EQUALIZER_DATA, Bundle.EMPTY))
            return MediaSession.ConnectionResult.accept(
                availableSessionCommands.build(),
                connectionResult.availablePlayerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                COMMAND_GET_AUDIO_SESSION_ID -> {
                    val resultBundle = Bundle().apply {
                        putInt("audio_session_id", player.audioSessionId)
                    }
                    return Futures.immediateFuture(
                        SessionResult(SessionResult.RESULT_SUCCESS, resultBundle)
                    )
                }
                COMMAND_SET_CROSSFADE_DURATION -> {
                    crossfadeDurationMs = args.getLong("duration_ms", 5000L)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                COMMAND_SET_EQUALIZER_BAND -> {
                    val band = args.getShort("band", -1)
                    val level = args.getShort("level", 0)
                    if (band >= 0) equalizer?.setBandLevel(band, level)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                COMMAND_GET_EQUALIZER_DATA -> {
                    val eq = equalizer
                    if (eq != null) {
                        val numBands = eq.numberOfBands
                        val minLevel = eq.bandLevelRange[0]
                        val maxLevel = eq.bandLevelRange[1]
                        val bands = IntArray(numBands.toInt()) { i ->
                            eq.getCenterFreq(i.toShort()) / 1000
                        }
                        val levels = ShortArray(numBands.toInt()) { i ->
                            eq.getBandLevel(i.toShort())
                        }
                        val resultBundle = Bundle().apply {
                            putShort("num_bands", numBands)
                            putShort("min_level", minLevel)
                            putShort("max_level", maxLevel)
                            putIntArray("center_freqs", bands)
                            putShortArray("band_levels", levels)
                        }
                        return Futures.immediateFuture(
                            SessionResult(SessionResult.RESULT_SUCCESS, resultBundle)
                        )
                    }
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }

    override fun onGetSession(controllerInfo: ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady ||
            player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_IDLE
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        handler.removeCallbacks(crossfadeCheckRunnable)
        equalizer?.release()
        equalizer = null
        player.release()
        mediaSession?.release()
        mediaSession = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(path)
        .setTag(this)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(android.net.Uri.parse(albumArtUri))
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build()
        )
        .build()

    private fun SongEntity.toDomainModel() = Song(
        id = id,
        albumId = albumId,
        title = title,
        artist = artist,
        album = album,
        genre = genre,
        folder = folder,
        path = path,
        albumArtUri = albumArtUri
    )
}
