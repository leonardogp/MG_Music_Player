package com.lg.monkeymusicplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.ui.LibraryUiState
import com.lg.monkeymusicplayer.ui.MusicViewModel
import com.lg.monkeymusicplayer.ui.components.dialogs.LanguageDialog
import com.lg.monkeymusicplayer.ui.components.dialogs.SleepTimerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: LibraryUiState,
    viewModel: MusicViewModel,
    navController: NavController,
    onBack: () -> Unit,
    onScanMusic: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onChangeLanguage: (String) -> Unit
) {
    var showSleepTimer by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showSleepTimer) {
        SleepTimerDialog(
            currentMinutes = uiState.playerState.sleepTimerMinutes,
            onDismiss = { showSleepTimer = false },
            onConfirm = { 
                onSetSleepTimer(it)
                showSleepTimer = false 
            }
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { 
                onChangeLanguage(it)
                showLanguageDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.scan_music)) },
                    leadingContent = { Icon(Icons.Default.Refresh, null) },
                    modifier = Modifier.clickable { onScanMusic() }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.equalizer)) },
                    leadingContent = { Icon(Icons.Default.GraphicEq, null) },
                    modifier = Modifier.clickable { onOpenEqualizer() }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.sleep_timer)) },
                    supportingContent = { 
                        if (uiState.playerState.sleepTimerMinutes > 0) {
                            Text(stringResource(R.string.timer_active, "${uiState.playerState.sleepTimerMinutes}m"))
                        }
                    },
                    leadingContent = { Icon(Icons.Default.Timer, null) },
                    modifier = Modifier.clickable { showSleepTimer = true }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.language)) },
                    leadingContent = { Icon(Icons.Default.Language, null) },
                    modifier = Modifier.clickable { showLanguageDialog = true }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.excluded_folders)) },
                    leadingContent = { Icon(Icons.Default.FolderOff, null) },
                    modifier = Modifier.clickable { navController.navigate("excluded_folders") }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.stats_title)) },
                    leadingContent = { Icon(Icons.Default.BarChart, null) },
                    modifier = Modifier.clickable { navController.navigate("stats") }
                )
            }
        }
    }
}
