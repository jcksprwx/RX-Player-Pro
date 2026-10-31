package com.rxplayer.pro.ui.screens.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rxplayer.pro.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onBackClick: () -> Unit,
    viewModel: EqualizerViewModel = hiltViewModel()
) {
    val isEnabled by viewModel.equalizerManager.isEnabled.collectAsStateWithLifecycle()
    val bands by viewModel.equalizerManager.bands.collectAsStateWithLifecycle()
    val presets by viewModel.equalizerManager.presets.collectAsStateWithLifecycle()
    val currentPresetIndex by viewModel.equalizerManager.currentPresetIndex.collectAsStateWithLifecycle()
    val bassBoostStrength by viewModel.equalizerManager.bassBoostStrength.collectAsStateWithLifecycle()
    val virtualizerStrength by viewModel.equalizerManager.virtualizerStrength.collectAsStateWithLifecycle()

    var presetMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Equalizer", color = NovaTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NovaTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.reset() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = NovaElectricCyan)
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
            // Power Switch & Preset Picker Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnabled) "Equalizer Active" else "Equalizer Disabled",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isEnabled) NovaElectricCyan else NovaTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { viewModel.toggleEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NovaElectricCyan,
                                checkedTrackColor = NovaElectricCyan.copy(alpha = 0.3f)
                            )
                        )
                    }

                    if (presets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box {
                            OutlinedButton(
                                onClick = { presetMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isEnabled,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                val currentName = if (currentPresetIndex >= 0 && currentPresetIndex < presets.size) {
                                    presets[currentPresetIndex.toInt()]
                                } else "Custom"
                                Text("Preset: $currentName", color = if (isEnabled) NovaElectricCyan else NovaTextSecondary)
                            }

                            DropdownMenu(
                                expanded = presetMenuExpanded,
                                onDismissRequest = { presetMenuExpanded = false },
                                modifier = Modifier.background(NovaDarkSurfaceElevated)
                            ) {
                                presets.forEachIndexed { idx, name ->
                                    DropdownMenuItem(
                                        text = { Text(name, color = NovaTextPrimary) },
                                        onClick = {
                                            viewModel.selectPreset(idx.toShort())
                                            presetMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Frequency Bands
            Card(
                colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Graphic Equalizer (${if (bands.isNotEmpty()) "${bands.size}-Band" else "5-Band"})",
                        style = MaterialTheme.typography.titleMedium,
                        color = NovaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (bands.isEmpty()) {
                        // Fallback UI if audio effect isn't attached yet
                        listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz").forEach { freq ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(freq, color = NovaTextSecondary, modifier = Modifier.width(70.dp), fontSize = 12.sp)
                                Slider(
                                    value = 0.5f,
                                    onValueChange = {},
                                    enabled = isEnabled,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(thumbColor = NovaElectricCyan, activeTrackColor = NovaElectricCyan)
                                )
                                Text("0 dB", color = NovaTextSecondary, modifier = Modifier.width(44.dp), fontSize = 11.sp)
                            }
                        }
                    } else {
                        bands.forEach { band ->
                            val min = band.minLevelMilliBels.toFloat()
                            val max = band.maxLevelMilliBels.toFloat()
                            val cur = band.currentLevelMilliBels.toFloat()
                            val sliderVal = if (max > min) ((cur - min) / (max - min)).coerceIn(0f, 1f) else 0.5f

                            val freqLabel = if (band.centerFreqHz >= 1000) {
                                "${band.centerFreqHz / 1000} kHz"
                            } else {
                                "${band.centerFreqHz} Hz"
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(freqLabel, color = NovaTextSecondary, modifier = Modifier.width(70.dp), fontSize = 12.sp)
                                Slider(
                                    value = sliderVal,
                                    onValueChange = { frac ->
                                        val newLevel = (min + frac * (max - min)).toInt().toShort()
                                        viewModel.setBandLevel(band.index, newLevel)
                                    },
                                    enabled = isEnabled,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = NovaElectricCyan,
                                        activeTrackColor = NovaElectricCyan
                                    )
                                )
                                Text(
                                    text = "${band.currentLevelMilliBels / 100} dB",
                                    color = NovaTextPrimary,
                                    modifier = Modifier.width(44.dp),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Audio Effects: Bass Boost & 3D Virtualizer
            Card(
                colors = CardDefaults.cardColors(containerColor = NovaDarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Audio Effects", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Bass Boost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bass Booster", color = NovaTextPrimary, modifier = Modifier.width(100.dp), fontSize = 13.sp)
                        Slider(
                            value = (bassBoostStrength.toFloat() / 1000f).coerceIn(0f, 1f),
                            onValueChange = { frac ->
                                viewModel.setBassBoost((frac * 1000).toInt().toShort())
                            },
                            enabled = isEnabled,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = NovaElectricViolet,
                                activeTrackColor = NovaElectricViolet
                            )
                        )
                        Text(
                            text = "${((bassBoostStrength.toFloat() / 1000f) * 100).toInt()}%",
                            color = NovaVioletGlow,
                            modifier = Modifier.width(44.dp),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3D Virtualizer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3D Virtualizer", color = NovaTextPrimary, modifier = Modifier.width(100.dp), fontSize = 13.sp)
                        Slider(
                            value = (virtualizerStrength.toFloat() / 1000f).coerceIn(0f, 1f),
                            onValueChange = { frac ->
                                viewModel.setVirtualizer((frac * 1000).toInt().toShort())
                            },
                            enabled = isEnabled,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = NovaAccentAmber,
                                activeTrackColor = NovaAccentAmber
                            )
                        )
                        Text(
                            text = "${((virtualizerStrength.toFloat() / 1000f) * 100).toInt()}%",
                            color = NovaAccentAmber,
                            modifier = Modifier.width(44.dp),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
