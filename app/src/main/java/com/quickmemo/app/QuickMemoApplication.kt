package com.quickmemo.app

import android.app.Application
import com.quickmemo.app.ads.AdsManager
import com.quickmemo.app.worker.AppBackupScheduler
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
        adsManager.initialize(this)
        TrashCleanupScheduler.schedule(this)
        TodoReminderScheduler.schedule(this)
        AppBackupScheduler.schedule(this)
    }
}
