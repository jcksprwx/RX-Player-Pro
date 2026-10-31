package com.rxplayer.pro.core.database.dao

import androidx.room.*
import com.rxplayer.pro.core.database.entity.FavoriteMediaEntity
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import com.rxplayer.pro.core.database.entity.PlaybackPositionEntity
import com.rxplayer.pro.core.database.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

data class ContinueWatchingItem(
    @Embedded val media: MediaFileEntity,
    val lastPositionMs: Long,
    val lastPlayedTimestamp: Long,
    val isCompleted: Boolean
)

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchHistory(history: WatchHistoryEntity): Long

    @Query("""
        SELECT m.*, w.lastPositionMs, w.lastPlayedTimestamp, w.isCompleted 
        FROM media_files m
        INNER JOIN watch_history w ON m.id = w.mediaId
        WHERE w.isCompleted = 0 AND w.lastPositionMs > 5000 AND (w.durationMs - w.lastPositionMs) > 10000
        ORDER BY w.lastPlayedTimestamp DESC
        LIMIT :limit
    """)
    fun getContinueWatchingFlow(limit: Int = 10): Flow<List<ContinueWatchingItem>>

    @Query("""
        SELECT m.*, w.lastPositionMs, w.lastPlayedTimestamp, w.isCompleted 
        FROM media_files m
        INNER JOIN watch_history w ON m.id = w.mediaId
        ORDER BY w.lastPlayedTimestamp DESC
        LIMIT :limit
    """)
    fun getRecentlyPlayedFlow(limit: Int = 15): Flow<List<ContinueWatchingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlaybackPosition(position: PlaybackPositionEntity)

    @Query("SELECT * FROM playback_positions WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getPlaybackPosition(mediaId: Long): PlaybackPositionEntity?

    @Query("DELETE FROM playback_positions WHERE mediaId = :mediaId")
    suspend fun clearPlaybackPosition(mediaId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(fav: FavoriteMediaEntity)

    @Delete
    suspend fun removeFavorite(fav: FavoriteMediaEntity)

    @Query("DELETE FROM favorite_media WHERE mediaId = :mediaId")
    suspend fun removeFavoriteByMediaId(mediaId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_media WHERE mediaId = :mediaId)")
    fun isFavoriteFlow(mediaId: Long): Flow<Boolean>

    @Query("""
        SELECT m.* FROM media_files m
        INNER JOIN favorite_media f ON m.id = f.mediaId
        ORDER BY f.addedAt DESC
    """)
    fun getFavoritesFlow(): Flow<List<MediaFileEntity>>

    @Query("DELETE FROM watch_history")
    suspend fun clearAllWatchHistory()
}
