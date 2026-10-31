package com.rxplayer.pro.ui.screens.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rxplayer.pro.core.common.TimeUtils
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import com.rxplayer.pro.core.theme.*
import com.rxplayer.pro.ui.components.MediaThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    onNavigateToPlayer: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    viewModel: MusicViewModel = hiltViewModel()
) {
    val musicList by viewModel.musicList.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()

    val currentMedia by viewModel.playerManager.currentMedia.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tracks (${musicList.size})", "Artists (${artists.size})", "Albums (${albums.size})")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Music Library", color = NovaTextPrimary) },
                actions = {
                    IconButton(onClick = onNavigateToEqualizer) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Equalizer", tint = NovaElectricCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NovaDarkSurface)
            )
        },
        containerColor = NovaAmoledBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = NovaDarkSurface,
                contentColor = NovaElectricCyan
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 13.sp) }
                    )
                }
            }

            // Quick Shuffle Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${musicList.size} Audio Tracks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NovaTextSecondary
                )
                Button(
                    onClick = { viewModel.shuffleAll() },
                    colors = ButtonDefaults.buttonColors(containerColor = NovaElectricViolet.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, tint = NovaVioletGlow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle All", color = NovaVioletGlow, fontSize = 12.sp)
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(musicList, key = { it.id }) { track ->
                                TrackListItem(
                                    track = track,
                                    isCurrentlyPlaying = currentMedia?.id == track.id,
                                    onClick = {
                                        viewModel.playTrack(track)
                                        onNavigateToPlayer()
                                    }
                                )
                            }
                        }
                    }
                    1 -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(artists) { artist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {}
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(NovaDarkSurfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = NovaElectricCyan)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(artist, color = NovaTextPrimary, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                    2 -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(albums) { album ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {}
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NovaDarkSurfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Album, contentDescription = null, tint = NovaElectricViolet)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(album, color = NovaTextPrimary, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Mini Player Bar if something is loaded
            if (currentMedia != null && !currentMedia!!.isVideo) {
                MiniMusicPlayerBar(
                    media = currentMedia!!,
                    isPlaying = isPlaying,
                    onPlayPause = { viewModel.playerManager.togglePlayPause() },
                    onClick = onNavigateToPlayer
                )
            }
        }
    }
}

@Composable
fun TrackListItem(
    track: MediaFileEntity,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            MediaThumbnail(
                media = track,
                modifier = Modifier.fillMaxSize(),
                iconSize = 22.dp
            )
            if (isCurrentlyPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = NovaElectricCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrentlyPlaying) NovaElectricCyan else NovaTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${track.artist.ifEmpty { "Unknown Artist" }} • ${track.album.ifEmpty { "Music" }}",
                style = MaterialTheme.typography.bodySmall,
                color = NovaTextSecondary,
                maxLines = 1
            )
        }

        Text(
            text = TimeUtils.formatDuration(track.durationMs),
            style = MaterialTheme.typography.bodySmall,
            color = NovaTextSecondary,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun MiniMusicPlayerBar(
    media: MediaFileEntity,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = NovaDarkSurfaceElevated,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaThumbnail(
                media = media,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(6.dp)),
                iconSize = 20.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.title,
                    color = NovaTextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = media.artist.ifEmpty { "Playing" },
                    color = NovaTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = NovaElectricCyan
                )
            }
        }
    }
}
