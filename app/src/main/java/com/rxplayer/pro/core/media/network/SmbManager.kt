package com.rxplayer.pro.core.media.network

import android.content.Context
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.rxplayer.pro.core.database.entity.NetworkServerEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Singleton

data class SmbItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long
)

@Singleton
class SmbManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = SMBClient()

    suspend fun testConnection(
        host: String,
        port: Int = 445,
        domain: String = "",
        username: String = "",
        password: String = "",
        isAnonymous: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var session: Session? = null
        try {
            connection = client.connect(host, port)
            val authContext = if (isAnonymous || username.isEmpty()) {
                AuthenticationContext.anonymous()
            } else {
                AuthenticationContext(username, password.toCharArray(), domain)
            }
            session = connection.authenticate(authContext)
            return@withContext session != null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            try { session?.close() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }
    }

    suspend fun listShareFiles(
        server: NetworkServerEntity,
        rawPassword: String,
        subPath: String = ""
    ): List<SmbItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<SmbItem>()
        var connection: Connection? = null
        var session: Session? = null
        var share: DiskShare? = null

        try {
            connection = client.connect(server.host, server.port)
            val authContext = if (server.isAnonymous || server.username.isEmpty()) {
                AuthenticationContext.anonymous()
            } else {
                AuthenticationContext(server.username, rawPassword.toCharArray(), server.domain)
            }
            session = connection.authenticate(authContext)
            share = session.connectShare(server.shareName) as DiskShare

            val files = share.list(subPath)
            for (f in files) {
                val name = f.fileName
                if (name == "." || name == "..") continue

                val isDir = (f.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                val itemPath = if (subPath.isEmpty()) name else "$subPath/$name"
                result.add(
                    SmbItem(
                        name = name,
                        path = itemPath,
                        isDirectory = isDir,
                        sizeBytes = f.endOfFile,
                        lastModified = f.changeTime.toEpochMillis()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { share?.close() } catch (_: Exception) {}
            try { session?.close() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }
        return@withContext result
    }
}
