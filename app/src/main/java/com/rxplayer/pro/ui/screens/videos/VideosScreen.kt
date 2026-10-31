package com.rxplayer.pro.ui.screens.videos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import com.rxplayer.pro.core.theme.*
import com.rxplayer.pro.ui.components.VideoGridCard
import com.rxplayer.pro.ui.components.VideoListCard
import com.rxplayer.pro.ui.screens.videoinfo.VideoInfoDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    onPlayVideo: (Long) -> Unit,
    onTrimVideo: (Long) -> Unit,
    onExtractAudio: (Long) -> Unit,
    viewModel: VideosViewModel = hiltViewModel()
) {
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isScanning by viewModel.mediaScanner.isScanning.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var selectedMediaForInfo by remember { mutableStateOf<MediaFileEntity?>(null) }
    var mediaToDelete by remember { mutableStateOf<MediaFileEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search videos...", color = NovaTextSecondary) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedTextColor = NovaTextPrimary,
                                unfocusedTextColor = NovaTextPrimary
                            )
                        )
                    } else {
                        Text("All Videos (${videos.size})", color = NovaTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) viewModel.updateSearchQuery("")
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NovaTextPrimary
                        )
                    }

                    IconButton(onClick = { viewModel.toggleGridView() }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View",
                            tint = NovaTextPrimary
                        )
                    }

                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort", tint = NovaTextPrimary)
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            listOf(
                                VideoSortOrder.DATE_DESC to "Newest First",
                                VideoSortOrder.DATE_ASC to "Oldest First",
                                VideoSortOrder.NAME_ASC to "Name (A to Z)",
                                VideoSortOrder.NAME_DESC to "Name (Z to A)",
                                VideoSortOrder.SIZE_DESC to "Size (Largest)",
                                VideoSortOrder.DURATION_DESC to "Duration (Longest)"
                            ).forEach { (order, label) ->
                                val isSelected = sortOrder == order
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = label,
                                            color = if (isSelected) NovaElectricCyan else NovaTextPrimary,
                                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, tint = NovaElectricCyan) }
                                    } else null,
                                    onClick = {
                                        viewModel.setSortOrder(order)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = { viewModel.refreshLibrary() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan Media", tint = NovaElectricCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NovaDarkSurface)
            )
        },
        containerColor = NovaAmoledBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isScanning) {
                LinearProgressIndicator(
                    color = NovaElectricCyan,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (videos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching videos found" else "No videos found in storage. Tap refresh to scan.",
                        color = NovaTextSecondary
                    )
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(videos, key = { it.id }) { video ->
                        VideoGridCard(
                            media = video,
                            onClick = { onPlayVideo(video.id) },
                            onInfoClick = { selectedMediaForInfo = video },
                            onTrimClick = { onTrimVideo(video.id) },
                            onExtractAudioClick = { onExtractAudio(video.id) },
                            onAddToPlaylistClick = {},
                            onDeleteClick = { mediaToDelete = video }
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(videos, key = { it.id }) { video ->
                        VideoListCard(
                            media = video,
                            onClick = { onPlayVideo(video.id) },
                            onInfoClick = { selectedMediaForInfo = video },
                            onTrimClick = { onTrimVideo(video.id) },
                            onExtractAudioClick = { onExtractAudio(video.id) },
                            onAddToPlaylistClick = {},
                            onDeleteClick = { mediaToDelete = video }
                        )
                        Divider(color = NovaDivider)
                    }
                }
            }

            // Info Dialog
            if (selectedMediaForInfo != null) {
                VideoInfoDialog(
                    media = selectedMediaForInfo!!,
                    onDismiss = { selectedMediaForInfo = null }
                )
            }

            // Delete Confirmation Dialog
            if (mediaToDelete != null) {
                AlertDialog(
                    onDismissRequest = { mediaToDelete = null },
                    title = { Text("Delete Video?", color = NovaTextPrimary) },
                    text = { Text("Are you sure you want to delete '${mediaToDelete!!.displayName}'? This cannot be undone.", color = NovaTextSecondary) },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteMedia(mediaToDelete!!)
                            mediaToDelete = null
                        }) {
                            Text("Delete", color = NovaAccentRose)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { mediaToDelete = null }) {
                            Text("Cancel", color = NovaTextSecondary)
                        }
                    },
                    containerColor = NovaDarkSurfaceElevated
                )
            }
        }
    }
}
