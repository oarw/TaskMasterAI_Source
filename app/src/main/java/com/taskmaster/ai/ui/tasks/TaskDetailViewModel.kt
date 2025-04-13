package com.taskmaster.ai.ui.tasks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskmaster.ai.data.Task
import com.taskmaster.ai.data.repository.TaskRepository
import kotlinx.coroutines.launch
import java.util.Date

/**
 * 任务详情ViewModel
 */
class TaskDetailViewModel(
    private val taskRepository: TaskRepository,
    private val taskId: Long
) : ViewModel() {
    
    // 当前任务
    val task: LiveData<Task> = taskRepository.getTaskById(taskId)
    
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
    
    /**
     * TaskDetailViewModel工厂类
     */
    class TaskDetailViewModelFactory(
        private val taskRepository: TaskRepository,
        private val taskId: Long
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TaskDetailViewModel(taskRepository, taskId) as T
            }
            throw IllegalArgumentException("未知的ViewModel类")
        }
    }
}
