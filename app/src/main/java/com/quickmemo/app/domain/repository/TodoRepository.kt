// File: app/src/main/java/com/quickmemo/app/domain/repository/TodoRepository.kt
package com.quickmemo.app.domain.repository

import com.quickmemo.app.domain.model.TodoItem
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun getUncheckedItems(tabId: Int): Flow<List<TodoItem>>
    fun getCheckedItems(tabId: Int): Flow<List<TodoItem>>
    fun getTotalCount(tabId: Int): Flow<Int>
    fun getCompletedCount(tabId: Int): Flow<Int>
    fun observeTabNames(): Flow<List<String>>

    suspend fun addItem(tabId: Int, text: String)
    suspend fun updateText(id: String, text: String)
    suspend fun updateChecked(id: String, checked: Boolean)
    suspend fun updateDueDate(id: String, dueDate: Long?)
    suspend fun reorderUncheckedItems(tabId: Int, orderedIds: List<String>)
    suspend fun deleteItem(id: String): TodoItem?
    suspend fun restoreItem(item: TodoItem)
    suspend fun setTabName(tabId: Int, name: String)
}
