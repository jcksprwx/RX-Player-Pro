package com.rxplayer.pro.ui.components

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.rxplayer.pro.core.common.FileUtils
import com.rxplayer.pro.core.common.TimeUtils
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import com.rxplayer.pro.core.theme.*

@Composable
fun MediaThumbnail(
    media: MediaFileEntity,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    iconSize: androidx.compose.ui.unit.Dp = 32.dp
) {
    val context = LocalContext.current
    var isError by remember(media.id, media.uriString) { mutableStateOf(false) }

    Box(
        modifier = modifier.background(NovaDarkSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!isError) {
            AsyncImage(
                model = remember(media.id, media.uriString, media.albumId) {
                    val model: Any = when {
                        media.isVideo -> {
                            if (media.uriString.isNotEmpty()) Uri.parse(media.uriString) else media.path
                        }
                        else -> {
                            if (media.albumId > 0) {
                                ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), media.albumId)
                            } else if (media.uriString.isNotEmpty()) {
                                Uri.parse(media.uriString)
                            } else {
                                media.path
                            }
                        }
                    }
                    ImageRequest.Builder(context)
                        .data(model)
                        .videoFrameMillis(1000L)
                        .crossfade(true)
                        .build()
                },
                contentDescription = media.title,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                onState = { state ->
                    isError = (state is AsyncImagePainter.State.Error)
                }
            )
        }

        if (isError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                NovaDarkSurfaceElevated,
                                NovaDarkSurfaceVariant
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (media.isVideo) Icons.Default.Movie else Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = if (media.isVideo) NovaElectricCyan.copy(alpha = 0.6f) else NovaElectricViolet.copy(alpha = 0.6f),
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

@Composable
fun VideoGridCard(
    media: MediaFileEntity,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
    onTrimClick: () -> Unit,
    onExtractAudioClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NovaElectricCyan.copy(alpha = 0.15f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(NovaDarkSurfaceVariant)
            ) {
                MediaThumbnail(
                    media = media,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gradient shadow overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                startY = 60f
                            )
                        )
                )

                // Duration badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = TimeUtils.formatDuration(media.durationMs),
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Resolution badge if HD / 4K
                if (media.height >= 720) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = NovaElectricCyan.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = if (media.height >= 2160) "4K" else if (media.height >= 1080) "FHD" else "HD",
                            color = Color.Black,
                            fontSize = 9.sp,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = NovaTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${FileUtils.formatFileSize(media.sizeBytes)} • ${media.folderName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaTextSecondary,
                        maxLines = 1
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = NovaTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    MediaDropdownMenu(
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        media = media,
                        onInfoClick = onInfoClick,
                        onTrimClick = onTrimClick,
                        onExtractAudioClick = onExtractAudioClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                        onDeleteClick = onDeleteClick
                    )
                }
            }
        }
    }
}

@Composable
fun VideoListCard(
    media: MediaFileEntity,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
    onTrimClick: () -> Unit,
    onExtractAudioClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(110.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NovaDarkSurfaceVariant)
        ) {
            MediaThumbnail(
                media = media,
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
                    text = TimeUtils.formatDuration(media.durationMs),
                    color = Color.White,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = media.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = NovaTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${FileUtils.formatFileSize(media.sizeBytes)} • ${media.resolution.ifEmpty { "Video" }} • ${media.folderName}",
                style = MaterialTheme.typography.bodySmall,
                color = NovaTextSecondary,
                maxLines = 1
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = NovaTextSecondary
                )
            }

            MediaDropdownMenu(
                expanded = menuExpanded,
                onDismiss = { menuExpanded = false },
                media = media,
                onInfoClick = onInfoClick,
                onTrimClick = onTrimClick,
                onExtractAudioClick = onExtractAudioClick,
                onAddToPlaylistClick = onAddToPlaylistClick,
                onDeleteClick = onDeleteClick
            )
        }
    }
}

@Composable
fun MediaDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    media: MediaFileEntity,
    onInfoClick: () -> Unit,
    onTrimClick: () -> Unit,
    onExtractAudioClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(NovaDarkSurfaceElevated)
    ) {
        DropdownMenuItem(
            text = { Text("Details & Codecs", color = NovaTextPrimary) },
            leadingIcon = { Icon(Icons.Default.Info, null, tint = NovaElectricCyan) },
            onClick = {
                onDismiss()
                onInfoClick()
            }
        )
        DropdownMenuItem(
            text = { Text("Video Cutter", color = NovaTextPrimary) },
            leadingIcon = { Icon(Icons.Default.ContentCut, null, tint = NovaElectricViolet) },
            onClick = {
                onDismiss()
                onTrimClick()
            }
        )
        DropdownMenuItem(
            text = { Text("Extract Audio", color = NovaTextPrimary) },
            leadingIcon = { Icon(Icons.Default.Audiotrack, null, tint = NovaAccentAmber) },
            onClick = {
                onDismiss()
                onExtractAudioClick()
            }
        )
        DropdownMenuItem(
            text = { Text("Add to Playlist", color = NovaTextPrimary) },
            leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, tint = NovaTextSecondary) },
            onClick = {
                onDismiss()
                onAddToPlaylistClick()
            }
        )
        DropdownMenuItem(
            text = { Text("Share", color = NovaTextPrimary) },
            leadingIcon = { Icon(Icons.Default.Share, null, tint = NovaTextSecondary) },
            onClick = {
                onDismiss()
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = media.mimeType.ifEmpty { "video/*" }
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(media.uriString))
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
            }
        )
        Divider(color = NovaDivider)
        DropdownMenuItem(
            text = { Text("Delete", color = NovaAccentRose) },
            leadingIcon = { Icon(Icons.Default.Delete, null, tint = NovaAccentRose) },
            onClick = {
                onDismiss()
                onDeleteClick()
            }
        )
    }
}
