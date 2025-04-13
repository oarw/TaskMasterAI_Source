package com.taskmaster.ai.ui.pomodoro

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskmaster.ai.data.PomodoroRecord
import com.taskmaster.ai.data.Task
import com.taskmaster.ai.data.UserSettings
import com.taskmaster.ai.data.repository.PomodoroRepository
import com.taskmaster.ai.data.repository.TaskRepository
import com.taskmaster.ai.data.repository.UserSettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date

/**
 * 番茄钟ViewModel
 */
class PomodoroViewModel(
    private val pomodoroRepository: PomodoroRepository,
    private val taskRepository: TaskRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {
    
    // 用户设置
    val userSettings: LiveData<UserSettings> = userSettingsRepository.userSettings
    
    // 所有任务
    val allTasks: LiveData<List<Task>> = taskRepository.activeTasks
    
    // 当前选中的任务
    private val _selectedTask = MutableLiveData<Task?>()
    val selectedTask: LiveData<Task?> = _selectedTask
    
    // 番茄钟状态
    private val _pomodoroState = MutableLiveData(POMODORO_STATE_IDLE)
    val pomodoroState: LiveData<Int> = _pomodoroState
    
    // 剩余时间（秒）
    private val _remainingTime = MutableLiveData(0)
    val remainingTime: LiveData<Int> = _remainingTime
    
    // 当前周期
    private val _currentCycle = MutableLiveData(1)
    val currentCycle: LiveData<Int> = _currentCycle
    
    // 完成的番茄钟数量
    private val _completedPomodoros = MutableLiveData(0)
    val completedPomodoros: LiveData<Int> = _completedPomodoros
    
    // 计时器任务
    private var timerJob: Job? = null
    
    // 开始时间
    private var startTime: Date? = null
    
    // 设置选中的任务
    fun setSelectedTask(task: Task?) {
        _selectedTask.value = task
    }
    
    // 开始番茄钟
    fun startPomodoro() {
        if (_pomodoroState.value != POMODORO_STATE_IDLE && _pomodoroState.value != POMODORO_STATE_PAUSED) {
            return
        }
        
        val settings = userSettings.value ?: return
        
        // 如果是暂停状态，继续计时
        if (_pomodoroState.value == POMODORO_STATE_PAUSED) {
            _pomodoroState.value = POMODORO_STATE_WORKING
            startTimer(_remainingTime.value ?: 0)
            return
        }
        
        // 开始新的番茄钟
        _pomodoroState.value = POMODORO_STATE_WORKING
        _remainingTime.value = settings.pomodoroWorkDuration * 60
        startTime = Date()
        startTimer(_remainingTime.value ?: 0)
    }
    
    // 暂停番茄钟
    fun pausePomodoro() {
        if (_pomodoroState.value != POMODORO_STATE_WORKING && _pomodoroState.value != POMODORO_STATE_BREAK) {
            return
        }
        
        _pomodoroState.value = POMODORO_STATE_PAUSED
        timerJob?.cancel()
    }
    
    // 停止番茄钟
    fun stopPomodoro() {
        _pomodoroState.value = POMODORO_STATE_IDLE
        timerJob?.cancel()
        _remainingTime.value = 0
        startTime = null
    }
    
    // 开始计时器
    private fun startTimer(initialSeconds: Int) {
        timerJob?.cancel()
        
        timerJob = viewModelScope.launch {
            var seconds = initialSeconds
            
            while (isActive && seconds > 0) {
                _remainingTime.value = seconds
                delay(1000)
                seconds--
            }
            
            if (isActive) {
                // 计时结束
                when (_pomodoroState.value) {
                    POMODORO_STATE_WORKING -> {
                        // 工作时间结束，记录番茄钟
                        recordPomodoro()
                        
                        // 进入休息状态
                        val settings = userSettings.value ?: return@launch
                        val currentCycle = _currentCycle.value ?: 1
                        
                        if (currentCycle % settings.pomodoroCyclesBeforeLongBreak == 0) {
                            // 长休息
                            _pomodoroState.value = POMODORO_STATE_LONG_BREAK
                            _remainingTime.value = settings.pomodoroLongBreakDuration * 60
                        } else {
                            // 短休息
                            _pomodoroState.value = POMODORO_STATE_SHORT_BREAK
                            _remainingTime.value = settings.pomodoroShortBreakDuration * 60
                        }
                        
                        startTimer(_remainingTime.value ?: 0)
                    }
                    POMODORO_STATE_SHORT_BREAK, POMODORO_STATE_LONG_BREAK -> {
                        // 休息时间结束，准备下一个番茄钟
                        _pomodoroState.value = POMODORO_STATE_IDLE
                        _currentCycle.value = (_currentCycle.value ?: 1) + 1
                    }
                }
            }
        }
    }
    
    // 记录番茄钟
    private fun recordPomodoro() {
        val startTimeValue = startTime ?: return
        val endTime = Date()
        val duration = endTime.time - startTimeValue.time
        
        viewModelScope.launch {
            val record = PomodoroRecord(
                taskId = _selectedTask.value?.id,
                startTime = startTimeValue,
                endTime = endTime,
                duration = duration,
                isCompleted = true
            )
            pomodoroRepository.insertPomodoroRecord(record)
            
            // 更新完成的番茄钟数量
            _completedPomodoros.value = (_completedPomodoros.value ?: 0) + 1
        }
    }
    
    companion object {
        const val POMODORO_STATE_IDLE = 0
        const val POMODORO_STATE_WORKING = 1
        const val POMODORO_STATE_SHORT_BREAK = 2
        const val POMODORO_STATE_LONG_BREAK = 3
        const val POMODORO_STATE_PAUSED = 4
    }
    
    /**
     * PomodoroViewModel工厂类
     */
    class PomodoroViewModelFactory(
        private val pomodoroRepository: PomodoroRepository,
        private val taskRepository: TaskRepository,
        private val userSettingsRepository: UserSettingsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PomodoroViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PomodoroViewModel(pomodoroRepository, taskRepository, userSettingsRepository) as T
            }
            throw IllegalArgumentException("未知的ViewModel类")
        }
    }
}
