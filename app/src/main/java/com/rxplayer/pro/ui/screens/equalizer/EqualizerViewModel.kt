package com.rxplayer.pro.ui.screens.equalizer

import androidx.lifecycle.ViewModel
import com.rxplayer.pro.core.media.equalizer.AudioEqualizerManager
import com.rxplayer.pro.core.media.player.NovaPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    val equalizerManager: AudioEqualizerManager,
    private val playerManager: NovaPlayerManager
) : ViewModel() {

    init {
        val audioSessionId = playerManager.audioSessionId.value
        if (audioSessionId > 0) {
            equalizerManager.attachAudioSession(audioSessionId)
        }
    }

    fun toggleEnabled(enabled: Boolean) {
        equalizerManager.setEnabled(enabled)
    }

    fun setBandLevel(bandIndex: Short, level: Short) {
        equalizerManager.setBandLevel(bandIndex, level)
    }

    fun selectPreset(presetIndex: Short) {
        equalizerManager.usePreset(presetIndex)
    }

    fun setBassBoost(strength: Short) {
        equalizerManager.setBassBoost(strength)
    }

    fun setVirtualizer(strength: Short) {
        equalizerManager.setVirtualizer(strength)
    }

    fun reset() {
        equalizerManager.reset()
    }
}
