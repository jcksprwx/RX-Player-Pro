package com.rxplayer.pro

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

@HiltAndroidApp
class NovaApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val crashReport = sw.toString()
                Log.e("NovaCrash", "FATAL UNCAUGHT EXCEPTION:\n$crashReport")

                val crashFile = File(filesDir, "crash_log.txt")
                crashFile.writeText(crashReport)
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return try {
            ImageLoader.Builder(this)
                .components {
                    add(VideoFrameDecoder.Factory())
                }
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.20)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizePercent(0.05)
                        .build()
                }
                .crossfade(true)
                .build()
        } catch (e: Throwable) {
            Log.e("NovaApp", "Failed to initialize custom ImageLoader, falling back to default", e)
            ImageLoader(this)
        }
    }
}
