package com.rxplayer.pro.ui.screens.network

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rxplayer.pro.core.common.FileUtils
import com.rxplayer.pro.core.database.entity.NetworkServerEntity
import com.rxplayer.pro.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    onNavigateToAddServer: () -> Unit,
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val serverFiles by viewModel.selectedServerFiles.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var selectedServer by remember { mutableStateOf<NetworkServerEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedServer != null) selectedServer!!.name else "Network Shares", color = NovaTextPrimary) },
                navigationIcon = {
                    if (selectedServer != null) {
                        IconButton(onClick = { selectedServer = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NovaTextPrimary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAddServer) {
                        Icon(Icons.Default.Add, contentDescription = "Add Server", tint = NovaElectricCyan)
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
            if (isLoading) {
                CircularProgressIndicator(
                    color = NovaElectricCyan,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (selectedServer != null) {
                // Browsing files on selected server
                if (serverFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No media files found in this share", color = NovaTextSecondary)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(serverFiles) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (item.isDirectory) {
                                            viewModel.browseServer(selectedServer!!, item.path)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.VideoFile,
                                    contentDescription = null,
                                    tint = if (item.isDirectory) NovaElectricViolet else NovaElectricCyan
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, color = NovaTextPrimary, style = MaterialTheme.typography.titleMedium)
                                    if (!item.isDirectory) {
                                        Text(FileUtils.formatFileSize(item.sizeBytes), color = NovaTextSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            Divider(color = NovaDivider)
                        }
                    }
                }
            } else {
                // Listing configured servers
                if (servers.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lan,
                            contentDescription = null,
                            tint = NovaElectricCyan.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Network Servers Configured",
                            style = MaterialTheme.typography.titleLarge,
                            color = NovaTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Connect to Windows SMB shares, NAS devices, or Samba servers over Wi-Fi for direct streaming.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NovaTextSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToAddServer,
                            colors = ButtonDefaults.buttonColors(containerColor = NovaElectricCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = androidx.compose.ui.graphics.Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add SMB Server", color = androidx.compose.ui.graphics.Color.Black)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        items(servers, key = { it.id }) { srv ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        selectedServer = srv
                                        viewModel.browseServer(srv)
                                    },
                                colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Dns, contentDescription = null, tint = NovaElectricCyan)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(srv.name, style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                                        Text("${srv.host}:${srv.port} / ${srv.shareName}", style = MaterialTheme.typography.bodySmall, color = NovaTextSecondary)
                                    }
                                    IconButton(onClick = { viewModel.deleteServer(srv) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NovaTextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
