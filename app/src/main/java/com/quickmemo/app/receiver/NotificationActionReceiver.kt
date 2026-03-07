package com.quickmemo.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.quickmemo.app.data.local.database.QuickMemoDatabase
import com.quickmemo.app.data.local.entity.TodoItemEntity
import com.quickmemo.app.service.QuickMemoForegroundService
import com.quickmemo.app.widget.WidgetUpdateDispatcher
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_ADD_TODO) {
            handleAddTodo(context, intent)
        }
    }

    private fun handleAddTodo(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent) ?: return
        val inputText = remoteInput.getCharSequence(REMOTE_INPUT_ADD_TODO)?.toString()?.trim()
        if (inputText.isNullOrBlank()) return

        Log.d(TAG, "Adding Todo from lock screen: $inputText")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = QuickMemoDatabase.getInstance(context)
                val todoDao = db.todoDao()

                val maxOrder = todoDao.getMaxSortOrder(0)
                val newItem = TodoItemEntity(
                    id = UUID.randomUUID().toString(),
                    tabId = 0,
                    text = inputText,
                    checked = false,
                    dueDate = null,
                    sortOrder = maxOrder + 1,
                    createdAt = System.currentTimeMillis(),
                    checkedAt = null,
                )
                todoDao.upsertItem(newItem)

                WidgetUpdateDispatcher.updateTodoWidgets(context)
                QuickMemoForegroundService.refreshFromOutside(context)
                Log.d(TAG, "Todo added successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add todo", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ADD_TODO = "com.quickmemo.app.ACTION_ADD_TODO_FROM_NOTIFICATION"
        const val REMOTE_INPUT_ADD_TODO = "key_add_todo_text"
        private const val TAG = "QM_NOTIF_ACTION"
    }
}
