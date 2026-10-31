package com.rxplayer.pro.ui.screens.home

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rxplayer.pro.core.common.FileUtils
import com.rxplayer.pro.core.common.TimeUtils
import com.rxplayer.pro.core.theme.*
import com.rxplayer.pro.ui.components.MediaThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToVideos: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToMusic: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onPlayVideo: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Double-back to exit on Home screen
    BackHandler {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000L) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = now
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val videoFolders by viewModel.videoFolders.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalVideoCount.collectAsStateWithLifecycle()
    val totalSize by viewModel.totalVideoSize.collectAsStateWithLifecycle()
    val isScanning by viewModel.mediaScanner.isScanning.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "RX",
                            color = NovaElectricCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "PLAYER PRO",
                            color = NovaElectricViolet,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshLibrary() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NovaElectricCyan)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = NovaTextPrimary)
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
                .verticalScroll(rememberScrollState())
        ) {
            if (isScanning) {
                LinearProgressIndicator(color = NovaElectricCyan, modifier = Modifier.fillMaxWidth())
            }

            // Storage Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NovaElectricCyan.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$totalCount", style = MaterialTheme.typography.headlineMedium, color = NovaElectricCyan)
                        Text("Indexed Videos", style = MaterialTheme.typography.bodySmall, color = NovaTextSecondary)
                    }
                    Divider(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp),
                        color = NovaDivider
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            FileUtils.formatFileSize(totalSize ?: 0L),
                            style = MaterialTheme.typography.headlineMedium,
                            color = NovaElectricViolet
                        )
                        Text("Media Size", style = MaterialTheme.typography.bodySmall, color = NovaTextSecondary)
                    }
                    Divider(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp),
                        color = NovaDivider
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${videoFolders.size}", style = MaterialTheme.typography.headlineMedium, color = NovaAccentAmber)
                        Text("Folders", style = MaterialTheme.typography.bodySmall, color = NovaTextSecondary)
                    }
                }
            }

            // Continue Watching Section
            if (continueWatching.isNotEmpty()) {
                SectionHeader("Continue Watching", onSeeAll = onNavigateToVideos)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(continueWatching, key = { it.media.id }) { item ->
                        Card(
                            modifier = Modifier
                                .width(200.dp)
                                .clickable { onPlayVideo(item.media.id) },
                            colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(115.dp)
                                        .background(NovaDarkSurfaceVariant)
                                ) {
                                    MediaThumbnail(
                                        media = item.media,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                                                )
                                            )
                                    )
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(38.dp).align(Alignment.Center)
                                    )
                                }
                                val progress = if (item.media.durationMs > 0) {
                                    (item.lastPositionMs.toFloat() / item.media.durationMs.toFloat()).coerceIn(0f, 1f)
                                } else 0f
                                LinearProgressIndicator(
                                    progress = progress,
                                    color = NovaElectricCyan,
                                    modifier = Modifier.fillMaxWidth().height(4.dp)
                                )
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = item.media.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = NovaTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${TimeUtils.formatDuration(item.lastPositionMs)} / ${TimeUtils.formatDuration(item.media.durationMs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NovaTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Recently Added Section
            if (recentlyAdded.isNotEmpty()) {
                SectionHeader("Recently Added", onSeeAll = onNavigateToVideos)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentlyAdded, key = { it.id }) { video ->
                        Card(
                            modifier = Modifier
                                .width(180.dp)
                                .clickable { onPlayVideo(video.id) },
                            colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(105.dp)
                                        .background(NovaDarkSurfaceVariant)
                                ) {
                                    MediaThumbnail(
                                        media = video,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp),
                                        shape = RoundedCornerShape(3.dp),
                                        color = Color.Black.copy(alpha = 0.8f)
                                    ) {
                                        Text(
                                            text = TimeUtils.formatDuration(video.durationMs),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = video.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = NovaTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = TimeUtils.formatDuration(video.durationMs),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NovaTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Video Folders Section
            SectionHeader("Folders", onSeeAll = onNavigateToFolders)
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                videoFolders.take(4).forEach { folder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(onClick = onNavigateToFolders),
                        colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NovaElectricCyan.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = NovaElectricCyan)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(folder.folderName, style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                                Text("${folder.mediaCount} videos", style = MaterialTheme.typography.bodySmall, color = NovaTextSecondary)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = NovaTextSecondary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary, fontWeight = FontWeight.Bold)
        TextButton(onClick = onSeeAll) {
            Text("See all", color = NovaElectricCyan, fontSize = 12.sp)
        }
    }
}
