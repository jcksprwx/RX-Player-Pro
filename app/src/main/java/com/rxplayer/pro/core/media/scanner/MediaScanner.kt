package com.rxplayer.pro.core.media.scanner

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.rxplayer.pro.core.common.FileUtils
import com.rxplayer.pro.core.database.dao.HiddenFolderDao
import com.rxplayer.pro.core.database.dao.MediaDao
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDao: MediaDao,
    private val hiddenFolderDao: HiddenFolderDao
) {
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    suspend fun scanMediaLibrary(): Int = withContext(Dispatchers.IO) {
        if (_isScanning.value) return@withContext 0
        _isScanning.value = true
        _scanProgress.value = 0.05f

        val scannedFiles = mutableListOf<MediaFileEntity>()

        try {
            // 1. Scan Videos from MediaStore
            val videoFiles = scanVideos()
            scannedFiles.addAll(videoFiles)
            _scanProgress.value = 0.5f

            // 2. Scan Music/Audio from MediaStore
            val audioFiles = scanAudio()
            scannedFiles.addAll(audioFiles)
            _scanProgress.value = 0.85f

            // 3. Persist to Room
            if (scannedFiles.isNotEmpty()) {
                mediaDao.insertMediaList(scannedFiles)

                // Clean up removed files
                val existingPaths = scannedFiles.map { it.path }
                mediaDao.removeStalePaths(existingPaths)
            }
            _scanProgress.value = 1.0f
        } catch (e: Throwable) {
            Log.e("MediaScanner", "Error scanning media library", e)
        } finally {
            _isScanning.value = false
        }

        return@withContext scannedFiles.size
    }

    private suspend fun scanVideos(): List<MediaFileEntity> {
        val result = mutableListOf<MediaFileEntity>()
        try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT
            )

            val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = if (idCol != -1) cursor.getLong(idCol) else 0L
                    val displayName = if (nameCol != -1) cursor.getString(nameCol) ?: "Video_$id" else "Video_$id"
                    val title = if (titleCol != -1) cursor.getString(titleCol) ?: FileUtils.getFileNameWithoutExtension(displayName) else FileUtils.getFileNameWithoutExtension(displayName)
                    val duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val dateAdded = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) else System.currentTimeMillis()
                    val dateMod = if (dateModCol != -1) cursor.getLong(dateModCol) else System.currentTimeMillis()
                    val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "video/*" else "video/*"
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                    val height = if (heightCol != -1) cursor.getInt(heightCol) else 0
                    val resolution = if (width > 0 && height > 0) "${width}x${height}" else ""

                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val file = if (path.isNotEmpty()) File(path) else null
                    val folderPath = file?.parent ?: "Internal Storage"
                    val folderName = file?.parentFile?.name ?: "Videos"

                    result.add(
                        MediaFileEntity(
                            uriString = contentUri.toString(),
                            path = path,
                            title = title,
                            displayName = displayName,
                            durationMs = duration,
                            sizeBytes = size,
                            dateAdded = dateAdded,
                            dateModified = dateMod,
                            mimeType = mime,
                            isVideo = true,
                            folderName = folderName,
                            folderPath = folderPath,
                            width = width,
                            height = height,
                            resolution = resolution
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            Log.e("MediaScanner", "Error querying videos", e)
        }
        return result
    }

    private suspend fun scanAudio(): List<MediaFileEntity> {
        val result = mutableListOf<MediaFileEntity>()
        try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = if (idCol != -1) cursor.getLong(idCol) else 0L
                    val displayName = if (nameCol != -1) cursor.getString(nameCol) ?: "Audio_$id" else "Audio_$id"
                    val title = if (titleCol != -1) cursor.getString(titleCol) ?: FileUtils.getFileNameWithoutExtension(displayName) else FileUtils.getFileNameWithoutExtension(displayName)
                    val duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val dateAdded = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) else System.currentTimeMillis()
                    val dateMod = if (dateModCol != -1) cursor.getLong(dateModCol) else System.currentTimeMillis()
                    val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "audio/*" else "audio/*"
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val artist = if (artistCol != -1) cursor.getString(artistCol) ?: "Unknown Artist" else "Unknown Artist"
                    val album = if (albumCol != -1) cursor.getString(albumCol) ?: "Unknown Album" else "Unknown Album"
                    val albumId = if (albumIdCol != -1) cursor.getLong(albumIdCol) else 0L

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val file = if (path.isNotEmpty()) File(path) else null
                    val folderPath = file?.parent ?: "Music"
                    val folderName = file?.parentFile?.name ?: "Music"

                    result.add(
                        MediaFileEntity(
                            uriString = contentUri.toString(),
                            path = path,
                            title = title,
                            displayName = displayName,
                            durationMs = duration,
                            sizeBytes = size,
                            dateAdded = dateAdded,
                            dateModified = dateMod,
                            mimeType = mime,
                            isVideo = false,
                            folderName = folderName,
                            folderPath = folderPath,
                            artist = artist,
                            album = album,
                            albumId = albumId
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            Log.e("MediaScanner", "Error querying audio", e)
        }
        return result
    }
}
