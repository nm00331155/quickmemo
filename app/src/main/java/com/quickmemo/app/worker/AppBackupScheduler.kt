package com.quickmemo.app.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object AppBackupScheduler {
    private const val UNIQUE_WORK_NAME = "app_backup_daily"

    fun schedule(
        context: Context,
        hour: Int = 0,
        minute: Int = 0,
    ) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)

        val now = LocalDateTime.now()
        var nextRun = now
            .withHour(safeHour)
            .withMinute(safeMinute)
            .withSecond(0)
            .withNano(0)
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1)
        }

        val initialDelayMillis = Duration.between(now, nextRun).toMillis()
        val request = PeriodicWorkRequestBuilder<AppBackupWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .addTag("app_backup")
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
