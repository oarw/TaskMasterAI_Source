package com.taskmaster.ai.ui.ai

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskmaster.ai.data.AiProvider
import com.taskmaster.ai.data.Task
import com.taskmaster.ai.data.ai.AiServiceClient
import com.taskmaster.ai.data.repository.AiProviderRepository
import com.taskmaster.ai.data.repository.TaskRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AI任务规划ViewModel
 */
class AiTaskPlanningViewModel(
    private val aiProviderRepository: AiProviderRepository,
    private val taskRepository: TaskRepository,
    private val aiServiceClient: AiServiceClient
) : ViewModel() {
    
    // AI提供商
    val aiProviders: LiveData<List<AiProvider>> = aiProviderRepository.allAiProviders
    
    // 默认AI提供商
    val defaultAiProvider: LiveData<AiProvider> = aiProviderRepository.defaultAiProvider
    
    // 当前选中的AI提供商
    private val _selectedAiProvider = MutableLiveData<AiProvider>()
    val selectedAiProvider: LiveData<AiProvider> = _selectedAiProvider
    
    // 任务标题
    private val _taskTitle = MutableLiveData<String>()
    val taskTitle: LiveData<String> = _taskTitle
    
    // 任务描述
    private val _taskDescription = MutableLiveData<String>()
    val taskDescription: LiveData<String> = _taskDescription
    
    // 任务截止日期
    private val _taskDueDate = MutableLiveData<Date?>()
    val taskDueDate: LiveData<Date?> = _taskDueDate
    
    // 子任务列表
    private val _subtasks = MutableLiveData<List<AiServiceClient.SubTask>>()
    val subtasks: LiveData<List<AiServiceClient.SubTask>> = _subtasks
    
    // 估算时间（分钟）
    private val _estimatedTime = MutableLiveData<Int>()
    val estimatedTime: LiveData<Int> = _estimatedTime
    
    // 建议优先级
    private val _suggestedPriority = MutableLiveData<Int>()
    val suggestedPriority: LiveData<Int> = _suggestedPriority
    
    // 优先级解释
    private val _priorityExplanation = MutableLiveData<String>()
    val priorityExplanation: LiveData<String> = _priorityExplanation
    
    // 加载状态
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    // 错误消息
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // 初始化
    init {
        // 观察默认AI提供商
        defaultAiProvider.observeForever { provider ->
            if (provider != null && _selectedAiProvider.value == null) {
                _selectedAiProvider.value = provider
            }
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
    
    // 设置任务截止日期
    fun setTaskDueDate(date: Date?) {
        _taskDueDate.value = date
    }
    
    // 设置选中的AI提供商
    fun setSelectedAiProvider(provider: AiProvider) {
        _selectedAiProvider.value = provider
    }
    
    // 分解任务
    fun decomposeTask() {
        val title = _taskTitle.value ?: return
        val description = _taskDescription.value ?: ""
        val provider = _selectedAiProvider.value ?: return
        
        if (title.isBlank()) {
            _errorMessage.value = "请输入任务标题"
            return
        }
        
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val subtasks = aiServiceClient.decomposeTask(provider, title, description)
                _subtasks.value = subtasks
            } catch (e: Exception) {
                _errorMessage.value = "任务分解失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // 估算时间
    fun estimateTime() {
        val title = _taskTitle.value ?: return
        val description = _taskDescription.value ?: ""
        val provider = _selectedAiProvider.value ?: return
        
        if (title.isBlank()) {
            _errorMessage.value = "请输入任务标题"
            return
        }
        
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val estimatedMinutes = aiServiceClient.estimateTaskTime(provider, title, description)
                _estimatedTime.value = estimatedMinutes
            } catch (e: Exception) {
                _errorMessage.value = "时间估算失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // 建议优先级
    fun suggestPriority() {
        val title = _taskTitle.value ?: return
        val description = _taskDescription.value ?: ""
        val provider = _selectedAiProvider.value ?: return
        
        if (title.isBlank()) {
            _errorMessage.value = "请输入任务标题"
            return
        }
        
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val dueDate = _taskDueDate.value
                val dueDateString = dueDate?.let {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                }
                
                val (priority, explanation) = aiServiceClient.suggestPriority(
                    provider, title, description, dueDateString
                )
                
                _suggestedPriority.value = priority
                _priorityExplanation.value = explanation
            } catch (e: Exception) {
                _errorMessage.value = "优先级建议失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // 创建主任务
    fun createMainTask(): Long? {
        val title = _taskTitle.value ?: return null
        val description = _taskDescription.value ?: ""
        val dueDate = _taskDueDate.value
        val priority = _suggestedPriority.value ?: Task.PRIORITY_NORMAL
        
        if (title.isBlank()) {
            _errorMessage.value = "请输入任务标题"
            return null
        }
        
        var mainTaskId: Long? = null
        
        viewModelScope.launch {
            try {
                val currentDate = Date()
                
                val mainTask = Task(
                    title = title,
                    description = description,
                    priority = priority,
                    dueDate = dueDate,
                    createdDate = currentDate,
                    modifiedDate = currentDate
                )
                
                mainTaskId = taskRepository.insertTask(mainTask)
            } catch (e: Exception) {
                _errorMessage.value = "创建主任务失败: ${e.message}"
            }
        }
        
        return mainTaskId
    }
    
    // 创建子任务
    fun createSubtasks(mainTaskId: Long) {
        val subtasks = _subtasks.value ?: return
        
        if (subtasks.isEmpty()) {
            return
        }
        
        viewModelScope.launch {
            try {
                val currentDate = Date()
                
                subtasks.forEach { subtask ->
                    val task = Task(
                        title = subtask.title,
                        description = subtask.description,
                        priority = _suggestedPriority.value ?: Task.PRIORITY_NORMAL,
                        dueDate = _taskDueDate.value,
                        categoryId = mainTaskId, // 使用主任务ID作为分类ID，表示子任务
                        createdDate = currentDate,
                        modifiedDate = currentDate
                    )
                    
                    taskRepository.insertTask(task)
                }
            } catch (e: Exception) {
                _errorMessage.value = "创建子任务失败: ${e.message}"
            }
        }
    }
    
    /**
     * AiTaskPlanningViewModel工厂类
     */
    class AiTaskPlanningViewModelFactory(
        private val aiProviderRepository: AiProviderRepository,
        private val taskRepository: TaskRepository,
        private val aiServiceClient: AiServiceClient
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AiTaskPlanningViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AiTaskPlanningViewModel(
                    aiProviderRepository,
                    taskRepository,
                    aiServiceClient
                ) as T
            }
            throw IllegalArgumentException("未知的ViewModel类")
        }
    }
}
