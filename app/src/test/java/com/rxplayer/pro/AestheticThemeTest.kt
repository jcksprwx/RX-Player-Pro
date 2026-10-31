package com.rxplayer.pro

import com.rxplayer.pro.core.theme.AestheticTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AestheticThemeTest {

    @Test
    fun testDefaultThemeFallback() {
        val theme = AestheticTheme.fromId("unknown_color")
        assertEquals(AestheticTheme.CYBERPUNK_CYAN, theme)
    }

    @Test
    fun testAllThemesResolvable() {
        assertEquals(AestheticTheme.CYBERPUNK_CYAN, AestheticTheme.fromId("cyan"))
        assertEquals(AestheticTheme.ELECTRIC_VIOLET, AestheticTheme.fromId("violet"))
        assertEquals(AestheticTheme.EMERALD_MATRIX, AestheticTheme.fromId("emerald"))
        assertEquals(AestheticTheme.SUNSET_GOLD, AestheticTheme.fromId("gold"))
        assertEquals(AestheticTheme.CRIMSON_ROSE, AestheticTheme.fromId("rose"))
        assertEquals(AestheticTheme.OCEAN_SAPPHIRE, AestheticTheme.fromId("sapphire"))
        assertEquals(AestheticTheme.MX_PLAYER, AestheticTheme.fromId("mx"))
        assertEquals(AestheticTheme.MX_PLAYER, AestheticTheme.fromId("mx_blue"))
    }

    @Test
    fun testThemeColorsNotNull() {
        for (theme in AestheticTheme.entries) {
            assertNotNull(theme.primary)
            assertNotNull(theme.secondary)
            assertNotNull(theme.glow)
            assertNotNull(theme.displayName)
        }
    }
}
