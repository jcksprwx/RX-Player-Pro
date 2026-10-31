package com.rxplayer.pro.core.media.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rxplayer.pro.core.common.Constants
import com.rxplayer.pro.core.media.player.NovaPlayerManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MediaNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var playerManager: NovaPlayerManager

    override fun onReceive(context: Context, intent: Intent?) {
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
            }
        }
    }
}
