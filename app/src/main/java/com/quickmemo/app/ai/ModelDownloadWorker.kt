package com.quickmemo.app.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.quickmemo.app.R

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            createNotificationChannel()

            val modelManager = ModelManager(applicationContext)
            setForeground(createForegroundInfo(0))

            val success = modelManager.downloadModel { progress ->
                val progressInt = (progress * 100).toInt().coerceIn(0, 100)
                NotificationManagerCompat.from(applicationContext)
                    .notify(NOTIFICATION_ID, buildNotification(progressInt))
            }

            if (success) {
                NotificationManagerCompat.from(applicationContext).cancel(NOTIFICATION_ID)
                Result.success()
            } else {
                Result.retry()
            }
        }.getOrElse {
            Result.failure()
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                buildNotification(progress),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, buildNotification(progress))
        }
    }

    private fun buildNotification(progress: Int) = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
        .setContentTitle("AI機能のダウンロード")
        .setContentText("${progress}%")
        .setSmallIcon(R.drawable.ic_notification_memo)
        .setProgress(100, progress, false)
        .setOngoing(true)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AIモデルダウンロード",
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "model_download"
        private const val NOTIFICATION_ID = 1001
    }
}
