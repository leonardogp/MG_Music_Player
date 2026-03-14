package com.mg.mgmusicplayer.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.mg.mgmusicplayer.core.utils.SongCoverFetcher
import com.mg.mgmusicplayer.ui.components.PermissionHandler
import com.mg.mgmusicplayer.ui.theme.MGMusicPlayerTheme

class MainActivity : ComponentActivity(), ImageLoaderFactory {

    private val viewModel: MusicViewModel by viewModels()

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SongCoverFetcher.Factory(this@MainActivity))
            }
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Definir los permisos según la versión del sistema
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
                    onPermissionsGranted = {
                        AppRoot(viewModel)
                    }
                )
            }
        }
    }
}
