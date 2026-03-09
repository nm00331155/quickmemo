package com.quickmemo.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.quickmemo.app.domain.repository.TodoRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TodoWidgetConfigActivity : ComponentActivity() {

    @Inject
    lateinit var todoRepository: TodoRepository

    @Inject
    lateinit var widgetRefreshCoordinator: WidgetRefreshCoordinator

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(RESULT_CANCELED)

        setContent {
            TodoConfigScreen(
                todoRepository = todoRepository,
                onConfirm = ::onConfirm,
            )
        }
    }

    private fun onConfirm(tabId: Int) {
        WidgetConfigStore.saveTodoTabId(this, appWidgetId, tabId)
        lifecycleScope.launch {
            widgetRefreshCoordinator.refreshTodoWidgets(reason = "todo_widget_config")
            setResult(
                RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoConfigScreen(
    todoRepository: TodoRepository,
    onConfirm: (Int) -> Unit,
) {
    val tabNames by todoRepository.observeTabNames().collectAsStateWithLifecycle(
        initialValue = listOf("Todo 1", "Todo 2", "Todo 3"),
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("表示するTodoタブを選択") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            tabNames.take(3).forEachIndexed { index, name ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTab = index }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Button(
                onClick = { onConfirm(selectedTab) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("決定")
            }
        }
    }
}
