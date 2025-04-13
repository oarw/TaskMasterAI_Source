package com.taskmaster.ai.data.repository

import androidx.lifecycle.LiveData
import com.taskmaster.ai.data.Task
import com.taskmaster.ai.data.TaskDao
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 任务仓库类
 */
class TaskRepository(private val taskDao: TaskDao) {
    
    // 获取所有任务
    val allTasks: LiveData<List<Task>> = taskDao.getAllTasks()
    
    // 获取活跃任务（未完成）
    val activeTasks: LiveData<List<Task>> = taskDao.getActiveTasks()
    
    // 获取已完成任务
    val completedTasks: LiveData<List<Task>> = taskDao.getCompletedTasks()
    
    // 根据ID获取任务
    fun getTaskById(taskId: Long): LiveData<Task> {
        return taskDao.getTaskById(taskId)
    }
    
    // 根据分类获取任务
    fun getTasksByCategory(categoryId: Long): LiveData<List<Task>> {
        return taskDao.getTasksByCategory(categoryId)
    }
    
    // 根据日期范围获取任务
    fun getTasksByDateRange(startDate: Date, endDate: Date): LiveData<List<Task>> {
        return taskDao.getTasksByDateRange(startDate, endDate)
    }
    
    // 插入任务
    suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task)
    }
    
    // 更新任务
    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }
    
    // 删除任务
    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }
    
    // 同步获取所有任务
    fun getAllTasksSync(): List<Task> {
        return taskDao.getAllTasksSync()
    }
    
    // 更新或插入任务（用于同步）
    suspend fun upsertTask(task: Task): Long {
        return taskDao.upsertTask(task)
    }

    // 同步获取指定日期范围的任务
    suspend fun getTasksByDueDateRange(startDate: Date, endDate: Date): List<Task> {
        return withContext(Dispatchers.IO) {
            taskDao.getTasksByDateRangeSync(startDate, endDate)
        }
    }

    // 删除所有任务
    suspend fun deleteAllTasks() {
        taskDao.deleteAllTasks()
    }
}
