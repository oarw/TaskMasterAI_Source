package com.taskmaster.ai.data.repository

import androidx.lifecycle.LiveData
import com.taskmaster.ai.data.AiProvider
import com.taskmaster.ai.data.AiProviderDao

/**
 * AI提供商仓库类
 */
class AiProviderRepository(private val aiProviderDao: AiProviderDao) {
    
    // 获取所有AI提供商
    val allAiProviders: LiveData<List<AiProvider>> = aiProviderDao.getAllAiProviders()
    
    // 获取默认AI提供商
    val defaultAiProvider: LiveData<AiProvider> = aiProviderDao.getDefaultAiProvider()
    
    // 插入AI提供商
    suspend fun insertAiProvider(aiProvider: AiProvider): Long {
        return aiProviderDao.insertAiProvider(aiProvider)
    }
    
    // 更新AI提供商
    suspend fun updateAiProvider(aiProvider: AiProvider) {
        aiProviderDao.updateAiProvider(aiProvider)
    }
    
    // 删除AI提供商
    suspend fun deleteAiProvider(aiProvider: AiProvider) {
        aiProviderDao.deleteAiProvider(aiProvider)
    }
    
    // 设置默认AI提供商
    suspend fun setDefaultProvider(providerId: Long) {
        aiProviderDao.clearDefaultProvider()
        aiProviderDao.setDefaultProvider(providerId)
    }
    
    // 获取所有AI提供商（同步）
    fun getAllAiProvidersSync(): List<AiProvider> {
        return aiProviderDao.getAllAiProvidersSync()
    }
    
    // 删除所有AI提供商
    suspend fun deleteAllAiProviders() {
        aiProviderDao.deleteAllAiProviders()
    }
}
