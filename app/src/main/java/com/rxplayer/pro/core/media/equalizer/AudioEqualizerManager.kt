package com.rxplayer.pro.core.media.equalizer

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class EqualizerBand(
    val index: Short,
    val centerFreqHz: Int,
    val minLevelMilliBels: Short,
    val maxLevelMilliBels: Short,
    val currentLevelMilliBels: Short
)

@Singleton
class AudioEqualizerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _bands = MutableStateFlow<List<EqualizerBand>>(emptyList())
    val bands: StateFlow<List<EqualizerBand>> = _bands.asStateFlow()

    private val _presets = MutableStateFlow<List<String>>(emptyList())
    val presets: StateFlow<List<String>> = _presets.asStateFlow()

    private val _currentPresetIndex = MutableStateFlow<Short>(-1)
    val currentPresetIndex: StateFlow<Short> = _currentPresetIndex.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow<Short>(0)
    val bassBoostStrength: StateFlow<Short> = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow<Short>(0)
    val virtualizerStrength: StateFlow<Short> = _virtualizerStrength.asStateFlow()

    private var currentSessionId: Int = -1

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        release()
        currentSessionId = audioSessionId

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _isEnabled.value
            }

            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _isEnabled.value
                if (strengthSupported) {
                    setStrength(_bassBoostStrength.value)
                }
            }

            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _isEnabled.value
                if (strengthSupported) {
                    setStrength(_virtualizerStrength.value)
                }
            }

            loadBandsAndPresets()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadBandsAndPresets() {
        equalizer?.let { eq ->
            val numBands = eq.numberOfBands
            val levelRange = eq.bandLevelRange
            val minLevel = levelRange[0]
            val maxLevel = levelRange[1]

            val bandList = mutableListOf<EqualizerBand>()
            for (i in 0 until numBands) {
                val bandIndex = i.toShort()
                val centerFreq = eq.getCenterFreq(bandIndex) / 1000 // Convert mHz to Hz
                val currentLevel = eq.getBandLevel(bandIndex)
                bandList.add(EqualizerBand(bandIndex, centerFreq, minLevel, maxLevel, currentLevel))
            }
            _bands.value = bandList

            val presetList = mutableListOf<String>()
            for (p in 0 until eq.numberOfPresets) {
                presetList.add(eq.getPresetName(p.toShort()))
            }
            presetList.add("Custom")
            _presets.value = presetList
            _currentPresetIndex.value = eq.currentPreset
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
            _bands.value = _bands.value.map {
                if (it.index == band) it.copy(currentLevelMilliBels = level) else it
            }
            _currentPresetIndex.value = (_presets.value.size - 1).toShort() // Custom
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun usePreset(presetIndex: Short) {
        try {
            if (presetIndex >= 0 && presetIndex < (equalizer?.numberOfPresets ?: 0)) {
                equalizer?.usePreset(presetIndex)
                _currentPresetIndex.value = presetIndex
                loadBandsAndPresets()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBassBoost(strength: Short) {
        _bassBoostStrength.value = strength
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(strength)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setVirtualizer(strength: Short) {
        _virtualizerStrength.value = strength
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(strength)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun reset() {
        try {
            equalizer?.let { eq ->
                for (b in 0 until eq.numberOfBands) {
                    eq.setBandLevel(b.toShort(), 0)
                }
            }
            bassBoost?.setStrength(0)
            virtualizer?.setStrength(0)
            _bassBoostStrength.value = 0
            _virtualizerStrength.value = 0
            loadBandsAndPresets()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            equalizer = null
            bassBoost = null
            virtualizer = null
            currentSessionId = -1
        }
    }
}
