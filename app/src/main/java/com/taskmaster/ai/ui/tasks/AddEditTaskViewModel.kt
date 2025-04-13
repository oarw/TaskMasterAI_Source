package com.taskmaster.ai.ui.tasks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskmaster.ai.data.Category
import com.taskmaster.ai.data.Task
import com.taskmaster.ai.data.repository.CategoryRepository
import com.taskmaster.ai.data.repository.TaskRepository
import kotlinx.coroutines.launch
import java.util.Date

/**
 * 添加/编辑任务ViewModel
 */
class AddEditTaskViewModel(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val taskId: Long
) : ViewModel() {
    
    // 所有分类
    val categories: LiveData<List<Category>> = categoryRepository.allCategories
    
    // 当前任务（如果是编辑模式）
    val task: LiveData<Task>? = if (taskId != -1L) taskRepository.getTaskById(taskId) else null
    
    // 任务标题
    private val _taskTitle = MutableLiveData<String>()
    val taskTitle: LiveData<String> = _taskTitle
    
    // 任务描述
    private val _taskDescription = MutableLiveData<String>()
    val taskDescription: LiveData<String> = _taskDescription
    
    // 任务优先级
    private val _taskPriority = MutableLiveData<Int>()
    val taskPriority: LiveData<Int> = _taskPriority
    
    // 任务截止日期
    private val _taskDueDate = MutableLiveData<Date?>()
    val taskDueDate: LiveData<Date?> = _taskDueDate
    
    // 任务分类ID
    private val _taskCategoryId = MutableLiveData<Long>()
    val taskCategoryId: LiveData<Long> = _taskCategoryId
    
    // 初始化
    init {
        if (taskId != -1L) {
            // 编辑模式，加载现有任务数据
            viewModelScope.launch {
                task?.value?.let { existingTask ->
                    _taskTitle.value = existingTask.title
                    _taskDescription.value = existingTask.description
                    _taskPriority.value = existingTask.priority
                    _taskDueDate.value = existingTask.dueDate
                    _taskCategoryId.value = existingTask.categoryId
                }
            }
        } else {
            // 新建模式，设置默认值
            _taskPriority.value = Task.PRIORITY_NORMAL
            _taskCategoryId.value = 0L
        }
    }
    
    // 设置任务标题
    fun setTaskTitle(title: String) {
        _taskTitle.value = title
    }
    
    // 设置任务描述
    fun setTaskDescription(description: String) {
        _taskDescription.value = description
    }
    
    // 设置任务优先级
    fun setTaskPriority(priority: Int) {
        _taskPriority.value = priority
    }
    
    // 设置任务截止日期
    fun setTaskDueDate(dueDate: Date?) {
        _taskDueDate.value = dueDate
    }
    
    // 设置任务分类
    fun setTaskCategory(categoryId: Long) {
        _taskCategoryId.value = categoryId
    }
    
    // 保存任务
    fun saveTask() = viewModelScope.launch {
        val title = _taskTitle.value ?: return@launch
        if (title.isBlank()) return@launch
        
        val currentDate = Date()
        
        if (taskId != -1L) {
            // 更新现有任务
            task?.value?.let { existingTask ->
                val updatedTask = existingTask.copy(
                    title = title,
                    description = _taskDescription.value ?: "",
                    priority = _taskPriority.value ?: Task.PRIORITY_NORMAL,
                    dueDate = _taskDueDate.value,
                    categoryId = _taskCategoryId.value ?: 0L,
                    modifiedDate = currentDate
                )
                taskRepository.updateTask(updatedTask)
            }
        } else {
            // 创建新任务
            val newTask = Task(
                title = title,
                description = _taskDescription.value ?: "",
                priority = _taskPriority.value ?: Task.PRIORITY_NORMAL,
                dueDate = _taskDueDate.value,
                categoryId = _taskCategoryId.value ?: 0L,
                createdDate = currentDate,
                modifiedDate = currentDate
            )
            taskRepository.insertTask(newTask)
        }
    }
    
    /**
     * AddEditTaskViewModel工厂类
     */
    class AddEditTaskViewModelFactory(
        private val taskRepository: TaskRepository,
        private val categoryRepository: CategoryRepository,
        private val taskId: Long
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AddEditTaskViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AddEditTaskViewModel(taskRepository, categoryRepository, taskId) as T
            }
            throw IllegalArgumentException("未知的ViewModel类")
        }
    }
}
