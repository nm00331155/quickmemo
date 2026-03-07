package com.quickmemo.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import com.quickmemo.app.domain.model.AppSettings
import com.quickmemo.app.domain.repository.SettingsRepository
import com.quickmemo.app.presentation.navigation.QuickMemoDestinations
import com.quickmemo.app.presentation.navigation.QuickMemoNavHost
import com.quickmemo.app.presentation.theme.QuickMemoTheme
import com.quickmemo.app.service.QuickMemoForegroundService
import com.quickmemo.app.util.BiometricAuthenticator
import com.quickmemo.app.util.QuickMemoIntents
import com.quickmemo.app.worker.TodoReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var biometricAuthenticator: BiometricAuthenticator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialRoute = buildInitialRoute(intent)

        setContent {
            val settings by settingsRepository.settingsFlow.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )

            QuickMemoTheme(themeMode = settings.themeMode) {
                MainActivityContent(
                    settings = settings,
                    initialRoute = initialRoute,
                    onRequireBiometric = { title, subtitle, onResult ->
                        biometricAuthenticator.authenticate(
                            activity = this@MainActivity,
                            title = title,
                            subtitle = subtitle,
                            onResult = onResult,
                        )
                    },
                    onToggleQuickService = { enabled ->
                        toggleQuickInputService(enabled)
                    },
                    onToggleTodoReminder = { enabled ->
                        toggleTodoReminder(enabled)
                    },
                    onSetLockscreenGuideShown = {
                        settingsRepository.setLockscreenGuideShown()
                    },
                    onSetQuickInputNotification = { enabled ->
                        settingsRepository.setQuickInputNotificationEnabled(enabled)
                    },
                )
            }
        }
    }

    private fun toggleQuickInputService(enabled: Boolean) {
        val serviceIntent = Intent(this, QuickMemoForegroundService::class.java)
        if (enabled) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            stopService(serviceIntent)
        }
    }

    private fun toggleTodoReminder(enabled: Boolean) {
        if (enabled) {
            TodoReminderScheduler.schedule(this)
        } else {
            TodoReminderScheduler.cancel(this)
        }
    }

    private fun buildInitialRoute(intent: Intent?): String {
        if (intent == null) return QuickMemoDestinations.HOME

        if (intent.action == QuickMemoIntents.ACTION_OPEN_TODO) {
            return QuickMemoDestinations.todoRoute(
                addTodo = intent.getBooleanExtra(QuickMemoIntents.EXTRA_ADD_TODO, false),
            )
        }

        val isOpenEditorAction = intent.action == QuickMemoIntents.ACTION_OPEN_EDITOR ||
            intent.action == QuickMemoIntents.ACTION_NEW_MEMO

        if (!isOpenEditorAction) return QuickMemoDestinations.HOME

        return QuickMemoDestinations.editorRoute(
            memoId = intent.getLongExtra(QuickMemoIntents.EXTRA_MEMO_ID, 0L),
            prefillText = intent.getStringExtra(QuickMemoIntents.EXTRA_PRE_FILLED_TEXT).orEmpty(),
            insertToday = intent.getBooleanExtra(QuickMemoIntents.EXTRA_INSERT_TODAY, false),
        )
    }
}

@Composable
private fun MainActivityContent(
    settings: AppSettings,
    initialRoute: String,
    onRequireBiometric: (
        title: String,
        subtitle: String,
        onResult: (Boolean) -> Unit,
    ) -> Unit,
    onToggleQuickService: (Boolean) -> Unit,
    onToggleTodoReminder: (Boolean) -> Unit,
    onSetLockscreenGuideShown: suspend () -> Unit,
    onSetQuickInputNotification: suspend (Boolean) -> Unit,
) {
    var launchAuthPassed by remember(settings.requireAuthOnLaunch) {
        mutableStateOf(!settings.requireAuthOnLaunch)
    }
    var showLockscreenGuide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(settings.quickInputNotificationEnabled) {
        onToggleQuickService(settings.quickInputNotificationEnabled)
    }

    LaunchedEffect(settings.lockscreenGuideShown) {
        if (!settings.lockscreenGuideShown) {
            showLockscreenGuide = true
        }
    }

    LaunchedEffect(settings.todoReminderEnabled) {
        onToggleTodoReminder(settings.todoReminderEnabled)
    }

    LaunchedEffect(settings.requireAuthOnLaunch) {
        if (settings.requireAuthOnLaunch && !launchAuthPassed) {
            onRequireBiometric(
                "認証が必要です",
                "QuickMemo を開くために認証してください",
            ) { passed ->
                launchAuthPassed = passed
            }
        }
    }

    if (settings.requireAuthOnLaunch && !launchAuthPassed) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "認証を待っています...",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    if (showLockscreenGuide) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("ロック画面にTodoを表示") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "QuickMemoはロック画面にTodoリストを表示できます。" +
                            "通知からロックを解除せずにTodoの追加も可能です。",
                    )
                    Text(
                        "この機能を利用するには、端末の設定で以下を確認してください:",
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("1. 設定 → 通知 → ロック画面の通知 → 「すべての通知内容を表示」を選択")
                    Text("2. QuickMemoの通知チャンネルが有効になっていること")
                    Text(
                        "この設定はアプリの設定画面からいつでも変更できます。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLockscreenGuide = false
                        scope.launch {
                            onSetLockscreenGuideShown()
                            onSetQuickInputNotification(true)
                        }
                    },
                ) {
                    Text("有効にする")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLockscreenGuide = false
                        scope.launch {
                            onSetLockscreenGuideShown()
                            onSetQuickInputNotification(false)
                        }
                    },
                ) {
                    Text("後で設定する")
                }
            },
        )
    }

    QuickMemoNavHost(
        startDestination = initialRoute,
        onToggleQuickService = onToggleQuickService,
        onToggleTodoReminder = onToggleTodoReminder,
        onRequestBiometric = { reason, callback ->
            onRequireBiometric(
                "認証が必要です",
                reason,
                callback,
            )
        },
    )
}
