package com.taskmaster.ai.data.repository

import androidx.lifecycle.LiveData
import com.taskmaster.ai.data.PomodoroRecord
import com.taskmaster.ai.data.PomodoroRecordDao
import java.util.Date

/**
 * 番茄钟记录仓库类
 */
class PomodoroRepository(private val pomodoroRecordDao: PomodoroRecordDao) {
    
    // 获取所有番茄钟记录
    val allPomodoroRecords: LiveData<List<PomodoroRecord>> = pomodoroRecordDao.getAllPomodoroRecords()
    
    // 根据任务获取番茄钟记录
    fun getPomodoroRecordsByTask(taskId: Long): LiveData<List<PomodoroRecord>> {
        return pomodoroRecordDao.getPomodoroRecordsByTask(taskId)
    }
    
    // 根据日期范围获取番茄钟记录
    fun getPomodoroRecordsByDateRange(startDate: Date, endDate: Date): LiveData<List<PomodoroRecord>> {
        return pomodoroRecordDao.getPomodoroRecordsByDateRange(startDate, endDate)
    }
    
    // 插入番茄钟记录
    suspend fun insertPomodoroRecord(pomodoroRecord: PomodoroRecord): Long {
        return pomodoroRecordDao.insertPomodoroRecord(pomodoroRecord)
    }
    
    // 更新番茄钟记录
    suspend fun updatePomodoroRecord(pomodoroRecord: PomodoroRecord) {
        pomodoroRecordDao.updatePomodoroRecord(pomodoroRecord)
    }
    
    // 删除番茄钟记录
    suspend fun deletePomodoroRecord(pomodoroRecord: PomodoroRecord) {
        pomodoroRecordDao.deletePomodoroRecord(pomodoroRecord)
    }
    
    // 获取所有番茄钟记录（同步）
    fun getAllPomodoroRecordsSync(): List<PomodoroRecord> {
        return pomodoroRecordDao.getAllPomodoroRecordsSync()
    }
    
    // 删除所有番茄钟记录
    suspend fun deleteAllPomodoroRecords() {
        pomodoroRecordDao.deleteAllPomodoroRecords()
    }
}
