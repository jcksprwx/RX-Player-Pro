package com.rxplayer.pro.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rxplayer.pro.core.security.SubtitleConfig
import com.rxplayer.pro.core.security.ThemeMode
import com.rxplayer.pro.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val autoResume by viewModel.autoResume.collectAsStateWithLifecycle()
    val backgroundPlayback by viewModel.backgroundPlayback.collectAsStateWithLifecycle()
    val useAsAudioPlayer by viewModel.useAsAudioPlayer.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val subtitleConfig by viewModel.subtitleConfig.collectAsStateWithLifecycle()
    val currentAestheticTheme by viewModel.aestheticTheme.collectAsStateWithLifecycle()

    var showClearHistoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = NovaTextPrimary) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Playback Category
            SettingsCategory(title = "Playback") {
                SettingSwitchItem(
                    title = "Auto Resume Playback",
                    subtitle = "Resume videos and tracks from where you left off",
                    checked = autoResume,
                    onCheckedChange = { viewModel.toggleAutoResume(it) }
                )
                SettingSwitchItem(
                    title = "Background Audio Playback",
                    subtitle = "Continue playing audio with screen off or app minimized",
                    checked = backgroundPlayback,
                    onCheckedChange = { viewModel.toggleBackgroundPlayback(it) }
                )
                SettingSwitchItem(
                    title = "Use as Audio Player (Background Video)",
                    subtitle = "When turned on, exiting the video player keeps audio playing in background & taskbar",
                    checked = useAsAudioPlayer,
                    onCheckedChange = { viewModel.setUseAsAudioPlayer(it) }
                )
            }

            // Audio & Equalizer Category
            SettingsCategory(title = "Audio") {
                SettingClickableItem(
                    icon = Icons.Default.GraphicEq,
                    title = "Audio Equalizer & Effects",
                    subtitle = "Tune frequencies, bass boost, and virtualizer",
                    onClick = onNavigateToEqualizer
                )
            }

            // Interface & Theme Category
            SettingsCategory(title = "Interface & Aesthetic Theme") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = NovaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Choose AMOLED Dark, Clean Light, or Follow System",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val isSelected = themeMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.displayName, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Aesthetic Accent Palette",
                        style = MaterialTheme.typography.titleMedium,
                        color = NovaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Customize the primary accent color and glow effects",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaTextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val allThemes = AestheticTheme.entries
                    val themeRow1 = allThemes.take(4)
                    val themeRow2 = allThemes.drop(4)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(themeRow1, themeRow2).forEach { rowList ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                rowList.forEach { theme ->
                                    val isSelected = (theme == currentAestheticTheme)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable { viewModel.setAestheticTheme(theme) }
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(theme.primary)
                                                .then(
                                                    if (isSelected) {
                                                        Modifier.border(2.5.dp, Color.White, CircleShape)
                                                    } else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (theme == AestheticTheme.MX_PLAYER) "MX Blue" else theme.displayName.split(" ").first(),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) theme.primary else NovaTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = NovaDivider)

                SettingSwitchItem(
                    title = "Default Grid View Layout",
                    subtitle = "Display media in a visual thumbnail grid by default",
                    checked = isGridView,
                    onCheckedChange = { viewModel.setGridView(it) }
                )
            }

            // Subtitles Category
            SettingsCategory(title = "Subtitles & Captions") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Default Subtitle Size",
                        style = MaterialTheme.typography.titleMedium,
                        color = NovaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(16f to "Small", 20f to "Normal", 26f to "Large", 32f to "XL").forEach { (size, label) ->
                            FilterChip(
                                selected = subtitleConfig.textSizeSp == size,
                                onClick = { viewModel.updateSubtitleConfig(subtitleConfig.copy(textSizeSp = size)) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Default Subtitle Color",
                        style = MaterialTheme.typography.titleMedium,
                        color = NovaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            0xFFFFFFFFL to "White",
                            0xFFFFEB3BL to "Yellow",
                            0xFF00E5FFL to "Cyan",
                            0xFF00E676L to "Green"
                        ).forEach { (colorArgb, label) ->
                            FilterChip(
                                selected = subtitleConfig.textColorArgb == colorArgb,
                                onClick = { viewModel.updateSubtitleConfig(subtitleConfig.copy(textColorArgb = colorArgb)) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Default Screen Position",
                        style = MaterialTheme.typography.titleMedium,
                        color = NovaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            0.08f to "Bottom",
                            0.45f to "Center",
                            0.82f to "Top"
                        ).forEach { (fraction, label) ->
                            FilterChip(
                                selected = kotlin.math.abs(subtitleConfig.positionFraction - fraction) < 0.05f,
                                onClick = { viewModel.updateSubtitleConfig(subtitleConfig.copy(positionFraction = fraction)) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Privacy & Security Category
            SettingsCategory(title = "Privacy & Security") {
                SettingSwitchItem(
                    title = "App Lock (Biometrics & PIN)",
                    subtitle = "Require device authentication to open RX Player Pro",
                    checked = isAppLockEnabled,
                    onCheckedChange = { viewModel.toggleAppLock(it) }
                )
                SettingClickableItem(
                    icon = Icons.Default.DeleteSweep,
                    title = "Clear Watch History",
                    subtitle = "Delete all saved playback positions and history",
                    onClick = { showClearHistoryDialog = true }
                )
                SettingClickableItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "Strict zero-telemetry, offline-first commitment",
                    onClick = onNavigateToPrivacy
                )
            }

            // About Category
            SettingsCategory(title = "About") {
                SettingClickableItem(
                    icon = Icons.Default.Info,
                    title = "About RX Player Pro",
                    subtitle = "Version 2.0.0 Pro • Ad-Free Architecture",
                    onClick = onNavigateToAbout
                )
            }
        }

        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("Clear Watch History?", color = NovaTextPrimary) },
                text = { Text("This will reset all playback progress for previously played videos.", color = NovaTextSecondary) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearWatchHistory()
                        showClearHistoryDialog = false
                    }) {
                        Text("Clear", color = NovaAccentRose)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("Cancel", color = NovaTextSecondary)
                    }
                },
                containerColor = NovaDarkSurfaceElevated
            )
        }
    }
}

@Composable
fun SettingsCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = NovaElectricCyan,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, NovaElectricCyan.copy(alpha = 0.15f))
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = NovaTextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NovaElectricCyan,
                checkedTrackColor = NovaElectricCyan.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun SettingClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = NovaElectricViolet)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = NovaTextSecondary)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NovaTextSecondary)
    }
}
