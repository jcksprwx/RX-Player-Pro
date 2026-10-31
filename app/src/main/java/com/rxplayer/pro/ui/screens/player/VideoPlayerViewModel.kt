package com.rxplayer.pro.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rxplayer.pro.core.database.dao.MediaDao
import com.rxplayer.pro.core.media.player.NovaPlayerManager
import com.rxplayer.pro.core.security.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    val playerManager: NovaPlayerManager,
    private val mediaDao: MediaDao,
    val secureStorage: SecureStorage
) : ViewModel() {

    fun loadAndPlay(mediaId: Long) {
        viewModelScope.launch {
            val media = mediaDao.getMediaById(mediaId)
            if (media != null) {
                val folderVideos = if (media.folderPath.isNotEmpty()) {
                    mediaDao.getVideosInFolder(media.folderPath)
                } else {
                    emptyList()
                }
                val playlist = if (folderVideos.isNotEmpty()) folderVideos else mediaDao.getAllVideos()
                val targetIndex = playlist.indexOfFirst { it.id == mediaId }.coerceAtLeast(0)
                android.util.Log.d("VideoPlayerVM", "loadAndPlay: media=${media.displayName}, folder=${media.folderPath}, targetIndex=$targetIndex, size=${playlist.size}")
                playerManager.setPlaylist(if (playlist.isNotEmpty()) playlist else listOf(media), targetIndex)
            }
        }
    }

    fun playNext() = playerManager.playNextVideo()
    fun playPrevious() = playerManager.playPreviousVideo()
}
