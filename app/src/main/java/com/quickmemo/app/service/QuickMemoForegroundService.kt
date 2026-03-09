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
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.quickmemo.app.MainActivity
import com.quickmemo.app.R
import com.quickmemo.app.domain.repository.SettingsRepository
import com.quickmemo.app.domain.repository.TodoRepository
import com.quickmemo.app.presentation.todo.QuickAddTodoActivity
import com.quickmemo.app.receiver.NotificationActionReceiver
import com.quickmemo.app.util.EditorDebugLog
import com.quickmemo.app.util.QuickMemoIntents
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuickMemoForegroundService : Service() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var todoRepository: TodoRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var hasStartedForeground = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForegroundStarted()
        EditorDebugLog.log(
            context = this,
            category = "Notification/Todo",
            message = "service start action=${intent?.action ?: "default"}",
        )
        serviceScope.launch { refreshNotification() }
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
    }

    private suspend fun refreshNotification() {
        val notification = buildNotification()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
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
        val settings = settingsRepository.settingsFlow.first()
        val tabId = settings.lockscreenTodoTabId.coerceAtLeast(0)
        val maxVisibleItems = settings.lockscreenTodoMaxItems.coerceIn(1, 15)
        val tabNames = todoRepository.observeTabNames().first()
        val tabName = tabNames.getOrElse(tabId) { "Todo ${tabId + 1}" }
        val uncheckedItems = todoRepository.getUncheckedItems(tabId).first()
        val checkedItems = todoRepository.getCheckedItems(tabId).first()
        val checkedCount = checkedItems.size
        val totalCount = uncheckedItems.size + checkedCount

        val normalizedItems = uncheckedItems
            .map { normalizeTodoLine(it.text) }
            .filter { it.isNotBlank() }
        val previewItems = normalizedItems.take(maxVisibleItems)
        val contentText = when {
            previewItems.isNotEmpty() -> {
                val moreText = if (normalizedItems.size > previewItems.size) {
                    " 他${normalizedItems.size - previewItems.size}件"
                } else {
                    ""
                }
                previewItems.joinToString(separator = "、") { "□ $it" } + moreText
            }

            checkedCount > 0 -> "すべて完了！"
            else -> "未完了Todoはありません"
        }
        val titleText = "☑ $tabName  $checkedCount/$totalCount 完了"

        EditorDebugLog.log(
            context = this,
            category = "Notification/Todo",
            message = "build tabId=$tabId maxLines=$maxVisibleItems unchecked=${uncheckedItems.size} checked=$checkedCount displayed=${previewItems.size}",
        )

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(titleText)
        if (previewItems.isEmpty()) {
            inboxStyle.addLine(
                if (checkedCount > 0) {
                    "すべて完了！"
                } else {
                    "未完了Todoはありません"
                },
            )
        } else {
            previewItems.forEach { inboxStyle.addLine("□ $it") }
        }
        if (normalizedItems.size > previewItems.size) {
            inboxStyle.setSummaryText("他${normalizedItems.size - previewItems.size}件")
        }

        val openTodoIntent = Intent(this, MainActivity::class.java).apply {
            action = QuickMemoIntents.ACTION_OPEN_TODO
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openTodoPending = PendingIntent.getActivity(
            this,
            REQUEST_OPEN_TODO,
            openTodoIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val remoteInputTodo = RemoteInput.Builder(NotificationActionReceiver.REMOTE_INPUT_ADD_TODO)
            .setLabel("$tabName に追加")
            .build()
        val addTodoBroadcastIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_ADD_TODO
        }
        val addTodoPending = PendingIntent.getBroadcast(
            this,
            REQUEST_ADD_TODO,
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

        val quickAddIntent = QuickAddTodoActivity.createIntent(this).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val quickAddPending = PendingIntent.getActivity(
            this,
            REQUEST_QUICK_ADD,
            quickAddIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val openEditorIntent = Intent(this, MainActivity::class.java).apply {
            action = QuickMemoIntents.ACTION_OPEN_EDITOR
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openEditorPending = PendingIntent.getActivity(
            this,
            REQUEST_OPEN_EDITOR,
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
            .setContentIntent(openTodoPending)
            .addAction(addTodoAction)
            .addAction(0, "入力画面", quickAddPending)
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
        private const val REQUEST_OPEN_TODO = 100
        private const val REQUEST_OPEN_EDITOR = 101
        private const val REQUEST_ADD_TODO = 200
        private const val REQUEST_QUICK_ADD = 201

        fun refreshFromOutside(context: Context) {
            EditorDebugLog.log(
                context = context,
                category = "Notification/Todo",
                message = "external refresh requested",
            )
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
