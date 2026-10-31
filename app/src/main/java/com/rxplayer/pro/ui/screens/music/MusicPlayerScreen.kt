package com.rxplayer.pro.ui.screens.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rxplayer.pro.core.common.TimeUtils
import com.rxplayer.pro.core.theme.*
import com.rxplayer.pro.ui.components.MediaThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    onBackClick: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    viewModel: MusicViewModel = hiltViewModel()
) {
    val playerManager = viewModel.playerManager
    val currentMedia by playerManager.currentMedia.collectAsStateWithLifecycle()
    val isPlaying by playerManager.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by playerManager.currentPosition.collectAsStateWithLifecycle()
    val durationMs by playerManager.duration.collectAsStateWithLifecycle()

    var isRepeatOne by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(false) }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var pendingSeekMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(currentPositionMs) {
        pendingSeekMs?.let { target ->
            if (kotlin.math.abs(currentPositionMs - target) < 1500L) {
                pendingSeekMs = null
            }
        }
    }

    LaunchedEffect(pendingSeekMs) {
        if (pendingSeekMs != null) {
            kotlinx.coroutines.delay(1200)
            pendingSeekMs = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing", color = NovaTextPrimary, textAlign = TextAlign.Center) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", tint = NovaTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEqualizer) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Equalizer", tint = NovaElectricCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = NovaAmoledBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Album Artwork Card
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(NovaDarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (currentMedia != null) {
                    MediaThumbnail(
                        media = currentMedia!!,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        iconSize = 80.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = NovaElectricCyan,
                        modifier = Modifier.size(100.dp)
                    )
                }
            }

            // Track details
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentMedia?.title ?: "No Track Selected",
                    style = MaterialTheme.typography.headlineSmall,
                    color = NovaTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentMedia?.artist?.ifEmpty { "Unknown Artist" } ?: "Select a song to start listening",
                    style = MaterialTheme.typography.bodyLarge,
                    color = NovaTextSecondary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentMedia?.album?.ifEmpty { "" } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaTextTertiary,
                    maxLines = 1
                )
            }

            // Progress Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                val effectivePos = when {
                    isDraggingSlider -> (dragFraction * durationMs).toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
                    pendingSeekMs != null -> pendingSeekMs!!
                    else -> currentPositionMs
                }
                val sliderValue = when {
                    isDraggingSlider -> dragFraction
                    durationMs > 0 -> (effectivePos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    else -> 0f
                }

                Slider(
                    value = sliderValue,
                    onValueChange = { frac ->
                        isDraggingSlider = true
                        dragFraction = frac
                    },
                    onValueChangeFinished = {
                        val target = (dragFraction * durationMs).toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
                        pendingSeekMs = target
                        isDraggingSlider = false
                        playerManager.seekTo(target)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = NovaElectricCyan,
                        activeTrackColor = NovaElectricCyan,
                        inactiveTrackColor = NovaDivider
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = TimeUtils.formatDuration(effectivePos), color = NovaTextSecondary, fontSize = 12.sp)
                    Text(text = TimeUtils.formatDuration(durationMs), color = NovaTextSecondary, fontSize = 12.sp)
                }
            }

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isShuffle = !isShuffle }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) NovaElectricCyan else NovaTextSecondary
                    )
                }

                IconButton(
                    onClick = { playerManager.rewind10Seconds() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = NovaTextPrimary, modifier = Modifier.size(36.dp))
                }

                IconButton(
                    onClick = { playerManager.togglePlayPause() },
                    modifier = Modifier
                        .size(68.dp)
                        .background(NovaElectricCyan, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = { playerManager.forward10Seconds() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = NovaTextPrimary, modifier = Modifier.size(36.dp))
                }

                IconButton(onClick = { isRepeatOne = !isRepeatOne }) {
                    Icon(
                        imageVector = if (isRepeatOne) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeatOne) NovaElectricCyan else NovaTextSecondary
                    )
                }
            }
        }
    }
}
