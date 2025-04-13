package com.taskmaster.ai.ui.tasks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskmaster.ai.data.Task
import com.taskmaster.ai.data.repository.CategoryRepository
import com.taskmaster.ai.data.repository.TaskRepository
import kotlinx.coroutines.launch
import java.util.Date

/**
 * 任务列表ViewModel
 */
class TasksViewModel(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    // 所有任务
    val allTasks = taskRepository.allTasks
    
    // 活跃任务（未完成）
    val activeTasks = taskRepository.activeTasks
    
    // 已完成任务
    val completedTasks = taskRepository.completedTasks
    
    // 所有分类
    val allCategories = categoryRepository.allCategories
    
    // 当前选中的过滤器
    private val _currentFilter = MutableLiveData(FILTER_ALL)
    val currentFilter: LiveData<Int> = _currentFilter
    
    // 设置过滤器
    fun setFilter(filter: Int) {
        _currentFilter.value = filter
    }
    
    // 根据分类获取任务
    fun getTasksByCategory(categoryId: Long) = taskRepository.getTasksByCategory(categoryId)
    
    // 根据日期范围获取任务
    fun getTasksByDateRange(startDate: Date, endDate: Date) = 
        taskRepository.getTasksByDateRange(startDate, endDate)
    
    // 添加任务
    fun addTask(task: Task) = viewModelScope.launch {
        taskRepository.insertTask(task)
    }
    
    // 更新任务
    fun updateTask(task: Task) = viewModelScope.launch {
        taskRepository.updateTask(task)
    }
    
    // 删除任务
    fun deleteTask(task: Task) = viewModelScope.launch {
        taskRepository.deleteTask(task)
    }
    
    // 切换任务完成状态
    fun toggleTaskCompleted(task: Task) = viewModelScope.launch {
        taskRepository.updateTask(
            task.copy(
                isCompleted = !task.isCompleted,
                modifiedDate = Date()
            )
        )
    }
    
    companion object {
        const val FILTER_ALL = 0
        const val FILTER_ACTIVE = 1
        const val FILTER_COMPLETED = 2
        const val FILTER_CATEGORY = 3
        const val FILTER_DATE = 4
    }
    
    /**
     * TasksViewModel工厂类
     */
    class TasksViewModelFactory(
        private val taskRepository: TaskRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TasksViewModel(taskRepository, categoryRepository) as T
            }
            throw IllegalArgumentException("未知的ViewModel类")
        }
    }
}
