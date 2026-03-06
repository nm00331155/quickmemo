package com.quickmemo.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            return QuickMemoDestinations.TODO
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
) {
    var launchAuthPassed by remember(settings.requireAuthOnLaunch) {
        mutableStateOf(!settings.requireAuthOnLaunch)
    }

    LaunchedEffect(settings.quickInputNotificationEnabled) {
        onToggleQuickService(settings.quickInputNotificationEnabled)
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
