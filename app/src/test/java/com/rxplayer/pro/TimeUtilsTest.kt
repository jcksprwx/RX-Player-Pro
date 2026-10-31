package com.rxplayer.pro

import com.rxplayer.pro.core.common.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {

    @Test
    fun testFormatDurationSecondsAndMinutes() {
        assertEquals("00:00", TimeUtils.formatDuration(0))
        assertEquals("00:45", TimeUtils.formatDuration(45000))
        assertEquals("02:30", TimeUtils.formatDuration(150000))
    }

    @Test
    fun testFormatDurationHours() {
        val oneHourTwoMins = (3600 + 120 + 5) * 1000L
        assertEquals("01:02:05", TimeUtils.formatDuration(oneHourTwoMins))
    }

    @Test
    fun testDetailedDuration() {
        val millis = (3600 * 2 + 60 * 15 + 30) * 1000L
        assertEquals("2h 15m 30s", TimeUtils.formatDurationDetailed(millis))
    }
}
