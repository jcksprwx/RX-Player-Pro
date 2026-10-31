package com.rxplayer.pro.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_history",
    indices = [
        Index(value = ["mediaId"]),
        Index(value = ["lastPlayedTimestamp"])
    ]
)
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: Long,
    val lastPositionMs: Long,
    val durationMs: Long,
    val lastPlayedTimestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "playback_positions",
    indices = [
        Index(value = ["mediaId"])
    ]
)
data class PlaybackPositionEntity(
    @PrimaryKey
    val mediaId: Long,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "favorite_media",
    indices = [
        Index(value = ["mediaId"])
    ]
)
data class FavoriteMediaEntity(
    @PrimaryKey
    val mediaId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "hidden_folders")
data class HiddenFolderEntity(
    @PrimaryKey
    val folderPath: String,
    val isHidden: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "network_servers")
data class NetworkServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 445,
    val shareName: String = "",
    val domain: String = "",
    val username: String = "",
    val encryptedPasswordKey: String = "",
    val isAnonymous: Boolean = false,
    val lastConnected: Long = 0L
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
