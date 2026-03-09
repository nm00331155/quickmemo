package com.quickmemo.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.quickmemo.app.domain.repository.SettingsRepository
import com.quickmemo.app.domain.repository.TodoRepository
import com.quickmemo.app.util.EditorDebugLog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var todoRepository: TodoRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_ADD_TODO) {
            handleAddTodo(context, intent)
        }
    }

    private fun handleAddTodo(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent) ?: return
        val inputText = remoteInput.getCharSequence(REMOTE_INPUT_ADD_TODO)?.toString()?.trim()
        if (inputText.isNullOrBlank()) return

        EditorDebugLog.log(
            context = context,
            category = "Notification/Todo",
            message = "direct reply received length=${inputText.length}",
        )

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tabId = settingsRepository.settingsFlow.first().lockscreenTodoTabId
                todoRepository.addItem(tabId, inputText)
                EditorDebugLog.log(
                    context = context,
                    category = "Notification/Todo",
                    message = "direct reply saved tabId=$tabId",
                )
            } catch (e: Exception) {
                EditorDebugLog.log(
                    context = context,
                    category = "Notification/Todo",
                    message = "direct reply failed",
                    throwable = e,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ADD_TODO = "com.quickmemo.app.ACTION_ADD_TODO_FROM_NOTIFICATION"
        const val REMOTE_INPUT_ADD_TODO = "key_add_todo_text"
    }
}
