package com.taskmaster.ai.data.repository

import androidx.lifecycle.LiveData
import com.taskmaster.ai.data.UserSettings
import com.taskmaster.ai.data.UserSettingsDao
import java.util.Date

/**
 * 用户设置仓库类
 */
class UserSettingsRepository(private val userSettingsDao: UserSettingsDao) {
    
    // 获取用户设置
    val userSettings: LiveData<UserSettings> = userSettingsDao.getUserSettings()
    
    // 更新用户设置
    suspend fun updateUserSettings(userSettings: UserSettings) {
        userSettingsDao.updateUserSettings(userSettings)
    }
    
    // 插入用户设置
    suspend fun insertUserSettings(userSettings: UserSettings) {
        userSettingsDao.insertUserSettings(userSettings)
    }
    
    // 同步获取用户设置
    fun getUserSettingsSync(): UserSettings? {
        return userSettingsDao.getUserSettingsSync()
    }
    
    // 更新WebDAV设置
    suspend fun updateWebDavSettings(url: String, username: String, password: String) {
        val currentSettings = getUserSettingsSync() ?: UserSettings()
        val updatedSettings = currentSettings.copy(
            webDavUrl = url,
            webDavUsername = username,
            webDavPassword = password
        )
        updateUserSettings(updatedSettings)
    }
    
    // 更新上次同步时间
    suspend fun updateLastSyncTime(date: Date) {
        val currentSettings = getUserSettingsSync() ?: UserSettings()
        val updatedSettings = currentSettings.copy(
            lastSyncTime = date
        )
        updateUserSettings(updatedSettings)
    }
    
    // 更新离线模式设置
    suspend fun updateOfflineMode(enabled: Boolean) {
        val currentSettings = getUserSettingsSync() ?: UserSettings()
        val updatedSettings = currentSettings.copy(
            offlineModeEnabled = enabled
        )
        updateUserSettings(updatedSettings)
    }
}
