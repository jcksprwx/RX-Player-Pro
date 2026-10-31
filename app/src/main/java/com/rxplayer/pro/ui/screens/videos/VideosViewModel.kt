package com.rxplayer.pro.ui.screens.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rxplayer.pro.core.database.dao.HistoryDao
import com.rxplayer.pro.core.database.dao.MediaDao
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import com.rxplayer.pro.core.media.scanner.MediaScanner
import com.rxplayer.pro.core.security.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class VideoSortOrder {
    DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, SIZE_DESC, DURATION_DESC;

    companion object {
        fun fromName(name: String): VideoSortOrder =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DATE_DESC
    }
}

@HiltViewModel
class VideosViewModel @Inject constructor(
    private val mediaDao: MediaDao,
    private val historyDao: HistoryDao,
    val mediaScanner: MediaScanner,
    private val secureStorage: SecureStorage
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(VideoSortOrder.fromName(secureStorage.videoSortOrderFlow.value))
    val sortOrder: StateFlow<VideoSortOrder> = _sortOrder.asStateFlow()

    private val _isGridView = MutableStateFlow(secureStorage.isGridViewFlow.value)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    val videos = combine(
        mediaDao.getAllVideosFlow(),
        _searchQuery,
        _sortOrder
    ) { allVideos, query, sort ->
        val filtered = if (query.isBlank()) {
            allVideos
        } else {
            allVideos.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                it.title.contains(query, ignoreCase = true)
            }
        }

        when (sort) {
            VideoSortOrder.DATE_DESC -> filtered.sortedByDescending { it.dateModified }
            VideoSortOrder.DATE_ASC -> filtered.sortedBy { it.dateModified }
            VideoSortOrder.NAME_ASC -> filtered.sortedBy { it.displayName.lowercase() }
            VideoSortOrder.NAME_DESC -> filtered.sortedByDescending { it.displayName.lowercase() }
            VideoSortOrder.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
            VideoSortOrder.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: VideoSortOrder) {
        _sortOrder.value = order
        secureStorage.setVideoSortOrder(order.name)
    }

    fun toggleGridView() {
        val newView = !_isGridView.value
        _isGridView.value = newView
        secureStorage.setGridView(newView)
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            mediaScanner.scanMediaLibrary()
        }
    }

    fun deleteMedia(media: MediaFileEntity) {
        viewModelScope.launch {
            try {
                if (media.path.isNotEmpty()) {
                    File(media.path).delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaDao.deleteMediaById(media.id)
        }
    }
}
