package com.taskmaster.ai.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskmaster.ai.TaskMasterApplication
import com.taskmaster.ai.data.Task
import com.taskmaster.ai.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * 通知接收器
 * 接收和处理通知事件
 */
class NotificationReceiver : BroadcastReceiver() {

    companion object {
        // 通知动作
        const val ACTION_TASK_REMINDER = "com.taskmaster.ai.ACTION_TASK_REMINDER"
        const val ACTION_DAILY_SUMMARY = "com.taskmaster.ai.ACTION_DAILY_SUMMARY"
        const val ACTION_WEEKLY_SUMMARY = "com.taskmaster.ai.ACTION_WEEKLY_SUMMARY"
        const val ACTION_POMODORO_COMPLETED = "com.taskmaster.ai.ACTION_POMODORO_COMPLETED"
        
        // 通知额外数据
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_TASK_DESCRIPTION = "task_description"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        when (action) {
            ACTION_TASK_REMINDER -> handleTaskReminder(context, intent)
            ACTION_DAILY_SUMMARY -> handleDailySummary(context)
            ACTION_WEEKLY_SUMMARY -> handleWeeklySummary(context)
            ACTION_POMODORO_COMPLETED -> handlePomodoroCompleted(context, intent)
        }
    }
    
    /**
     * 处理任务提醒
     */
    private fun handleTaskReminder(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "任务提醒"
        val taskDescription = intent.getStringExtra(EXTRA_TASK_DESCRIPTION) ?: ""
        
        if (taskId != -1L) {
            val notificationManager = NotificationManager(context)
            notificationManager.showTaskReminderNotification(taskId, taskTitle, taskDescription)
        }
    }
    
    /**
     * 处理每日摘要
     */
    private fun handleDailySummary(context: Context) {
        val application = context.applicationContext as TaskMasterApplication
        val taskRepository = application.taskRepository
        
        CoroutineScope(Dispatchers.IO).launch {
            // 获取今天的任务
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val tomorrow = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1)
            }.time
            
            val todayTasks = taskRepository.getTasksByDueDateRange(today, tomorrow)
            
            // 创建摘要消息
            val title = "今日任务摘要"
            val message = if (todayTasks.isEmpty()) {
                "今天没有待办任务"
            } else {
                "今天有 ${todayTasks.size} 个待办任务"
            }
            
            // 显示通知
            val notificationManager = NotificationManager(context)
            notificationManager.showTaskReminderNotification(-1, title, message)
            
            // 重新调度明天的每日摘要
            val notificationScheduler = NotificationScheduler(context)
            notificationScheduler.scheduleDailySummary(9, 0) // 每天早上9点
        }
    }
    
    /**
     * 处理每周摘要
     */
    private fun handleWeeklySummary(context: Context) {
        val application = context.applicationContext as TaskMasterApplication
        val taskRepository = application.taskRepository
        
        CoroutineScope(Dispatchers.IO).launch {
            // 获取本周的任务
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            
            // 计算本周开始时间（周一）
            val weekStart = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, Calendar.MONDAY - dayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            // 计算本周结束时间（周日）
            val weekEnd = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, Calendar.SUNDAY - dayOfWeek)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time
            
            val weekTasks = taskRepository.getTasksByDueDateRange(weekStart, weekEnd)
            val completedTasks = weekTasks.filter { it.isCompleted }
            
            // 创建摘要消息
            val title = "本周任务摘要"
            val message = "本周共有 ${weekTasks.size} 个任务，已完成 ${completedTasks.size} 个"
            
            // 显示通知
            val notificationManager = NotificationManager(context)
            notificationManager.showTaskReminderNotification(-2, title, message)
            
            // 重新调度下周的每周摘要
            val notificationScheduler = NotificationScheduler(context)
            notificationScheduler.scheduleWeeklySummary(Calendar.MONDAY, 9, 0) // 每周一早上9点
        }
    }
    
    /**
     * 处理番茄钟完成
     */
    private fun handlePomodoroCompleted(context: Context, intent: Intent) {
        val notificationManager = NotificationManager(context)
        notificationManager.showPomodoroNotification(
            "番茄钟完成",
            "休息一下吧！",
            false
        )
    }
}
