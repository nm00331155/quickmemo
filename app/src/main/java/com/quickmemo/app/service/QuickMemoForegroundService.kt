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
import android.widget.RemoteViews
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
        when (intent?.action) {
            ACTION_REFRESH -> {
                serviceScope.launch {
                    if (hasStartedForeground) {
                        refreshNotification()
                    } else {
                        startWithNotification()
                    }
                }
            }

            else -> {
                serviceScope.launch { startWithNotification() }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        hasStartedForeground = false
        serviceScope.cancel()
    }

    private suspend fun startWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        hasStartedForeground = true
        Log.d(TAG, "ForegroundService started with VISIBILITY_PUBLIC notification")
    }

    private suspend fun refreshNotification() {
        val notification = buildNotification()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Notification refreshed")
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

        val collapsedView = RemoteViews(packageName, R.layout.notification_collapsed)

        val summaryText = if (totalCount > 0) {
            "☑ Todo $checkedCount/$totalCount 完了"
        } else {
            "☑ Todoなし"
        }
        collapsedView.setTextViewText(R.id.notification_summary, summaryText)

        val lineIds = listOf(
            R.id.notification_line1,
            R.id.notification_line2,
            R.id.notification_line3,
        )

        val collapsedItems = uncheckedItems.take(3)
        for (index in lineIds.indices) {
            if (index < collapsedItems.size) {
                val line = collapsedItems[index].text.trim().replace("\n", " ")
                collapsedView.setTextViewText(lineIds[index], "☐ $line")
                collapsedView.setViewVisibility(lineIds[index], android.view.View.VISIBLE)
            } else {
                collapsedView.setViewVisibility(lineIds[index], android.view.View.GONE)
            }
        }

        val expandedView = RemoteViews(packageName, R.layout.notification_expanded)
        expandedView.setTextViewText(R.id.notification_header, "☑ Todo ($checkedCount/$totalCount 完了)")

        val todoTextIds = listOf(
            R.id.notification_todo_1,
            R.id.notification_todo_2,
            R.id.notification_todo_3,
            R.id.notification_todo_4,
            R.id.notification_todo_5,
        )
        val expandedItems = uncheckedItems.take(5)

        for (index in todoTextIds.indices) {
            if (index < expandedItems.size) {
                val itemText = expandedItems[index].text.trim().replace("\n", " ")
                expandedView.setTextViewText(todoTextIds[index], "☐ $itemText")
                expandedView.setViewVisibility(todoTextIds[index], android.view.View.VISIBLE)
            } else {
                expandedView.setViewVisibility(todoTextIds[index], android.view.View.GONE)
            }
        }

        expandedView.setViewVisibility(R.id.notification_pinned_memo, android.view.View.GONE)

        val remainingCount = uncheckedItems.size - 5
        if (remainingCount > 0) {
            expandedView.setTextViewText(R.id.notification_footer, "他 ${remainingCount}件 → タップして全件表示")
        } else if (uncheckedItems.isEmpty()) {
            expandedView.setTextViewText(R.id.notification_footer, "すべて完了！ →")
        } else {
            expandedView.setTextViewText(R.id.notification_footer, "タップして開く →")
        }
        expandedView.setViewVisibility(R.id.notification_footer, android.view.View.VISIBLE)

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
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
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
