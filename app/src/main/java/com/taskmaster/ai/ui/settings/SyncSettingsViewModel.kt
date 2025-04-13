package com.taskmaster.ai.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskmaster.ai.data.UserSettings
import com.taskmaster.ai.data.repository.UserSettingsRepository
import com.taskmaster.ai.data.sync.WebDavSyncManager
import kotlinx.coroutines.launch
import java.util.Date

/**
 * 同步设置ViewModel
 */
class SyncSettingsViewModel(
    private val userSettingsRepository: UserSettingsRepository,
    private val webDavSyncManager: WebDavSyncManager
) : ViewModel() {
    
    // 用户设置
    val userSettings: LiveData<UserSettings?> = userSettingsRepository.getUserSettings()
    
    // 同步状态
    val syncStatus: LiveData<WebDavSyncManager.SyncStatus> = webDavSyncManager.syncStatus
    
    // 上次同步时间
    val lastSyncTime: LiveData<Date?> = webDavSyncManager.lastSyncTime
    
    // 操作状态
    private val _operationStatus = MutableLiveData<String>()
    val operationStatus: LiveData<String> = _operationStatus
    
    // 保存WebDAV设置
    fun saveWebDavSettings(url: String, username: String, password: String) {
        if (url.isBlank() || username.isBlank() || password.isBlank()) {
            _operationStatus.value = "请填写所有字段"
            return
        }
        
        viewModelScope.launch {
            try {
                userSettingsRepository.updateWebDavSettings(url, username, password)
                _operationStatus.value = "WebDAV设置已保存"
            } catch (e: Exception) {
                _operationStatus.value = "保存设置失败: ${e.message}"
            }
        }
    }
    
    // 开始同步
    fun startSync() {
        viewModelScope.launch {
            try {
                webDavSyncManager.syncAll()
            } catch (e: Exception) {
                _operationStatus.value = "同步失败: ${e.message}"
            }
        }
    }
    
    // 清除操作状态
    fun clearOperationStatus() {
        _operationStatus.value = ""
    }
    
    /**
     * SyncSettingsViewModel工厂类
     */
    class SyncSettingsViewModelFactory(
        private val userSettingsRepository: UserSettingsRepository,
        private val webDavSyncManager: WebDavSyncManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SyncSettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SyncSettingsViewModel(userSettingsRepository, webDavSyncManager) as T
            }
            throw IllegalArgumentException("未知的ViewModel类")
        }
    }
}
