package com.rxplayer.pro.core.media.player

import android.content.Context
import android.content.Intent
import android.media.MediaCodecInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import com.rxplayer.pro.core.media.service.NovaMediaSessionService
import androidx.annotation.OptIn
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.rxplayer.pro.core.database.dao.HistoryDao
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import com.rxplayer.pro.core.database.entity.PlaybackPositionEntity
import com.rxplayer.pro.core.database.entity.WatchHistoryEntity
import com.rxplayer.pro.core.media.decoder.DecoderMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class TrackInfo(
    val index: Int,
    val name: String,
    val language: String?,
    val isSelected: Boolean,
    val codec: String = "",
    val details: String = ""
)

@OptIn(UnstableApi::class)
@Singleton
class NovaPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val historyDao: HistoryDao
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var player: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _bufferedPosition = MutableStateFlow(0L)
    val bufferedPosition: StateFlow<Long> = _bufferedPosition.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatio: StateFlow<AspectRatioMode> = _aspectRatio.asStateFlow()

    private val _rotationDegrees = MutableStateFlow(0)
    val rotationDegrees: StateFlow<Int> = _rotationDegrees.asStateFlow()

    private val _decoderMode = MutableStateFlow(DecoderMode.AUTO)
    val decoderMode: StateFlow<DecoderMode> = _decoderMode.asStateFlow()

    private val _audioSessionId = MutableStateFlow(C.AUDIO_SESSION_ID_UNSET)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    private val _currentMedia = MutableStateFlow<MediaFileEntity?>(null)
    val currentMedia: StateFlow<MediaFileEntity?> = _currentMedia.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val subtitleTracks: StateFlow<List<TrackInfo>> = _subtitleTracks.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val audioTracks: StateFlow<List<TrackInfo>> = _audioTracks.asStateFlow()

    private val _subtitleDelayMs = MutableStateFlow(0L)
    val subtitleDelayMs: StateFlow<Long> = _subtitleDelayMs.asStateFlow()

    private val _videoSize = MutableStateFlow(androidx.media3.common.VideoSize.UNKNOWN)
    val videoSize: StateFlow<androidx.media3.common.VideoSize> = _videoSize.asStateFlow()

    private val _playlist = MutableStateFlow<List<MediaFileEntity>>(emptyList())
    val playlist: StateFlow<List<MediaFileEntity>> = _playlist.asStateFlow()

    private val _currentPlaylistIndex = MutableStateFlow(-1)
    val currentPlaylistIndex: StateFlow<Int> = _currentPlaylistIndex.asStateFlow()

    private val _hasNextVideo = MutableStateFlow(false)
    val hasNextVideo: StateFlow<Boolean> = _hasNextVideo.asStateFlow()

    private val _hasPreviousVideo = MutableStateFlow(false)
    val hasPreviousVideo: StateFlow<Boolean> = _hasPreviousVideo.asStateFlow()

    private var progressJob: Job? = null
    @Volatile private var _targetSeekPosition: Long? = null

    private val playerListener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
            _videoSize.value = videoSize
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                _currentPosition.value = newPosition.positionMs
                _targetSeekPosition = null
            }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateName = when(playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN"
            }
            Log.d("NovaPlayerManager", "onPlaybackStateChanged: $stateName, pos=${player?.currentPosition}, dur=${player?.duration}")
            _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
            if (playbackState == Player.STATE_READY) {
                _audioSessionId.value = player?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
                updateTrackLists()
            } else if (playbackState == Player.STATE_ENDED) {
                _currentMedia.value?.let { media ->
                    scope.launch {
                        try {
                            historyDao.insertWatchHistory(
                                WatchHistoryEntity(
                                    mediaId = media.id,
                                    lastPositionMs = _duration.value,
                                    durationMs = _duration.value,
                                    isCompleted = true
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("NovaPlayerManager", "Failed to save history: ${e.message}")
                        }
                    }
                }
                // Auto-play next video upon ending current video
                if (_hasNextVideo.value) {
                    playNextVideo()
                }
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateTrackLists()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (!isPlaying) {
                _currentMedia.value?.let { media ->
                    val pos = _currentPosition.value
                    val dur = _duration.value
                    if (pos > 0 && dur > 0) {
                        scope.launch {
                            try {
                                historyDao.insertWatchHistory(
                                    WatchHistoryEntity(
                                        mediaId = media.id,
                                        lastPositionMs = pos,
                                        durationMs = dur,
                                        isCompleted = (dur - pos < 5000)
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e("NovaPlayerManager", "Failed to insert watch history: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        initializePlayer()
    }

    fun getPlayer(): ExoPlayer {
        if (player == null) {
            initializePlayer()
        }
        return player!!
    }

    private fun initializePlayer() {
        try {
            trackSelector = DefaultTrackSelector(context).apply {
                setParameters(buildUponParameters().setPreferredTextLanguage("en"))
            }

            val renderersFactory = DefaultRenderersFactory(context).apply {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                setMediaCodecSelector(createMediaCodecSelector(_decoderMode.value))
            }

            player = ExoPlayer.Builder(context, renderersFactory)
                .setTrackSelector(trackSelector!!)
                .setSeekForwardIncrementMs(10000)
                .setSeekBackIncrementMs(10000)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                .build().apply {
                    setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    addListener(playerListener)
                }
        } catch (e: Throwable) {
            android.util.Log.e("NovaPlayerManager", "Failed to build custom ExoPlayer, falling back to default", e)
            player = ExoPlayer.Builder(context).build().apply {
                setSeekParameters(SeekParameters.CLOSEST_SYNC)
                addListener(playerListener)
            }
        }

        startProgressUpdates()
    }

    private fun createMediaCodecSelector(mode: DecoderMode): MediaCodecSelector {
        return when (mode) {
            DecoderMode.AUTO -> MediaCodecSelector.DEFAULT
            DecoderMode.HW -> MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                val decoders = try {
                    MediaCodecUtil.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
                } catch (e: Throwable) {
                    emptyList()
                }
                val hw = decoders.filter { it.hardwareAccelerated }
                if (hw.isNotEmpty()) hw else decoders
            }
            DecoderMode.SW -> MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                val decoders = try {
                    MediaCodecUtil.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
                } catch (e: Throwable) {
                    emptyList()
                }
                val swDecoders = decoders.filter { it.softwareOnly }
                if (swDecoders.isNotEmpty()) swDecoders else decoders
            }
        }
    }

    fun setDecoderMode(mode: DecoderMode) {
        if (_decoderMode.value == mode) return
        _decoderMode.value = mode
        val currentPos = player?.currentPosition ?: 0L
        val currentItem = player?.currentMediaItem
        val wasPlaying = player?.isPlaying ?: false

        player?.release()
        initializePlayer()

        if (currentItem != null) {
            player?.setMediaItem(currentItem, currentPos)
            player?.prepare()
            if (wasPlaying) player?.play()
        }
    }

    fun setPlaylist(list: List<MediaFileEntity>, startIndex: Int = 0) {
        _playlist.value = list
        val validIndex = if (list.isNotEmpty()) startIndex.coerceIn(0, list.size - 1) else -1
        _currentPlaylistIndex.value = validIndex
        updatePlaylistNavigationState()
        if (validIndex in list.indices) {
            playMedia(list[validIndex])
        }
    }

    private fun updatePlaylistNavigationState() {
        val list = _playlist.value
        val idx = _currentPlaylistIndex.value
        _hasPreviousVideo.value = idx > 0
        _hasNextVideo.value = idx >= 0 && idx < list.size - 1
    }

    fun playNextVideo(): Boolean {
        val list = _playlist.value
        val currentIdx = _currentPlaylistIndex.value
        Log.d("NovaPlayerManager", "playNextVideo: idx=$currentIdx, size=${list.size}")
        if (currentIdx in 0 until (list.size - 1)) {
            val nextIdx = currentIdx + 1
            _currentPlaylistIndex.value = nextIdx
            updatePlaylistNavigationState()
            playMedia(list[nextIdx], startPositionMs = 0L)
            return true
        }
        return false
    }

    fun playPreviousVideo(): Boolean {
        val list = _playlist.value
        val currentIdx = _currentPlaylistIndex.value
        Log.d("NovaPlayerManager", "playPreviousVideo: idx=$currentIdx, size=${list.size}")
        if (currentIdx > 0 && currentIdx < list.size) {
            val prevIdx = currentIdx - 1
            _currentPlaylistIndex.value = prevIdx
            updatePlaylistNavigationState()
            playMedia(list[prevIdx], startPositionMs = 0L)
            return true
        } else {
            seekTo(0L)
            return false
        }
    }

    fun playMedia(media: MediaFileEntity, startPositionMs: Long? = null) {
        _currentMedia.value = media

        val foundIdx = _playlist.value.indexOfFirst { it.id == media.id }
        Log.d("NovaPlayerManager", "playMedia: ${media.displayName}, foundIdx=$foundIdx, playlistSize=${_playlist.value.size}")
        if (foundIdx != -1) {
            _currentPlaylistIndex.value = foundIdx
        } else {
            _playlist.value = listOf(media)
            _currentPlaylistIndex.value = 0
        }
        updatePlaylistNavigationState()

        val mediaUri = Uri.parse(media.uriString.ifEmpty { media.path })

        val mediaItem = MediaItem.Builder()
            .setUri(mediaUri)
            .setMediaId(media.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(media.displayName)
                    .setDisplayTitle(media.displayName)
                    .setArtist(if (media.artist.isNotEmpty() && media.artist != "<unknown>") media.artist else "RX Player Pro")
                    .setAlbumTitle(if (media.album.isNotEmpty() && media.album != "<unknown>") media.album else if (media.isVideo) "Videos" else "Music")
                    .setArtworkUri(if (media.uriString.isNotEmpty()) Uri.parse(media.uriString) else null)
                    .build()
            )
            .build()

        getPlayer().setMediaItem(mediaItem, /* resetPosition = */ true)

        // Start Foreground MediaSessionService for System Taskbar / Notification controls
        try {
            val serviceIntent = Intent(context, NovaMediaSessionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Throwable) {
            Log.e("NovaPlayerManager", "Failed to start MediaSessionService for Taskbar: ${e.message}")
        }

        scope.launch {
            val savedPos = if (startPositionMs == null) historyDao.getPlaybackPosition(media.id) else null
            val posMs = startPositionMs ?: savedPos?.positionMs ?: 0L
            val durMs = savedPos?.durationMs ?: media.durationMs
            val resumePos = if (durMs > 3000L && posMs >= durMs - 2500L) 0L else posMs
            if (resumePos > 0) {
                getPlayer().seekTo(resumePos)
            } else {
                getPlayer().seekTo(0)
            }
            getPlayer().prepare()
            getPlayer().play()
        }
    }

    fun loadExternalSubtitle(uri: Uri, mimeType: String = MimeTypes.APPLICATION_SUBRIP, label: String = "External Subtitle") {
        val currentItem = player?.currentMediaItem ?: return
        val currentPos = player?.currentPosition ?: 0L

        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mimeType)
            .setLanguage("und")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .setLabel(label)
            .build()

        val updatedItem = currentItem.buildUpon()
            .setSubtitleConfigurations(listOf(subtitleConfig))
            .build()

        player?.setMediaItem(updatedItem, currentPos)
        player?.prepare()
    }

    fun setSubtitleDelay(delayMs: Long) {
        _subtitleDelayMs.value = delayMs
    }

    fun pause() {
        player?.pause()
    }

    fun play() {
        player?.let {
            if (it.playbackState == Player.STATE_ENDED) {
                it.seekTo(0)
            }
            it.play()
        }
    }

    fun togglePlayPause() {
        player?.let {
            if (it.playbackState == Player.STATE_ENDED) {
                it.seekTo(0)
                it.play()
            } else if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        player?.let { p ->
            val maxDur = _duration.value.coerceAtLeast(0L)
            val clamped = positionMs.coerceIn(0L, if (maxDur > 0) maxDur else Long.MAX_VALUE)
            _targetSeekPosition = clamped
            _currentPosition.value = clamped
            p.seekTo(clamped)
        }
    }

    fun forward10Seconds() {
        seekBy(10000L)
    }

    fun rewind10Seconds() {
        seekBy(-10000L)
    }

    fun seekBy(deltaMs: Long) {
        player?.let { p ->
            val maxDur = _duration.value.coerceAtLeast(0L)
            val basePos = _targetSeekPosition ?: _currentPosition.value
            val target = (basePos + deltaMs).coerceIn(0L, if (maxDur > 0) maxDur else Long.MAX_VALUE)
            _targetSeekPosition = target
            _currentPosition.value = target
            p.seekTo(target)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        player?.setPlaybackSpeed(speed)
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _aspectRatio.value = mode
    }

    fun rotateScreen() {
        _rotationDegrees.value = (_rotationDegrees.value + 90) % 360
    }

    fun selectSubtitleTrack(index: Int) {
        val tracks = player?.currentTracks ?: return
        var textTrackIndex = 0
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_TEXT) {
                if (textTrackIndex == index) {
                    val trackParams = trackSelector?.parameters?.buildUpon()
                        ?.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                        ?.build()
                    if (trackParams != null) {
                        trackSelector?.parameters = trackParams
                    }
                    updateTrackLists()
                    return
                }
                textTrackIndex++
            }
        }
    }

    fun disableSubtitles() {
        val trackParams = trackSelector?.parameters?.buildUpon()
            ?.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            ?.build()
        if (trackParams != null) {
            trackSelector?.parameters = trackParams
        }
        updateTrackLists()
    }

    fun selectAudioTrack(index: Int) {
        val tracks = player?.currentTracks ?: return
        var audioTrackIndex = 0
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                if (audioTrackIndex == index) {
                    val trackParams = trackSelector?.parameters?.buildUpon()
                        ?.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                        ?.build()
                    if (trackParams != null) {
                        trackSelector?.parameters = trackParams
                    }
                    updateTrackLists()
                    return
                }
                audioTrackIndex++
            }
        }
    }

    private fun formatLanguage(lang: String?): String {
        if (lang.isNullOrBlank() || lang.equals("und", ignoreCase = true)) return "Default"
        return try {
            val locale = java.util.Locale.forLanguageTag(lang)
            val display = locale.getDisplayLanguage(java.util.Locale.ENGLISH)
            if (display.isNotBlank()) display else java.util.Locale(lang).getDisplayLanguage(java.util.Locale.ENGLISH).ifEmpty { lang }
        } catch (_: Throwable) {
            lang
        }
    }

    private fun formatAudioCodec(mime: String?): String {
        return when {
            mime == null -> "Audio"
            mime.contains("mp4a", true) || mime.contains("aac", true) -> "AAC"
            mime.contains("eac3", true) -> "E-AC3 (Dolby Digital Plus)"
            mime.contains("ac3", true) -> "AC3 (Dolby Digital)"
            mime.contains("opus", true) -> "Opus"
            mime.contains("flac", true) -> "FLAC"
            mime.contains("mpeg", true) || mime.contains("mp3", true) -> "MP3"
            mime.contains("dts", true) -> "DTS Surround"
            mime.contains("true-hd", true) -> "Dolby TrueHD"
            mime.contains("vorbis", true) -> "Vorbis"
            else -> mime.substringAfterLast("/").uppercase()
        }
    }

    private fun formatSubtitleCodec(mime: String?): String {
        return when {
            mime == null -> "Embedded"
            mime.contains("subrip", true) -> "SRT"
            mime.contains("vtt", true) -> "WebVTT"
            mime.contains("ssa", true) -> "ASS / SSA"
            mime.contains("ttml", true) -> "TTML"
            else -> "Embedded"
        }
    }

    private fun updateTrackLists() {
        val tracks = player?.currentTracks ?: return
        val subtitles = mutableListOf<TrackInfo>()
        val audios = mutableListOf<TrackInfo>()

        var subIdx = 0
        var audIdx = 0

        for (group in tracks.groups) {
            val format = group.mediaTrackGroup.getFormat(0)
            if (group.type == C.TRACK_TYPE_TEXT) {
                val lang = formatLanguage(format.language)
                val codec = formatSubtitleCodec(format.sampleMimeType)
                val name = format.label ?: "Track ${subIdx + 1}: $lang"
                val details = "$codec • $lang"
                subtitles.add(
                    TrackInfo(
                        index = subIdx,
                        name = name,
                        language = format.language,
                        isSelected = group.isSelected,
                        codec = codec,
                        details = details
                    )
                )
                subIdx++
            } else if (group.type == C.TRACK_TYPE_AUDIO) {
                val lang = formatLanguage(format.language)
                val codec = formatAudioCodec(format.sampleMimeType)
                val channels = when (format.channelCount) {
                    1 -> "Mono"
                    2 -> "Stereo (2.0)"
                    6 -> "5.1 Surround"
                    8 -> "7.1 Surround"
                    else -> if (format.channelCount > 0) "${format.channelCount} Channels" else "Audio"
                }
                val bitrate = if (format.bitrate > 0) " • ${format.bitrate / 1000} kbps" else ""
                val name = format.label ?: "Track ${audIdx + 1}: $lang"
                val details = "$codec • $channels$bitrate"
                audios.add(
                    TrackInfo(
                        index = audIdx,
                        name = name,
                        language = format.language,
                        isSelected = group.isSelected,
                        codec = codec,
                        details = details
                    )
                )
                audIdx++
            }
        }

        _subtitleTracks.value = subtitles
        _audioTracks.value = audios
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var lastSaveTime = 0L
            while (isActive) {
                player?.let { p ->
                    _isPlaying.value = p.isPlaying
                    val target = _targetSeekPosition
                    if (target != null) {
                        val pPos = p.currentPosition
                        // Release seek lock once ExoPlayer catches up to target frame or is paused
                        if (kotlin.math.abs(pPos - target) < 1200L || !p.isPlaying) {
                            _targetSeekPosition = null
                            _currentPosition.value = pPos
                        } else {
                            _currentPosition.value = target
                        }
                    } else {
                        _currentPosition.value = p.currentPosition
                    }
                    _duration.value = p.duration.coerceAtLeast(0L)
                    _bufferedPosition.value = p.bufferedPosition

                    // Auto-persist watch position every 5 seconds
                    val now = System.currentTimeMillis()
                    if (now - lastSaveTime > 5000L) {
                        lastSaveTime = now
                        _currentMedia.value?.let { media ->
                            if (p.currentPosition > 0 && p.duration > 0) {
                                try {
                                    historyDao.savePlaybackPosition(
                                        PlaybackPositionEntity(
                                            mediaId = media.id,
                                            positionMs = p.currentPosition,
                                            durationMs = p.duration
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.e("NovaPlayerManager", "Failed to save playback position: ${e.message}")
                                }
                            }
                        }
                    }
                }
                delay(50)
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        player?.removeListener(playerListener)
        player?.release()
        player = null
    }
}
