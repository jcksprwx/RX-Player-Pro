package com.rxplayer.pro.core.database.dao

import androidx.room.*
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import com.rxplayer.pro.core.database.entity.PlaylistEntity
import com.rxplayer.pro.core.database.entity.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

data class PlaylistWithItemCount(
    @Embedded val playlist: PlaylistEntity,
    val itemCount: Int
)

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Query("""
        SELECT p.*, COUNT(pi.id) as itemCount
        FROM playlists p
        LEFT JOIN playlist_items pi ON p.id = pi.playlistId
        GROUP BY p.id
        ORDER BY p.createdAt DESC
    """)
    fun getAllPlaylistsWithCountFlow(): Flow<List<PlaylistWithItemCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity): Long

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun removePlaylistItem(playlistId: Long, mediaId: Long)

    @Query("""
        SELECT m.* FROM media_files m
        INNER JOIN playlist_items pi ON m.id = pi.mediaId
        WHERE pi.playlistId = :playlistId
        ORDER BY pi.sortOrder ASC, pi.addedAt ASC
    """)
    fun getPlaylistMediaFilesFlow(playlistId: Long): Flow<List<MediaFileEntity>>
}
