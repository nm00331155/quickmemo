// File: app/src/main/java/com/quickmemo/app/data/repository/TodoRepositoryImpl.kt
package com.quickmemo.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quickmemo.app.data.datastore.settingsDataStore
import com.quickmemo.app.data.local.dao.TodoDao
import com.quickmemo.app.domain.model.TodoItem
import com.quickmemo.app.domain.repository.TodoRepository
import com.quickmemo.app.service.QuickMemoForegroundService
import com.quickmemo.app.widget.WidgetUpdateDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

@Singleton
class TodoRepositoryImpl @Inject constructor(
    private val todoDao: TodoDao,
    @ApplicationContext private val context: Context,
) : TodoRepository {

    override fun getUncheckedItems(tabId: Int): Flow<List<TodoItem>> {
        return todoDao.getUncheckedItems(tabId).map { list -> list.map { it.toDomain() } }
    }

    override fun getCheckedItems(tabId: Int): Flow<List<TodoItem>> {
        return todoDao.getCheckedItems(tabId).map { list -> list.map { it.toDomain() } }
    }

    override fun getTotalCount(tabId: Int): Flow<Int> {
        return todoDao.getTotalCount(tabId)
    }

    override fun getCompletedCount(tabId: Int): Flow<Int> {
        return todoDao.getCompletedCount(tabId)
    }

    override fun observeTabNames(): Flow<List<String>> {
        return context.settingsDataStore.data
            .map { preferences ->
                parseTabNames(preferences[TAB_NAMES_KEY])
            }
    }

    override suspend fun addItem(tabId: Int, text: String) {
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) return

        val nextSortOrder = todoDao.getMaxSortOrder(tabId) + 1
        todoDao.upsertItem(
            TodoItem(
                text = normalizedText,
                tabId = tabId,
                sortOrder = nextSortOrder,
            ).toEntity(),
        )
        notifyTodoWidgetUpdated()
        notifyQuickNotificationUpdated()
    }

    override suspend fun updateText(id: String, text: String) {
        todoDao.updateText(id, text.trim())
        notifyTodoWidgetUpdated()
        notifyQuickNotificationUpdated()
    }

    override suspend fun updateChecked(id: String, checked: Boolean) {
        val item = todoDao.getItemById(id) ?: return
        if (item.checked == checked) return

        if (checked) {
            todoDao.updateCheckedState(
                id = id,
                checked = true,
                checkedAt = System.currentTimeMillis(),
            )
            notifyTodoWidgetUpdated()
            notifyQuickNotificationUpdated()
            return
        }

        val nextSortOrder = todoDao.getMaxSortOrder(item.tabId) + 1
        todoDao.moveToUncheckedTail(id, nextSortOrder)
        notifyTodoWidgetUpdated()
        notifyQuickNotificationUpdated()
    }

    override suspend fun updateDueDate(id: String, dueDate: Long?) {
        todoDao.updateDueDate(id, dueDate)
        notifyTodoWidgetUpdated()
        notifyQuickNotificationUpdated()
    }

    override suspend fun reorderUncheckedItems(tabId: Int, orderedIds: List<String>) {
        if (orderedIds.isEmpty()) return

        val currentItemsById = todoDao.getUncheckedItemsSnapshot(tabId).associateBy { it.id }
        val reordered = orderedIds.mapIndexedNotNull { index, id ->
            currentItemsById[id]?.copy(sortOrder = index)
        }
        if (reordered.isNotEmpty()) {
            todoDao.upsertItems(reordered)
            notifyTodoWidgetUpdated()
            notifyQuickNotificationUpdated()
        }
    }

    override suspend fun deleteItem(id: String): TodoItem? {
        val item = todoDao.getItemById(id)?.toDomain()
        todoDao.deleteItem(id)
        notifyTodoWidgetUpdated()
        notifyQuickNotificationUpdated()
        return item
    }

    override suspend fun restoreItem(item: TodoItem) {
        val restored = if (item.checked) {
            item.copy(checkedAt = item.checkedAt ?: System.currentTimeMillis())
        } else {
            val nextSortOrder = todoDao.getMaxSortOrder(item.tabId) + 1
            item.copy(sortOrder = nextSortOrder)
        }
        todoDao.upsertItem(restored.toEntity())
        notifyTodoWidgetUpdated()
        notifyQuickNotificationUpdated()
    }

    override suspend fun setTabName(tabId: Int, name: String) {
        if (tabId !in 0..2) return

        val normalizedName = name.trim().ifBlank { DEFAULT_TAB_NAMES[tabId] }
        context.settingsDataStore.edit { preferences ->
            val current = parseTabNames(preferences[TAB_NAMES_KEY]).toMutableList()
            current[tabId] = normalizedName
            preferences[TAB_NAMES_KEY] = JSONArray(current).toString()
        }
        notifyTodoWidgetUpdated()
        notifyQuickNotificationUpdated()
    }

    private suspend fun notifyTodoWidgetUpdated() {
        runCatching {
            WidgetUpdateDispatcher.updateTodoWidgets(context)
        }
    }

    private fun notifyQuickNotificationUpdated() {
        runCatching {
            QuickMemoForegroundService.refreshFromOutside(context)
        }
    }

    private fun parseTabNames(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return DEFAULT_TAB_NAMES

        val resolved = DEFAULT_TAB_NAMES.toMutableList()
        runCatching {
            val array = JSONArray(raw)
            for (index in 0..2) {
                val value = array.optString(index).trim()
                if (value.isNotBlank()) {
                    resolved[index] = value
                }
            }
        }
        return resolved
    }

    companion object {
        private val DEFAULT_TAB_NAMES = listOf("Todo 1", "Todo 2", "Todo 3")
        private val TAB_NAMES_KEY = stringPreferencesKey("todo_tab_names")
    }
}
