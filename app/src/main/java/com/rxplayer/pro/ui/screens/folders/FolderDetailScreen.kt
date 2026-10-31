package com.rxplayer.pro.ui.screens.folders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import com.rxplayer.pro.core.theme.*
import com.rxplayer.pro.ui.components.VideoListCard
import com.rxplayer.pro.ui.screens.videoinfo.VideoInfoDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderPath: String,
    folderName: String,
    onBackClick: () -> Unit,
    onPlayVideo: (Long) -> Unit,
    onTrimVideo: (Long) -> Unit,
    onExtractAudio: (Long) -> Unit,
    viewModel: FoldersViewModel = hiltViewModel()
) {
    val videosFlow = remember(folderPath) { viewModel.getVideosInFolder(folderPath) }
    val videos by videosFlow.collectAsStateWithLifecycle(emptyList())

    var selectedMediaForInfo by remember { mutableStateOf<MediaFileEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folderName, color = NovaTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NovaTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NovaDarkSurface)
            )
        },
        containerColor = NovaAmoledBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(videos, key = { it.id }) { video ->
                VideoListCard(
                    media = video,
                    onClick = { onPlayVideo(video.id) },
                    onInfoClick = { selectedMediaForInfo = video },
                    onTrimClick = { onTrimVideo(video.id) },
                    onExtractAudioClick = { onExtractAudio(video.id) },
                    onAddToPlaylistClick = {},
                    onDeleteClick = {}
                )
                Divider(color = NovaDivider)
            }
        }

        if (selectedMediaForInfo != null) {
            VideoInfoDialog(
                media = selectedMediaForInfo!!,
                onDismiss = { selectedMediaForInfo = null }
            )
        }
    }
}
