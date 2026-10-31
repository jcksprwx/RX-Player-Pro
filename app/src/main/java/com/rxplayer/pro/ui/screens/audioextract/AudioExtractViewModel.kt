package com.rxplayer.pro.ui.screens.audioextract

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rxplayer.pro.core.common.FileUtils
import com.rxplayer.pro.core.database.dao.MediaDao
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import com.rxplayer.pro.core.media.transformer.AudioExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AudioExtractViewModel @Inject constructor(
    private val mediaDao: MediaDao,
    private val audioExtractor: AudioExtractor
) : ViewModel() {

    private val _media = MutableStateFlow<MediaFileEntity?>(null)
    val media: StateFlow<MediaFileEntity?> = _media.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _exportSuccess = MutableStateFlow<String?>(null)
    val exportSuccess: StateFlow<String?> = _exportSuccess.asStateFlow()

    fun loadMedia(mediaId: Long) {
        viewModelScope.launch {
            _media.value = mediaDao.getMediaById(mediaId)
        }
    }

    fun startExtraction(outputFileName: String) {
        val current = _media.value ?: return
        viewModelScope.launch {
            _isExtracting.value = true
            _exportSuccess.value = null

            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val outputFolder = File(musicDir, "RX_Audio").apply { mkdirs() }
            val cleanName = FileUtils.getFileNameWithoutExtension(outputFileName).ifEmpty { "Track" }
            val outputFile = File(outputFolder, "${cleanName}.m4a")

            val success = audioExtractor.extractAudioTrack(
                sourceVideoPath = current.path,
                destAudioPath = outputFile.absolutePath
            ) { p ->
                _progress.value = p
            }

            _isExtracting.value = false
            if (success) {
                _exportSuccess.value = outputFile.absolutePath
            }
        }
    }
}
