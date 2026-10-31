package com.rxplayer.pro.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rxplayer.pro.core.database.dao.PlaylistDao
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import com.rxplayer.pro.core.database.entity.PlaylistEntity
import com.rxplayer.pro.core.database.entity.PlaylistItemEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistDao: PlaylistDao
) : ViewModel() {

    val playlists = playlistDao.getAllPlaylistsWithCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlaylist(name: String, isVideo: Boolean = true) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                playlistDao.insertPlaylist(
                    PlaylistEntity(name = name.trim(), isVideoPlaylist = isVideo)
                )
            }
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(playlist)
        }
    }

    fun getPlaylistMedia(playlistId: Long): Flow<List<MediaFileEntity>> {
        return playlistDao.getPlaylistMediaFilesFlow(playlistId)
    }

    fun removeMediaFromPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch {
            playlistDao.removePlaylistItem(playlistId, mediaId)
        }
    }

    fun addMediaToPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch {
            playlistDao.insertPlaylistItem(
                PlaylistItemEntity(
                    playlistId = playlistId,
                    mediaId = mediaId,
                    sortOrder = 0
                )
            )
        }
    }
}
