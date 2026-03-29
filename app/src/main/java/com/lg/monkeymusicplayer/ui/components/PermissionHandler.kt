package com.lg.monkeymusicplayer.ui.components

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
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.ui.theme.monkeymusicplayerTheme

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
        val list = value as List<Any?>
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

@Composable
private fun getPermissionLabel(permission: String): String {
    return when (permission) {
        Manifest.permission.READ_MEDIA_AUDIO      -> stringResource(R.string.permission_label_audio)
        Manifest.permission.POST_NOTIFICATIONS    -> stringResource(R.string.permission_label_notifications)
        Manifest.permission.READ_EXTERNAL_STORAGE -> stringResource(R.string.permission_label_storage)
        else -> permission.split(".").last().replace("_", " ")
                    .lowercase().replaceFirstChar { it.uppercase() }
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
            .background(Color(0xFF121212)) // Fondo oscuro
            .padding(32.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = stringResource(R.string.permission_info_desc),
            modifier = Modifier.size(100.dp),
            tint = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.permission_rationale_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.permission_rationale_body),
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.8f),
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
                        contentDescription = stringResource(R.string.permission_required_desc),
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getPermissionLabel(permission),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(stringResource(R.string.permission_retry), color = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.exit_app), color = Color.White.copy(alpha = 0.6f))
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
            .background(Color(0xFF121212)) // Fondo oscuro
            .padding(32.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AudioFile,
            contentDescription = stringResource(R.string.permission_denied_desc),
            modifier = Modifier.size(100.dp),
            tint = Color(0xFF22C55E)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.permission_denied_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.permission_denied_body),
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.8f),
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
                        contentDescription = stringResource(R.string.permission_denied_item_desc),
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.permission_open_settings), color = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.exit_app), color = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionRationalePreview() {
    monkeymusicplayerTheme {
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
    monkeymusicplayerTheme {
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
