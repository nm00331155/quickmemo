package com.quickmemo.app.presentation.todo

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.quickmemo.app.domain.model.AppSettings
import com.quickmemo.app.domain.repository.SettingsRepository
import com.quickmemo.app.domain.repository.TodoRepository
import com.quickmemo.app.presentation.theme.QuickMemoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuickAddTodoActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var todoRepository: TodoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableLockscreenPresentation()

        setContent {
            val settings by settingsRepository.settingsFlow.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )
            val tabNames by todoRepository.observeTabNames().collectAsStateWithLifecycle(
                initialValue = listOf("Todo 1", "Todo 2", "Todo 3"),
            )

            QuickMemoTheme(themeMode = settings.themeMode) {
                QuickAddTodoScreen(
                    targetTabName = tabNames.getOrElse(settings.lockscreenTodoTabId) {
                        "Todo ${settings.lockscreenTodoTabId + 1}"
                    },
                    onBack = ::finish,
                    onSubmit = { text ->
                        lifecycleScope.launch {
                            todoRepository.addItem(settings.lockscreenTodoTabId, text)
                            finish()
                        }
                    },
                )
            }
        }
    }

    private fun enableLockscreenPresentation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, QuickAddTodoActivity::class.java)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddTodoScreen(
    targetTabName: String,
    onBack: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Add Todo") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("閉じる")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "追加先: $targetTabName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "端末によってはロック解除が必要になる場合があります。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(120) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                label = { Text("Todo を入力") },
                placeholder = { Text("1行で追加") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = {
                        val normalized = text.trim()
                        if (normalized.isNotBlank()) {
                            focusManager.clearFocus(force = true)
                            onSubmit(normalized)
                        }
                    },
                ),
            )
            Button(
                onClick = { onSubmit(text.trim()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = text.isNotBlank(),
            ) {
                Text("追加")
            }
        }
    }
}