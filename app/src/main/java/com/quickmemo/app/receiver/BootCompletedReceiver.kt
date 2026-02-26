package com.quickmemo.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import com.quickmemo.app.data.datastore.settingsDataStore
import com.quickmemo.app.service.QuickMemoForegroundService
import com.quickmemo.app.worker.TodoReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = context.settingsDataStore.data
                    .catch { throwable ->
                        if (throwable is IOException) emit(emptyPreferences()) else throw throwable
                    }
                    .map { prefs ->
                        val quickInput = prefs[booleanPreferencesKey("notification_quick_input")]
                            ?: prefs[booleanPreferencesKey("quick_input_notification")]
                            ?: true
                        val todoReminder = prefs[booleanPreferencesKey("todo_reminder_enabled")] ?: true
                        quickInput to todoReminder
                    }
                    .first()

                if (enabled.first) {
                    val serviceIntent = Intent(context, QuickMemoForegroundService::class.java)
                    context.startForegroundService(serviceIntent)
                }

                if (enabled.second) {
                    TodoReminderScheduler.schedule(context)
                } else {
                    TodoReminderScheduler.cancel(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
