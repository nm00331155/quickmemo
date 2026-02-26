package com.quickmemo.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Application
import android.os.Build
import com.quickmemo.app.ads.AdsManager
import com.quickmemo.app.ai.ModelDownloadWorker
import com.quickmemo.app.worker.TodoReminderScheduler
import com.quickmemo.app.worker.TrashCleanupScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class QuickMemoApplication : Application() {

    @Inject
    lateinit var adsManager: AdsManager

    override fun onCreate() {
        super.onCreate()
        createModelDownloadChannel()
        adsManager.initialize(this)
        TrashCleanupScheduler.schedule(this)
        TodoReminderScheduler.schedule(this)
    }

    private fun createModelDownloadChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            ModelDownloadWorker.CHANNEL_ID,
            "AIモデルダウンロード",
            NotificationManager.IMPORTANCE_LOW,
        )
        notificationManager.createNotificationChannel(channel)
    }
}
