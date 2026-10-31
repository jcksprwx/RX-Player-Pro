package com.rxplayer.pro.core.media.transformer

import android.content.Context
import android.database.Cursor
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

data class VideoDetailedInfo(
    val fileName: String,
    val filePath: String,
    val fileSizeFormatted: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val resolution: String,
    val aspectRatioText: String,
    val frameRate: Float,
    val videoCodec: String,
    val audioCodec: String,
    val audioChannelsText: String,
    val sampleRateText: String,
    val bitrateKbps: Long,
    val audioBitrateKbps: Long,
    val containerFormat: String,
    val subtitleTracksCount: Int
)

@Singleton
class AudioExtractor @Inject constructor() {

    suspend fun extractAudioTrack(
        sourceVideoPath: String,
        destAudioPath: String,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(sourceVideoPath)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                extractor.release()
                return@withContext false
            }

            extractor.selectTrack(audioTrackIndex)

            val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                audioFormat.getLong(MediaFormat.KEY_DURATION)
            } else 1L

            muxer = MediaMuxer(destAudioPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrack = muxer.addTrack(audioFormat)
            muxer.start()

            var bufferSize = 256 * 1024
            if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                bufferSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            }

            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(muxerTrack, buffer, bufferInfo)

                if (durationUs > 0) {
                    val progress = (bufferInfo.presentationTimeUs.toFloat() / durationUs).coerceIn(0f, 1f)
                    onProgress(progress)
                }

                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                muxer?.release()
                extractor.release()
                File(destAudioPath).delete()
            } catch (_: Exception) {}
            return@withContext false
        }
    }
}

@Singleton
class MediaMetadataInspector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun inspectVideo(
        filePathOrUri: String,
        fallbackName: String = "",
        fallbackSize: Long = 0L,
        fallbackVideoCodec: String = "",
        fallbackAudioCodec: String = ""
    ): VideoDetailedInfo = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var videoCodec = fallbackVideoCodec
        var audioCodec = fallbackAudioCodec
        var frameRate = 0f
        var width = 0
        var height = 0
        var duration = 0L
        var bitrate = 0L
        var audioBitrate = 0L
        var sampleRate = 0
        var channels = 0
        var subtitleCount = 0
        var resolvedFileName = fallbackName
        var resolvedFileSize = fallbackSize

        // Resolve Content URI Display Name and Size if needed
        if (filePathOrUri.startsWith("content://")) {
            try {
                val uri = Uri.parse(filePathOrUri)
                val cursor: Cursor? = context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null, null, null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1 && resolvedFileName.isEmpty()) {
                            resolvedFileName = it.getString(nameIndex) ?: ""
                        }
                        if (sizeIndex != -1 && resolvedFileSize <= 0L) {
                            resolvedFileSize = it.getLong(sizeIndex)
                        }
                    }
                }
            } catch (_: Exception) {}
        } else {
            val file = File(filePathOrUri)
            if (resolvedFileName.isEmpty()) resolvedFileName = file.name
            if (resolvedFileSize <= 0L && file.exists()) resolvedFileSize = file.length()
        }

        if (resolvedFileName.isEmpty()) {
            resolvedFileName = filePathOrUri.substringAfterLast('/', "Video")
        }

        // 1. Inspect using MediaMetadataRetriever
        try {
            if (filePathOrUri.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(filePathOrUri))
            } else {
                retriever.setDataSource(filePathOrUri)
            }

            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val br = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
            if (br > 0) bitrate = br / 1000

            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
            if (mime.isNotEmpty() && videoCodec.isEmpty()) {
                videoCodec = mime
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        // 2. Safe Inspection using MediaExtractor (with type-safe format parsing)
        try {
            val extractor = MediaExtractor()
            if (filePathOrUri.startsWith("content://")) {
                extractor.setDataSource(context, Uri.parse(filePathOrUri), null)
            } else {
                extractor.setDataSource(filePathOrUri)
            }

            for (i in 0 until extractor.trackCount) {
                try {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

                    if (mime.startsWith("video/")) {
                        videoCodec = mime
                        if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                            frameRate = try {
                                format.getFloat(MediaFormat.KEY_FRAME_RATE)
                            } catch (_: Exception) {
                                try { format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat() } catch (_: Exception) { 0f }
                            }
                        }
                        if (format.containsKey(MediaFormat.KEY_WIDTH) && width == 0) {
                            width = format.getInteger(MediaFormat.KEY_WIDTH)
                        }
                        if (format.containsKey(MediaFormat.KEY_HEIGHT) && height == 0) {
                            height = format.getInteger(MediaFormat.KEY_HEIGHT)
                        }
                    } else if (mime.startsWith("audio/")) {
                        audioCodec = mime
                        if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = try { format.getInteger(MediaFormat.KEY_SAMPLE_RATE) } catch (_: Exception) { 0 }
                        }
                        if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channels = try { format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) } catch (_: Exception) { 0 }
                        }
                        if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                            val abr = try { format.getInteger(MediaFormat.KEY_BIT_RATE) } catch (_: Exception) { 0 }
                            if (abr > 0) audioBitrate = (abr / 1000).toLong()
                        }
                    } else if (mime.startsWith("text/") || mime.startsWith("application/")) {
                        subtitleCount++
                    }
                } catch (_: Exception) {
                    // Continue to next track if one track format fails parsing
                }
            }
            extractor.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sizeFormatted = com.rxplayer.pro.core.common.FileUtils.formatFileSize(resolvedFileSize)
        val container = com.rxplayer.pro.core.common.FileUtils.getExtension(resolvedFileName).uppercase()

        val friendlyVideoCodec = mapFriendlyVideoCodec(videoCodec)
        val friendlyAudioCodec = mapFriendlyAudioCodec(audioCodec)

        val aspectText = if (width > 0 && height > 0) {
            val gcd = findGcd(width, height)
            val wRatio = width / gcd
            val hRatio = height / gcd
            if (wRatio == 16 && hRatio == 9) "16:9 (Widescreen)"
            else if (wRatio == 4 && hRatio == 3) "4:3 (Standard)"
            else if (wRatio == 21 && hRatio == 9) "21:9 (Cinematic)"
            else "$wRatio:$hRatio"
        } else "Unknown"

        val channelsText = when (channels) {
            1 -> "Mono (1.0)"
            2 -> "Stereo (2.0)"
            6 -> "Surround 5.1"
            8 -> "Surround 7.1"
            else -> if (channels > 0) "$channels Channels" else "Stereo (2.0)"
        }

        val sampleRateText = if (sampleRate > 0) "${sampleRate / 1000f} kHz ($sampleRate Hz)" else "44.1 / 48.0 kHz"

        return@withContext VideoDetailedInfo(
            fileName = resolvedFileName,
            filePath = filePathOrUri,
            fileSizeFormatted = sizeFormatted,
            durationMs = duration,
            width = width,
            height = height,
            resolution = if (width > 0 && height > 0) "${width}x${height}" else "HD / 1080p",
            aspectRatioText = aspectText,
            frameRate = frameRate,
            videoCodec = friendlyVideoCodec,
            audioCodec = friendlyAudioCodec,
            audioChannelsText = channelsText,
            sampleRateText = sampleRateText,
            bitrateKbps = bitrate,
            audioBitrateKbps = audioBitrate,
            containerFormat = container.ifEmpty { "MP4" },
            subtitleTracksCount = subtitleCount
        )
    }

    private fun mapFriendlyVideoCodec(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("avc") || lower.contains("h264") || lower.contains("avc1") -> "H.264 / AVC"
            lower.contains("hevc") || lower.contains("h265") || lower.contains("hev1") || lower.contains("hvc1") -> "H.265 / HEVC"
            lower.contains("vp9") -> "Google VP9"
            lower.contains("vp8") -> "Google VP8"
            lower.contains("av01") || lower.contains("av1") -> "AOMedia AV1"
            lower.contains("mp4v") || lower.contains("mpeg4") -> "MPEG-4 Part 2"
            lower.contains("mpeg2") -> "MPEG-2"
            lower.isNotEmpty() && lower != "unknown" -> raw.removePrefix("video/").uppercase()
            else -> "H.264 / AVC (Hardware Decoded)"
        }
    }

    private fun mapFriendlyAudioCodec(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("mp4a") || lower.contains("aac") -> "AAC (Advanced Audio Coding)"
            lower.contains("mp3") || lower.contains("mpeg") -> "MP3 (MPEG Audio Layer 3)"
            lower.contains("eac3") -> "Dolby Digital Plus (E-AC-3)"
            lower.contains("ac3") -> "Dolby Digital (AC-3)"
            lower.contains("opus") -> "Opus Interactive Audio"
            lower.contains("flac") -> "FLAC (Free Lossless Audio)"
            lower.contains("vorbis") -> "Ogg Vorbis"
            lower.contains("dts") -> "DTS Digital Surround"
            lower.contains("raw") || lower.contains("pcm") -> "PCM Linear Audio"
            lower.isNotEmpty() && lower != "unknown" -> raw.removePrefix("audio/").uppercase()
            else -> "AAC / Dolby Compatible"
        }
    }

    private fun findGcd(a: Int, b: Int): Int {
        var n1 = a
        var n2 = b
        while (n2 != 0) {
            val t = n2
            n2 = n1 % n2
            n1 = t
        }
        return if (n1 > 0) n1 else 1
    }
}
