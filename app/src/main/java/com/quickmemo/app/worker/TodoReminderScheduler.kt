// File: app/src/main/java/com/quickmemo/app/worker/TodoReminderScheduler.kt
package com.quickmemo.app.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object TodoReminderScheduler {
    private const val UNIQUE_WORK_NAME = "todo_reminder_daily_9am"

    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        var nextRun = now.withHour(9).withMinute(0).withSecond(0).withNano(0)
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1)
        }
        val initialDelayMillis = Duration.between(now, nextRun).toMillis()

        val request = PeriodicWorkRequestBuilder<TodoReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
