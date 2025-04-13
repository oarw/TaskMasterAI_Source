package com.taskmaster.ai.data.repository

import androidx.lifecycle.LiveData
import com.taskmaster.ai.data.UserSettings
import com.taskmaster.ai.data.UserSettingsDao

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
}
