package com.lg.monkeymusicplayer.ui.components.core

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lg.monkeymusicplayer.R
import com.lg.monkeymusicplayer.data.model.SmartPlaylist
import com.lg.monkeymusicplayer.data.model.SmartPlaylistType

@Composable
fun SmartPlaylistCard(
    playlist: SmartPlaylist,
    onClick: () -> Unit
) {
    val title = when (playlist.type) {
        SmartPlaylistType.DAILY_MIX -> stringResource(R.string.smart_daily_mix_title)
        SmartPlaylistType.REDISCOVER -> stringResource(R.string.smart_rediscover_title)
        SmartPlaylistType.TOP_SONGS -> stringResource(R.string.smart_top_songs_title)
    }

    MonkeyCard(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        onClick = onClick,
        content = {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = title)
                // Optional: show more details if needed in future
            }
        }
    )
}
