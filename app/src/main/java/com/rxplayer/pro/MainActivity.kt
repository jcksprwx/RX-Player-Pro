package com.rxplayer.pro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rxplayer.pro.core.media.player.NovaPlayerManager
import com.rxplayer.pro.core.media.scanner.MediaScanner
import com.rxplayer.pro.core.security.BiometricAuthManager
import com.rxplayer.pro.core.security.SecureStorage
import com.rxplayer.pro.core.security.ThemeMode
import com.rxplayer.pro.core.theme.NovaPlayerTheme
import com.rxplayer.pro.ui.navigation.NovaNavGraph
import com.rxplayer.pro.ui.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var secureStorage: SecureStorage

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    @Inject
    lateinit var mediaScanner: MediaScanner

    @Inject
    lateinit var playerManager: NovaPlayerManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission for Taskbar / Status Bar media controls on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val hasPermission = checkStoragePermissions()
        if (hasPermission) {
            lifecycleScope.launch {
                mediaScanner.scanMediaLibrary()
            }
        }

        // Handle external Intent if opened from File Manager
        handleExternalIntent(intent)

        setContent {
            val aestheticTheme by secureStorage.aestheticThemeFlow.collectAsStateWithLifecycle()
            val themeMode by secureStorage.themeModeFlow.collectAsStateWithLifecycle()
            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemDark
            }

            NovaPlayerTheme(aestheticTheme = aestheticTheme, darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NovaNavGraph(
                        navController = navController,
                        secureStorage = secureStorage,
                        biometricAuthManager = biometricAuthManager,
                        hasStoragePermission = hasPermission
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleExternalIntent(intent)
    }

    private fun handleExternalIntent(intent: Intent?) {
        try {
            if (intent?.action == Intent.ACTION_VIEW) {
                val uri: Uri? = intent.data
                if (uri != null) {
                    val dummyMedia = com.rxplayer.pro.core.database.entity.MediaFileEntity(
                        uriString = uri.toString(),
                        path = uri.path ?: "",
                        title = uri.lastPathSegment ?: "External Media",
                        displayName = uri.lastPathSegment ?: "External Media",
                        durationMs = 0L,
                        sizeBytes = 0L,
                        dateAdded = System.currentTimeMillis(),
                        dateModified = System.currentTimeMillis(),
                        mimeType = intent.type ?: "video/*",
                        isVideo = !(intent.type?.startsWith("audio/") ?: false),
                        folderName = "External",
                        folderPath = "External"
                    )
                    playerManager.playMedia(dummyMedia)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error handling external intent", e)
        }
    }

    private fun checkStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
    }
}
