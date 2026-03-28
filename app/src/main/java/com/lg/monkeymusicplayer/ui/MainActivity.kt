package com.lg.monkeymusicplayer.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ImageLoaderFactory {

    private val viewModel: MusicViewModel by viewModels()

    // ── PUNTO 5: launcher para la pantalla de Settings de MANAGE_EXTERNAL_STORAGE ──
    // No se puede pedir con requestPermissions() normal — Android exige abrir
    // Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION directamente.
    // Al volver de Settings verificamos si el permiso fue otorgado y notificamos al ViewModel.
    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // El usuario volvió de Settings — actualizar el estado en el ViewModel
        viewModel.onManageStoragePermissionResult(hasManageStoragePermission())
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components { add(SongCoverFetcher.Factory(this@MainActivity)) }
            .build()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Informar al ViewModel del estado actual del permiso al arrancar
        viewModel.onManageStoragePermissionResult(hasManageStoragePermission())

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

                        // Escuchar cuando el ViewModel pide abrir Settings de almacenamiento
                        LaunchedEffect(Unit) {
                            viewModel.requestManageStorageEvent.collect {
                                requestManageStoragePermission()
                            }
                        }

                        ScaffoldWithInsets {
                            AppRoot(viewModel, windowSizeClass)
                        }
                    }
                )
            }
        }
    }

    // Devuelve true si la app tiene permiso para gestionar todo el almacenamiento externo.
    // En Android 10 e inferior siempre es true porque WRITE_EXTERNAL_STORAGE era suficiente.
    fun hasManageStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    // Abre la pantalla de Settings específica de esta app para que el usuario
    // pueda activar "Permitir gestión de todos los archivos".
    private fun requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            manageStorageLauncher.launch(intent)
        }
    }
}
