package com.rxplayer.pro.ui.screens.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rxplayer.pro.core.database.entity.PlaylistEntity
import com.rxplayer.pro.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    onNavigateToDetail: (Long, String) -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val playlistsWithCount by viewModel.playlists.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlists", color = NovaTextPrimary) },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Playlist", tint = NovaElectricCyan)
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
            if (playlistsWithCount.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = NovaElectricCyan.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Playlists Yet", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NovaElectricCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Create New Playlist", color = androidx.compose.ui.graphics.Color.Black)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    items(playlistsWithCount, key = { it.playlist.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onNavigateToDetail(item.playlist.id, item.playlist.name) },
                            colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlaylistPlay, null, tint = NovaElectricCyan)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.playlist.name, style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                                    Text("${item.itemCount} items", style = MaterialTheme.typography.bodySmall, color = NovaTextSecondary)
                                }
                                IconButton(onClick = { viewModel.deletePlaylist(item.playlist) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NovaTextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            if (showCreateDialog) {
                var newName by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = { Text("New Playlist", color = NovaTextPrimary) },
                    text = {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Playlist Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.createPlaylist(newName)
                                showCreateDialog = false
                            },
                            enabled = newName.isNotBlank()
                        ) {
                            Text("Create", color = NovaElectricCyan)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel", color = NovaTextSecondary)
                        }
                    },
                    containerColor = NovaDarkSurfaceElevated
                )
            }
        }
    }
}
