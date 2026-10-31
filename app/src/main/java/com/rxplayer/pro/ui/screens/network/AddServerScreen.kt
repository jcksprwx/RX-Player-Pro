package com.rxplayer.pro.ui.screens.network

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rxplayer.pro.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerScreen(
    onBackClick: () -> Unit,
    viewModel: NetworkViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("445") }
    var shareName by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val status by viewModel.connectionStatus.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add SMB Server", color = NovaTextPrimary) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Server Display Name (e.g. Living Room NAS)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Host or IP Address (e.g. 192.168.1.100)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = shareName,
                    onValueChange = { shareName = it },
                    label = { Text("Share Name (e.g. Movies)") },
                    modifier = Modifier.weight(2f)
                )
            }

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(
                    checked = isAnonymous,
                    onCheckedChange = { isAnonymous = it },
                    colors = CheckboxDefaults.colors(checkedColor = NovaElectricCyan)
                )
                Text("Anonymous / Guest Access", color = NovaTextPrimary)
            }

            if (!isAnonymous) {
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Domain / Workgroup (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (Stored with Hardware Encryption)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (status != null) {
                Text(
                    text = status!!,
                    color = if (status!!.contains("success", ignoreCase = true)) NovaAccentEmerald else NovaAccentRose,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = {
                    viewModel.testAndAddServer(
                        name = name,
                        host = host,
                        port = port.toIntOrNull() ?: 445,
                        shareName = shareName,
                        domain = domain,
                        username = username,
                        password = password,
                        isAnonymous = isAnonymous,
                        onSuccess = onBackClick
                    )
                },
                enabled = !isLoading && host.isNotBlank() && shareName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NovaElectricCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else {
                    Text("Connect & Save Server", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
