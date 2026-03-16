package com.mg.mgmusicplayer.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.mg.mgmusicplayer.core.player.MusicPlayerManager
import com.mg.mgmusicplayer.core.utils.SongCoverFetcher
import com.mg.mgmusicplayer.data.database.MusicDatabase
import com.mg.mgmusicplayer.data.repository.MusicRepository
import com.mg.mgmusicplayer.ui.components.PermissionHandler
import com.mg.mgmusicplayer.ui.theme.MGMusicPlayerTheme

class MainActivity : ComponentActivity(), ImageLoaderFactory {

    @OptIn(UnstableApi::class)
    private val viewModel: MusicViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = MusicDatabase.getDatabase(applicationContext)
                val repository = MusicRepository(applicationContext, database.musicDao())
                val playerManager = MusicPlayerManager(applicationContext)
                return MusicViewModel(repository, playerManager) as T
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SongCoverFetcher.Factory(this@MainActivity))
            }
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        setContent {
            MGMusicPlayerTheme {
                PermissionHandler(
                    requiredPermissions = permissions,
                    onExit = { finish() },
                    onPermissionsGranted = {
                        AppRoot(viewModel)
                    }
                )
            }
        }
    }
}
