package com.taskmaster.ai

import android.app.Application
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.WorkManager
import com.taskmaster.ai.data.AppDatabase
import com.taskmaster.ai.data.repository.AiProviderRepository
import com.taskmaster.ai.data.repository.CategoryRepository
import com.taskmaster.ai.data.repository.PomodoroRepository
import com.taskmaster.ai.data.repository.TaskRepository
import com.taskmaster.ai.data.repository.UserSettingsRepository
import com.taskmaster.ai.data.sync.WebDavSyncManager

class TaskMasterApplication : Application(), Configuration.Provider {
    
    // 数据库实例
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "taskmaster_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    // 仓库实例
    val taskRepository: TaskRepository by lazy {
        TaskRepository(database.taskDao())
    }
    
    val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(database.categoryDao())
    }
    
    val pomodoroRepository: PomodoroRepository by lazy {
        PomodoroRepository(database.pomodoroRecordDao())
    }
    
    val aiProviderRepository: AiProviderRepository by lazy {
        AiProviderRepository(database.aiProviderDao())
    }
    
    val userSettingsRepository: UserSettingsRepository by lazy {
        UserSettingsRepository(database.userSettingsDao())
    }
    
    // WebDAV同步管理器
    val webDavSyncManager: WebDavSyncManager by lazy {
        WebDavSyncManager(
            applicationContext,
            taskRepository,
            userSettingsRepository
        )
    }
    
    override fun onCreate() {
        super.onCreate()
        // 初始化应用程序级别的组件
    }
    
    // 配置WorkManager
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }
}
