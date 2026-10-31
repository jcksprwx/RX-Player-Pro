package com.rxplayer.pro.ui.screens.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Verified
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
fun AboutScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About RX Player Pro", color = NovaTextPrimary) },
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
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "RX Player Pro",
                        style = MaterialTheme.typography.headlineMedium,
                        color = NovaElectricCyan,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version 2.0.1 Pro (Build 201)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = NovaAccentEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = NovaAccentEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PRO LIFETIME ACTIVE • AD-FREE", color = NovaAccentEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Creator Credit Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = NovaElectricCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = NovaElectricCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Created by Rituraj",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NovaTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Lead Architect & Developer",
                            style = MaterialTheme.typography.bodySmall,
                            color = NovaElectricViolet,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = NovaAccentAmber,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Architecture & Media Stack", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    BulletPoint("Media Engine: AndroidX Media3 ExoPlayer 1.3.1")
                    BulletPoint("UI Framework: Android Jetpack Compose Material 3")
                    BulletPoint("Database: SQLite with AndroidX Room Persistence")
                    BulletPoint("Decoders: Hardware MediaCodec + Software Fallback")
                    BulletPoint("Network: Hierynomus SMBJ Protocol Suite (SMB2/3)")
                    BulletPoint("Audio: Android OpenSL / AudioFX DSP")
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Legal & Open Source License", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "RX Player Pro is open-source software licensed under the Apache License, Version 2.0. You may obtain a copy of the License in the repository root (LICENSE) or at http://www.apache.org/licenses/LICENSE-2.0.\n\nDesigned independently with 100% original architecture, zero trackers, and zero proprietary commercial assets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodyMedium,
        color = NovaTextSecondary,
        modifier = Modifier.padding(vertical = 3.dp)
    )
}
