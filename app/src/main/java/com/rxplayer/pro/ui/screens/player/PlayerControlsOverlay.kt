package com.rxplayer.pro.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rxplayer.pro.core.common.TimeUtils
import com.rxplayer.pro.core.media.decoder.DecoderMode
import com.rxplayer.pro.core.media.player.AspectRatioMode
import com.rxplayer.pro.core.theme.NovaAmoledBackground
import com.rxplayer.pro.core.theme.NovaElectricCyan
import com.rxplayer.pro.core.theme.NovaElectricViolet
import kotlinx.coroutines.delay

@Composable
fun PlayerControlsOverlay(
    visible: Boolean,
    title: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    playbackSpeed: Float,
    decoderMode: DecoderMode,
    aspectRatio: AspectRatioMode,
    isLocked: Boolean,
    isGestureActive: Boolean = false,
    onLockToggle: () -> Unit,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onRewindClick: () -> Unit,
    onForwardClick: () -> Unit,
    onPreviousVideoClick: () -> Unit = {},
    onNextVideoClick: () -> Unit = {},
    hasPreviousVideo: Boolean = false,
    hasNextVideo: Boolean = false,
    onSeek: (Long) -> Unit,
    onSeekStarted: () -> Unit = {},
    onSeekFinished: () -> Unit = {},
    onUserInteraction: () -> Unit = {},
    onSpeedClick: () -> Unit,
    onDecoderClick: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onRotateClick: () -> Unit,
    onSubtitlesClick: () -> Unit,
    onAudioTrackClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onPipClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var pendingSeekMs by remember { mutableStateOf<Long?>(null) }
    var showRemainingTime by rememberSaveable { mutableStateOf(false) }

    // When the real playback position catches up to our seek target, release the lock
    LaunchedEffect(currentPositionMs) {
        pendingSeekMs?.let { target ->
            if (kotlin.math.abs(currentPositionMs - target) < 800L) {
                pendingSeekMs = null
            }
        }
    }

    // Fallback release in case video is paused or near the end
    LaunchedEffect(pendingSeekMs) {
        if (pendingSeekMs != null) {
            delay(900)
            pendingSeekMs = null
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.75f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (!isLocked) {
                    // Decoder Mode Pill (HW / SW / Auto)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onDecoderClick),
                        color = NovaElectricCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = decoderMode.name,
                            color = NovaElectricCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(onClick = onAudioTrackClick) {
                        Icon(Icons.Default.Audiotrack, contentDescription = "Audio Track", tint = Color.White)
                    }

                    IconButton(onClick = onSubtitlesClick) {
                        Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                    }

                    IconButton(onClick = onEqualizerClick) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Equalizer", tint = Color.White)
                    }

                    IconButton(onClick = onInfoClick) {
                        Icon(Icons.Default.Info, contentDescription = "Video Details", tint = Color.White)
                    }
                }
            }

            // Center Actions
            if (isBuffering) {
                CircularProgressIndicator(
                    color = NovaElectricCyan,
                    modifier = Modifier
                        .size(54.dp)
                        .align(Alignment.Center)
                )
            } else {
                AnimatedVisibility(
                    visible = !isGestureActive,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isLocked) {
                            // Previous Video Button
                            IconButton(
                                onClick = {
                                    onPreviousVideoClick()
                                    onUserInteraction()
                                },
                                enabled = hasPreviousVideo,
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        if (hasPreviousVideo) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous Video",
                                    tint = if (hasPreviousVideo) Color.White else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Rewind 10s Button
                            IconButton(
                                onClick = {
                                    onRewindClick()
                                    onUserInteraction()
                                },
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Replay10,
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        // Play / Pause Button
                        IconButton(
                            onClick = {
                                if (isLocked) onLockToggle() else onPlayPauseClick()
                                onUserInteraction()
                            },
                            modifier = Modifier
                                .size(70.dp)
                                .background(
                                    if (isLocked) NovaElectricViolet.copy(alpha = 0.85f) else NovaElectricCyan.copy(alpha = 0.95f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        if (!isLocked) {
                            // Forward 10s Button
                            IconButton(
                                onClick = {
                                    onForwardClick()
                                    onUserInteraction()
                                },
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Forward10,
                                    contentDescription = "Forward 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            // Next Video Button
                            IconButton(
                                onClick = {
                                    onNextVideoClick()
                                    onUserInteraction()
                                },
                                enabled = hasNextVideo,
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        if (hasNextVideo) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Video",
                                    tint = if (hasNextVideo) Color.White else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Left Lock Button
            IconButton(
                onClick = onLockToggle,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Lock",
                    tint = if (isLocked) NovaElectricViolet else Color.White
                )
            }

            // Bottom Controls
            if (!isLocked) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            onUserInteraction()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val effectivePositionMs = when {
                        isDraggingSlider -> (dragFraction * durationMs).toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
                        pendingSeekMs != null -> pendingSeekMs!!
                        else -> currentPositionMs
                    }
                    val remainingMs = (durationMs - effectivePositionMs).coerceAtLeast(0L)
                    val sliderFraction = when {
                        isDraggingSlider -> dragFraction
                        durationMs > 0 -> (effectivePositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        else -> 0f
                    }

                    // Floating Scrub Tooltip
                    AnimatedVisibility(
                        visible = isDraggingSlider,
                        enter = fadeIn() + scaleIn(initialScale = 0.85f),
                        exit = fadeOut() + scaleOut(targetScale = 0.85f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NovaElectricCyan.copy(alpha = 0.6f)),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            val tooltipText = if (showRemainingTime) {
                                "${TimeUtils.formatDuration(effectivePositionMs)} / -${TimeUtils.formatDuration(remainingMs)}"
                            } else {
                                "${TimeUtils.formatDuration(effectivePositionMs)} / ${TimeUtils.formatDuration(durationMs)}"
                            }
                            Text(
                                text = tooltipText,
                                color = NovaElectricCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current position time (clickable toggle)
                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    showRemainingTime = !showRemainingTime
                                    onUserInteraction()
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = TimeUtils.formatDuration(effectivePositionMs),
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isDraggingSlider) FontWeight.Bold else FontWeight.Medium
                            )
                        }

                        Slider(
                            value = sliderFraction,
                            onValueChange = { frac ->
                                if (!isDraggingSlider) {
                                    isDraggingSlider = true
                                    onSeekStarted()
                                }
                                dragFraction = frac
                                onUserInteraction()
                            },
                            onValueChangeFinished = {
                                val targetMs = (dragFraction * durationMs).toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
                                pendingSeekMs = targetMs
                                isDraggingSlider = false
                                onSeek(targetMs)
                                onSeekFinished()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = NovaElectricCyan,
                                activeTrackColor = NovaElectricCyan,
                                inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                            )
                        )

                        // Duration / Remaining time (clickable toggle between Total and Remaining Time)
                        Surface(
                            color = if (showRemainingTime) NovaElectricCyan.copy(alpha = 0.18f) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    showRemainingTime = !showRemainingTime
                                    onUserInteraction()
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (showRemainingTime) {
                                    "-${TimeUtils.formatDuration(remainingMs)}"
                                } else {
                                    TimeUtils.formatDuration(durationMs)
                                },
                                color = if (showRemainingTime) NovaElectricCyan else Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (showRemainingTime) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Bottom Row Tools (Speed, Aspect Ratio, Rotation, PiP)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speed selector pill
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onSpeedClick),
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${playbackSpeed}x",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Aspect ratio pill
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onAspectRatioClick),
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = aspectRatio.displayName,
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Rotate screen
                        IconButton(onClick = onRotateClick) {
                            Icon(Icons.Default.ScreenRotation, contentDescription = "Rotate", tint = Color.White)
                        }

                        // Picture in Picture
                        IconButton(onClick = onPipClick) {
                            Icon(Icons.Default.PictureInPictureAlt, contentDescription = "PiP", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
