package com.taskmaster.ai.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.taskmaster.ai.R
import com.taskmaster.ai.ui.MainActivity

/**
 * 通知管理器
 * 负责创建和显示各种通知
 */
class NotificationManager(private val context: Context) {

    companion object {
        // 通知渠道ID
        const val CHANNEL_TASK_REMINDERS = "task_reminders"
        const val CHANNEL_POMODORO = "pomodoro_timer"
        const val CHANNEL_SYNC = "sync_status"
        
        // 通知ID
        const val NOTIFICATION_ID_TASK_REMINDER = 1001
        const val NOTIFICATION_ID_POMODORO = 2001
        const val NOTIFICATION_ID_SYNC = 3001
    }
    
    /**
     * 初始化通知渠道
     */
    fun createNotificationChannels() {
        // 仅在Android 8.0及以上版本需要创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            // 任务提醒渠道
            val taskRemindersChannel = NotificationChannel(
                CHANNEL_TASK_REMINDERS,
                context.getString(R.string.channel_task_reminders),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_task_reminders_description)
            }
            
            // 番茄钟渠道
            val pomodoroChannel = NotificationChannel(
                CHANNEL_POMODORO,
                context.getString(R.string.channel_pomodoro),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_pomodoro_description)
            }
            
            // 同步状态渠道
            val syncChannel = NotificationChannel(
                CHANNEL_SYNC,
                context.getString(R.string.channel_sync),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_sync_description)
            }
            
            // 创建通知渠道
            notificationManager.createNotificationChannels(
                listOf(taskRemindersChannel, pomodoroChannel, syncChannel)
            )
        }
    }
    
    /**
     * 显示任务提醒通知
     */
    fun showTaskReminderNotification(taskId: Long, taskTitle: String, taskDescription: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("taskId", taskId)
            putExtra("openTaskDetail", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_TASK_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_task)
            .setContentTitle(taskTitle)
            .setContentText(taskDescription)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_TASK_REMINDER + taskId.toInt(), builder.build())
        }
    }
    
    /**
     * 显示番茄钟通知
     */
    fun showPomodoroNotification(title: String, message: String, isOngoing: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openPomodoro", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_POMODORO)
            .setSmallIcon(R.drawable.ic_notification_pomodoro)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(isOngoing)
            .setAutoCancel(!isOngoing)
        
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_POMODORO, builder.build())
        }
    }
    
    /**
     * 取消番茄钟通知
     */
    fun cancelPomodoroNotification() {
        with(NotificationManagerCompat.from(context)) {
            cancel(NOTIFICATION_ID_POMODORO)
        }
    }
    
    /**
     * 显示同步状态通知
     */
    fun showSyncNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openSyncSettings", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_notification_sync)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_SYNC, builder.build())
        }
    }
}
