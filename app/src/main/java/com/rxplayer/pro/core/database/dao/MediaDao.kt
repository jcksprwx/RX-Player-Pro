package com.rxplayer.pro.core.database.dao

import androidx.room.*
import com.rxplayer.pro.core.database.entity.MediaFileEntity
import kotlinx.coroutines.flow.Flow

data class FolderSummary(
    val folderPath: String,
    val folderName: String,
    val mediaCount: Int,
    val totalSizeBytes: Long
)

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaFileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaList(mediaList: List<MediaFileEntity>): List<Long>

    @Update
    suspend fun updateMedia(media: MediaFileEntity)

    @Delete
    suspend fun deleteMedia(media: MediaFileEntity)

    @Query("DELETE FROM media_files WHERE id = :id")
    suspend fun deleteMediaById(id: Long)

    @Query("SELECT * FROM media_files WHERE id = :id")
    suspend fun getMediaById(id: Long): MediaFileEntity?

    @Query("SELECT * FROM media_files WHERE path = :path LIMIT 1")
    suspend fun getMediaByPath(path: String): MediaFileEntity?

    @Query("SELECT * FROM media_files WHERE isVideo = 1 ORDER BY dateModified DESC")
    fun getAllVideosFlow(): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files WHERE isVideo = 0 ORDER BY title ASC")
    fun getAllMusicFlow(): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files WHERE isVideo = 1 ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentlyAddedVideosFlow(limit: Int = 10): Flow<List<MediaFileEntity>>

    @Query("""
        SELECT folderPath, folderName, COUNT(id) as mediaCount, SUM(sizeBytes) as totalSizeBytes 
        FROM media_files 
        WHERE isVideo = 1 AND folderPath NOT IN (SELECT folderPath FROM hidden_folders WHERE isHidden = 1)
        GROUP BY folderPath 
        ORDER BY folderName ASC
    """)
    fun getVideoFoldersFlow(): Flow<List<FolderSummary>>

    @Query("""
        SELECT folderPath, folderName, COUNT(id) as mediaCount, SUM(sizeBytes) as totalSizeBytes 
        FROM media_files 
        WHERE isVideo = 1
        GROUP BY folderPath 
        ORDER BY folderName ASC
    """)
    fun getAllVideoFoldersFlow(): Flow<List<FolderSummary>>

    @Query("SELECT * FROM media_files WHERE folderPath = :folderPath AND isVideo = 1 ORDER BY displayName ASC")
    fun getVideosInFolderFlow(folderPath: String): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files WHERE folderPath = :folderPath AND isVideo = 1 ORDER BY displayName ASC")
    suspend fun getVideosInFolder(folderPath: String): List<MediaFileEntity>

    @Query("SELECT * FROM media_files WHERE isVideo = 1 ORDER BY displayName ASC")
    suspend fun getAllVideos(): List<MediaFileEntity>

    @Query("""
        SELECT * FROM media_files 
        WHERE (title LIKE '%' || :query || '%' OR displayName LIKE '%' || :query || '%')
        ORDER BY dateModified DESC
    """)
    fun searchMediaFlow(query: String): Flow<List<MediaFileEntity>>

    @Query("SELECT COUNT(*) FROM media_files WHERE isVideo = 1")
    fun getVideoCountFlow(): Flow<Int>

    @Query("SELECT SUM(sizeBytes) FROM media_files WHERE isVideo = 1")
    fun getTotalVideoSizeBytesFlow(): Flow<Long?>

    @Query("SELECT DISTINCT artist FROM media_files WHERE isVideo = 0 AND artist != '' ORDER BY artist ASC")
    fun getArtistsFlow(): Flow<List<String>>

    @Query("SELECT DISTINCT album FROM media_files WHERE isVideo = 0 AND album != '' ORDER BY album ASC")
    fun getAlbumsFlow(): Flow<List<String>>

    @Query("SELECT * FROM media_files WHERE isVideo = 0 AND artist = :artist ORDER BY title ASC")
    fun getMusicByArtistFlow(artist: String): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files WHERE isVideo = 0 AND album = :album ORDER BY title ASC")
    fun getMusicByAlbumFlow(album: String): Flow<List<MediaFileEntity>>

    @Query("SELECT path FROM media_files")
    suspend fun getAllIndexedPaths(): List<String>

    @Query("DELETE FROM media_files WHERE path NOT IN (:existingPaths)")
    suspend fun removeStalePaths(existingPaths: List<String>)
}
