package com.rxplayer.pro.ui.screens.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rxplayer.pro.core.database.dao.MediaDao
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import com.rxplayer.pro.core.media.player.NovaPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    val playerManager: NovaPlayerManager,
    private val mediaDao: MediaDao
) : ViewModel() {

    val musicList = mediaDao.getAllMusicFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists = mediaDao.getArtistsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums = mediaDao.getAlbumsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun playTrack(media: MediaFileEntity) {
        playerManager.playMedia(media)
    }

    fun shuffleAll() {
        val tracks = musicList.value
        if (tracks.isNotEmpty()) {
            val randomTrack = tracks.random()
            playerManager.playMedia(randomTrack)
        }
    }
}
