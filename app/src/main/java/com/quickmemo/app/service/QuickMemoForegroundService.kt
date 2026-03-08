package com.quickmemo.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.quickmemo.app.MainActivity
import com.quickmemo.app.R
import com.quickmemo.app.data.local.database.QuickMemoDatabase
import com.quickmemo.app.receiver.NotificationActionReceiver
import com.quickmemo.app.util.QuickMemoIntents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class QuickMemoForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var hasStartedForeground = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForegroundStarted()

        when (intent?.action) {
            ACTION_REFRESH -> serviceScope.launch { refreshNotification() }
            else -> serviceScope.launch { refreshNotification() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        hasStartedForeground = false
        serviceScope.cancel()
    }

    private fun ensureForegroundStarted() {
        if (hasStartedForeground) return

        val startupNotification = buildStartupNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                startupNotification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, startupNotification)
        }
        hasStartedForeground = true
        Log.d(TAG, "ForegroundService started with startup notification")
    }

    private suspend fun refreshNotification() {
        val notification = buildNotification()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Notification refreshed")
    }

    private fun buildStartupNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_memo)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle("☑ Todoを準備中")
            .setContentText("QuickMemoの通知を更新しています")
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private suspend fun buildNotification(): Notification {
        val db = QuickMemoDatabase.getInstance(applicationContext)
        val todoDao = db.todoDao()

        val uncheckedItems = try {
            todoDao.getUncheckedItemsSync()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load unchecked todos", e)
            emptyList()
        }

        val checkedCount = try {
            todoDao.getCheckedCountSync()
        } catch (_: Exception) {
            0
        }

        val totalCount = uncheckedItems.size + checkedCount

        Log.d(
            TAG,
            "Building notification: unchecked=${uncheckedItems.size}, checked=$checkedCount, total=$totalCount",
        )
        val titleText = "☑ Todo $checkedCount/$totalCount 完了"
        val previewItems = uncheckedItems
            .asSequence()
            .map { normalizeTodoLine(it.text) }
            .filter { it.isNotBlank() }
            .take(3)
            .toList()
        val previewText = if (previewItems.isNotEmpty()) {
            previewItems.joinToString(separator = "、") { "□ $it" }
        } else if (checkedCount > 0) {
            "すべて完了！"
        } else {
            "未完了Todoはありません"
        }
        val moreText = if (uncheckedItems.size > 3) " 他${uncheckedItems.size - 3}件" else ""
        val contentText = "$previewText$moreText"

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(titleText)
        val expandedItems = uncheckedItems
            .asSequence()
            .map { normalizeTodoLine(it.text) }
            .filter { it.isNotBlank() }
            .take(8)
            .toList()
        if (expandedItems.isEmpty()) {
            inboxStyle.addLine(
                if (checkedCount > 0) {
                    "すべて完了！"
                } else {
                    "未完了Todoはありません"
                },
            )
        } else {
            expandedItems.forEach { inboxStyle.addLine("□ $it") }
        }
        if (uncheckedItems.size > 8) {
            inboxStyle.setSummaryText("他${uncheckedItems.size - 8}件")
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            action = QuickMemoIntents.ACTION_OPEN_TODO
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val remoteInputTodo = RemoteInput.Builder(NotificationActionReceiver.REMOTE_INPUT_ADD_TODO)
            .setLabel("Todoを入力...")
            .build()

        val addTodoBroadcastIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_ADD_TODO
        }
        val addTodoPending = PendingIntent.getBroadcast(
            this,
            200,
            addTodoBroadcastIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val addTodoAction = NotificationCompat.Action.Builder(
            0,
            "Todo追加",
            addTodoPending,
        )
            .addRemoteInput(remoteInputTodo)
            .setAllowGeneratedReplies(false)
            .build()

        val openEditorIntent = Intent(this, MainActivity::class.java).apply {
            action = QuickMemoIntents.ACTION_OPEN_EDITOR
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val openEditorPending = PendingIntent.getActivity(
            this,
            101,
            openEditorIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_memo)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setStyle(inboxStyle)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPending)
            .addAction(addTodoAction)
            .addAction(0, "メモを編集", openEditorPending)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "QuickMemo Todo表示",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "ロック画面・通知バーにTodoを表示します"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            Log.d(
                TAG,
                "NotificationChannel created: importance=DEFAULT, sound=null, lockscreenVisibility=PUBLIC",
            )
        }
    }

    private fun normalizeTodoLine(source: String): String {
        return source
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    companion object {
        const val CHANNEL_ID = "quickmemo_todo_display_v1"
        const val NOTIFICATION_ID = 1101
        const val ACTION_REFRESH = "com.quickmemo.app.ACTION_REFRESH_NOTIFICATION"
        private const val TAG = "QM_NOTIF"

        fun refreshFromOutside(context: Context) {
            val intent = Intent(context, QuickMemoForegroundService::class.java).apply {
                action = ACTION_REFRESH
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
