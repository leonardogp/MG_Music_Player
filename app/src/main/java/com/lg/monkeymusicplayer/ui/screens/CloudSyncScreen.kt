package com.lg.monkeymusicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.core.result.Result
import com.lg.monkeymusicplayer.ui.MusicViewModel
import com.lg.monkeymusicplayer.ui.theme.PrimaryOrange
import kotlinx.coroutines.launch

/**
 * CloudSyncScreen — permite al usuario subir y restaurar un backup en nube manualmente.
 *
 * El sync automático (CloudSyncWorker) corre en background cada 12h cuando hay red.
 * Esta pantalla expone las acciones manuales y el estado del último backup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isUploading by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    val strUploadOk    = stringResource(R.string.cloud_sync_upload_ok)
    val strDownloadOk  = stringResource(R.string.cloud_sync_download_ok)
    val strNoBackup    = stringResource(R.string.cloud_sync_no_backup)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cloud_sync_menu_item), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Info card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    Text(
                        text = stringResource(R.string.backup_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Upload card
            CloudSyncActionCard(
                icon = Icons.Default.CloudUpload,
                title = stringResource(R.string.cloud_sync_upload),
                subtitle = stringResource(R.string.backup_export_subtitle),
                isLoading = isUploading,
                enabled = !isUploading && !isDownloading,
                onClick = {
                    scope.launch {
                        isUploading = true
                        val result = viewModel.cloudSyncUpload()
                        isUploading = false
                        val msg = when (result) {
                            is Result.Success -> strUploadOk
                            is Result.Error   -> result.message
                            else              -> strUploadOk
                        }
                        snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                    }
                }
            )

            // Download card
            CloudSyncActionCard(
                icon = Icons.Default.CloudDownload,
                title = stringResource(R.string.cloud_sync_download),
                subtitle = stringResource(R.string.backup_import_subtitle),
                isLoading = isDownloading,
                enabled = !isUploading && !isDownloading,
                onClick = {
                    scope.launch {
                        isDownloading = true
                        val result = viewModel.cloudSyncDownload()
                        isDownloading = false
                        val msg = when (result) {
                            is Result.Success -> strDownloadOk
                            is Result.Error   -> if (result.message.contains("No cloud backup"))
                                strNoBackup else result.message
                            else              -> strDownloadOk
                        }
                        snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                    }
                }
            )

            // Auto-sync note
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PrimaryOrange.copy(alpha = 0.07f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Autorenew, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
                    Text(
                        text = stringResource(R.string.cloud_sync_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryOrange
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudSyncActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(PrimaryOrange.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = PrimaryOrange)
                } else {
                    Icon(icon, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
