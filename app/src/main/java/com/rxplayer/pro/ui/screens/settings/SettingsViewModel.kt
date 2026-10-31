package com.rxplayer.pro.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rxplayer.pro.core.database.dao.HistoryDao
import com.rxplayer.pro.core.database.dao.SettingsDao
import com.rxplayer.pro.core.database.entity.UserSettingsEntity
import com.rxplayer.pro.core.security.SecureStorage
import com.rxplayer.pro.core.security.SubtitleConfig
import com.rxplayer.pro.core.security.ThemeMode
import com.rxplayer.pro.core.theme.AestheticTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDao: SettingsDao,
    private val historyDao: HistoryDao,
    private val secureStorage: SecureStorage
) : ViewModel() {

    val aestheticTheme: StateFlow<AestheticTheme> = secureStorage.aestheticThemeFlow
    val themeMode: StateFlow<ThemeMode> = secureStorage.themeModeFlow
    val useAsAudioPlayer: StateFlow<Boolean> = secureStorage.useAsAudioPlayerFlow
    val isGridView: StateFlow<Boolean> = secureStorage.isGridViewFlow
    val subtitleConfig: StateFlow<SubtitleConfig> = secureStorage.subtitleConfigFlow

    private val _isAppLockEnabled = MutableStateFlow(secureStorage.isAppLockEnabled())
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _autoResume = MutableStateFlow(true)
    val autoResume: StateFlow<Boolean> = _autoResume.asStateFlow()

    private val _backgroundPlayback = MutableStateFlow(true)
    val backgroundPlayback: StateFlow<Boolean> = _backgroundPlayback.asStateFlow()

    fun setAestheticTheme(theme: AestheticTheme) {
        secureStorage.setAestheticTheme(theme)
    }

    fun setThemeMode(mode: ThemeMode) {
        secureStorage.setThemeMode(mode)
    }

    fun setUseAsAudioPlayer(enabled: Boolean) {
        secureStorage.setUseAsAudioPlayer(enabled)
    }

    fun setGridView(isGrid: Boolean) {
        secureStorage.setGridView(isGrid)
    }

    fun updateSubtitleConfig(config: SubtitleConfig) {
        secureStorage.updateSubtitleConfig(config)
    }

    fun toggleAppLock(enabled: Boolean) {
        secureStorage.setAppLockEnabled(enabled)
        _isAppLockEnabled.value = enabled
    }

    fun toggleAutoResume(enabled: Boolean) {
        _autoResume.value = enabled
        viewModelScope.launch {
            settingsDao.setSetting(UserSettingsEntity("auto_resume", enabled.toString()))
        }
    }

    fun toggleBackgroundPlayback(enabled: Boolean) {
        _backgroundPlayback.value = enabled
        viewModelScope.launch {
            settingsDao.setSetting(UserSettingsEntity("background_playback", enabled.toString()))
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            historyDao.clearAllWatchHistory()
        }
    }
}
