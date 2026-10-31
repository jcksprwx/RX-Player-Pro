package com.rxplayer.pro.ui.screens.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rxplayer.pro.core.common.TimeUtils
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import com.rxplayer.pro.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    onBackClick: () -> Unit,
    onPlayMedia: (Long) -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val itemsFlow = remember(playlistId) { viewModel.getPlaylistMedia(playlistId) }
    val mediaItems by itemsFlow.collectAsStateWithLifecycle(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName, color = NovaTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NovaTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NovaDarkSurface)
            )
        },
        containerColor = NovaAmoledBackground
    ) { padding ->
        if (mediaItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No items in this playlist yet.", color = NovaTextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(mediaItems, key = { it.id }) { media ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayMedia(media.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = NovaElectricCyan)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(media.displayName, style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                            Text(TimeUtils.formatDuration(media.durationMs), style = MaterialTheme.typography.bodySmall, color = NovaTextSecondary)
                        }
                        IconButton(onClick = { viewModel.removeMediaFromPlaylist(playlistId, media.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = NovaTextSecondary)
                        }
                    }
                    Divider(color = NovaDivider)
                }
            }
        }
    }
}
