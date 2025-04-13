package com.taskmaster.ai

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

class TaskMasterApplication : Application(), Configuration.Provider {
    
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
