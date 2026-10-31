package com.rxplayer.pro.ui.screens.videoinfo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rxplayer.pro.core.common.TimeUtils
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import com.rxplayer.pro.core.media.transformer.MediaMetadataInspector
import com.rxplayer.pro.core.media.transformer.VideoDetailedInfo
import com.rxplayer.pro.core.theme.*

@Composable
fun VideoInfoDialog(
    media: MediaFileEntity,
    currentVideoCodec: String = "",
    currentAudioCodec: String = "",
    decoderMode: String = "HW (Hardware Accelerated)",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val inspector = remember { MediaMetadataInspector(context) }
    var details by remember { mutableStateOf<VideoDetailedInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val decoderBadge = when {
        decoderMode.startsWith("SW", ignoreCase = true) -> "SW"
        decoderMode.startsWith("AUTO", ignoreCase = true) -> "AUTO"
        else -> "HW"
    }
    val isSoftwareDecoder = decoderBadge == "SW"

    LaunchedEffect(media) {
        val targetPath = media.path.ifEmpty { media.uriString }
        details = inspector.inspectVideo(
            filePathOrUri = targetPath,
            fallbackName = media.displayName,
            fallbackSize = media.sizeBytes,
            fallbackVideoCodec = currentVideoCodec,
            fallbackAudioCodec = currentAudioCodec
        )
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = NovaElectricCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = NovaElectricCyan, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Media Information", color = NovaTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Technical Codecs & Streams", color = NovaTextSecondary, fontSize = 11.sp)
                }
            }
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NovaElectricCyan)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // File General Card
                    InfoCard(title = "File Details") {
                        InfoRow("File Name", details?.fileName ?: media.displayName)
                        InfoRow("Location", details?.filePath ?: media.path)
                        InfoRow("Size", details?.fileSizeFormatted ?: "${media.sizeBytes / (1024 * 1024)} MB")
                        InfoRow("Container", details?.containerFormat ?: "MPEG-4 (.mp4)")
                        InfoRow("Duration", TimeUtils.formatDurationDetailed(details?.durationMs ?: media.durationMs))
                    }

                    // Video Stream Card with Decoder Info
                    InfoCard(
                        title = "Video Stream",
                        icon = Icons.Default.Videocam,
                        badge = decoderBadge,
                        tint = if (isSoftwareDecoder) androidx.compose.ui.graphics.Color(0xFFFFB74D) else NovaElectricCyan
                    ) {
                        InfoRow("Decoder", decoderMode)
                        InfoRow("Acceleration", if (isSoftwareDecoder) "Software Decoder (CPU)" else "Hardware Accelerated (MediaCodec HW)")
                        InfoRow("Video Codec", details?.videoCodec ?: "H.264 / AVC")
                        InfoRow("Resolution", "${details?.resolution ?: "1920x1080"} (${details?.aspectRatioText ?: "16:9"})")
                        InfoRow("Frame Rate", if ((details?.frameRate ?: 0f) > 0) "${details?.frameRate} fps" else "Native / Variable")
                        if ((details?.bitrateKbps ?: 0L) > 0) {
                            InfoRow("Video Bitrate", "${details?.bitrateKbps} kbps")
                        }
                    }

                    // Audio Stream Card
                    InfoCard(title = "Audio Stream", icon = Icons.Default.Audiotrack, tint = NovaElectricViolet) {
                        InfoRow("Audio Codec", details?.audioCodec ?: "AAC (Advanced Audio Coding)")
                        InfoRow("Channels", details?.audioChannelsText ?: "Stereo (2.0)")
                        InfoRow("Sample Rate", details?.sampleRateText ?: "48.0 kHz")
                        if ((details?.audioBitrateKbps ?: 0L) > 0) {
                            InfoRow("Audio Bitrate", "${details?.audioBitrateKbps} kbps")
                        }
                    }

                    // Subtitles & Extra
                    if ((details?.subtitleTracksCount ?: 0) > 0) {
                        InfoCard(title = "Embedded Tracks") {
                            InfoRow("Subtitles", "${details?.subtitleTracksCount} track(s) available")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = NovaElectricCyan, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = NovaDarkSurfaceElevated,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun InfoCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    badge: String? = null,
    tint: androidx.compose.ui.graphics.Color = NovaElectricCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = tint,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!badge.isNullOrEmpty()) {
                    Surface(
                        color = tint.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, tint.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = badge,
                            color = tint,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = NovaDivider)
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = NovaTextSecondary,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = NovaTextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.62f)
        )
    }
}
