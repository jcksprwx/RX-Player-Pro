package com.rxplayer.pro.ui.screens.audioextract

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rxplayer.pro.core.common.FileUtils
import com.rxplayer.pro.core.common.TimeUtils
import com.rxplayer.pro.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioExtractScreen(
    mediaId: Long,
    onBackClick: () -> Unit,
    viewModel: AudioExtractViewModel = hiltViewModel()
) {
    val media by viewModel.media.collectAsStateWithLifecycle()
    val isExtracting by viewModel.isExtracting.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val exportSuccess by viewModel.exportSuccess.collectAsStateWithLifecycle()

    var outputFileName by remember { mutableStateOf("") }

    LaunchedEffect(mediaId) {
        viewModel.loadMedia(mediaId)
    }

    LaunchedEffect(media) {
        media?.let {
            outputFileName = FileUtils.getFileNameWithoutExtension(it.displayName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extract Audio", color = NovaTextPrimary) },
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
                            text = "Duration: ${TimeUtils.formatDuration(m.durationMs)} • Size: ${FileUtils.formatFileSize(m.sizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NovaTextSecondary
                        )
                    }
                }

                OutlinedTextField(
                    value = outputFileName,
                    onValueChange = { outputFileName = it },
                    label = { Text("Output Audio Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaElectricCyan,
                        unfocusedBorderColor = NovaDivider,
                        focusedTextColor = NovaTextPrimary,
                        unfocusedTextColor = NovaTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Output Format: High-Fidelity AAC / M4A (Direct Stream Demux)",
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaTextSecondary
                )

                if (isExtracting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = progress,
                            color = NovaAccentAmber,
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Extracting audio track... ${(progress * 100).toInt()}%",
                            color = NovaAccentAmber
                        )
                    }
                } else if (exportSuccess != null) {
                    Surface(
                        color = NovaAccentEmerald.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Extracted to Music/NOVA_Audio!\nPath: $exportSuccess",
                            color = NovaAccentEmerald,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = { viewModel.startExtraction(outputFileName) },
                    enabled = !isExtracting && outputFileName.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NovaAccentAmber),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extract Audio Stream", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
