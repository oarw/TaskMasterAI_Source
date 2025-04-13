package com.taskmaster.ai.ui.settings

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskmaster.ai.data.backup.BackupManager
import kotlinx.coroutines.launch
import java.io.File

/**
 * 备份与恢复ViewModel
 */
class BackupRestoreViewModel(
    private val backupManager: BackupManager
) : ViewModel() {
    
    // 备份文件列表
    private val _backupFiles = MutableLiveData<List<File>>()
    val backupFiles: LiveData<List<File>> = _backupFiles
    
    // 操作状态
    private val _operationStatus = MutableLiveData<String>()
    val operationStatus: LiveData<String> = _operationStatus
    
    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    /**
     * 加载备份文件列表
     */
    fun loadBackupFiles() {
        viewModelScope.launch {
            val files = backupManager.getBackupFiles()
            _backupFiles.value = files.sortedByDescending { it.lastModified() }
        }
    }
    
    /**
     * 创建备份
     */
    fun createBackup() {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val backupFilePath = backupManager.createBackup()
                _operationStatus.value = "备份已创建: ${File(backupFilePath).name}"
            } catch (e: Exception) {
                _operationStatus.value = "创建备份失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 从文件恢复备份
     */
    fun restoreBackup(backupFilePath: String) {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                backupManager.restoreBackup(backupFilePath)
                _operationStatus.value = "备份已恢复"
            } catch (e: Exception) {
                _operationStatus.value = "恢复备份失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 从URI恢复备份
     */
    fun restoreBackupFromUri(uri: Uri) {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                backupManager.restoreBackupFromUri(uri)
                _operationStatus.value = "备份已恢复"
            } catch (e: Exception) {
                _operationStatus.value = "恢复备份失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 删除备份
     */
    fun deleteBackup(backupFilePath: String) {
        viewModelScope.launch {
            try {
                val success = backupManager.deleteBackup(backupFilePath)
                
                if (success) {
                    _operationStatus.value = "备份已删除"
                    
                    // 更新备份文件列表
                    loadBackupFiles()
                } else {
                    _operationStatus.value = "删除备份失败"
                }
            } catch (e: Exception) {
                _operationStatus.value = "删除备份失败: ${e.message}"
            }
        }
    }
    
    /**
     * 清除操作状态
     */
    fun clearOperationStatus() {
        _operationStatus.value = ""
    }
    
    /**
     * BackupRestoreViewModel工厂类
     */
    class BackupRestoreViewModelFactory(
        private val backupManager: BackupManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BackupRestoreViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BackupRestoreViewModel(backupManager) as T
            }
            throw IllegalArgumentException("未知的ViewModel类")
        }
    }
}
