package com.lg.monkeymusicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.core.queue.PlayQueue
import com.lg.monkeymusicplayer.core.queue.QueueManager
import com.lg.monkeymusicplayer.ui.MusicViewModel
import com.lg.monkeymusicplayer.ui.theme.PrimaryOrange

/**
 * QueuesScreen — permite al usuario crear, cambiar y eliminar colas de reproducción.
 *
 * Las colas predefinidas (main, smart) no pueden eliminarse.
 * Al tocar una cola se hace switch: el player carga esa cola automáticamente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueuesScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val queues by viewModel.queueManager.queues.collectAsState()
    val activeQueueName by viewModel.queueManager.activeQueueName.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newQueueName by remember { mutableStateOf("") }
    var queueToDelete by remember { mutableStateOf<PlayQueue?>(null) }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false; newQueueName = "" },
            title = { Text(stringResource(R.string.queue_new)) },
            text = {
                OutlinedTextField(
                    value = newQueueName,
                    onValueChange = { newQueueName = it },
                    placeholder = { Text(stringResource(R.string.queue_name_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newQueueName.isNotBlank(),
                    onClick = {
                        viewModel.createAndSwitchQueue(newQueueName.trim())
                        showCreateDialog = false
                        newQueueName = ""
                    }
                ) { Text(stringResource(R.string.queue_new), color = PrimaryOrange) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; newQueueName = "" }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    queueToDelete?.let { queue ->
        AlertDialog(
            onDismissRequest = { queueToDelete = null },
            title = { Text(stringResource(R.string.queue_delete)) },
            text = { Text(queue.displayName) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteQueue(queue.name)
                    queueToDelete = null
                }) { Text(stringResource(R.string.delete_queue), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { queueToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.queues_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.queue_new))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(queues.values.toList(), key = { it.name }) { queue ->
                val isActive = queue.name == activeQueueName
                val isProtected = queue.name == QueueManager.QUEUE_MAIN ||
                        queue.name == QueueManager.QUEUE_SMART

                ListItem(
                    headlineContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(queue.displayName, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                            if (isActive) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = PrimaryOrange.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.queue_active_badge),
                                        color = PrimaryOrange,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    supportingContent = {
                        Text(
                            stringResource(R.string.queue_songs_count, queue.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isActive) PrimaryOrange.copy(0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = if (isActive) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    trailingContent = {
                        Row {
                            if (!isActive) {
                                IconButton(onClick = { viewModel.switchToQueue(queue.name) }) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.queue_switch),
                                        tint = PrimaryOrange
                                    )
                                }
                            }
                            if (!isProtected) {
                                IconButton(onClick = { queueToDelete = queue }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = stringResource(R.string.queue_delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}
