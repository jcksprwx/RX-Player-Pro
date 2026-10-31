package com.rxplayer.pro.ui.screens.cutter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rxplayer.pro.core.common.TimeUtils
import com.rxplayer.pro.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCutterScreen(
    mediaId: Long,
    onBackClick: () -> Unit,
    viewModel: VideoCutterViewModel = hiltViewModel()
) {
    val media by viewModel.media.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val trimProgress by viewModel.trimProgress.collectAsStateWithLifecycle()
    val exportSuccess by viewModel.exportSuccess.collectAsStateWithLifecycle()

    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(10000L) }

    LaunchedEffect(mediaId) {
        viewModel.loadMedia(mediaId)
    }

    LaunchedEffect(media) {
        media?.let {
            startMs = 0L
            endMs = it.durationMs
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video Cutter", color = NovaTextPrimary) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            media?.let { m ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = m.displayName, style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total Duration: ${TimeUtils.formatDuration(m.durationMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NovaTextSecondary
                        )
                    }
                }

                // Range Sliders
                Card(
                    colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Trim Boundaries", style = MaterialTheme.typography.titleSmall, color = NovaElectricCyan)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Start Slider
                        Text("Start: ${TimeUtils.formatDuration(startMs)}", color = NovaTextPrimary)
                        Slider(
                            value = if (m.durationMs > 0) (startMs.toFloat() / m.durationMs.toFloat()) else 0f,
                            onValueChange = { frac ->
                                val target = (frac * m.durationMs).toLong()
                                if (target < endMs - 1000) startMs = target
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = NovaElectricCyan,
                                activeTrackColor = NovaElectricCyan
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // End Slider
                        Text("End: ${TimeUtils.formatDuration(endMs)}", color = NovaTextPrimary)
                        Slider(
                            value = if (m.durationMs > 0) (endMs.toFloat() / m.durationMs.toFloat()) else 1f,
                            onValueChange = { frac ->
                                val target = (frac * m.durationMs).toLong()
                                if (target > startMs + 1000) endMs = target
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = NovaElectricViolet,
                                activeTrackColor = NovaElectricViolet
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Trimmed Length: ${TimeUtils.formatDuration(endMs - startMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NovaTextSecondary
                        )
                    }
                }

                if (isExporting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = trimProgress?.progress ?: 0f,
                            color = NovaElectricCyan,
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Exporting lossless segment... ${((trimProgress?.progress ?: 0f) * 100).toInt()}%",
                            color = NovaElectricCyan
                        )
                    }
                } else if (exportSuccess != null) {
                    Surface(
                        color = NovaAccentEmerald.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Successfully saved to Movies/NOVA_Cuts!\nPath: $exportSuccess",
                            color = NovaAccentEmerald,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = { viewModel.startTrim(startMs, endMs) },
                    enabled = !isExporting && (endMs > startMs),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NovaElectricCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Trimmed Video", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
