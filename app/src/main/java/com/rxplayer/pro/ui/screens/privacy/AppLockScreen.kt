package com.rxplayer.pro.ui.screens.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.rxplayer.pro.core.security.BiometricAuthManager
import com.rxplayer.pro.core.theme.*

@Composable
fun AppLockScreen(
    biometricAuthManager: BiometricAuthManager,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun triggerAuth() {
        activity?.let { act ->
            biometricAuthManager.promptAuthenticate(
                activity = act,
                onSuccess = onUnlocked,
                onError = { errorMessage = it }
            )
        }
    }

    LaunchedEffect(Unit) {
        triggerAuth()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaAmoledBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(NovaElectricViolet.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = NovaElectricViolet, modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "RX Player Pro is Locked",
            style = MaterialTheme.typography.headlineSmall,
            color = NovaTextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Please authenticate using your fingerprint, face, or device PIN to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = NovaTextSecondary
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = errorMessage!!, color = NovaAccentRose, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { triggerAuth() },
            colors = ButtonDefaults.buttonColors(containerColor = NovaElectricCyan),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(50.dp)
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Unlock with Biometrics / PIN", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
