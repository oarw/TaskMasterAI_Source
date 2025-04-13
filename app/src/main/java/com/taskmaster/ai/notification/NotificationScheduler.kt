package com.taskmaster.ai.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.taskmaster.ai.data.Task
import java.util.Calendar
import java.util.Date

/**
 * 通知调度器
 * 负责调度各种通知的发送时间
 */
class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    /**
     * 调度任务提醒通知
     */
    fun scheduleTaskReminder(task: Task) {
        // 只为有截止日期且未完成的任务调度提醒
        if (task.dueDate == null || task.isCompleted) {
            return
        }
        
        // 创建提醒意图
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_TASK_REMINDER
            putExtra(NotificationReceiver.EXTRA_TASK_ID, task.id)
            putExtra(NotificationReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(NotificationReceiver.EXTRA_TASK_DESCRIPTION, task.description)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 计算提醒时间（截止日期前1小时）
        val reminderTime = Calendar.getInstance().apply {
            time = task.dueDate!!
            add(Calendar.HOUR, -1)
        }
        
        // 如果提醒时间已过，则不调度
        if (reminderTime.timeInMillis <= System.currentTimeMillis()) {
            return
        }
        
        // 调度提醒
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                reminderTime.timeInMillis,
                pendingIntent
            )
        }
    }
    
    /**
     * 取消任务提醒通知
     */
    fun cancelTaskReminder(taskId: Long) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_TASK_REMINDER
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    /**
     * 调度每日任务摘要通知
     */
    fun scheduleDailySummary(hour: Int, minute: Int) {
        // 创建每日摘要意图
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_DAILY_SUMMARY
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 计算下一次通知时间
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            // 如果当前时间已过今天的调度时间，则调度到明天
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        // 调度每日摘要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
    
    /**
     * 取消每日任务摘要通知
     */
    fun cancelDailySummary() {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_DAILY_SUMMARY
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    /**
     * 调度每周计划通知
     */
    fun scheduleWeeklySummary(dayOfWeek: Int, hour: Int, minute: Int) {
        // 创建每周摘要意图
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_WEEKLY_SUMMARY
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 计算下一次通知时间
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            // 如果当前时间已过本周的调度时间，则调度到下周
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }
        
        // 调度每周摘要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
    
    /**
     * 取消每周计划通知
     */
    fun cancelWeeklySummary() {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_WEEKLY_SUMMARY
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
}
