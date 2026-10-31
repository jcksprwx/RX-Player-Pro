package com.rxplayer.pro.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Raw text & neutral colors
val RawDarkTextPrimary = Color(0xFFF1F5F9)
val RawDarkTextSecondary = Color(0xFF94A3B8)
val RawDarkTextTertiary = Color(0xFF64748B)

val RawLightTextPrimary = Color(0xFF0F172A)
val RawLightTextSecondary = Color(0xFF475569)
val RawLightTextTertiary = Color(0xFF64748B)
val RawLightDivider = Color(0xFFCBD5E1)

// Dynamic theme-aware accessors:
// These dynamically link all screens, buttons, sliders, badges, and controls
// to the user's actively selected Aesthetic Theme and Light/Dark mode.
val NovaElectricCyan: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAestheticTheme.current.primary

val NovaElectricViolet: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAestheticTheme.current.secondary

val NovaCyanGlow: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAestheticTheme.current.glow

val NovaVioletGlow: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAestheticTheme.current.secondary.copy(alpha = 0.5f)

val NovaAccentAmber: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAestheticTheme.current.glow

val NovaAccentEmerald: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAestheticTheme.current.primary

val NovaAccentRose: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAestheticTheme.current.secondary

val NovaAmoledBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDark.current) LocalAestheticTheme.current.darkBackground else LocalAestheticTheme.current.lightBackground

val NovaDarkSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDark.current) LocalAestheticTheme.current.darkSurface else LocalAestheticTheme.current.lightSurface

val NovaDarkSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDark.current) LocalAestheticTheme.current.darkSurfaceVariant else LocalAestheticTheme.current.lightSurfaceVariant

val NovaDarkSurfaceElevated: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDark.current) LocalAestheticTheme.current.darkSurfaceElevated else LocalAestheticTheme.current.lightSurfaceVariant

val NovaTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDark.current) RawDarkTextPrimary else RawLightTextPrimary

val NovaTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDark.current) RawDarkTextSecondary else RawLightTextSecondary

val NovaTextTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDark.current) RawDarkTextTertiary else RawLightTextTertiary

val NovaDivider: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDark.current) LocalAestheticTheme.current.darkSurfaceVariant else RawLightDivider
