package com.rxplayer.pro.core.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AestheticTheme(
    val id: String,
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val glow: Color,
    val darkBackground: Color,
    val darkSurface: Color,
    val darkSurfaceVariant: Color,
    val darkSurfaceElevated: Color,
    val lightBackground: Color,
    val lightSurface: Color,
    val lightSurfaceVariant: Color
) {
    CYBERPUNK_CYAN(
        id = "cyan",
        displayName = "Cyberpunk Cyan",
        primary = Color(0xFF00E5FF),
        secondary = Color(0xFF7C4DFF),
        glow = Color(0xFF80F2FF),
        darkBackground = Color(0xFF06090E),
        darkSurface = Color(0xFF0D141F),
        darkSurfaceVariant = Color(0xFF142032),
        darkSurfaceElevated = Color(0xFF1C2C44),
        lightBackground = Color(0xFFF0F9FF),
        lightSurface = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFE0F2FE)
    ),
    MX_PLAYER(
        id = "mx",
        displayName = "MX Classic Blue",
        primary = Color(0xFF0078FF),
        secondary = Color(0xFF00B4D8),
        glow = Color(0xFF60A5FA),
        darkBackground = Color(0xFF0B0F19),
        darkSurface = Color(0xFF131926),
        darkSurfaceVariant = Color(0xFF1F293D),
        darkSurfaceElevated = Color(0xFF28364F),
        lightBackground = Color(0xFFF4F6F9),
        lightSurface = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFE5E9F0)
    ),
    ELECTRIC_VIOLET(
        id = "violet",
        displayName = "Electric Violet",
        primary = Color(0xFFA855F7),
        secondary = Color(0xFFEC4899),
        glow = Color(0xFFD8B4FE),
        darkBackground = Color(0xFF0B0614),
        darkSurface = Color(0xFF170D28),
        darkSurfaceVariant = Color(0xFF261642),
        darkSurfaceElevated = Color(0xFF351F5B),
        lightBackground = Color(0xFFFAF5FF),
        lightSurface = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFF3E8FF)
    ),
    EMERALD_MATRIX(
        id = "emerald",
        displayName = "Emerald Matrix",
        primary = Color(0xFF00E676),
        secondary = Color(0xFF00B0FF),
        glow = Color(0xFF69F0AE),
        darkBackground = Color(0xFF040F08),
        darkSurface = Color(0xFF0A1F13),
        darkSurfaceVariant = Color(0xFF123320),
        darkSurfaceElevated = Color(0xFF1B472D),
        lightBackground = Color(0xFFF0FDF4),
        lightSurface = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFDCFCE7)
    ),
    SUNSET_GOLD(
        id = "gold",
        displayName = "Sunset Gold",
        primary = Color(0xFFFFB300),
        secondary = Color(0xFFFF5722),
        glow = Color(0xFFFFE082),
        darkBackground = Color(0xFF0F0B04),
        darkSurface = Color(0xFF1E1609),
        darkSurfaceVariant = Color(0xFF32240F),
        darkSurfaceElevated = Color(0xFF453216),
        lightBackground = Color(0xFFFFFBEB),
        lightSurface = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFFEF3C7)
    ),
    CRIMSON_ROSE(
        id = "rose",
        displayName = "Crimson Rose",
        primary = Color(0xFFFF1744),
        secondary = Color(0xFFE040FB),
        glow = Color(0xFFFF80AB),
        darkBackground = Color(0xFF120508),
        darkSurface = Color(0xFF220B11),
        darkSurfaceVariant = Color(0xFF36121C),
        darkSurfaceElevated = Color(0xFF4B1A27),
        lightBackground = Color(0xFFFFF1F2),
        lightSurface = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFFFE4E6)
    ),
    OCEAN_SAPPHIRE(
        id = "sapphire",
        displayName = "Ocean Sapphire",
        primary = Color(0xFF2979FF),
        secondary = Color(0xFF00E5FF),
        glow = Color(0xFF82B1FF),
        darkBackground = Color(0xFF040A14),
        darkSurface = Color(0xFF0A162B),
        darkSurfaceVariant = Color(0xFF122444),
        darkSurfaceElevated = Color(0xFF1A335F),
        lightBackground = Color(0xFFF0F7FF),
        lightSurface = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFDBEAFE)
    );

    companion object {
        fun fromId(id: String): AestheticTheme =
            entries.firstOrNull {
                it.id.equals(id, ignoreCase = true) ||
                (it == MX_PLAYER && (id.equals("mx_blue", ignoreCase = true) || id.equals("mx_player", ignoreCase = true)))
            } ?: CYBERPUNK_CYAN
    }
}

val LocalAestheticTheme = staticCompositionLocalOf { AestheticTheme.CYBERPUNK_CYAN }
val LocalIsDark = staticCompositionLocalOf { true }

fun buildDarkColorScheme(aesthetic: AestheticTheme) = darkColorScheme(
    primary = aesthetic.primary,
    onPrimary = Color.Black,
    primaryContainer = aesthetic.primary.copy(alpha = 0.22f),
    onPrimaryContainer = aesthetic.glow,
    secondary = aesthetic.secondary,
    onSecondary = Color.White,
    secondaryContainer = aesthetic.secondary.copy(alpha = 0.22f),
    onSecondaryContainer = aesthetic.glow,
    tertiary = aesthetic.glow,
    background = aesthetic.darkBackground,
    onBackground = RawDarkTextPrimary,
    surface = aesthetic.darkSurface,
    onSurface = RawDarkTextPrimary,
    surfaceVariant = aesthetic.darkSurfaceVariant,
    onSurfaceVariant = RawDarkTextSecondary,
    outline = aesthetic.darkSurfaceVariant
)

fun buildLightColorScheme(aesthetic: AestheticTheme) = lightColorScheme(
    primary = aesthetic.primary,
    onPrimary = Color.White,
    primaryContainer = aesthetic.primary.copy(alpha = 0.15f),
    onPrimaryContainer = Color.Black,
    secondary = aesthetic.secondary,
    onSecondary = Color.White,
    secondaryContainer = aesthetic.secondary.copy(alpha = 0.15f),
    onSecondaryContainer = Color.Black,
    tertiary = aesthetic.glow,
    background = aesthetic.lightBackground,
    onBackground = RawLightTextPrimary,
    surface = aesthetic.lightSurface,
    onSurface = RawLightTextPrimary,
    surfaceVariant = aesthetic.lightSurfaceVariant,
    onSurfaceVariant = RawLightTextSecondary,
    outline = RawLightDivider
)

@Composable
fun NovaPlayerTheme(
    aestheticTheme: AestheticTheme = AestheticTheme.CYBERPUNK_CYAN,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        buildDarkColorScheme(aestheticTheme)
    } else {
        buildLightColorScheme(aestheticTheme)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalAestheticTheme provides aestheticTheme,
        LocalIsDark provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NovaTypography,
            content = content
        )
    }
}
