package com.rxplayer.pro.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_files",
    indices = [
        Index(value = ["path"], unique = true),
        Index(value = ["folderPath"]),
        Index(value = ["isVideo"]),
        Index(value = ["dateModified"])
    ]
)
data class MediaFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uriString: String,
    val path: String,
    val title: String,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val mimeType: String,
    val isVideo: Boolean,
    val folderName: String,
    val folderPath: String,
    val width: Int = 0,
    val height: Int = 0,
    val resolution: String = "",
    val bitrate: Long = 0,
    val artist: String = "",
    val album: String = "",
    val albumId: Long = 0L
)
