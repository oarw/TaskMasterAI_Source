package com.taskmaster.ai.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 任务实体类
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val priority: Int = PRIORITY_NORMAL,
    val isCompleted: Boolean = false,
    val dueDate: Date? = null,
    val completedDate: Date? = null,
    val categoryId: Long = 0,
    val createdDate: Date = Date(),
    val modifiedDate: Date = Date()
) {
    companion object {
        const val PRIORITY_LOW = 0
        const val PRIORITY_NORMAL = 1
        const val PRIORITY_HIGH = 2
    }
}

/**
 * 任务分类实体类
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int
)

/**
 * 番茄钟记录实体类
 */
@Entity(tableName = "pomodoro_records")
data class PomodoroRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val startTime: Date,
    val endTime: Date,
    val duration: Long, // 持续时间（毫秒）
    val isCompleted: Boolean = true
)

/**
 * AI提供商配置实体类
 */
@Entity(tableName = "ai_providers")
data class AiProvider(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val apiUrl: String,
    val apiKey: String,
    val isDefault: Boolean = false
)

/**
 * 用户设置实体类
 */
@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1, // 只有一条记录
    val darkMode: Int = MODE_SYSTEM, // 0: 关闭, 1: 开启, 2: 跟随系统
    val pomodoroWorkDuration: Int = 25, // 默认25分钟
    val pomodoroShortBreakDuration: Int = 5, // 默认5分钟
    val pomodoroLongBreakDuration: Int = 15, // 默认15分钟
    val pomodoroCyclesBeforeLongBreak: Int = 4, // 默认4个周期后长休息
    val webDavUrl: String = "",
    val webDavUsername: String = "",
    val webDavPassword: String = "",
    val autoSyncEnabled: Boolean = false,
    val autoBackupEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true
) {
    companion object {
        const val MODE_LIGHT = 0
        const val MODE_DARK = 1
        const val MODE_SYSTEM = 2
    }
}
