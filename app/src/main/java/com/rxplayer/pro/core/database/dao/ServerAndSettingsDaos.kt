package com.rxplayer.pro.core.database.dao

import androidx.room.*
import com.rxplayer.pro.core.database.entity.HiddenFolderEntity
import com.rxplayer.pro.core.database.entity.NetworkServerEntity
import com.rxplayer.pro.core.database.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: NetworkServerEntity): Long

    @Update
    suspend fun updateServer(server: NetworkServerEntity)

    @Delete
    suspend fun deleteServer(server: NetworkServerEntity)

    @Query("SELECT * FROM network_servers WHERE id = :id LIMIT 1")
    suspend fun getServerById(id: Long): NetworkServerEntity?

    @Query("SELECT * FROM network_servers ORDER BY lastConnected DESC, name ASC")
    fun getAllServersFlow(): Flow<List<NetworkServerEntity>>

    @Query("UPDATE network_servers SET lastConnected = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: Long, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: UserSettingsEntity)

    @Query("SELECT value FROM user_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Query("SELECT value FROM user_settings WHERE `key` = :key LIMIT 1")
    fun getSettingValueFlow(key: String): Flow<String?>

    @Query("DELETE FROM user_settings WHERE `key` = :key")
    suspend fun removeSetting(key: String)
}

@Dao
interface HiddenFolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideFolder(folder: HiddenFolderEntity)

    @Delete
    suspend fun unhideFolder(folder: HiddenFolderEntity)

    @Query("DELETE FROM hidden_folders WHERE folderPath = :path")
    suspend fun unhideByPath(path: String)

    @Query("DELETE FROM hidden_folders")
    suspend fun unhideAll()

    @Query("SELECT * FROM hidden_folders")
    fun getHiddenFoldersFlow(): Flow<List<HiddenFolderEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM hidden_folders WHERE folderPath = :path AND isHidden = 1)")
    suspend fun isFolderHidden(path: String): Boolean
}
