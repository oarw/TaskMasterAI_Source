package com.taskmaster.ai.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 应用数据库
 */
@Database(
    entities = [
        Task::class,
        Category::class,
        PomodoroRecord::class,
        AiProvider::class,
        UserSettings::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun pomodoroRecordDao(): PomodoroRecordDao
    abstract fun aiProviderDao(): AiProviderDao
    abstract fun userSettingsDao(): UserSettingsDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taskmaster_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        // 初始化数据库
                        initializeDatabase(database)
                    }
                }
            }
        }
        
        private suspend fun initializeDatabase(database: AppDatabase) {
            // 添加默认分类
            val categoryDao = database.categoryDao()
            categoryDao.insertCategory(Category(name = "工作", color = 0xFF4285F4.toInt()))
            categoryDao.insertCategory(Category(name = "学习", color = 0xFFDB4437.toInt()))
            categoryDao.insertCategory(Category(name = "个人", color = 0xFFF4B400.toInt()))
            
            // 添加默认AI提供商
            val aiProviderDao = database.aiProviderDao()
            aiProviderDao.insertAiProvider(
                AiProvider(
                    name = "OpenAI",
                    apiUrl = "https://api.openai.com/v1",
                    apiKey = "",
                    isDefault = true
                )
            )
            
            // 添加默认用户设置
            val userSettingsDao = database.userSettingsDao()
            userSettingsDao.insertUserSettings(UserSettings())
        }
    }
}
