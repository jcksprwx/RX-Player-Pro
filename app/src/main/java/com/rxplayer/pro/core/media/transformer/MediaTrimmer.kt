package com.rxplayer.pro.core.media.transformer

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

data class TrimProgress(
    val currentMs: Long,
    val totalMs: Long,
    val progress: Float
)

@Singleton
class MediaTrimmer @Inject constructor() {

    suspend fun trimVideo(
        sourcePath: String,
        destPath: String,
        startMs: Long,
        endMs: Long,
        onProgress: (TrimProgress) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(sourcePath)
            val trackCount = extractor.trackCount

            muxer = MediaMuxer(destPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val indexMap = HashMap<Int, Int>(trackCount)
            var bufferSize = -1

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val dstIndex = muxer.addTrack(format)
                    indexMap[i] = dstIndex
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        bufferSize = maxOf(newSize, bufferSize)
                    }
                }
            }

            if (bufferSize < 0) {
                bufferSize = 2 * 1024 * 1024 // 2MB default buffer
            }

            val startUs = startMs * 1000
            val endUs = endMs * 1000
            val durationUs = endUs - startUs

            // Seek to start position at sync keyframe
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            muxer.start()

            val dstBuf = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(dstBuf, 0)
                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                } else {
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    if (bufferInfo.presentationTimeUs > endUs) {
                        break
                    } else if (bufferInfo.presentationTimeUs >= startUs) {
                        val flags = extractor.sampleFlags
                        bufferInfo.flags = flags
                        val trackIndex = extractor.sampleTrackIndex
                        val muxerTrackIndex = indexMap[trackIndex]
                        if (muxerTrackIndex != null) {
                            muxer.writeSampleData(muxerTrackIndex, dstBuf, bufferInfo)
                            val currentElapsed = (bufferInfo.presentationTimeUs - startUs) / 1000
                            val pct = if (durationUs > 0) (currentElapsed.toFloat() / (durationUs / 1000)).coerceIn(0f, 1f) else 1f
                            onProgress(TrimProgress(currentElapsed, durationUs / 1000, pct))
                        }
                    }
                    extractor.advance()
                }
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
                File(destPath).delete()
            } catch (_: Exception) {}
            return@withContext false
        }
    }
}
