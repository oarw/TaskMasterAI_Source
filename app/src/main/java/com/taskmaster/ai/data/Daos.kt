package com.taskmaster.ai.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.util.Date

/**
 * 任务数据访问对象
 */
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, priority DESC")
    fun getAllTasks(): LiveData<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: Long): LiveData<Task>
    
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDate ASC, priority DESC")
    fun getActiveTasks(): LiveData<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY modifiedDate DESC")
    fun getCompletedTasks(): LiveData<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE categoryId = :categoryId ORDER BY dueDate ASC, priority DESC")
    fun getTasksByCategory(categoryId: Long): LiveData<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE dueDate BETWEEN :startDate AND :endDate ORDER BY dueDate ASC, priority DESC")
    fun getTasksByDateRange(startDate: Date, endDate: Date): LiveData<List<Task>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long
    
    @Update
    suspend fun updateTask(task: Task)
    
    @Delete
    suspend fun deleteTask(task: Task)
}

/**
 * 分类数据访问对象
 */
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): LiveData<List<Category>>
    
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    fun getCategoryById(categoryId: Long): LiveData<Category>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long
    
    @Update
    suspend fun updateCategory(category: Category)
    
    @Delete
    suspend fun deleteCategory(category: Category)
}

/**
 * 番茄钟记录数据访问对象
 */
@Dao
interface PomodoroRecordDao {
    @Query("SELECT * FROM pomodoro_records ORDER BY startTime DESC")
    fun getAllPomodoroRecords(): LiveData<List<PomodoroRecord>>
    
    @Query("SELECT * FROM pomodoro_records WHERE taskId = :taskId ORDER BY startTime DESC")
    fun getPomodoroRecordsByTask(taskId: Long): LiveData<List<PomodoroRecord>>
    
    @Query("SELECT * FROM pomodoro_records WHERE startTime BETWEEN :startDate AND :endDate ORDER BY startTime DESC")
    fun getPomodoroRecordsByDateRange(startDate: Date, endDate: Date): LiveData<List<PomodoroRecord>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPomodoroRecord(pomodoroRecord: PomodoroRecord): Long
    
    @Update
    suspend fun updatePomodoroRecord(pomodoroRecord: PomodoroRecord)
    
    @Delete
    suspend fun deletePomodoroRecord(pomodoroRecord: PomodoroRecord)
}

/**
 * AI提供商数据访问对象
 */
@Dao
interface AiProviderDao {
    @Query("SELECT * FROM ai_providers ORDER BY name ASC")
    fun getAllAiProviders(): LiveData<List<AiProvider>>
    
    @Query("SELECT * FROM ai_providers WHERE isDefault = 1 LIMIT 1")
    fun getDefaultAiProvider(): LiveData<AiProvider>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiProvider(aiProvider: AiProvider): Long
    
    @Update
    suspend fun updateAiProvider(aiProvider: AiProvider)
    
    @Delete
    suspend fun deleteAiProvider(aiProvider: AiProvider)
    
    @Query("UPDATE ai_providers SET isDefault = 0")
    suspend fun clearDefaultProvider()
    
    @Query("UPDATE ai_providers SET isDefault = 1 WHERE id = :providerId")
    suspend fun setDefaultProvider(providerId: Long)
}

/**
 * 用户设置数据访问对象
 */
@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettings(): LiveData<UserSettings>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(userSettings: UserSettings)
    
    @Update
    suspend fun updateUserSettings(userSettings: UserSettings)
}
