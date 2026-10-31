package com.rxplayer.pro

import com.rxplayer.pro.core.common.FileUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileUtilsTest {

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", FileUtils.formatFileSize(0))
        assertEquals("1 KB", FileUtils.formatFileSize(1024))
        assertEquals("1.5 MB", FileUtils.formatFileSize((1.5 * 1024 * 1024).toLong()))
        assertEquals("2 GB", FileUtils.formatFileSize((2L * 1024 * 1024 * 1024)))
    }

    @Test
    fun testFileExtensions() {
        assertEquals("mp4", FileUtils.getExtension("movie.sample.mp4"))
        assertEquals("mkv", FileUtils.getExtension("video.MKV"))
        assertEquals("srt", FileUtils.getExtension("/storage/emulated/0/subs/sub.srt"))
        assertEquals("movie", FileUtils.getFileNameWithoutExtension("movie.mp4"))
    }

    @Test
    fun testMediaTypeDetection() {
        assertTrue(FileUtils.isVideoFile("film.mkv"))
        assertTrue(FileUtils.isVideoFile("clip.mp4"))
        assertTrue(FileUtils.isAudioFile("track.flac"))
        assertTrue(FileUtils.isAudioFile("song.mp3"))
        assertTrue(FileUtils.isSubtitleFile("subtitles.vtt"))
    }
}
