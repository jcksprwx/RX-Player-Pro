package com.rxplayer.pro.core.common

import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {
    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return "00:00"

        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatDurationDetailed(durationMs: Long): String {
        if (durationMs <= 0) return "0s"
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        val builder = StringBuilder()
        if (hours > 0) builder.append("${hours}h ")
        if (minutes > 0 || hours > 0) builder.append("${minutes}m ")
        builder.append("${seconds}s")
        return builder.toString().trim()
    }
}
