package com.rxplayer.pro.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rxplayer.pro.core.database.dao.*
import com.rxplayer.pro.core.database.entity.*

@Database(
    entities = [
        MediaFileEntity::class,
        VideoMetadataEntity::class,
        AudioMetadataEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        WatchHistoryEntity::class,
        PlaybackPositionEntity::class,
        FavoriteMediaEntity::class,
        HiddenFolderEntity::class,
        NetworkServerEntity::class,
        UserSettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun serverDao(): ServerDao
    abstract fun settingsDao(): SettingsDao
    abstract fun hiddenFolderDao(): HiddenFolderDao
}
