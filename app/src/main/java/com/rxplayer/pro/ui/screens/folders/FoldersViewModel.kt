package com.rxplayer.pro.ui.screens.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rxplayer.pro.core.database.dao.HiddenFolderDao
import com.rxplayer.pro.core.database.dao.MediaDao
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FolderItemUi(
    val folderPath: String,
    val folderName: String,
    val mediaCount: Int,
    val totalSizeBytes: Long
)

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val mediaDao: MediaDao,
    private val hiddenFolderDao: HiddenFolderDao
) : ViewModel() {

    init {
        // Automatically restore any previously hidden folders so all folders appear
        viewModelScope.launch {
            hiddenFolderDao.unhideAll()
        }
    }

    val folders: StateFlow<List<FolderItemUi>> = mediaDao.getAllVideoFoldersFlow()
        .map { list ->
            list.map { summary ->
                FolderItemUi(
                    folderPath = summary.folderPath,
                    folderName = summary.folderName,
                    mediaCount = summary.mediaCount,
                    totalSizeBytes = summary.totalSizeBytes
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getVideosInFolder(folderPath: String): Flow<List<MediaFileEntity>> {
        return mediaDao.getVideosInFolderFlow(folderPath)
    }
}
