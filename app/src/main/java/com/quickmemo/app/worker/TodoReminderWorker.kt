// File: app/src/main/java/com/quickmemo/app/worker/TodoReminderWorker.kt
package com.quickmemo.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quickmemo.app.MainActivity
import com.quickmemo.app.R
import com.quickmemo.app.data.datastore.settingsDataStore
import com.quickmemo.app.data.local.database.DatabaseMigrations
import com.quickmemo.app.data.local.database.QuickMemoDatabase
import com.quickmemo.app.data.local.entity.TodoItemEntity
import com.quickmemo.app.util.QuickMemoIntents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TodoReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settings = applicationContext.settingsDataStore.data
            .map { prefs ->
                ReminderConfig(
                    enabled = prefs[booleanPreferencesKey("todo_reminder_enabled")] ?: true,
                    customHoursBefore = prefs[intPreferencesKey("todo_reminder_custom_hours")],
                    oneDayBefore = prefs[booleanPreferencesKey("todo_reminder_1day")] ?: true,
                    threeDaysBefore = prefs[booleanPreferencesKey("todo_reminder_3days")] ?: false,
                    oneWeekBefore = prefs[booleanPreferencesKey("todo_reminder_1week")] ?: false,
                )
            }
            .first()

        if (!settings.enabled) return Result.success()

        val hasAnyTiming = settings.customHoursBefore != null ||
            settings.oneDayBefore ||
            settings.threeDaysBefore ||
            settings.oneWeekBefore
        if (!hasAnyTiming) return Result.success()

        val db = Room.databaseBuilder(
            applicationContext,
            QuickMemoDatabase::class.java,
            QuickMemoDatabase.DB_NAME,
        )
            .addMigrations(
                DatabaseMigrations.MIGRATION_1_2,
                DatabaseMigrations.MIGRATION_2_3,
                DatabaseMigrations.MIGRATION_3_4,
                DatabaseMigrations.MIGRATION_4_5,
                DatabaseMigrations.MIGRATION_5_6,
                DatabaseMigrations.MIGRATION_6_7,
                DatabaseMigrations.MIGRATION_7_8,
                DatabaseMigrations.MIGRATION_8_9,
            )
            .build()

        return try {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)

            val reminderTargets = db.todoDao()
                .getItemsWithDueDate()
                .filter { item ->
                    shouldNotifyToday(
                        item = item,
                        today = today,
                        zone = zone,
                        settings = settings,
                    )
                }

            if (reminderTargets.isEmpty()) {
                return Result.success()
            }

            createNotificationChannel()
            val notificationManager = applicationContext.getSystemService<NotificationManager>()
                ?: return Result.retry()

            reminderTargets.forEach { item ->
                val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_memo)
                    .setContentTitle("Todoリマインド")
                    .setContentText(buildContentText(item, zone))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(buildTodoPendingIntent(item.id.hashCode()))
                    .build()

                notificationManager.notify(item.id.hashCode(), notification)
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        } finally {
            db.close()
        }
    }

    private fun shouldNotifyToday(
        item: TodoItemEntity,
        today: LocalDate,
        zone: ZoneId,
        settings: ReminderConfig,
    ): Boolean {
        val dueDateMillis = item.dueDate ?: return false
        val dueDate = Instant.ofEpochMilli(dueDateMillis)
            .atZone(zone)
            .toLocalDate()

        val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)

        val oneDayMatched = settings.oneDayBefore && daysUntilDue == 1L
        val threeDaysMatched = settings.threeDaysBefore && daysUntilDue == 3L
        val oneWeekMatched = settings.oneWeekBefore && daysUntilDue == 7L

        val customMatched = settings.customHoursBefore?.let { hours ->
            val triggerDate = Instant.ofEpochMilli(dueDateMillis)
                .atZone(zone)
                .minusHours(hours.toLong())
                .toLocalDate()
            triggerDate == today
        } ?: false

        return oneDayMatched || threeDaysMatched || oneWeekMatched || customMatched
    }

    private fun buildContentText(item: TodoItemEntity, zone: ZoneId): String {
        val dueDate = item.dueDate?.let {
            val localDate = Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
            "期限: ${localDate.monthValue}/${localDate.dayOfMonth}"
        } ?: "期限未設定"

        val text = item.text.ifBlank { "(無題)" }
        return "$text（$dueDate）"
    }

    private fun buildTodoPendingIntent(requestCode: Int): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = QuickMemoIntents.ACTION_OPEN_TODO
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannel() {
        val notificationManager = applicationContext.getSystemService<NotificationManager>() ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Todoリマインダー",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager.createNotificationChannel(channel)
    }

    private data class ReminderConfig(
        val enabled: Boolean,
        val customHoursBefore: Int?,
        val oneDayBefore: Boolean,
        val threeDaysBefore: Boolean,
        val oneWeekBefore: Boolean,
    )

    companion object {
        private const val CHANNEL_ID = "todo_reminder"
    }
}
