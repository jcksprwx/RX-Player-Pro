package com.rxplayer.pro.core.common

import java.io.File
import java.text.DecimalFormat
import java.util.Locale

object FileUtils {
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return DecimalFormat("#,##0.#").format(value) + " " + units[digitGroups]
    }

    fun getExtension(pathOrName: String): String {
        val lastDot = pathOrName.lastIndexOf('.')
        return if (lastDot != -1 && lastDot < pathOrName.length - 1) {
            pathOrName.substring(lastDot + 1).lowercase(Locale.ROOT)
        } else ""
    }

    fun getFileNameWithoutExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot > 0) fileName.substring(0, lastDot) else fileName
    }

    fun isVideoFile(path: String): Boolean {
        val ext = getExtension(path)
        return ext in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "mpeg", "ts", "m4v")
    }

    fun isAudioFile(path: String): Boolean {
        val ext = getExtension(path)
        return ext in listOf("mp3", "aac", "flac", "wav", "opus", "ogg", "m4a", "wma", "mid")
    }

    fun isSubtitleFile(path: String): Boolean {
        val ext = getExtension(path)
        return ext in listOf("srt", "ass", "ssa", "vtt", "sub", "idx", "smi", "txt", "mpl", "pjs")
    }
}
