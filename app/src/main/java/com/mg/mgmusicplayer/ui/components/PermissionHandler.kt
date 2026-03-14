package com.mg.mgmusicplayer.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mg.mgmusicplayer.ui.theme.MGMusicPlayerTheme

sealed class PermissionState {
    data object Granted : PermissionState()
    data object Requesting : PermissionState()
    data class Rationale(val deniedPermissions: List<String>) : PermissionState()
    data class Denied(val deniedPermissions: List<String>) : PermissionState()
}

val PermissionStateSaver = Saver<MutableState<PermissionState>, Any>(
    save = { state ->
        when (val s = state.value) {
            is PermissionState.Granted -> listOf("Granted")
            is PermissionState.Requesting -> listOf("Requesting")
            is PermissionState.Rationale -> listOf("Rationale", s.deniedPermissions)
            is PermissionState.Denied -> listOf("Denied", s.deniedPermissions)
        }
    },
    restore = { value ->
        val list = value as List<*>
        @Suppress("UNCHECKED_CAST")
        val restored = when (list[0] as String) {
            "Granted" -> PermissionState.Granted
            "Requesting" -> PermissionState.Requesting
            "Rationale" -> PermissionState.Rationale(list[1] as List<String>)
            "Denied" -> PermissionState.Denied(list[1] as List<String>)
            else -> PermissionState.Requesting
        }
        mutableStateOf(restored)
    }
)

@Composable
fun PermissionHandler(
    requiredPermissions: List<String>,
    onExit: () -> Unit = {},
    onPermissionsGranted: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val permissionState = rememberSaveable(saver = PermissionStateSaver) { 
        mutableStateOf(
            if (checkPermissions(context, requiredPermissions)) PermissionState.Granted 
            else PermissionState.Requesting
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = results.filter { !it.value }.keys.toList()
        if (denied.isEmpty()) {
            permissionState.value = PermissionState.Granted
        } else {
            val showRationale = denied.any { 
                shouldShowRationale(context, listOf(it))
            }
            permissionState.value = if (showRationale) PermissionState.Rationale(denied) else PermissionState.Denied(denied)
        }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (checkPermissions(context, requiredPermissions)) {
            permissionState.value = PermissionState.Granted
        }
    }

    when (val state = permissionState.value) {
        is PermissionState.Granted -> {
            onPermissionsGranted()
        }
        is PermissionState.Rationale -> {
            PermissionRationaleScreen(
                deniedPermissions = state.deniedPermissions,
                onRetry = {
                    permissionState.value = PermissionState.Requesting
                },
                onExit = onExit
            )
        }
        is PermissionState.Denied -> {
            PermissionDeniedScreen(
                deniedPermissions = state.deniedPermissions,
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    settingsLauncher.launch(intent)
                },
                onExit = onExit
            )
        }
        is PermissionState.Requesting -> {
            LaunchedEffect(requiredPermissions) {
                permissionLauncher.launch(requiredPermissions.toTypedArray())
            }
        }
    }
}

private fun checkPermissions(context: Context, permissions: List<String>): Boolean {
    return permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

private fun shouldShowRationale(context: Context, permissions: List<String>): Boolean {
    val activity = context.findActivity() ?: return false
    return permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

private fun getPermissionLabel(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_MEDIA_AUDIO -> "Acceso a Música y Audio"
        Manifest.permission.POST_NOTIFICATIONS -> "Notificaciones de Reproducción"
        Manifest.permission.READ_EXTERNAL_STORAGE -> "Acceso al Almacenamiento"
        else -> permission.split(".").last().replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }
}

@Composable
private fun PermissionRationaleScreen(
    deniedPermissions: List<String>,
    onRetry: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Información de permisos",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Acceso Necesario",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Para que la app funcione correctamente, necesitamos que aceptes los siguientes permisos:",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        deniedPermissions.forEach { permission ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle, 
                        contentDescription = "Permiso requerido", 
                        tint = MaterialTheme.colorScheme.secondary, 
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getPermissionLabel(permission), 
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text("Entendido, reintentar")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salir de la aplicación")
        }
    }
}

@Composable
private fun PermissionDeniedScreen(
    deniedPermissions: List<String>,
    onOpenSettings: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AudioFile,
            contentDescription = "Error de permisos",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Permisos Desactivados",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Has desactivado permisos críticos. Por favor, habilítalos en la configuración para continuar:",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        deniedPermissions.forEach { permission ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Permiso denegado",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getPermissionLabel(permission),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Abrir Configuración")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salir", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionRationalePreview() {
    MGMusicPlayerTheme {
        Surface {
            PermissionRationaleScreen(
                deniedPermissions = listOf(
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                onRetry = {},
                onExit = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionDeniedPreview() {
    MGMusicPlayerTheme {
        Surface {
            PermissionDeniedScreen(
                deniedPermissions = listOf(
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                onOpenSettings = {},
                onExit = {}
            )
        }
    }
}
