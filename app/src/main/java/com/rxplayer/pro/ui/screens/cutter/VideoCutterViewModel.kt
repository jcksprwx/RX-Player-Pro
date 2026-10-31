package com.rxplayer.pro.ui.screens.cutter

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rxplayer.pro.core.database.dao.MediaDao
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import com.rxplayer.pro.core.media.transformer.MediaTrimmer
import com.rxplayer.pro.core.media.transformer.TrimProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VideoCutterViewModel @Inject constructor(
    private val mediaDao: MediaDao,
    private val mediaTrimmer: MediaTrimmer
) : ViewModel() {

    private val _media = MutableStateFlow<MediaFileEntity?>(null)
    val media: StateFlow<MediaFileEntity?> = _media.asStateFlow()

    private val _trimProgress = MutableStateFlow<TrimProgress?>(null)
    val trimProgress: StateFlow<TrimProgress?> = _trimProgress.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportSuccess = MutableStateFlow<String?>(null)
    val exportSuccess: StateFlow<String?> = _exportSuccess.asStateFlow()

    fun loadMedia(mediaId: Long) {
        viewModelScope.launch {
            _media.value = mediaDao.getMediaById(mediaId)
        }
    }

    fun startTrim(startMs: Long, endMs: Long) {
        val current = _media.value ?: return
        viewModelScope.launch {
            _isExporting.value = true
            _exportSuccess.value = null

            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val outputFolder = File(moviesDir, "RX_Cuts").apply { mkdirs() }
            val outputFile = File(outputFolder, "TRIM_${System.currentTimeMillis()}_${current.displayName}")

            val success = mediaTrimmer.trimVideo(
                sourcePath = current.path,
                destPath = outputFile.absolutePath,
                startMs = startMs,
                endMs = endMs
            ) { progress ->
                _trimProgress.value = progress
            }

            _isExporting.value = false
            if (success) {
                _exportSuccess.value = outputFile.absolutePath
            }
        }
    }
}
