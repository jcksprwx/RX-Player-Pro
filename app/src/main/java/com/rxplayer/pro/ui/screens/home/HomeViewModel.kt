package com.rxplayer.pro.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rxplayer.pro.core.database.dao.HistoryDao
import com.rxplayer.pro.core.database.dao.MediaDao
import com.rxplayer.pro.core.media.scanner.MediaScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaDao: MediaDao,
    private val historyDao: HistoryDao,
    val mediaScanner: MediaScanner
) : ViewModel() {

    val continueWatching = historyDao.getContinueWatchingFlow(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded = mediaDao.getRecentlyAddedVideosFlow(8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videoFolders = mediaDao.getVideoFoldersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalVideoCount = mediaDao.getVideoCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalVideoSize = mediaDao.getTotalVideoSizeBytesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun refreshLibrary() {
        viewModelScope.launch {
            mediaScanner.scanMediaLibrary()
        }
    }
}
