package com.taskmaster.ai.data.ai

import com.taskmaster.ai.data.AiProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * AI服务客户端
 * 负责与AI提供商API通信
 */
class AiServiceClient {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * 发送任务分解请求
     * @param aiProvider AI提供商配置
     * @param taskTitle 任务标题
     * @param taskDescription 任务描述
     * @return 分解后的子任务列表
     */
    suspend fun decomposeTask(
        aiProvider: AiProvider,
        taskTitle: String,
        taskDescription: String
    ): List<SubTask> {
        val prompt = """
            请将以下任务分解为更小的子任务，以便更容易完成：
            
            任务标题：$taskTitle
            任务描述：$taskDescription
            
            请以JSON格式返回子任务列表，每个子任务包含标题、描述和预估时间（分钟）。
            格式如下：
            {
              "subtasks": [
                {
                  "title": "子任务标题",
                  "description": "子任务描述",
                  "estimatedMinutes": 30
                },
                ...
              ]
            }
        """.trimIndent()
        
        val response = sendRequest(aiProvider, prompt)
        return parseSubTasksResponse(response)
    }
    
    /**
     * 发送时间估算请求
     * @param aiProvider AI提供商配置
     * @param taskTitle 任务标题
     * @param taskDescription 任务描述
     * @return 估算的时间（分钟）
     */
    suspend fun estimateTaskTime(
        aiProvider: AiProvider,
        taskTitle: String,
        taskDescription: String
    ): Int {
        val prompt = """
            请估算完成以下任务所需的时间：
            
            任务标题：$taskTitle
            任务描述：$taskDescription
            
            请以JSON格式返回估算结果，包含估算的分钟数和简短的解释。
            格式如下：
            {
              "estimatedMinutes": 120,
              "explanation": "这个任务需要大约2小时，因为..."
            }
        """.trimIndent()
        
        val response = sendRequest(aiProvider, prompt)
        return parseTimeEstimationResponse(response)
    }
    
    /**
     * 发送优先级建议请求
     * @param aiProvider AI提供商配置
     * @param taskTitle 任务标题
     * @param taskDescription 任务描述
     * @param deadline 截止日期
     * @return 建议的优先级（0-低，1-中，2-高）和解释
     */
    suspend fun suggestPriority(
        aiProvider: AiProvider,
        taskTitle: String,
        taskDescription: String,
        deadline: String?
    ): Pair<Int, String> {
        val deadlineText = deadline?.let { "截止日期：$it" } ?: "无截止日期"
        
        val prompt = """
            请为以下任务建议一个优先级：
            
            任务标题：$taskTitle
            任务描述：$taskDescription
            $deadlineText
            
            请以JSON格式返回建议的优先级（0-低，1-中，2-高）和简短的解释。
            格式如下：
            {
              "priority": 2,
              "explanation": "这个任务应该设为高优先级，因为..."
            }
        """.trimIndent()
        
        val response = sendRequest(aiProvider, prompt)
        return parsePrioritySuggestionResponse(response)
    }
    
    /**
     * 发送请求到AI提供商API
     */
    private suspend fun sendRequest(aiProvider: AiProvider, prompt: String): String {
        val jsonBody = when {
            aiProvider.apiUrl.contains("openai") -> {
                JSONObject().apply {
                    put("model", "gpt-3.5-turbo")
                    put("messages", listOf(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        }
                    ))
                    put("temperature", 0.7)
                }.toString()
            }
            else -> {
                // 默认格式，适用于大多数API
                JSONObject().apply {
                    put("prompt", prompt)
                    put("max_tokens", 1000)
                    put("temperature", 0.7)
                }.toString()
            }
        }
        
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(aiProvider.apiUrl)
            .addHeader("Authorization", "Bearer ${aiProvider.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()
        
        val response = client.newCall(request).execute()
        return response.body?.string() ?: throw Exception("API返回为空")
    }
    
    /**
     * 解析子任务响应
     */
    private fun parseSubTasksResponse(response: String): List<SubTask> {
        try {
            // 简化处理，实际应用中需要根据不同AI提供商的响应格式进行适配
            val jsonResponse = extractJsonFromResponse(response)
            val jsonObject = JSONObject(jsonResponse)
            
            val subtasksArray = jsonObject.getJSONArray("subtasks")
            val subtasks = mutableListOf<SubTask>()
            
            for (i in 0 until subtasksArray.length()) {
                val subtaskObject = subtasksArray.getJSONObject(i)
                subtasks.add(
                    SubTask(
                        title = subtaskObject.getString("title"),
                        description = subtaskObject.getString("description"),
                        estimatedMinutes = subtaskObject.getInt("estimatedMinutes")
                    )
                )
            }
            
            return subtasks
        } catch (e: Exception) {
            // 错误处理，返回空列表
            return emptyList()
        }
    }
    
    /**
     * 解析时间估算响应
     */
    private fun parseTimeEstimationResponse(response: String): Int {
        try {
            val jsonResponse = extractJsonFromResponse(response)
            val jsonObject = JSONObject(jsonResponse)
            return jsonObject.getInt("estimatedMinutes")
        } catch (e: Exception) {
            // 错误处理，返回默认值
            return 60
        }
    }
    
    /**
     * 解析优先级建议响应
     */
    private fun parsePrioritySuggestionResponse(response: String): Pair<Int, String> {
        try {
            val jsonResponse = extractJsonFromResponse(response)
            val jsonObject = JSONObject(jsonResponse)
            val priority = jsonObject.getInt("priority")
            val explanation = jsonObject.getString("explanation")
            return Pair(priority, explanation)
        } catch (e: Exception) {
            // 错误处理，返回默认值
            return Pair(1, "无法获取AI建议，使用默认中等优先级")
        }
    }
    
    /**
     * 从AI响应中提取JSON
     * 不同AI提供商的响应格式可能不同，需要适配
     */
    private fun extractJsonFromResponse(response: String): String {
        return try {
            // 尝试解析为OpenAI格式
            val jsonObject = JSONObject(response)
            if (jsonObject.has("choices")) {
                val choices = jsonObject.getJSONArray("choices")
                val firstChoice = choices.getJSONObject(0)
                if (firstChoice.has("message")) {
                    val message = firstChoice.getJSONObject("message")
                    message.getString("content")
                } else {
                    firstChoice.getString("text")
                }
            } else {
                // 假设响应本身就是JSON
                response
            }
        } catch (e: Exception) {
            // 如果解析失败，尝试从文本中提取JSON部分
            val jsonPattern = """\{.*\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val matchResult = jsonPattern.find(response)
            matchResult?.value ?: response
        }
    }
    
    /**
     * 子任务数据类
     */
    data class SubTask(
        val title: String,
        val description: String,
        val estimatedMinutes: Int
    )
}
