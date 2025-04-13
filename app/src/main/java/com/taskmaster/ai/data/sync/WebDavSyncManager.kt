package com.taskmaster.ai.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.taskmaster.ai.data.Task
import com.taskmaster.ai.data.repository.TaskRepository
import com.taskmaster.ai.data.repository.UserSettingsRepository
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WebDAV同步管理器
 * 负责与WebDAV服务器同步数据
 */
class WebDavSyncManager(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val userSettingsRepository: UserSettingsRepository
) {
    // 同步状态
    private val _syncStatus = MutableLiveData<SyncStatus>()
    val syncStatus: LiveData<SyncStatus> = _syncStatus
    
    // 上次同步时间
    private val _lastSyncTime = MutableLiveData<Date?>()
    val lastSyncTime: LiveData<Date?> = _lastSyncTime
    
    // 初始化
    init {
        // 从设置中加载上次同步时间
        userSettingsRepository.getUserSettings().observeForever { settings ->
            settings?.lastSyncTime?.let {
                _lastSyncTime.value = it
            }
        }
    }
    
    /**
     * 同步所有数据
     */
    suspend fun syncAll() {
        if (!isNetworkAvailable()) {
            _syncStatus.postValue(SyncStatus.ERROR_NO_NETWORK)
            return
        }
        
        val settings = userSettingsRepository.getUserSettingsSync()
        
        if (settings?.webDavUrl.isNullOrEmpty() || 
            settings?.webDavUsername.isNullOrEmpty() || 
            settings?.webDavPassword.isNullOrEmpty()) {
            _syncStatus.postValue(SyncStatus.ERROR_NO_CREDENTIALS)
            return
        }
        
        try {
            _syncStatus.postValue(SyncStatus.SYNCING)
            
            val sardine = OkHttpSardine()
            sardine.setCredentials(settings!!.webDavUsername, settings.webDavPassword)
            
            // 确保目录存在
            val baseUrl = ensureTrailingSlash(settings.webDavUrl)
            val appFolderUrl = baseUrl + "TaskMasterAI/"
            
            if (!sardine.exists(appFolderUrl)) {
                sardine.createDirectory(appFolderUrl)
            }
            
            // 同步任务
            syncTasks(sardine, appFolderUrl)
            
            // 更新上次同步时间
            val now = Date()
            userSettingsRepository.updateLastSyncTime(now)
            _lastSyncTime.postValue(now)
            
            _syncStatus.postValue(SyncStatus.SUCCESS)
        } catch (e: Exception) {
            _syncStatus.postValue(SyncStatus.ERROR_SYNC_FAILED)
        }
    }
    
    /**
     * 同步任务
     */
    private suspend fun syncTasks(sardine: Sardine, baseUrl: String) {
        val tasksUrl = baseUrl + "tasks.json"
        
        // 获取本地任务
        val localTasks = taskRepository.getAllTasksSync()
        
        // 检查远程任务是否存在
        if (sardine.exists(tasksUrl)) {
            // 下载远程任务
            val remoteTasksJson = withContext(Dispatchers.IO) {
                sardine.get(tasksUrl).bufferedReader().use { it.readText() }
            }
            
            // 解析远程任务
            val remoteTasks = parseTasksFromJson(remoteTasksJson)
            
            // 合并任务
            val mergedTasks = mergeTasks(localTasks, remoteTasks)
            
            // 更新本地数据库
            updateLocalTasks(mergedTasks)
        }
        
        // 上传所有任务到服务器
        val updatedLocalTasks = taskRepository.getAllTasksSync()
        val tasksJson = convertTasksToJson(updatedLocalTasks)
        
        val inputStream: InputStream = ByteArrayInputStream(tasksJson.toByteArray())
        withContext(Dispatchers.IO) {
            sardine.put(tasksUrl, inputStream, "application/json")
        }
    }
    
    /**
     * 解析JSON中的任务
     */
    private fun parseTasksFromJson(json: String): List<Task> {
        val tasks = mutableListOf<Task>()
        
        try {
            val jsonArray = JSONArray(json)
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                val task = Task(
                    id = jsonObject.getLong("id"),
                    title = jsonObject.getString("title"),
                    description = jsonObject.optString("description", ""),
                    isCompleted = jsonObject.optBoolean("isCompleted", false),
                    priority = jsonObject.optInt("priority", Task.PRIORITY_NORMAL),
                    categoryId = if (jsonObject.has("categoryId")) jsonObject.getLong("categoryId") else null,
                    dueDate = if (jsonObject.has("dueDate")) parseDate(jsonObject.getString("dueDate")) else null,
                    completedDate = if (jsonObject.has("completedDate")) parseDate(jsonObject.getString("completedDate")) else null,
                    createdDate = parseDate(jsonObject.getString("createdDate")),
                    modifiedDate = parseDate(jsonObject.getString("modifiedDate"))
                )
                
                tasks.add(task)
            }
        } catch (e: Exception) {
            // 解析错误，返回空列表
        }
        
        return tasks
    }
    
    /**
     * 将任务转换为JSON
     */
    private fun convertTasksToJson(tasks: List<Task>): String {
        val jsonArray = JSONArray()
        
        for (task in tasks) {
            val jsonObject = JSONObject()
            
            jsonObject.put("id", task.id)
            jsonObject.put("title", task.title)
            jsonObject.put("description", task.description)
            jsonObject.put("isCompleted", task.isCompleted)
            jsonObject.put("priority", task.priority)
            task.categoryId?.let { jsonObject.put("categoryId", it) }
            task.dueDate?.let { jsonObject.put("dueDate", formatDate(it)) }
            task.completedDate?.let { jsonObject.put("completedDate", formatDate(it)) }
            jsonObject.put("createdDate", formatDate(task.createdDate))
            jsonObject.put("modifiedDate", formatDate(task.modifiedDate))
            
            jsonArray.put(jsonObject)
        }
        
        return jsonArray.toString()
    }
    
    /**
     * 合并任务
     * 使用修改时间较新的版本
     */
    private fun mergeTasks(localTasks: List<Task>, remoteTasks: List<Task>): List<Task> {
        val taskMap = mutableMapOf<Long, Task>()
        
        // 添加本地任务
        for (task in localTasks) {
            taskMap[task.id] = task
        }
        
        // 合并远程任务
        for (remoteTask in remoteTasks) {
            val localTask = taskMap[remoteTask.id]
            
            if (localTask == null) {
                // 本地不存在，添加远程任务
                taskMap[remoteTask.id] = remoteTask
            } else {
                // 本地存在，比较修改时间
                if (remoteTask.modifiedDate.after(localTask.modifiedDate)) {
                    // 远程任务更新，使用远程任务
                    taskMap[remoteTask.id] = remoteTask
                }
            }
        }
        
        return taskMap.values.toList()
    }
    
    /**
     * 更新本地任务
     */
    private suspend fun updateLocalTasks(tasks: List<Task>) {
        for (task in tasks) {
            taskRepository.upsertTask(task)
        }
    }
    
    /**
     * 检查网络是否可用
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * 确保URL以斜杠结尾
     */
    private fun ensureTrailingSlash(url: String): String {
        return if (url.endsWith("/")) url else "$url/"
    }
    
    /**
     * 解析日期字符串
     */
    private fun parseDate(dateString: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return format.parse(dateString) ?: Date()
    }
    
    /**
     * 格式化日期为字符串
     */
    private fun formatDate(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return format.format(date)
    }
    
    /**
     * 同步状态枚举
     */
    enum class SyncStatus {
        IDLE,
        SYNCING,
        SUCCESS,
        ERROR_NO_NETWORK,
        ERROR_NO_CREDENTIALS,
        ERROR_SYNC_FAILED
    }
}
