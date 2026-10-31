package com.rxplayer.pro.ui.screens.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rxplayer.pro.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", color = NovaTextPrimary) },
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
            Card(
                colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NovaAccentEmerald, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Zero-Tracking Guarantee", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary, fontWeight = FontWeight.Bold)
                        Text("RX Player Pro is 100% local-first", style = MaterialTheme.typography.bodySmall, color = NovaTextSecondary)
                    }
                }
            }

            PolicySection(
                title = "1. No Advertisements & No Tracking SDKs",
                body = "RX Player Pro contains zero advertising SDKs, analytics frameworks, telemetry beacons, or behavioral tracking libraries. Your usage habits, watch logs, and file titles remain strictly on your local device."
            )

            PolicySection(
                title = "2. Offline-First Processing",
                body = "All video playback, subtitle parsing, audio decoding, trimming, and extraction are executed on-device using local Android Media Frameworks. No video or audio file is ever uploaded to any external server."
            )

            PolicySection(
                title = "3. Scoped Storage & Local Network Only",
                body = "Permissions requested are exclusively used for reading and managing media on your device's internal storage, SD cards, USB OTG drives, and user-specified local area network SMB shares. Internet permission is strictly restricted to streaming from local SMB servers you configure."
            )

            PolicySection(
                title = "4. Hardware-Encrypted Credentials",
                body = "Network server passwords are never stored in plain text. They are encrypted using the Android Keystore system with AES-256 GCM encryption via EncryptedSharedPreferences."
            )
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = NovaElectricCyan, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = body, style = MaterialTheme.typography.bodySmall, color = NovaTextSecondary, lineHeight = 18.sp)
        }
    }
}
