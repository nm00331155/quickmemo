package com.quickmemo.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.quickmemo.app.MainActivity
import com.quickmemo.app.R
import com.quickmemo.app.util.QuickMemoIntents

class QuickMemoForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.quick_input_notification_title))
            .setContentText(getString(R.string.quick_input_notification_text))
            .setSmallIcon(R.drawable.ic_notification_memo)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    100,
                    buildEditorIntent(),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .addAction(
                0,
                getString(R.string.quick_new_memo),
                PendingIntent.getActivity(
                    this,
                    101,
                    buildEditorIntent(),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildEditorIntent(): Intent {
        return Intent(this, MainActivity::class.java).apply {
            action = QuickMemoIntents.ACTION_OPEN_EDITOR
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "QuickMemo Quick Input",
                NotificationManager.IMPORTANCE_MIN,
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "quickmemo_quick_input"
        const val NOTIFICATION_ID = 1101
    }
}
