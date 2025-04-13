package com.taskmaster.ai.data.backup

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.taskmaster.ai.data.AppDatabase
import com.taskmaster.ai.data.Task
import com.taskmaster.ai.data.Category
import com.taskmaster.ai.data.PomodoroRecord
import com.taskmaster.ai.data.UserSettings
import com.taskmaster.ai.data.AiProvider
import com.taskmaster.ai.data.repository.TaskRepository
import com.taskmaster.ai.data.repository.UserSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 数据备份管理器
 * 负责应用数据的备份和恢复
 */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val taskRepository: TaskRepository,
    private val userSettingsRepository: UserSettingsRepository
) {
    companion object {
        private const val BACKUP_FOLDER = "TaskMasterAI/backups"
        private const val BACKUP_FILE_PREFIX = "taskmaster_backup_"
        private const val BACKUP_FILE_EXTENSION = ".zip"
        
        // 备份文件中的JSON文件名
        private const val TASKS_JSON_FILENAME = "tasks.json"
        private const val CATEGORIES_JSON_FILENAME = "categories.json"
        private const val POMODORO_RECORDS_JSON_FILENAME = "pomodoro_records.json"
        private const val SETTINGS_JSON_FILENAME = "settings.json"
        private const val AI_PROVIDERS_JSON_FILENAME = "ai_providers.json"
    }
    
    /**
     * 创建备份
     * @return 备份文件路径
     */
    suspend fun createBackup(): String = withContext(Dispatchers.IO) {
        // 创建备份文件夹
        val backupFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            BACKUP_FOLDER
        )
        
        if (!backupFolder.exists()) {
            backupFolder.mkdirs()
        }
        
        // 创建备份文件
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupFile = File(backupFolder, "$BACKUP_FILE_PREFIX$timestamp$BACKUP_FILE_EXTENSION")
        
        ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
            // 备份任务
            val tasks = taskRepository.getAllTasksSync()
            val tasksJson = convertTasksToJson(tasks)
            addJsonToZip(zipOut, TASKS_JSON_FILENAME, tasksJson)
            
            // 备份分类
            val categories = database.categoryDao().getAllCategoriesSync()
            val categoriesJson = convertCategoriesToJson(categories)
            addJsonToZip(zipOut, CATEGORIES_JSON_FILENAME, categoriesJson)
            
            // 备份番茄钟记录
            val pomodoroRecords = database.pomodoroRecordDao().getAllPomodoroRecordsSync()
            val pomodoroRecordsJson = convertPomodoroRecordsToJson(pomodoroRecords)
            addJsonToZip(zipOut, POMODORO_RECORDS_JSON_FILENAME, pomodoroRecordsJson)
            
            // 备份设置
            val settings = userSettingsRepository.getUserSettingsSync()
            settings?.let {
                val settingsJson = convertSettingsToJson(it)
                addJsonToZip(zipOut, SETTINGS_JSON_FILENAME, settingsJson)
            }
            
            // 备份AI提供商
            val aiProviders = database.aiProviderDao().getAllAiProvidersSync()
            val aiProvidersJson = convertAiProvidersToJson(aiProviders)
            addJsonToZip(zipOut, AI_PROVIDERS_JSON_FILENAME, aiProvidersJson)
        }
        
        return@withContext backupFile.absolutePath
    }
    
    /**
     * 从URI恢复备份
     * @param uri 备份文件URI
     */
    suspend fun restoreBackupFromUri(uri: Uri) = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        
        inputStream?.use { stream ->
            restoreFromStream(stream)
        }
    }
    
    /**
     * 从文件恢复备份
     * @param backupFilePath 备份文件路径
     */
    suspend fun restoreBackup(backupFilePath: String) = withContext(Dispatchers.IO) {
        val backupFile = File(backupFilePath)
        
        if (backupFile.exists()) {
            FileInputStream(backupFile).use { stream ->
                restoreFromStream(stream)
            }
        }
    }
    
    /**
     * 从输入流恢复备份
     */
    private suspend fun restoreFromStream(inputStream: InputStream) {
        ZipInputStream(inputStream).use { zipIn ->
            var entry: ZipEntry? = zipIn.nextEntry
            
            while (entry != null) {
                when (entry.name) {
                    TASKS_JSON_FILENAME -> {
                        val tasksJson = readTextFromZipEntry(zipIn)
                        val tasks = parseTasksFromJson(tasksJson)
                        restoreTasks(tasks)
                    }
                    CATEGORIES_JSON_FILENAME -> {
                        val categoriesJson = readTextFromZipEntry(zipIn)
                        val categories = parseCategoriesFromJson(categoriesJson)
                        restoreCategories(categories)
                    }
                    POMODORO_RECORDS_JSON_FILENAME -> {
                        val pomodoroRecordsJson = readTextFromZipEntry(zipIn)
                        val pomodoroRecords = parsePomodoroRecordsFromJson(pomodoroRecordsJson)
                        restorePomodoroRecords(pomodoroRecords)
                    }
                    SETTINGS_JSON_FILENAME -> {
                        val settingsJson = readTextFromZipEntry(zipIn)
                        val settings = parseSettingsFromJson(settingsJson)
                        settings?.let { restoreSettings(it) }
                    }
                    AI_PROVIDERS_JSON_FILENAME -> {
                        val aiProvidersJson = readTextFromZipEntry(zipIn)
                        val aiProviders = parseAiProvidersFromJson(aiProvidersJson)
                        restoreAiProviders(aiProviders)
                    }
                }
                
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }
    
    /**
     * 获取所有备份文件
     * @return 备份文件列表
     */
    fun getBackupFiles(): List<File> {
        val backupFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            BACKUP_FOLDER
        )
        
        if (!backupFolder.exists()) {
            return emptyList()
        }
        
        return backupFolder.listFiles { file ->
            file.isFile && file.name.startsWith(BACKUP_FILE_PREFIX) && file.name.endsWith(BACKUP_FILE_EXTENSION)
        }?.toList() ?: emptyList()
    }
    
    /**
     * 删除备份文件
     * @param backupFilePath 备份文件路径
     * @return 是否删除成功
     */
    fun deleteBackup(backupFilePath: String): Boolean {
        val backupFile = File(backupFilePath)
        return backupFile.exists() && backupFile.delete()
    }
    
    /**
     * 将JSON添加到ZIP文件
     */
    private fun addJsonToZip(zipOut: ZipOutputStream, filename: String, jsonContent: String) {
        val entry = ZipEntry(filename)
        zipOut.putNextEntry(entry)
        zipOut.write(jsonContent.toByteArray())
        zipOut.closeEntry()
    }
    
    /**
     * 从ZIP条目读取文本
     */
    private fun readTextFromZipEntry(zipIn: ZipInputStream): String {
        return zipIn.bufferedReader().use { it.readText() }
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
            jsonObject.put("categoryId", task.categoryId)
            task.dueDate?.let { jsonObject.put("dueDate", formatDate(it)) }
            task.completedDate?.let { jsonObject.put("completedDate", formatDate(it)) }
            jsonObject.put("createdDate", formatDate(task.createdDate))
            jsonObject.put("modifiedDate", formatDate(task.modifiedDate))
            
            jsonArray.put(jsonObject)
        }
        
        return jsonArray.toString()
    }
    
    /**
     * 将分类转换为JSON
     */
    private fun convertCategoriesToJson(categories: List<Category>): String {
        val jsonArray = JSONArray()
        
        for (category in categories) {
            val jsonObject = JSONObject()
            
            jsonObject.put("id", category.id)
            jsonObject.put("name", category.name)
            jsonObject.put("color", category.color)
            
            jsonArray.put(jsonObject)
        }
        
        return jsonArray.toString()
    }
    
    /**
     * 将番茄钟记录转换为JSON
     */
    private fun convertPomodoroRecordsToJson(pomodoroRecords: List<PomodoroRecord>): String {
        val jsonArray = JSONArray()
        
        for (record in pomodoroRecords) {
            val jsonObject = JSONObject()
            
            jsonObject.put("id", record.id)
            record.taskId?.let { jsonObject.put("taskId", it) }
            jsonObject.put("startTime", formatDate(record.startTime))
            jsonObject.put("endTime", formatDate(record.endTime))
            jsonObject.put("duration", record.duration)
            jsonObject.put("isCompleted", record.isCompleted)
            
            jsonArray.put(jsonObject)
        }
        
        return jsonArray.toString()
    }
    
    /**
     * 将用户设置转换为JSON
     */
    private fun convertSettingsToJson(settings: UserSettings): String {
        val jsonObject = JSONObject()
        
        jsonObject.put("id", settings.id)
        jsonObject.put("darkMode", settings.darkMode)
        jsonObject.put("pomodoroWorkDuration", settings.pomodoroWorkDuration)
        jsonObject.put("pomodoroShortBreakDuration", settings.pomodoroShortBreakDuration)
        jsonObject.put("pomodoroLongBreakDuration", settings.pomodoroLongBreakDuration)
        jsonObject.put("pomodoroCyclesBeforeLongBreak", settings.pomodoroCyclesBeforeLongBreak)
        jsonObject.put("webDavUrl", settings.webDavUrl)
        jsonObject.put("webDavUsername", settings.webDavUsername)
        jsonObject.put("webDavPassword", settings.webDavPassword)
        jsonObject.put("autoSyncEnabled", settings.autoSyncEnabled)
        jsonObject.put("autoBackupEnabled", settings.autoBackupEnabled)
        jsonObject.put("notificationsEnabled", settings.notificationsEnabled)
        
        return jsonObject.toString()
    }
    
    /**
     * 将AI提供商转换为JSON
     */
    private fun convertAiProvidersToJson(aiProviders: List<AiProvider>): String {
        val jsonArray = JSONArray()
        
        for (provider in aiProviders) {
            val jsonObject = JSONObject()
            
            jsonObject.put("id", provider.id)
            jsonObject.put("name", provider.name)
            jsonObject.put("apiUrl", provider.apiUrl)
            jsonObject.put("apiKey", provider.apiKey)
            jsonObject.put("isDefault", provider.isDefault)
            
            jsonArray.put(jsonObject)
        }
        
        return jsonArray.toString()
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
                    categoryId = jsonObject.optLong("categoryId", 0),
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
     * 解析JSON中的分类
     */
    private fun parseCategoriesFromJson(json: String): List<Category> {
        val categories = mutableListOf<Category>()
        
        try {
            val jsonArray = JSONArray(json)
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                val category = Category(
                    id = jsonObject.getLong("id"),
                    name = jsonObject.getString("name"),
                    color = jsonObject.getInt("color")
                )
                
                categories.add(category)
            }
        } catch (e: Exception) {
            // 解析错误，返回空列表
        }
        
        return categories
    }
    
    /**
     * 解析JSON中的番茄钟记录
     */
    private fun parsePomodoroRecordsFromJson(json: String): List<PomodoroRecord> {
        val records = mutableListOf<PomodoroRecord>()
        
        try {
            val jsonArray = JSONArray(json)
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                val record = PomodoroRecord(
                    id = jsonObject.getLong("id"),
                    taskId = if (jsonObject.has("taskId")) jsonObject.optLong("taskId") else null,
                    startTime = parseDate(jsonObject.getString("startTime")),
                    endTime = parseDate(jsonObject.getString("endTime")),
                    duration = jsonObject.getLong("duration"),
                    isCompleted = jsonObject.optBoolean("isCompleted", true)
                )
                
                records.add(record)
            }
        } catch (e: Exception) {
            // 解析错误，返回空列表
        }
        
        return records
    }
    
    /**
     * 解析JSON中的用户设置
     */
    private fun parseSettingsFromJson(json: String): UserSettings? {
        return try {
            val jsonObject = JSONObject(json)
            
            UserSettings(
                id = jsonObject.getInt("id"),
                darkMode = jsonObject.getInt("darkMode"),
                pomodoroWorkDuration = jsonObject.getInt("pomodoroWorkDuration"),
                pomodoroShortBreakDuration = jsonObject.getInt("pomodoroShortBreakDuration"),
                pomodoroLongBreakDuration = jsonObject.getInt("pomodoroLongBreakDuration"),
                pomodoroCyclesBeforeLongBreak = jsonObject.getInt("pomodoroCyclesBeforeLongBreak"),
                webDavUrl = jsonObject.optString("webDavUrl", ""),
                webDavUsername = jsonObject.optString("webDavUsername", ""),
                webDavPassword = jsonObject.optString("webDavPassword", ""),
                autoSyncEnabled = jsonObject.optBoolean("autoSyncEnabled", false),
                autoBackupEnabled = jsonObject.optBoolean("autoBackupEnabled", false),
                notificationsEnabled = jsonObject.optBoolean("notificationsEnabled", true)
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 解析JSON中的AI提供商
     */
    private fun parseAiProvidersFromJson(json: String): List<AiProvider> {
        val providers = mutableListOf<AiProvider>()
        
        try {
            val jsonArray = JSONArray(json)
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                val provider = AiProvider(
                    id = jsonObject.getLong("id"),
                    name = jsonObject.getString("name"),
                    apiUrl = jsonObject.getString("apiUrl"),
                    apiKey = jsonObject.getString("apiKey"),
                    isDefault = jsonObject.optBoolean("isDefault", false)
                )
                
                providers.add(provider)
            }
        } catch (e: Exception) {
            // 解析错误，返回空列表
        }
        
        return providers
    }
    
    /**
     * 恢复任务
     */
    private suspend fun restoreTasks(tasks: List<Task>) {
        // 清空现有任务
        database.taskDao().deleteAllTasks()
        
        // 恢复备份的任务
        for (task in tasks) {
            database.taskDao().insertTask(task)
        }
    }
    
    /**
     * 恢复分类
     */
    private suspend fun restoreCategories(categories: List<Category>) {
        // 清空现有分类
        database.categoryDao().deleteAllCategories()
        
        // 恢复备份的分类
        for (category in categories) {
            database.categoryDao().insertCategory(category)
        }
    }
    
    /**
     * 恢复番茄钟记录
     */
    private suspend fun restorePomodoroRecords(records: List<PomodoroRecord>) {
        // 清空现有番茄钟记录
        database.pomodoroRecordDao().deleteAllPomodoroRecords()
        
        // 恢复备份的番茄钟记录
        for (record in records) {
            database.pomodoroRecordDao().insertPomodoroRecord(record)
        }
    }
    
    /**
     * 恢复用户设置
     */
    private suspend fun restoreSettings(settings: UserSettings) {
        database.userSettingsDao().insertUserSettings(settings)
    }
    
    /**
     * 恢复AI提供商
     */
    private suspend fun restoreAiProviders(providers: List<AiProvider>) {
        // 清空现有AI提供商
        database.aiProviderDao().deleteAllAiProviders()
        
        // 恢复备份的AI提供商
        for (provider in providers) {
            database.aiProviderDao().insertAiProvider(provider)
        }
    }
    
    /**
     * 格式化日期为字符串
     */
    private fun formatDate(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return format.format(date)
    }
    
    /**
     * 解析日期字符串
     */
    private fun parseDate(dateString: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return format.parse(dateString) ?: Date()
    }
}
