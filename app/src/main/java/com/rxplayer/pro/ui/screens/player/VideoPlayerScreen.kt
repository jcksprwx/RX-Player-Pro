package com.rxplayer.pro.ui.screens.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import com.rxplayer.pro.core.media.decoder.DecoderMode
import com.rxplayer.pro.core.media.player.AspectRatioMode
import com.rxplayer.pro.core.media.player.NovaPlayerManager
import com.rxplayer.pro.core.media.player.TrackInfo
import com.rxplayer.pro.core.security.SubtitleConfig
import com.rxplayer.pro.core.theme.*
import com.rxplayer.pro.ui.screens.videoinfo.VideoInfoDialog
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    mediaId: Long,
    onBackClick: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val playerManager = viewModel.playerManager
    val isPlaying by playerManager.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by playerManager.isBuffering.collectAsStateWithLifecycle()
    val currentPositionMs by playerManager.currentPosition.collectAsStateWithLifecycle()
    val durationMs by playerManager.duration.collectAsStateWithLifecycle()
    val bufferedPositionMs by playerManager.bufferedPosition.collectAsStateWithLifecycle()
    val playbackSpeed by playerManager.playbackSpeed.collectAsStateWithLifecycle()
    val decoderMode by playerManager.decoderMode.collectAsStateWithLifecycle()
    val aspectRatio by playerManager.aspectRatio.collectAsStateWithLifecycle()
    val rotationDegrees by playerManager.rotationDegrees.collectAsStateWithLifecycle()
    val subtitleTracks by playerManager.subtitleTracks.collectAsStateWithLifecycle()
    val audioTracks by playerManager.audioTracks.collectAsStateWithLifecycle()
    val currentMedia by playerManager.currentMedia.collectAsStateWithLifecycle()
    val videoSize by playerManager.videoSize.collectAsStateWithLifecycle()
    val hasNextVideo by playerManager.hasNextVideo.collectAsStateWithLifecycle()
    val hasPreviousVideo by playerManager.hasPreviousVideo.collectAsStateWithLifecycle()

    val secureStorage = viewModel.secureStorage
    val useAsAudioPlayer by secureStorage.useAsAudioPlayerFlow.collectAsStateWithLifecycle()
    val subtitleConfig by secureStorage.subtitleConfigFlow.collectAsStateWithLifecycle()

    val handleExit = {
        activity?.let { act ->
            val lp = act.window.attributes
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            act.window.attributes = lp
        }
        if (!useAsAudioPlayer) {
            playerManager.pause()
        }
        onBackClick()
    }

    BackHandler {
        handleExit()
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }

    // Auto-rotate for landscape video, and do not auto-rotate for portrait video
    LaunchedEffect(videoSize, currentMedia) {
        val width = if (videoSize.width > 0) videoSize.width else (currentMedia?.width ?: 0)
        val height = if (videoSize.height > 0) videoSize.height else (currentMedia?.height ?: 0)

        if (width > 0 && height > 0) {
            if (width > height) {
                // Landscape video: automatically rotate to landscape
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                // Portrait video: stay in portrait (do not auto-rotate)
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    // Reset orientation and brightness on exiting the player screen
    DisposableEffect(activity) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.let { act ->
                val lp = act.window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                act.window.attributes = lp
            }
        }
    }

    // Immersive Fullscreen: Hide Status Bar and Navigation Bar while in player
    val window = activity?.window
    DisposableEffect(activity, window) {
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Zoom & Pan state
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    // Dialogs
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showDecoderDialog by remember { mutableStateOf(false) }
    var showAspectRatioDialog by remember { mutableStateOf(false) }
    var showSubtitlesDialog by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Load media on start
    LaunchedEffect(mediaId) {
        viewModel.loadAndPlay(mediaId)
    }

    // MX Player style start/playback change notification banner
    var showTitleBanner by remember { mutableStateOf(false) }
    LaunchedEffect(currentMedia?.id) {
        if (currentMedia != null) {
            showTitleBanner = true
            delay(2800)
            showTitleBanner = false
        }
    }

    // Auto-hide controls timeout - Resets on user interaction, never hides while actively dragging the seekbar or gesturing!
    var isUserSeeking by remember { mutableStateOf(false) }
    var isGestureActive by remember { mutableStateOf(false) }
    var userInteractionTick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(controlsVisible, isPlaying, userInteractionTick, isUserSeeking, isGestureActive) {
        if (controlsVisible && isPlaying && !isLocked && !isUserSeeking && !isGestureActive) {
            delay(4500)
            controlsVisible = false
        }
    }

    // External Subtitle Picker
    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { playerManager.loadExternalSubtitle(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GestureOverlay(
            modifier = Modifier.fillMaxSize(),
            isLocked = isLocked,
            controlsVisible = controlsVisible,
            onSingleTap = { controlsVisible = !controlsVisible },
            onDoubleTapLeft = { playerManager.rewind10Seconds() },
            onDoubleTapRight = { playerManager.forward10Seconds() },
            onDoubleTapCenter = { playerManager.togglePlayPause() },
            onLongPressStart = { playerManager.setPlaybackSpeed(2.0f) },
            onLongPressEnd = { playerManager.setPlaybackSpeed(1.0f) },
            onGestureActiveChanged = { active ->
                isGestureActive = active
                if (active) {
                    userInteractionTick = System.currentTimeMillis()
                }
            },
            onUserInteraction = {
                userInteractionTick = System.currentTimeMillis()
            },
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            onSeekConfirm = { targetMs -> playerManager.seekTo(targetMs) },
            zoomScale = zoomScale,
            onZoomChange = { zoom, dx, dy ->
                zoomScale = (zoomScale * zoom).coerceIn(1.0f, 4.0f)
                if (zoomScale > 1.0f) {
                    panX += dx
                    panY += dy
                } else {
                    panX = 0f
                    panY = 0f
                }
            }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = playerManager.getPlayer()
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        resizeMode = when (aspectRatio) {
                            AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatioMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            AspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                            AspectRatioMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                            AspectRatioMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        subtitleView?.apply {
                            val style = CaptionStyleCompat(
                                subtitleConfig.textColorArgb.toInt(),
                                subtitleConfig.bgColorArgb.toInt(),
                                android.graphics.Color.TRANSPARENT,
                                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                android.graphics.Color.BLACK,
                                null
                            )
                            setStyle(style)
                            setFractionalTextSize(subtitleConfig.textSizeSp / 400f)
                            setBottomPaddingFraction(subtitleConfig.positionFraction)
                        }
                    }
                },
                update = { view ->
                    view.player = playerManager.getPlayer()
                    view.resizeMode = when (aspectRatio) {
                        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatioMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                        AspectRatioMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                        AspectRatioMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    view.subtitleView?.apply {
                        val style = CaptionStyleCompat(
                            subtitleConfig.textColorArgb.toInt(),
                            subtitleConfig.bgColorArgb.toInt(),
                            android.graphics.Color.TRANSPARENT,
                            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                            android.graphics.Color.BLACK,
                            null
                        )
                        setStyle(style)
                        setFractionalTextSize(subtitleConfig.textSizeSp / 400f)
                        setBottomPaddingFraction(subtitleConfig.positionFraction)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panX
                        translationY = panY
                        rotationZ = rotationDegrees.toFloat()
                    }
            )

            PlayerControlsOverlay(
                visible = controlsVisible,
                title = currentMedia?.displayName ?: "Video Player",
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                bufferedPositionMs = bufferedPositionMs,
                playbackSpeed = playbackSpeed,
                decoderMode = decoderMode,
                aspectRatio = aspectRatio,
                isLocked = isLocked,
                isGestureActive = isGestureActive,
                onLockToggle = { isLocked = !isLocked },
                onBackClick = handleExit,
                onPlayPauseClick = { playerManager.togglePlayPause() },
                onRewindClick = { playerManager.rewind10Seconds() },
                onForwardClick = {
                    playerManager.forward10Seconds()
                    userInteractionTick = System.currentTimeMillis()
                },
                onPreviousVideoClick = {
                    playerManager.playPreviousVideo()
                    userInteractionTick = System.currentTimeMillis()
                },
                onNextVideoClick = {
                    playerManager.playNextVideo()
                    userInteractionTick = System.currentTimeMillis()
                },
                hasPreviousVideo = hasPreviousVideo,
                hasNextVideo = hasNextVideo,
                onSeek = { targetMs ->
                    playerManager.seekTo(targetMs)
                    userInteractionTick = System.currentTimeMillis()
                },
                onSeekStarted = {
                    isUserSeeking = true
                },
                onSeekFinished = {
                    isUserSeeking = false
                    userInteractionTick = System.currentTimeMillis()
                },
                onUserInteraction = {
                    userInteractionTick = System.currentTimeMillis()
                },
                onSpeedClick = { showSpeedDialog = true },
                onDecoderClick = { showDecoderDialog = true },
                onAspectRatioClick = { showAspectRatioDialog = true },
                onRotateClick = {
                    val currentOrientation = activity?.resources?.configuration?.orientation
                    if (currentOrientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
                },
                onSubtitlesClick = { showSubtitlesDialog = true },
                onAudioTrackClick = { showAudioTrackDialog = true },
                onEqualizerClick = onNavigateToEqualizer,
                onPipClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        activity?.let { act ->
                            val lp = act.window.attributes
                            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                            act.window.attributes = lp
                        }
                        val rational = Rational(16, 9)
                        val params = PictureInPictureParams.Builder()
                            .setAspectRatio(rational)
                            .build()
                        activity?.enterPictureInPictureMode(params)
                    }
                },
                onInfoClick = { showInfoDialog = true }
            )
        }

        // MX Player style sleek video start notification pill
        AnimatedVisibility(
            visible = showTitleBanner && !controlsVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, NovaElectricCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = NovaElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentMedia?.displayName ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Speed Dialog
        if (showSpeedDialog) {
            SpeedSelectionDialog(
                currentSpeed = playbackSpeed,
                onSelectSpeed = {
                    playerManager.setPlaybackSpeed(it)
                    showSpeedDialog = false
                },
                onDismiss = { showSpeedDialog = false }
            )
        }

        // Decoder Dialog
        if (showDecoderDialog) {
            DecoderSelectionDialog(
                currentMode = decoderMode,
                onSelectMode = {
                    playerManager.setDecoderMode(it)
                    showDecoderDialog = false
                },
                onDismiss = { showDecoderDialog = false }
            )
        }

        // Aspect Ratio Dialog
        if (showAspectRatioDialog) {
            AspectRatioDialog(
                currentRatio = aspectRatio,
                onSelectRatio = {
                    playerManager.setAspectRatio(it)
                    showAspectRatioDialog = false
                },
                onDismiss = { showAspectRatioDialog = false }
            )
        }

        // Subtitle Dialog
        if (showSubtitlesDialog) {
            SubtitlesDialog(
                tracks = subtitleTracks,
                subtitleConfig = subtitleConfig,
                onSelectTrack = { playerManager.selectSubtitleTrack(it) },
                onDisableSubtitles = { playerManager.disableSubtitles() },
                onLoadExternal = {
                    subtitlePickerLauncher.launch(
                        arrayOf(
                            "text/plain",
                            "application/x-subrip",
                            "application/ttml+xml",
                            "*/*"
                        )
                    )
                    showSubtitlesDialog = false
                },
                onUpdateSubtitleConfig = { newConfig ->
                    secureStorage.updateSubtitleConfig(newConfig)
                },
                onSetSubtitleDelay = { delayMs ->
                    playerManager.setSubtitleDelay(delayMs)
                    secureStorage.updateSubtitleConfig(subtitleConfig.copy(delayMs = delayMs))
                },
                onDismiss = { showSubtitlesDialog = false }
            )
        }

        // Audio Track Dialog
        if (showAudioTrackDialog) {
            AudioTrackDialog(
                tracks = audioTracks,
                onSelectTrack = {
                    playerManager.selectAudioTrack(it)
                    showAudioTrackDialog = false
                },
                onDismiss = { showAudioTrackDialog = false }
            )
        }

        // Video Info Dialog
        if (showInfoDialog && currentMedia != null) {
            val selectedAudioCodec = audioTracks.firstOrNull { it.isSelected }?.codec ?: ""
            val activeDecoderDesc = when (decoderMode) {
                DecoderMode.HW -> "HW (Hardware Accelerated)"
                DecoderMode.SW -> "SW (Software Decoder)"
                DecoderMode.AUTO -> "HW (Auto - Hardware Decoded)"
            }
            VideoInfoDialog(
                media = currentMedia!!,
                currentAudioCodec = selectedAudioCodec,
                decoderMode = activeDecoderDesc,
                onDismiss = { showInfoDialog = false }
            )
        }
    }
}

@Composable
fun SpeedSelectionDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f, 4.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Speed", color = NovaTextPrimary) },
        text = {
            LazyColumn {
                items(speeds) { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSpeed(speed) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${speed}x",
                            color = if (speed == currentSpeed) NovaElectricCyan else NovaTextPrimary,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (speed == currentSpeed) {
                            Icon(Icons.Default.Check, null, tint = NovaElectricCyan)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = NovaElectricCyan)
            }
        },
        containerColor = NovaDarkSurfaceElevated
    )
}

@Composable
fun DecoderSelectionDialog(
    currentMode: DecoderMode,
    onSelectMode: (DecoderMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Decoder Selection", color = NovaTextPrimary) },
        text = {
            Column {
                DecoderMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMode(mode) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.displayName,
                                color = if (mode == currentMode) NovaElectricCyan else NovaTextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = mode.description,
                                color = NovaTextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (mode == currentMode) {
                            Icon(Icons.Default.Check, null, tint = NovaElectricCyan)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = NovaElectricCyan)
            }
        },
        containerColor = NovaDarkSurfaceElevated
    )
}

@Composable
fun AspectRatioDialog(
    currentRatio: AspectRatioMode,
    onSelectRatio: (AspectRatioMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aspect Ratio", color = NovaTextPrimary) },
        text = {
            Column {
                AspectRatioMode.values().forEach { ratio ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRatio(ratio) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ratio.displayName,
                            color = if (ratio == currentRatio) NovaElectricCyan else NovaTextPrimary,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (ratio == currentRatio) {
                            Icon(Icons.Default.Check, null, tint = NovaElectricCyan)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = NovaElectricCyan)
            }
        },
        containerColor = NovaDarkSurfaceElevated
    )
}

@Composable
fun SubtitlesDialog(
    tracks: List<TrackInfo>,
    subtitleConfig: SubtitleConfig,
    onSelectTrack: (Int) -> Unit,
    onDisableSubtitles: () -> Unit,
    onLoadExternal: () -> Unit,
    onUpdateSubtitleConfig: (SubtitleConfig) -> Unit,
    onSetSubtitleDelay: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val anySelected = tracks.any { it.isSelected }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Subtitles, contentDescription = null, tint = NovaElectricCyan)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Subtitles & Styling", color = NovaTextPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = NovaDarkSurface,
                    contentColor = NovaElectricCyan,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Tracks", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Style & Pos", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Sync Delay", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        // Tracks Tab
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onDisableSubtitles()
                                    onDismiss()
                                }
                                .background(if (!anySelected) NovaElectricCyan.copy(alpha = 0.15f) else Color.Transparent)
                                .padding(vertical = 10.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.SubtitlesOff,
                                contentDescription = null,
                                tint = if (!anySelected) NovaElectricCyan else NovaTextSecondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Disable Subtitles",
                                color = if (!anySelected) NovaElectricCyan else NovaTextPrimary,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            if (!anySelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NovaElectricCyan)
                            }
                        }

                        HorizontalDivider(color = NovaDivider, modifier = Modifier.padding(vertical = 6.dp))

                        if (tracks.isEmpty()) {
                            Text(
                                text = "No embedded subtitle tracks detected in this video file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NovaTextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                                items(tracks) { track ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                onSelectTrack(track.index)
                                                onDismiss()
                                            }
                                            .background(if (track.isSelected) NovaElectricCyan.copy(alpha = 0.15f) else Color.Transparent)
                                            .padding(vertical = 10.dp, horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Subtitles,
                                            contentDescription = null,
                                            tint = if (track.isSelected) NovaElectricCyan else NovaTextSecondary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.name,
                                                color = if (track.isSelected) NovaElectricCyan else NovaTextPrimary,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            if (track.details.isNotEmpty()) {
                                                Text(
                                                    text = track.details,
                                                    color = NovaTextSecondary,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        if (track.isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = NovaElectricCyan)
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = NovaDivider, modifier = Modifier.padding(vertical = 6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onLoadExternal)
                                .padding(vertical = 10.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, null, tint = NovaElectricCyan)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Load External Subtitle (.srt, .vtt, .ass)",
                                color = NovaElectricCyan,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    1 -> {
                        // Style & Position Tab
                        Text("Font Size", style = MaterialTheme.typography.labelMedium, color = NovaTextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(16f to "Small", 20f to "Normal", 26f to "Large", 32f to "XL").forEach { (size, label) ->
                                FilterChip(
                                    selected = subtitleConfig.textSizeSp == size,
                                    onClick = { onUpdateSubtitleConfig(subtitleConfig.copy(textSizeSp = size)) },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Font Color", style = MaterialTheme.typography.labelMedium, color = NovaTextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                0xFFFFFFFFL to "White",
                                0xFFFFEB3BL to "Yellow",
                                0xFF00E5FFL to "Cyan",
                                0xFF00E676L to "Green"
                            ).forEach { (colorArgb, label) ->
                                FilterChip(
                                    selected = subtitleConfig.textColorArgb == colorArgb,
                                    onClick = { onUpdateSubtitleConfig(subtitleConfig.copy(textColorArgb = colorArgb)) },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Screen Position", style = MaterialTheme.typography.labelMedium, color = NovaTextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                0.08f to "Bottom",
                                0.45f to "Center",
                                0.82f to "Top"
                            ).forEach { (fraction, label) ->
                                FilterChip(
                                    selected = kotlin.math.abs(subtitleConfig.positionFraction - fraction) < 0.05f,
                                    onClick = { onUpdateSubtitleConfig(subtitleConfig.copy(positionFraction = fraction)) },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                    2 -> {
                        // Sync / Delay Tab
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Subtitle Sync Offset",
                                style = MaterialTheme.typography.titleSmall,
                                color = NovaTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${if (subtitleConfig.delayMs > 0) "+" else ""}${subtitleConfig.delayMs} ms",
                                style = MaterialTheme.typography.headlineMedium,
                                color = NovaElectricCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onSetSubtitleDelay(subtitleConfig.delayMs - 500L) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NovaDarkSurfaceVariant)
                                ) {
                                    Text("-500ms", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { onSetSubtitleDelay(subtitleConfig.delayMs - 100L) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NovaDarkSurfaceVariant)
                                ) {
                                    Text("-100ms", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { onSetSubtitleDelay(0L) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NovaElectricCyan.copy(alpha = 0.2f))
                                ) {
                                    Text("0ms", color = NovaElectricCyan, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { onSetSubtitleDelay(subtitleConfig.delayMs + 100L) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NovaDarkSurfaceVariant)
                                ) {
                                    Text("+100ms", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { onSetSubtitleDelay(subtitleConfig.delayMs + 500L) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NovaDarkSurfaceVariant)
                                ) {
                                    Text("+500ms", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = NovaElectricCyan)
            }
        },
        containerColor = NovaDarkSurfaceElevated
    )
}

@Composable
fun AudioTrackDialog(
    tracks: List<TrackInfo>,
    onSelectTrack: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Audiotrack, contentDescription = null, tint = NovaElectricCyan)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Audio Tracks", color = NovaTextPrimary)
            }
        },
        text = {
            if (tracks.isEmpty()) {
                Text(
                    text = "Default audio track is currently playing. No alternate audio streams detected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NovaTextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(tracks) { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectTrack(track.index) }
                                .background(if (track.isSelected) NovaElectricCyan.copy(alpha = 0.15f) else Color.Transparent)
                                .padding(vertical = 10.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = if (track.isSelected) NovaElectricCyan else NovaTextSecondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.name,
                                    color = if (track.isSelected) NovaElectricCyan else NovaTextPrimary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (track.details.isNotEmpty()) {
                                    Text(
                                        text = track.details,
                                        color = NovaTextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            if (track.isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NovaElectricCyan)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = NovaElectricCyan)
            }
        },
        containerColor = NovaDarkSurfaceElevated
    )
}
