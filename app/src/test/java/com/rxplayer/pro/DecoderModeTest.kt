package com.rxplayer.pro

import com.rxplayer.pro.core.media.decoder.DecoderMode
import org.junit.Assert.assertEquals
import org.junit.Test

class DecoderModeTest {

    @Test
    fun testDecoderModesExist() {
        assertEquals("Auto", DecoderMode.AUTO.displayName)
        assertEquals("HW", DecoderMode.HW.displayName)
        assertEquals("SW", DecoderMode.SW.displayName)
    }
}
