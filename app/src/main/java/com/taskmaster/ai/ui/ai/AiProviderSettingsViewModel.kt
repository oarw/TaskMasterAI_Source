package com.taskmaster.ai.ui.ai

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskmaster.ai.data.AiProvider
import com.taskmaster.ai.data.repository.AiProviderRepository
import kotlinx.coroutines.launch

/**
 * AI提供商设置ViewModel
 */
class AiProviderSettingsViewModel(
    private val aiProviderRepository: AiProviderRepository
) : ViewModel() {
    
    // AI提供商列表
    val aiProviders: LiveData<List<AiProvider>> = aiProviderRepository.allAiProviders
    
    // 操作状态
    private val _operationStatus = MutableLiveData<String>()
    val operationStatus: LiveData<String> = _operationStatus
    
    // 添加AI提供商
    fun addAiProvider(provider: AiProvider) {
        viewModelScope.launch {
            try {
                val id = aiProviderRepository.insertAiProvider(provider)
                if (id > 0) {
                    _operationStatus.value = "添加成功"
                    
                    // 如果是第一个提供商，自动设为默认
                    if (aiProviders.value?.size == 0) {
                        aiProviderRepository.setDefaultProvider(id)
                    }
                } else {
                    _operationStatus.value = "添加失败"
                }
            } catch (e: Exception) {
                _operationStatus.value = "添加失败: ${e.message}"
            }
        }
    }
    
    // 更新AI提供商
    fun updateAiProvider(provider: AiProvider) {
        viewModelScope.launch {
            try {
                aiProviderRepository.updateAiProvider(provider)
                _operationStatus.value = "更新成功"
            } catch (e: Exception) {
                _operationStatus.value = "更新失败: ${e.message}"
            }
        }
    }
    
    // 删除AI提供商
    fun deleteAiProvider(provider: AiProvider) {
        viewModelScope.launch {
            try {
                aiProviderRepository.deleteAiProvider(provider)
                _operationStatus.value = "删除成功"
            } catch (e: Exception) {
                _operationStatus.value = "删除失败: ${e.message}"
            }
        }
    }
    
    // 设置默认提供商
    fun setDefaultProvider(providerId: Long) {
        viewModelScope.launch {
            try {
                aiProviderRepository.setDefaultProvider(providerId)
                _operationStatus.value = "已设为默认提供商"
            } catch (e: Exception) {
                _operationStatus.value = "设置默认提供商失败: ${e.message}"
            }
        }
    }
    
    // 清除操作状态
    fun clearOperationStatus() {
        _operationStatus.value = ""
    }
    
    /**
     * AiProviderSettingsViewModel工厂类
     */
    class AiProviderSettingsViewModelFactory(
        private val aiProviderRepository: AiProviderRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AiProviderSettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AiProviderSettingsViewModel(aiProviderRepository) as T
            }
            throw IllegalArgumentException("未知的ViewModel类")
        }
    }
}
