package com.lg.monkeymusicplayer.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.lg.monkeymusicplayer.core.player.MusicPlayerManager
import com.lg.monkeymusicplayer.core.utils.SongCoverFetcher
import com.lg.monkeymusicplayer.data.database.MusicDatabase
import com.lg.monkeymusicplayer.data.repository.MusicRepository
import com.lg.monkeymusicplayer.ui.components.PermissionHandler
import com.lg.monkeymusicplayer.ui.components.ScaffoldWithInsets
import com.lg.monkeymusicplayer.ui.theme.monkeymusicplayerTheme

class MainActivity : AppCompatActivity(), ImageLoaderFactory {

    private val viewModel: MusicViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = MusicDatabase.getDatabase(applicationContext)
                val repository = MusicRepository(applicationContext, database.musicDao())
                val playerManager = MusicPlayerManager(applicationContext)
                return MusicViewModel(repository, playerManager, applicationContext) as T
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

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, UnstableApi::class)
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
            val windowSizeClass = calculateWindowSizeClass(this)
            
            monkeymusicplayerTheme {
                PermissionHandler(
                    requiredPermissions = permissions,
                    onExit = { finish() },
                    onPermissionsGranted = {
                        LaunchedEffect(Unit) {
                            viewModel.scanMusic()
                        }

                        ScaffoldWithInsets {
                            AppRoot(viewModel, windowSizeClass)
                        }
                    }
                )
            }
        }
    }
}
