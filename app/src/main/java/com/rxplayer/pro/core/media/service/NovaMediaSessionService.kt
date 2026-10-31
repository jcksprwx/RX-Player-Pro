package com.rxplayer.pro.core.media.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionResult
import com.rxplayer.pro.MainActivity
import com.rxplayer.pro.R
import com.rxplayer.pro.core.common.Constants
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import com.rxplayer.pro.core.media.player.NovaPlayerManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class NovaMediaSessionService : MediaSessionService() {

    @Inject
    lateinit var playerManager: NovaPlayerManager

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var cachedArtworkBitmap: Bitmap? = null
    private var lastMediaId: Long? = null

    private val notificationActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constants.ACTION_PLAY_PAUSE -> playerManager.togglePlayPause()
                Constants.ACTION_PREVIOUS -> {
                    if (!playerManager.playPreviousVideo()) {
                        playerManager.rewind10Seconds()
                    }
                }
                Constants.ACTION_NEXT -> {
                    if (!playerManager.playNextVideo()) {
                        playerManager.forward10Seconds()
                    }
                }
                Constants.ACTION_STOP -> {
                    playerManager.pause()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction(Constants.ACTION_PLAY_PAUSE)
            addAction(Constants.ACTION_PREVIOUS)
            addAction(Constants.ACTION_NEXT)
            addAction(Constants.ACTION_STOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(notificationActionReceiver, filter)
        }

        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val availablePlayerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_PREPARE)
                    .add(Player.COMMAND_STOP)
                    .add(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_BACK)
                    .add(Player.COMMAND_SEEK_FORWARD)
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailablePlayerCommands(availablePlayerCommands)
                    .build()
            }

            override fun onPlayerCommandRequest(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                playerCommand: Int
            ): Int {
                when (playerCommand) {
                    Player.COMMAND_PLAY_PAUSE -> {
                        if (playerManager.getPlayer().playbackState == Player.STATE_ENDED) {
                            playerManager.getPlayer().seekTo(0)
                        }
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_SEEK_TO_PREVIOUS -> {
                        playerManager.playPreviousVideo()
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_SEEK_TO_NEXT -> {
                        playerManager.playNextVideo()
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_SEEK_BACK -> {
                        playerManager.rewind10Seconds()
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_SEEK_FORWARD -> {
                        playerManager.forward10Seconds()
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_STOP -> {
                        playerManager.pause()
                        return SessionResult.RESULT_SUCCESS
                    }
                }
                return super.onPlayerCommandRequest(session, controller, playerCommand)
            }
        }

        mediaSession = MediaSession.Builder(this, playerManager.getPlayer())
            .setSessionActivity(pendingIntent)
            .setCallback(sessionCallback)
            .build()

        // Observe player state & media changes to update the notification dynamically
        serviceScope.launch {
            launch {
                playerManager.isPlaying.collect { isPlaying ->
                    updateNotification(isPlaying = isPlaying)
                }
            }
            launch {
                playerManager.currentMedia.collect { media ->
                    if (media != null && media.id != lastMediaId) {
                        lastMediaId = media.id
                        loadArtworkAsync(media)
                    }
                    updateNotification(media = media)
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        updateNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        when (intent?.action) {
            Constants.ACTION_PLAY_PAUSE -> {
                playerManager.togglePlayPause()
            }
            Constants.ACTION_PREVIOUS -> {
                playerManager.rewind10Seconds()
            }
            Constants.ACTION_NEXT -> {
                playerManager.forward10Seconds()
            }
            Constants.ACTION_STOP -> {
                playerManager.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        updateNotification()
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun loadArtworkAsync(media: MediaFileEntity) {
        serviceScope.launch(Dispatchers.IO) {
            var bmp: Bitmap? = null
            try {
                if (media.uriString.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val uri = Uri.parse(media.uriString)
                    bmp = contentResolver.loadThumbnail(uri, Size(300, 300), null)
                }
            } catch (_: Exception) {}

            if (bmp == null && media.path.isNotEmpty() && File(media.path).exists()) {
                try {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(media.path)
                    val artBytes = mmr.embeddedPicture
                    if (artBytes != null) {
                        bmp = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                    } else if (media.isVideo) {
                        bmp = mmr.getFrameAtTime(1000000)
                    }
                    mmr.release()
                } catch (_: Exception) {}
            }

            withContext(Dispatchers.Main) {
                cachedArtworkBitmap = bmp
                updateNotification()
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun updateNotification(
        media: MediaFileEntity? = playerManager.currentMedia.value,
        isPlaying: Boolean = playerManager.isPlaying.value
    ) {
        val session = mediaSession ?: return
        val currentMedia = media ?: playerManager.currentMedia.value

        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action PendingIntents using getBroadcast to prevent Android 12+ background execution restrictions
        val prevIntent = Intent(Constants.ACTION_PREVIOUS).setPackage(packageName)
        val prevPending = PendingIntent.getBroadcast(this, 1, prevIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val playPauseIntent = Intent(Constants.ACTION_PLAY_PAUSE).setPackage(packageName)
        val playPausePending = PendingIntent.getBroadcast(this, 2, playPauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val nextIntent = Intent(Constants.ACTION_NEXT).setPackage(packageName)
        val nextPending = PendingIntent.getBroadcast(this, 3, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val stopIntent = Intent(Constants.ACTION_STOP).setPackage(packageName)
        val stopPending = PendingIntent.getBroadcast(this, 4, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // YouTube-style MediaStyle Notification connected directly to MediaSession
        val mediaStyle = MediaStyleNotificationHelper.MediaStyle(session)
            .setShowActionsInCompactView(0, 1, 2)

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val title = currentMedia?.displayName?.ifEmpty { "RX Player Pro" } ?: "RX Player Pro"
        val artist = if (currentMedia?.artist?.isNotEmpty() == true && currentMedia.artist != "<unknown>") {
            currentMedia.artist
        } else if (currentMedia?.isVideo == true) {
            "Video Playback"
        } else {
            "Audio Playback"
        }

        val builder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setStyle(mediaStyle)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(if (currentMedia?.isVideo == true) "RX Video" else "RX Music")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(stopPending)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Rewind 10s", prevPending)
            .addAction(playPauseIcon, playPauseTitle, playPausePending)
            .addAction(android.R.drawable.ic_media_next, "Forward 10s", nextPending)

        cachedArtworkBitmap?.let {
            builder.setLargeIcon(it)
        }

        val notification = builder.build()

        try {
            if (isPlaying) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        Constants.NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(Constants.NOTIFICATION_ID, notification)
                }
            } else {
                // When paused, maintain notification in status bar/quick settings without locking foreground
                stopForeground(STOP_FOREGROUND_DETACH)
                NotificationManagerCompat.from(this).notify(Constants.NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            // Fallback for security exceptions
            NotificationManagerCompat.from(this).notify(Constants.NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_playback),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_playback_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(notificationActionReceiver)
        } catch (_: Exception) {}
        serviceScope.cancel()
        cachedArtworkBitmap?.recycle()
        cachedArtworkBitmap = null
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
