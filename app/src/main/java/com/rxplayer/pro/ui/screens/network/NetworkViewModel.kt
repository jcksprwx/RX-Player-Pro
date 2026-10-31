package com.rxplayer.pro.ui.screens.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rxplayer.pro.core.database.dao.ServerDao
import com.rxplayer.pro.core.database.entity.NetworkServerEntity
import com.rxplayer.pro.core.media.network.SmbItem
import com.rxplayer.pro.core.media.network.SmbManager
import com.rxplayer.pro.core.security.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val serverDao: ServerDao,
    private val smbManager: SmbManager,
    private val secureStorage: SecureStorage
) : ViewModel() {

    val servers = serverDao.getAllServersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedServerFiles = MutableStateFlow<List<SmbItem>>(emptyList())
    val selectedServerFiles: StateFlow<List<SmbItem>> = _selectedServerFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _connectionStatus = MutableStateFlow<String?>(null)
    val connectionStatus: StateFlow<String?> = _connectionStatus.asStateFlow()

    fun testAndAddServer(
        name: String,
        host: String,
        port: Int,
        shareName: String,
        domain: String,
        username: String,
        password: String,
        isAnonymous: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _connectionStatus.value = "Testing SMB connection..."

            val success = smbManager.testConnection(
                host = host,
                port = port,
                domain = domain,
                username = username,
                password = password,
                isAnonymous = isAnonymous
            )

            if (success) {
                val passKey = "smb_pass_${UUID.randomUUID()}"
                if (password.isNotEmpty()) {
                    secureStorage.savePassword(passKey, password)
                }

                val server = NetworkServerEntity(
                    name = name.ifEmpty { host },
                    host = host,
                    port = port,
                    shareName = shareName,
                    domain = domain,
                    username = username,
                    encryptedPasswordKey = passKey,
                    isAnonymous = isAnonymous
                )
                serverDao.insertServer(server)
                _connectionStatus.value = "Connected successfully!"
                _isLoading.value = false
                onSuccess()
            } else {
                _connectionStatus.value = "Failed to connect to SMB server. Verify host IP and credentials."
                _isLoading.value = false
            }
        }
    }

    fun browseServer(server: NetworkServerEntity, subPath: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            val rawPassword = secureStorage.getPassword(server.encryptedPasswordKey)
            val files = smbManager.listShareFiles(server, rawPassword, subPath)
            _selectedServerFiles.value = files
            serverDao.updateLastConnected(server.id)
            _isLoading.value = false
        }
    }

    fun deleteServer(server: NetworkServerEntity) {
        viewModelScope.launch {
            secureStorage.removePassword(server.encryptedPasswordKey)
            serverDao.deleteServer(server)
        }
    }
}
