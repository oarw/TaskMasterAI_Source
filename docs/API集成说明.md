# TaskMasterAI API集成说明

## 概述

TaskMasterAI应用程序提供了灵活的AI集成功能，允许开发者和用户配置不同的AI服务提供商。本文档详细介绍了如何集成各种AI服务提供商的API，以及如何扩展应用程序的AI功能。

## 支持的AI提供商

TaskMasterAI目前支持以下AI服务提供商：

1. **OpenAI**：包括GPT-3.5和GPT-4模型
2. **Azure OpenAI**：微软Azure平台上的OpenAI服务
3. **Anthropic Claude**：Claude系列模型
4. **百度文心一言**：百度的大型语言模型
5. **讯飞星火**：科大讯飞的认知大模型
6. **自定义API**：支持符合特定格式的自定义AI服务

## API配置

### 基本配置

每个AI提供商需要配置以下基本信息：

- **名称**：提供商的显示名称
- **类型**：提供商类型（如OpenAI、Azure等）
- **API密钥**：访问AI服务所需的密钥
- **端点URL**：API服务的基础URL

### OpenAI配置

```json
{
  "name": "OpenAI GPT-4",
  "type": "openai",
  "apiKey": "your-api-key",
  "endpointUrl": "https://api.openai.com/v1",
  "model": "gpt-4",
  "temperature": 0.7,
  "maxTokens": 2000
}
```

### Azure OpenAI配置

```json
{
  "name": "Azure OpenAI",
  "type": "azure",
  "apiKey": "your-api-key",
  "endpointUrl": "https://your-resource-name.openai.azure.com",
  "deploymentId": "your-deployment-id",
  "apiVersion": "2023-05-15",
  "temperature": 0.7,
  "maxTokens": 2000
}
```

### Anthropic Claude配置

```json
{
  "name": "Anthropic Claude",
  "type": "anthropic",
  "apiKey": "your-api-key",
  "endpointUrl": "https://api.anthropic.com/v1",
  "model": "claude-2",
  "temperature": 0.7,
  "maxTokens": 2000
}
```

### 百度文心一言配置

```json
{
  "name": "百度文心一言",
  "type": "baidu",
  "apiKey": "your-api-key",
  "secretKey": "your-secret-key",
  "endpointUrl": "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat",
  "model": "ernie-bot-4",
  "temperature": 0.7,
  "maxTokens": 2000
}
```

### 讯飞星火配置

```json
{
  "name": "讯飞星火",
  "type": "xunfei",
  "appId": "your-app-id",
  "apiKey": "your-api-key",
  "apiSecret": "your-api-secret",
  "endpointUrl": "https://spark-api.xf-yun.com/v2.1/chat",
  "model": "general",
  "temperature": 0.7,
  "maxTokens": 2000
}
```

## API请求格式

### 任务分解请求

```json
{
  "messages": [
    {
      "role": "system",
      "content": "你是一个任务分解助手，帮助用户将大型任务分解为可管理的小步骤。请分析用户提供的任务，并将其分解为5-10个具体、可操作的子任务。每个子任务应该清晰明确，并按照逻辑顺序排列。"
    },
    {
      "role": "user",
      "content": "请帮我分解以下任务：{taskDescription}"
    }
  ],
  "temperature": 0.7,
  "max_tokens": 2000
}
```

### 时间估算请求

```json
{
  "messages": [
    {
      "role": "system",
      "content": "你是一个时间估算助手，帮助用户估算完成任务所需的时间。请分析用户提供的任务，考虑其复杂性和范围，然后提供合理的时间估算。请以小时为单位给出估算结果，并解释你的估算依据。"
    },
    {
      "role": "user",
      "content": "请估算完成以下任务所需的时间：{taskDescription}"
    }
  ],
  "temperature": 0.7,
  "max_tokens": 1000
}
```

### 优先级推荐请求

```json
{
  "messages": [
    {
      "role": "system",
      "content": "你是一个任务优先级助手，帮助用户确定任务的优先级。请分析用户提供的任务，考虑其重要性、紧急性和影响范围，然后推荐适当的优先级。优先级分为四个等级：紧急（最高）、高、中、低（最低）。请给出推荐的优先级，并解释你的推荐依据。"
    },
    {
      "role": "user",
      "content": "请为以下任务推荐优先级：\n标题：{taskTitle}\n描述：{taskDescription}\n截止日期：{dueDate}"
    }
  ],
  "temperature": 0.7,
  "max_tokens": 1000
}
```

## API响应处理

### 任务分解响应

应用程序期望任务分解响应包含一个子任务列表，每个子任务应包含标题和可选的描述。响应格式示例：

```json
{
  "subtasks": [
    {
      "title": "子任务1标题",
      "description": "子任务1的详细描述"
    },
    {
      "title": "子任务2标题",
      "description": "子任务2的详细描述"
    },
    ...
  ]
}
```

如果AI提供商返回的不是这种格式，应用程序会尝试解析文本响应，提取子任务信息。

### 时间估算响应

应用程序期望时间估算响应包含估算的小时数和可选的解释。响应格式示例：

```json
{
  "estimatedHours": 5.5,
  "explanation": "这个任务需要约5.5小时完成，因为..."
}
```

如果AI提供商返回的不是这种格式，应用程序会尝试从文本响应中提取数字和时间单位。

### 优先级推荐响应

应用程序期望优先级推荐响应包含推荐的优先级级别和可选的解释。响应格式示例：

```json
{
  "priority": "high",
  "explanation": "这个任务被推荐为高优先级，因为..."
}
```

优先级值应为以下之一：`urgent`、`high`、`medium`、`low`。

如果AI提供商返回的不是这种格式，应用程序会尝试从文本响应中识别优先级关键词。

## 自定义AI提供商集成

### 实现自定义AI服务客户端

要集成新的AI提供商，需要实现`AiServiceClient`接口：

```kotlin
interface AiServiceClient {
    suspend fun decomposeTask(taskDescription: String): List<SubTask>
    suspend fun estimateTime(taskDescription: String): TimeEstimation
    suspend fun recommendPriority(taskTitle: String, taskDescription: String, dueDate: Date?): PriorityRecommendation
}
```

实现示例：

```kotlin
class CustomAiServiceClient(
    private val apiKey: String,
    private val endpointUrl: String
) : AiServiceClient {

    override suspend fun decomposeTask(taskDescription: String): List<SubTask> {
        // 实现自定义API调用逻辑
        // 返回子任务列表
    }

    override suspend fun estimateTime(taskDescription: String): TimeEstimation {
        // 实现自定义API调用逻辑
        // 返回时间估算结果
    }

    override suspend fun recommendPriority(
        taskTitle: String,
        taskDescription: String,
        dueDate: Date?
    ): PriorityRecommendation {
        // 实现自定义API调用逻辑
        // 返回优先级推荐结果
    }
}
```

### 注册自定义AI服务客户端

在`AiServiceClientFactory`中注册自定义AI服务客户端：

```kotlin
class AiServiceClientFactory {
    companion object {
        fun createClient(provider: AiProvider): AiServiceClient {
            return when (provider.type) {
                "openai" -> OpenAiServiceClient(provider.apiKey, provider.endpointUrl)
                "azure" -> AzureOpenAiServiceClient(provider.apiKey, provider.endpointUrl)
                "anthropic" -> AnthropicServiceClient(provider.apiKey, provider.endpointUrl)
                "baidu" -> BaiduServiceClient(provider.apiKey, provider.secretKey, provider.endpointUrl)
                "xunfei" -> XunfeiServiceClient(provider.appId, provider.apiKey, provider.apiSecret, provider.endpointUrl)
                "custom" -> CustomAiServiceClient(provider.apiKey, provider.endpointUrl)
                else -> throw IllegalArgumentException("不支持的AI提供商类型: ${provider.type}")
            }
        }
    }
}
```

## 本地模型集成

TaskMasterAI还支持集成本地运行的AI模型，适用于对隐私有高要求或在离线环境中使用的场景。

### 支持的本地模型

- **ONNX Runtime**：支持转换为ONNX格式的模型
- **TensorFlow Lite**：支持TensorFlow Lite格式的模型
- **llama.cpp**：支持Llama、Alpaca等模型的量化版本

### 本地模型配置

```json
{
  "name": "本地Llama模型",
  "type": "local",
  "modelPath": "/storage/emulated/0/Download/models/llama-7b-q4.bin",
  "modelType": "llama.cpp",
  "contextSize": 2048,
  "temperature": 0.7
}
```

### 本地模型性能考虑

- 本地模型通常需要较高的设备性能，建议在高端设备上使用
- 量化模型可以减小模型大小并提高推理速度，但可能会降低输出质量
- 应用程序会自动调整模型参数以适应设备性能

## 安全最佳实践

### API密钥安全

- API密钥存储在Android KeyStore中，使用加密保护
- 应用程序不会将API密钥发送到除指定端点以外的任何服务器
- 用户可以设置访问限制，要求输入密码或生物认证才能访问API设置

### 数据隐私

- 默认情况下，任务数据仅在本地处理，不会发送到外部服务器
- 使用AI功能时，只有必要的任务信息会发送到AI服务提供商
- 用户可以查看和删除已发送到AI服务提供商的数据历史记录

### 网络安全

- 所有API请求都使用HTTPS协议加密传输
- 应用程序验证服务器证书，防止中间人攻击
- 实现请求速率限制，防止API滥用

## 故障排除

### 常见错误

1. **API密钥无效**
   - 错误消息：`Invalid API key`
   - 解决方案：检查API密钥是否正确，是否已过期

2. **请求超时**
   - 错误消息：`Request timed out`
   - 解决方案：检查网络连接，或者增加超时时间

3. **模型不可用**
   - 错误消息：`Model not available`
   - 解决方案：检查模型名称是否正确，或者选择其他可用模型

4. **配额超限**
   - 错误消息：`Rate limit exceeded`
   - 解决方案：减少请求频率，或者升级API计划

### 日志记录

应用程序会记录AI API调用的详细日志，包括请求和响应，但不包括API密钥。可以在设置页面中启用"导出API日志"功能，将日志保存到文件，以便进行故障排除。

## 示例代码

### 任务分解示例

```kotlin
// 获取默认AI提供商
val aiProviderRepository = AiProviderRepository(database.aiProviderDao())
val defaultProvider = aiProviderRepository.getDefaultProvider()

// 创建AI服务客户端
val aiServiceClient = AiServiceClientFactory.createClient(defaultProvider)

// 分解任务
val taskDescription = "开发一个Android应用程序，包含任务管理和番茄钟功能"
val subtasks = aiServiceClient.decomposeTask(taskDescription)

// 处理结果
subtasks.forEach { subtask ->
    println("子任务: ${subtask.title}")
    println("描述: ${subtask.description}")
}
```

### 时间估算示例

```kotlin
// 获取默认AI提供商
val aiProviderRepository = AiProviderRepository(database.aiProviderDao())
val defaultProvider = aiProviderRepository.getDefaultProvider()

// 创建AI服务客户端
val aiServiceClient = AiServiceClientFactory.createClient(defaultProvider)

// 估算时间
val taskDescription = "编写一篇2000字的技术博客文章"
val timeEstimation = aiServiceClient.estimateTime(taskDescription)

// 处理结果
println("估算时间: ${timeEstimation.estimatedHours}小时")
println("解释: ${timeEstimation.explanation}")
```

### 优先级推荐示例

```kotlin
// 获取默认AI提供商
val aiProviderRepository = AiProviderRepository(database.aiProviderDao())
val defaultProvider = aiProviderRepository.getDefaultProvider()

// 创建AI服务客户端
val aiServiceClient = AiServiceClientFactory.createClient(defaultProvider)

// 推荐优先级
val taskTitle = "完成季度报告"
val taskDescription = "汇总第三季度销售数据，分析趋势，并准备演示文稿"
val dueDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3) }.time
val priorityRecommendation = aiServiceClient.recommendPriority(taskTitle, taskDescription, dueDate)

// 处理结果
println("推荐优先级: ${priorityRecommendation.priority}")
println("解释: ${priorityRecommendation.explanation}")
```

## 资源和参考

- [OpenAI API文档](https://platform.openai.com/docs/api-reference)
- [Azure OpenAI服务文档](https://learn.microsoft.com/zh-cn/azure/cognitive-services/openai/)
- [Anthropic Claude API文档](https://docs.anthropic.com/claude/reference/getting-started-with-the-api)
- [百度文心一言API文档](https://cloud.baidu.com/doc/WENXINWORKSHOP/index.html)
- [讯飞星火API文档](https://www.xfyun.cn/doc/spark/Web.html)
- [ONNX Runtime文档](https://onnxruntime.ai/)
- [TensorFlow Lite文档](https://www.tensorflow.org/lite)
- [llama.cpp项目](https://github.com/ggerganov/llama.cpp)

## 联系与支持

如果您在集成AI API时遇到问题，或者有任何建议，请通过以下方式联系我们：

- 电子邮件：api-support@taskmasterai.com
- 开发者论坛：https://forum.taskmasterai.com
- GitHub问题：https://github.com/taskmasterai/android-app/issues
