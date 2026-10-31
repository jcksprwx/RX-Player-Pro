package com.rxplayer.pro.ui.screens.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rxplayer.pro.core.common.TimeUtils
import com.rxplayer.pro.core.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class GestureType {
    NONE, BRIGHTNESS, VOLUME, SEEK, ZOOM
}

@Composable
fun GestureOverlay(
    modifier: Modifier = Modifier,
    isLocked: Boolean,
    controlsVisible: Boolean = false,
    onSingleTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onDoubleTapCenter: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    onGestureActiveChanged: (Boolean) -> Unit = {},
    onUserInteraction: () -> Unit = {},
    currentPositionMs: Long,
    durationMs: Long,
    onSeekConfirm: (Long) -> Unit,
    zoomScale: Float,
    onZoomChange: (Float, Float, Float) -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val activity = context as? Activity

    // Use rememberUpdatedState so pointerInput is never cancelled during playback ticks!
    val currentPositionState by rememberUpdatedState(currentPositionMs)
    val durationState by rememberUpdatedState(durationMs)
    val isLockedState by rememberUpdatedState(isLocked)
    val controlsVisibleState by rememberUpdatedState(controlsVisible)
    val onSeekConfirmState by rememberUpdatedState(onSeekConfirm)
    val onSingleTapState by rememberUpdatedState(onSingleTap)
    val onDoubleTapLeftState by rememberUpdatedState(onDoubleTapLeft)
    val onDoubleTapRightState by rememberUpdatedState(onDoubleTapRight)
    val onDoubleTapCenterState by rememberUpdatedState(onDoubleTapCenter)
    val onLongPressStartState by rememberUpdatedState(onLongPressStart)
    val onLongPressEndState by rememberUpdatedState(onLongPressEnd)
    val onGestureActiveChangedState by rememberUpdatedState(onGestureActiveChanged)
    val onUserInteractionState by rememberUpdatedState(onUserInteraction)

    val coroutineScope = rememberCoroutineScope()

    var gestureType by remember { mutableStateOf(GestureType.NONE) }
    val initialBrightness = remember {
        try {
            val sysBright = android.provider.Settings.System.getInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS
            )
            (sysBright / 255f).coerceIn(0.01f, 1.0f)
        } catch (_: Throwable) {
            0.5f
        }
    }
    var brightnessValue by remember { mutableFloatStateOf(initialBrightness) }
    val maxStreamVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val curStreamVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    var volumePercent by remember {
        mutableIntStateOf(((curStreamVol.toFloat() / maxStreamVol.toFloat()) * 100).toInt())
    }
    var seekTargetMs by remember { mutableLongStateOf(0L) }
    var seekDeltaSeconds by remember { mutableLongStateOf(0L) }
    var showIndicator by remember { mutableStateOf(false) }

    // Tap tracking for double-tap detection & screen indicators
    var pendingSingleTapJob by remember { mutableStateOf<Job?>(null) }
    var indicatorDismissJob by remember { mutableStateOf<Job?>(null) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapX by remember { mutableFloatStateOf(0f) }
    var showDoubleTapForward by remember { mutableStateOf(false) }
    var showDoubleTapRewind by remember { mutableStateOf(false) }
    var doubleTapForwardSeconds by remember { mutableIntStateOf(10) }
    var doubleTapRewindSeconds by remember { mutableIntStateOf(10) }
    var lastDoubleTapTime by remember { mutableLongStateOf(0L) }

    fun showHudIndicator(type: GestureType) {
        indicatorDismissJob?.cancel()
        indicatorDismissJob = null
        gestureType = type
        showIndicator = true
    }

    fun scheduleHudDismissal(delayMs: Long = 1000L) {
        indicatorDismissJob?.cancel()
        indicatorDismissJob = coroutineScope.launch {
            delay(delayMs)
            showIndicator = false
            gestureType = GestureType.NONE
        }
    }

    // Auto-dismiss double tap indicators
    LaunchedEffect(showDoubleTapForward, doubleTapForwardSeconds) {
        if (showDoubleTapForward) {
            delay(650)
            showDoubleTapForward = false
            doubleTapForwardSeconds = 10
        }
    }

    LaunchedEffect(showDoubleTapRewind, doubleTapRewindSeconds) {
        if (showDoubleTapRewind) {
            delay(650)
            showDoubleTapRewind = false
            doubleTapRewindSeconds = 10
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            indicatorDismissJob?.cancel()
            pendingSingleTapJob?.cancel()
            onGestureActiveChangedState(false)
            activity?.let { act ->
                val lp = act.window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                act.window.attributes = lp
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Pinch-to-zoom multi-touch detector
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (!isLockedState) {
                        if (zoom != 1f || zoomScale > 1.05f) {
                            onZoomChange(zoom, pan.x, pan.y)
                        }
                    }
                }
            }
            // Master Gesture Detector (Seek, Volume, Brightness, Taps) - Never re-keys!
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    val downX = down.position.x
                    val downY = down.position.y
                    val isLeftSide = downX < (size.width / 2f)
                    val startPosition = currentPositionState
                    val duration = durationState.coerceAtLeast(1L)
                    val dragTouchSlop = viewConfiguration.touchSlop

                    // Guard against accidental seeking during system edge gestures:
                    // Top edge = Status bar pull-down (54dp)
                    // Bottom edge = Navigation bar / home swipe (54dp)
                    // Side edges = Back gesture margin (28dp)
                    val topEdgeMarginPx = 54.dp.toPx()
                    val bottomEdgeMarginPx = 54.dp.toPx()
                    val sideEdgeMarginPx = 28.dp.toPx()
                    val isSystemGestureZone = downY < topEdgeMarginPx ||
                            downY > (size.height - bottomEdgeMarginPx) ||
                            downX < sideEdgeMarginPx ||
                            downX > (size.width - sideEdgeMarginPx)

                    var totalDragX = 0f
                    var totalDragY = 0f
                    var isDragging = false
                    var activeGesture = GestureType.NONE
                    var isLongPressing = false
                    var wasConsumedByChild = false

                    // Determine adaptive seek range based on video length
                    val seekRangeSeconds = when {
                        duration < 120_000L -> 60f // Under 2 mins: full width = 60s
                        duration < 600_000L -> 120f // Under 10 mins: full width = 120s
                        else -> ((duration / 1000f) * 0.15f).coerceIn(180f, 600f) // Movies: up to 10 mins
                    }

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.isConsumed) {
                            wasConsumedByChild = true
                        }

                        // If user uses 2 or more fingers, let zoom handler take over
                        if (event.changes.size > 1) {
                            if (isDragging) {
                                isDragging = false
                                onGestureActiveChangedState(false)
                                showIndicator = false
                            }
                            break
                        }

                        if (change.pressed) {
                            val dx = change.position.x - change.previousPosition.x
                            val dy = change.position.y - change.previousPosition.y
                            totalDragX += dx
                            totalDragY += dy

                            // Initiate seek, volume, brightness, or long-press if outside system margins and unconsumed
                            if (!isLockedState && !isDragging && !isSystemGestureZone) {
                                if (!change.isConsumed) {
                                    if (abs(totalDragX) > dragTouchSlop && abs(totalDragX) > abs(totalDragY) * 1.25f) {
                                        isDragging = true
                                        activeGesture = GestureType.SEEK
                                        showHudIndicator(GestureType.SEEK)
                                        onGestureActiveChangedState(true)
                                        onUserInteractionState()
                                    } else if (abs(totalDragY) > dragTouchSlop && abs(totalDragY) > abs(totalDragX) * 1.25f) {
                                        isDragging = true
                                        activeGesture = if (isLeftSide) GestureType.BRIGHTNESS else GestureType.VOLUME
                                        showHudIndicator(activeGesture)
                                        onGestureActiveChangedState(true)
                                        onUserInteractionState()
                                    } else if (!isDragging && System.currentTimeMillis() - downTime > 500L && abs(totalDragX) < dragTouchSlop && abs(totalDragY) < dragTouchSlop) {
                                        if (!isLongPressing) {
                                            isLongPressing = true
                                            onLongPressStartState()
                                        }
                                    }
                                }
                            }

                            if (isDragging) {
                                change.consume()
                                showHudIndicator(activeGesture)
                                onUserInteractionState()
                                when (activeGesture) {
                                    GestureType.SEEK -> {
                                        // Silky-smooth scrubbing: Calculate target time smoothly
                                        val dragFraction = (totalDragX / size.width.toFloat()).coerceIn(-1.5f, 1.5f)
                                        val seekSeconds = (dragFraction * seekRangeSeconds).toLong()
                                        seekDeltaSeconds = seekSeconds
                                        seekTargetMs = (startPosition + (seekSeconds * 1000L)).coerceIn(0L, duration)
                                    }
                                    GestureType.BRIGHTNESS -> {
                                        activity?.let { act ->
                                            val lp = act.window.attributes
                                            val curBrightness = (brightnessValue - (dy / 350f)).coerceIn(0.01f, 1.0f)
                                            lp.screenBrightness = curBrightness
                                            act.window.attributes = lp
                                            brightnessValue = curBrightness
                                        }
                                    }
                                    GestureType.VOLUME -> {
                                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        val step = if (dy < 0) 1 else -1
                                        if (abs(totalDragY) > 25f) {
                                            val newVol = (curVol + step).coerceIn(0, maxVol)
                                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                            volumePercent = ((newVol.toFloat() / maxVol.toFloat()) * 100).toInt()
                                            totalDragY = 0f
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    // Gesture Released / Finger Lifted
                    if (isLongPressing) {
                        onLongPressEndState()
                    }

                    if (isDragging) {
                        if (activeGesture == GestureType.SEEK) {
                            // Apply smooth seek directly to the target frame!
                            onSeekConfirmState(seekTargetMs)
                        }
                        // Always auto-dismiss the volume/brightness/seek HUD dialog after lifting finger!
                        scheduleHudDismissal(1000L)
                        onGestureActiveChangedState(false)
                    } else {
                        // Dismiss any active HUD immediately on tap
                        if (showIndicator) {
                            scheduleHudDismissal(150L)
                        }
                        // It was a tap!
                        val tapDuration = System.currentTimeMillis() - downTime
                        if (!isSystemGestureZone && !wasConsumedByChild && tapDuration < 350L && abs(totalDragX) < dragTouchSlop && abs(totalDragY) < dragTouchSlop) {
                            val now = System.currentTimeMillis()
                            val isDoubleTap = (now - lastTapTime < 320L && abs(downX - lastTapX) < 180f)

                            if (isDoubleTap) {
                                // Cancel pending single tap to completely eliminate controls flickering!
                                pendingSingleTapJob?.cancel()
                                pendingSingleTapJob = null
                                lastTapTime = 0L

                                if (!isLockedState) {
                                    val third = size.width / 3f
                                    when {
                                        downX < third -> {
                                            if (now - lastDoubleTapTime < 650L && showDoubleTapRewind) {
                                                doubleTapRewindSeconds += 10
                                            } else {
                                                doubleTapRewindSeconds = 10
                                            }
                                            lastDoubleTapTime = now
                                            showDoubleTapRewind = true
                                            onDoubleTapLeftState()
                                        }
                                        downX > third * 2 -> {
                                            if (now - lastDoubleTapTime < 650L && showDoubleTapForward) {
                                                doubleTapForwardSeconds += 10
                                            } else {
                                                doubleTapForwardSeconds = 10
                                            }
                                            lastDoubleTapTime = now
                                            showDoubleTapForward = true
                                            onDoubleTapRightState()
                                        }
                                        else -> {
                                            onDoubleTapCenterState()
                                        }
                                    }
                                }
                            } else {
                                if (controlsVisibleState) {
                                    // When controls are visible, tap on open area dismisses them immediately
                                    onSingleTapState()
                                } else {
                                    // First tap: register position and time, and wait 260ms before triggering single tap
                                    lastTapTime = now
                                    lastTapX = downX
                                    pendingSingleTapJob?.cancel()
                                    pendingSingleTapJob = coroutineScope.launch {
                                        delay(260)
                                        onSingleTapState()
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        content()

        // Center on-screen HUD gesture indicators (Volume, Brightness, Seek)
        AnimatedVisibility(
            visible = showIndicator && gestureType != GestureType.NONE,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(150)),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(250)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.88f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NovaElectricCyan.copy(alpha = 0.3f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (gestureType) {
                        GestureType.VOLUME -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (volumePercent == 0) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = NovaElectricCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Volume $volumePercent%",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { (volumePercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(6.dp),
                                color = NovaElectricCyan,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                        }
                        GestureType.BRIGHTNESS -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BrightnessMedium,
                                    contentDescription = null,
                                    tint = NovaAccentAmber,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Brightness ${(brightnessValue * 100).toInt()}%",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { brightnessValue.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(6.dp),
                                color = NovaAccentAmber,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                        }
                        GestureType.SEEK -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (seekDeltaSeconds >= 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                                    contentDescription = null,
                                    tint = NovaElectricCyan,
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = TimeUtils.formatDuration(seekTargetMs),
                                            color = NovaElectricCyan,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                        Text(
                                            text = " / ${TimeUtils.formatDuration(durationMs)}",
                                            color = Color.White.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Text(
                                        text = "${if (seekDeltaSeconds >= 0) "+" else ""}${seekDeltaSeconds}s",
                                        color = if (seekDeltaSeconds >= 0) NovaAccentEmerald else NovaAccentRose,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }
                            if (durationMs > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { (seekTargetMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .width(220.dp)
                                        .height(6.dp),
                                    color = NovaElectricCyan,
                                    trackColor = Color.White.copy(alpha = 0.25f)
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        // Animated Double-Tap Indicator (Left - Rewind)
        AnimatedVisibility(
            visible = showDoubleTapRewind,
            enter = fadeIn() + scaleIn(initialScale = 0.75f),
            exit = fadeOut() + scaleOut(targetScale = 0.75f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NovaElectricCyan.copy(alpha = 0.75f)),
                modifier = Modifier.size(88.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Default.FastRewind,
                        contentDescription = "Rewind",
                        tint = NovaElectricCyan,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "-${doubleTapRewindSeconds}s",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Animated Double-Tap Indicator (Right - Forward)
        AnimatedVisibility(
            visible = showDoubleTapForward,
            enter = fadeIn() + scaleIn(initialScale = 0.75f),
            exit = fadeOut() + scaleOut(targetScale = 0.75f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NovaElectricCyan.copy(alpha = 0.75f)),
                modifier = Modifier.size(88.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Default.FastForward,
                        contentDescription = "Forward",
                        tint = NovaElectricCyan,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "+${doubleTapForwardSeconds}s",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
