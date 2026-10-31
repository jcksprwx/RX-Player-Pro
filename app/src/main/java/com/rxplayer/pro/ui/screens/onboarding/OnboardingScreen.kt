package com.rxplayer.pro.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rxplayer.pro.core.theme.*

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaAmoledBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text(
                text = "Welcome to RX Player Pro",
                style = MaterialTheme.typography.headlineMedium,
                color = NovaTextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "A next-generation, hardware-accelerated media player engineered for ultimate clarity, privacy, and speed.",
                style = MaterialTheme.typography.bodyMedium,
                color = NovaTextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FeatureHighlight(
                icon = Icons.Default.HighQuality,
                iconTint = NovaElectricCyan,
                title = "True Hardware & Software Decoders",
                description = "Seamlessly switch between hardware acceleration and robust software decoding for 4K, 8K, and high-bitrate containers."
            )
            FeatureHighlight(
                icon = Icons.Default.TouchApp,
                iconTint = NovaElectricViolet,
                title = "Advanced Intuitive Gestures",
                description = "Swipe for volume, brightness, precision scrubbing, double-tap seek, and pinch to zoom up to 400%."
            )
            FeatureHighlight(
                icon = Icons.Default.GraphicEq,
                iconTint = NovaAccentAmber,
                title = "Audiophile DSP & Equalizer",
                description = "Multi-band graphic equalizer, dynamic bass booster, and 3D virtualizer tailored for headphones and external sound systems."
            )
            FeatureHighlight(
                icon = Icons.Default.Security,
                iconTint = NovaAccentEmerald,
                title = "100% Ad-Free & Local First",
                description = "Zero trackers, zero data collection. Media never leaves your device. Optional biometric app lock."
            )
        }

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NovaElectricCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Get Started", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun FeatureHighlight(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = iconTint.copy(alpha = 0.15f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = NovaTextSecondary)
        }
    }
}
