package com.taskmaster.ai.data.repository

import androidx.lifecycle.LiveData
import com.taskmaster.ai.data.Category
import com.taskmaster.ai.data.CategoryDao

/**
 * 任务分类仓库类
 */
class CategoryRepository(private val categoryDao: CategoryDao) {
    
    // 获取所有分类
    val allCategories: LiveData<List<Category>> = categoryDao.getAllCategories()
    
    // 根据ID获取分类
    fun getCategoryById(categoryId: Long): LiveData<Category> {
        return categoryDao.getCategoryById(categoryId)
    }
    
    // 插入分类
    suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }
    
    // 更新分类
    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }
    
    // 删除分类
    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }
}
