package com.taskmaster.ai.ui.statistics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.taskmaster.ai.data.PomodoroRecord
import com.taskmaster.ai.data.Task
import com.taskmaster.ai.data.repository.PomodoroRepository
import com.taskmaster.ai.data.repository.TaskRepository
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * 时间统计ViewModel
 */
class StatisticsViewModel(
    private val pomodoroRepository: PomodoroRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    // 时间范围
    private val _timeRange = MutableLiveData(TIME_RANGE_TODAY)
    val timeRange: LiveData<Int> = _timeRange
    
    // 自定义日期范围
    private val _startDate = MutableLiveData<Date>()
    val startDate: LiveData<Date> = _startDate
    
    private val _endDate = MutableLiveData<Date>()
    val endDate: LiveData<Date> = _endDate
    
    // 番茄钟记录
    private val _pomodoroRecords = MutableLiveData<List<PomodoroRecord>>()
    val pomodoroRecords: LiveData<List<PomodoroRecord>> = _pomodoroRecords
    
    // 总专注时间（毫秒）
    val totalFocusTime: LiveData<Long> = _pomodoroRecords.map { records ->
        records.sumOf { it.duration }
    }
    
    // 完成的番茄钟数量
    val completedPomodoros: LiveData<Int> = _pomodoroRecords.map { records ->
        records.count { it.isCompleted }
    }
    
    // 任务完成情况
    private val _completedTasks = MutableLiveData<List<Task>>()
    val completedTasks: LiveData<List<Task>> = _completedTasks
    
    // 任务完成数量
    val completedTaskCount: LiveData<Int> = _completedTasks.map { tasks ->
        tasks.size
    }
    
    // 初始化
    init {
        setTimeRange(TIME_RANGE_TODAY)
    }
    
    // 设置时间范围
    fun setTimeRange(range: Int) {
        _timeRange.value = range
        
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        
        when (range) {
            TIME_RANGE_TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time
                
                _startDate.value = startDate
                _endDate.value = endDate
                
                loadStatistics(startDate, endDate)
            }
            TIME_RANGE_YESTERDAY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val yesterdayEnd = calendar.time
                
                _startDate.value = startDate
                _endDate.value = yesterdayEnd
                
                loadStatistics(startDate, yesterdayEnd)
            }
            TIME_RANGE_THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time
                
                _startDate.value = startDate
                _endDate.value = endDate
                
                loadStatistics(startDate, endDate)
            }
            TIME_RANGE_LAST_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val startDate = calendar.time
                
                calendar.add(Calendar.DAY_OF_YEAR, 7)
                calendar.add(Calendar.MILLISECOND, -1)
                val lastWeekEnd = calendar.time
                
                _startDate.value = startDate
                _endDate.value = lastWeekEnd
                
                loadStatistics(startDate, lastWeekEnd)
            }
            TIME_RANGE_THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time
                
                _startDate.value = startDate
                _endDate.value = endDate
                
                loadStatistics(startDate, endDate)
            }
            TIME_RANGE_CUSTOM -> {
                // 自定义范围需要单独设置开始和结束日期
                _startDate.value?.let { start ->
                    _endDate.value?.let { end ->
                        loadStatistics(start, end)
                    }
                }
            }
        }
    }
    
    // 设置自定义开始日期
    fun setCustomStartDate(date: Date) {
        _startDate.value = date
        if (_timeRange.value == TIME_RANGE_CUSTOM) {
            _endDate.value?.let { end ->
                loadStatistics(date, end)
            }
        }
    }
    
    // 设置自定义结束日期
    fun setCustomEndDate(date: Date) {
        _endDate.value = date
        if (_timeRange.value == TIME_RANGE_CUSTOM) {
            _startDate.value?.let { start ->
                loadStatistics(start, date)
            }
        }
    }
    
    // 加载统计数据
    private fun loadStatistics(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            // 加载番茄钟记录
            pomodoroRepository.getPomodoroRecordsByDateRange(startDate, endDate).observeForever { records ->
                _pomodoroRecords.value = records
            }
            
            // 加载已完成任务
            taskRepository.getTasksByDateRange(startDate, endDate).observeForever { tasks ->
                _completedTasks.value = tasks.filter { it.isCompleted }
            }
        }
    }
    
    // 获取按任务分组的专注时间
    fun getFocusTimeByTask(): Map<Long?, Long> {
        val records = _pomodoroRecords.value ?: return emptyMap()
        return records.groupBy { it.taskId }
            .mapValues { (_, records) -> records.sumOf { it.duration } }
    }
    
    // 获取按日期分组的专注时间
    fun getFocusTimeByDate(): Map<String, Long> {
        val records = _pomodoroRecords.value ?: return emptyMap()
        val calendar = Calendar.getInstance()
        
        return records.groupBy { record ->
            calendar.time = record.startTime
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        }.mapValues { (_, records) -> records.sumOf { it.duration } }
    }
    
    companion object {
        const val TIME_RANGE_TODAY = 0
        const val TIME_RANGE_YESTERDAY = 1
        const val TIME_RANGE_THIS_WEEK = 2
        const val TIME_RANGE_LAST_WEEK = 3
        const val TIME_RANGE_THIS_MONTH = 4
        const val TIME_RANGE_CUSTOM = 5
    }
    
    /**
     * StatisticsViewModel工厂类
     */
    class StatisticsViewModelFactory(
        private val pomodoroRepository: PomodoroRepository,
        private val taskRepository: TaskRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return StatisticsViewModel(pomodoroRepository, taskRepository) as T
            }
            throw IllegalArgumentException("未知的ViewModel类")
        }
    }
}
