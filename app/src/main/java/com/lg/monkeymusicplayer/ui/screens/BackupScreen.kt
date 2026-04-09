package com.lg.monkeymusicplayer.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.core.result.Result
import com.lg.monkeymusicplayer.data.model.BackupData
import com.lg.monkeymusicplayer.data.repository.BackupRepository
import com.lg.monkeymusicplayer.data.repository.BackupSummary
import com.lg.monkeymusicplayer.ui.theme.PrimaryOrange
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    backupRepository: BackupRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var exportState by remember { mutableStateOf<OperationState>(OperationState.Idle) }
    var importState by remember { mutableStateOf<OperationState>(OperationState.Idle) }

    // SAF: crear archivo de backup
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupData.MIME_TYPE)
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            exportState = OperationState.Loading
            exportState = when (val r = backupRepository.exportBackup(uri)) {
                is Result.Success -> OperationState.Success(null)
                is Result.Error   -> OperationState.Error(r.message)
                is Result.Loading -> OperationState.Loading
            }
        }
    }

    // SAF: abrir archivo de backup
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            importState = OperationState.Loading
            importState = when (val r = backupRepository.importBackup(uri)) {
                is Result.Success -> OperationState.Success(r.data)
                is Result.Error   -> OperationState.Error(r.message)
                is Result.Loading -> OperationState.Loading
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.size(32.dp)
                            .background(PrimaryOrange.copy(0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CloudUpload, null, tint = PrimaryOrange,
                                modifier = Modifier.size(18.dp))
                        }
                        Text(stringResource(R.string.backup_title), fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Descripción
            Text(stringResource(R.string.backup_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            // ── Export ───────────────────────────────────────────────────
            BackupActionCard(
                icon        = Icons.Default.CloudUpload,
                title       = stringResource(R.string.backup_export_title),
                subtitle    = stringResource(R.string.backup_export_subtitle),
                buttonLabel = stringResource(R.string.backup_export_button),
                state       = exportState,
                onAction    = {
                    exportState = OperationState.Idle
                    importState = OperationState.Idle
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(Date())
                    exportLauncher.launch("MonkeyBackup_$timestamp.${BackupData.FILE_EXTENSION}")
                },
                successContent = {
                    FeedbackRow(Icons.Default.CheckCircle, Color(0xFF4CAF50),
                        stringResource(R.string.backup_export_ok))
                },
                summaryContent = null
            )

            // ── Import ───────────────────────────────────────────────────
            BackupActionCard(
                icon        = Icons.Default.CloudDownload,
                title       = stringResource(R.string.backup_import_title),
                subtitle    = stringResource(R.string.backup_import_subtitle),
                buttonLabel = stringResource(R.string.backup_import_button),
                state       = importState,
                onAction    = {
                    exportState = OperationState.Idle
                    importState = OperationState.Idle
                    importLauncher.launch(arrayOf(BackupData.MIME_TYPE, "*/*"))
                },
                successContent = { summary ->
                    if (summary != null) ImportSummaryContent(summary)
                },
                summaryContent = null
            )

            // ── Info chip ─────────────────────────────────────────────────
            BackupInfoChip()
        }
    }
}

// ── Componentes ──────────────────────────────────────────────────────────────

@Composable
private fun BackupActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonLabel: String,
    state: OperationState,
    onAction: () -> Unit,
    successContent: @Composable ((BackupSummary?) -> Unit)?,
    summaryContent: @Composable (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(40.dp)
                    .background(PrimaryOrange.copy(0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = PrimaryOrange, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(
                onClick = onAction,
                enabled = state !is OperationState.Loading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                if (state is OperationState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp, color = Color.Black)
                    Spacer(Modifier.width(8.dp))
                }
                Text(buttonLabel, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            AnimatedVisibility(state is OperationState.Success) {
                val summary = (state as? OperationState.Success)?.summary
                successContent?.invoke(summary)
            }
            AnimatedVisibility(state is OperationState.Error) {
                val msg = (state as? OperationState.Error)?.message ?: ""
                FeedbackRow(Icons.Default.Error, MaterialTheme.colorScheme.error, msg)
            }
        }
    }
}

@Composable
private fun FeedbackRow(icon: ImageVector, color: Color, message: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun ImportSummaryContent(summary: BackupSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FeedbackRow(Icons.Default.CheckCircle, Color(0xFF4CAF50),
            stringResource(R.string.backup_import_ok))
        Text(
            stringResource(R.string.backup_import_summary,
                summary.favoritesRestored, summary.playlistsRestored,
                summary.presetsRestored, summary.statsRestored),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BackupInfoChip() {
    Row(modifier = Modifier.fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
        .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp).padding(top = 1.dp))
        Text(stringResource(R.string.backup_info),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Estado de operación ───────────────────────────────────────────────────────

private sealed class OperationState {
    data object Idle    : OperationState()
    data object Loading : OperationState()
    data class Success(val summary: BackupSummary?) : OperationState()
    data class Error(val message: String) : OperationState()
}
