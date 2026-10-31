package com.rxplayer.pro.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import com.rxplayer.pro.core.theme.AestheticTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode(val id: String, val displayName: String) {
    DARK("dark", "Dark (AMOLED)"),
    LIGHT("light", "Light Mode"),
    SYSTEM("system", "Follow System");

    companion object {
        fun fromId(id: String): ThemeMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DARK
    }
}

data class SubtitleConfig(
    val textSizeSp: Float = 20f,
    val textColorArgb: Long = 0xFFFFFFFFL,
    val bgColorArgb: Long = 0x80000000L,
    val positionFraction: Float = 0.08f,
    val delayMs: Long = 0L
)

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = initializePreferences()

    private val _aestheticThemeFlow = MutableStateFlow(AestheticTheme.fromId(getAestheticTheme()))
    val aestheticThemeFlow: StateFlow<AestheticTheme> = _aestheticThemeFlow.asStateFlow()

    private val _themeModeFlow = MutableStateFlow(ThemeMode.fromId(prefs.getString("theme_mode", "dark") ?: "dark"))
    val themeModeFlow: StateFlow<ThemeMode> = _themeModeFlow.asStateFlow()

    private val _useAsAudioPlayerFlow = MutableStateFlow(prefs.getBoolean("use_as_audio_player", false))
    val useAsAudioPlayerFlow: StateFlow<Boolean> = _useAsAudioPlayerFlow.asStateFlow()

    private val _videoSortOrderFlow = MutableStateFlow(prefs.getString("video_sort_order", "DATE_DESC") ?: "DATE_DESC")
    val videoSortOrderFlow: StateFlow<String> = _videoSortOrderFlow.asStateFlow()

    private val _isGridViewFlow = MutableStateFlow(prefs.getBoolean("is_grid_view", false))
    val isGridViewFlow: StateFlow<Boolean> = _isGridViewFlow.asStateFlow()

    private val _subtitleConfigFlow = MutableStateFlow(
        SubtitleConfig(
            textSizeSp = prefs.getFloat("sub_size_sp", 20f),
            textColorArgb = prefs.getLong("sub_color_argb", 0xFFFFFFFFL),
            bgColorArgb = prefs.getLong("sub_bg_argb", 0x80000000L),
            positionFraction = prefs.getFloat("sub_pos_fraction", 0.08f),
            delayMs = prefs.getLong("sub_delay_ms", 0L)
        )
    )
    val subtitleConfigFlow: StateFlow<SubtitleConfig> = _subtitleConfigFlow.asStateFlow()

    private fun initializePreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "nova_secure_storage",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            Log.w("SecureStorage", "Hardware Keystore encryption unavailable, falling back to private storage", e)
            context.getSharedPreferences("nova_secure_storage_fallback", Context.MODE_PRIVATE)
        }
    }

    fun savePassword(key: String, secret: String) {
        try {
            prefs.edit().putString(key, secret).apply()
        } catch (e: Throwable) {
            Log.e("SecureStorage", "Failed to save secure value", e)
        }
    }

    fun getPassword(key: String): String {
        return try {
            prefs.getString(key, "") ?: ""
        } catch (e: Throwable) {
            ""
        }
    }

    fun removePassword(key: String) {
        try {
            prefs.edit().remove(key).apply()
        } catch (e: Throwable) {
            Log.e("SecureStorage", "Failed to remove secure value", e)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        try {
            prefs.edit().putBoolean("app_lock_enabled", enabled).apply()
        } catch (e: Throwable) {
            Log.e("SecureStorage", "Failed to set app lock", e)
        }
    }

    fun isAppLockEnabled(): Boolean {
        return try {
            prefs.getBoolean("app_lock_enabled", false)
        } catch (e: Throwable) {
            false
        }
    }

    fun setAestheticTheme(theme: AestheticTheme) {
        try {
            prefs.edit().putString("aesthetic_theme", theme.id).apply()
            _aestheticThemeFlow.value = theme
        } catch (e: Throwable) {
            Log.e("SecureStorage", "Failed to set aesthetic theme", e)
        }
    }

    fun getAestheticTheme(): String {
        return try {
            prefs.getString("aesthetic_theme", "cyan") ?: "cyan"
        } catch (e: Throwable) {
            "cyan"
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        try {
            prefs.edit().putString("theme_mode", mode.id).apply()
            _themeModeFlow.value = mode
        } catch (e: Throwable) {
            Log.e("SecureStorage", "Failed to set theme mode", e)
        }
    }

    fun setUseAsAudioPlayer(enabled: Boolean) {
        try {
            prefs.edit().putBoolean("use_as_audio_player", enabled).apply()
            _useAsAudioPlayerFlow.value = enabled
        } catch (e: Throwable) {
            Log.e("SecureStorage", "Failed to set use_as_audio_player", e)
        }
    }

    fun setVideoSortOrder(order: String) {
        try {
            prefs.edit().putString("video_sort_order", order).apply()
            _videoSortOrderFlow.value = order
        } catch (e: Throwable) {
            Log.e("SecureStorage", "Failed to set video sort order", e)
        }
    }

    fun setGridView(isGrid: Boolean) {
        try {
            prefs.edit().putBoolean("is_grid_view", isGrid).apply()
            _isGridViewFlow.value = isGrid
        } catch (e: Throwable) {
            Log.e("SecureStorage", "Failed to set grid view", e)
        }
    }

    fun updateSubtitleConfig(config: SubtitleConfig) {
        try {
            prefs.edit()
                .putFloat("sub_size_sp", config.textSizeSp)
                .putLong("sub_color_argb", config.textColorArgb)
                .putLong("sub_bg_argb", config.bgColorArgb)
                .putFloat("sub_pos_fraction", config.positionFraction)
                .putLong("sub_delay_ms", config.delayMs)
                .apply()
            _subtitleConfigFlow.value = config
        } catch (e: Throwable) {
            Log.e("SecureStorage", "Failed to update subtitle config", e)
        }
    }
}
