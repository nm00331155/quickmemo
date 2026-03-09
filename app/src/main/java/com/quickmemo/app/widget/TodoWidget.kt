package com.quickmemo.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.quickmemo.app.MainActivity
import com.quickmemo.app.domain.model.TodoItem
import com.quickmemo.app.util.QuickMemoIntents
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

class TodoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val tabId = WidgetConfigStore.getTodoTabId(context, appWidgetId)
        val todoRepository = widgetEntryPoint(context).todoRepository()

        val tabNames = todoRepository.observeTabNames().first()
        val uncheckedItems = todoRepository.getUncheckedItems(tabId).first()
        val checkedItems = todoRepository.getCheckedItems(tabId).first()
        val displayedItems = (uncheckedItems + checkedItems).take(MAX_VISIBLE_ITEMS)
        val completedCount = checkedItems.size
        val totalCount = uncheckedItems.size + checkedItems.size
        val tabName = tabNames.getOrElse(tabId) { "Todo ${tabId + 1}" }

        provideContent {
            TodoWidgetContent(
                tabName = tabName,
                completedCount = completedCount,
                totalCount = totalCount,
                items = displayedItems,
                openTodoIntent = Intent(context, MainActivity::class.java).apply {
                    action = QuickMemoIntents.ACTION_OPEN_TODO
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
            )
        }
    }

    companion object {
        private const val MAX_VISIBLE_ITEMS = 6
        val ITEM_ID_KEY = ActionParameters.Key<String>("item_id")
        val ITEM_CHECKED_KEY = ActionParameters.Key<Boolean>("item_checked")
    }
}

@Composable
private fun TodoWidgetContent(
    tabName: String,
    completedCount: Int,
    totalCount: Int,
    items: List<TodoItem>,
    openTodoIntent: Intent,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetTheme.containerColor)
            .cornerRadius(WidgetTheme.cornerRadius)
            .padding(WidgetTheme.contentPadding),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.Start,
        ) {
            Text(
                text = "☑ $tabName",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(0xFF111111.toInt()),
                ),
                maxLines = 1,
            )
            Text(
                text = "   ${buildProgressBar(completedCount, totalCount)}",
                style = TextStyle(color = WidgetTheme.subtleTextColor),
                maxLines = 1,
            )
        }

        if (items.isEmpty()) {
            Text(
                text = "Todoがありません",
                modifier = GlanceModifier.padding(top = 8.dp),
                style = TextStyle(color = WidgetTheme.subtleTextColor),
            )
        } else {
            items.forEach { item ->
                Text(
                    text = "${if (item.checked) "☑" else "☐"} ${item.text.ifBlank { "(空の項目)" }}",
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clickable(
                            actionRunCallback<ToggleTodoAction>(
                                actionParametersOf(
                                    TodoWidget.ITEM_ID_KEY to item.id,
                                    TodoWidget.ITEM_CHECKED_KEY to item.checked,
                                ),
                            ),
                        ),
                    style = TextStyle(
                        color = if (item.checked) {
                            ColorProvider(0xFF777777.toInt())
                        } else {
                            ColorProvider(0xFF202020.toInt())
                        },
                    ),
                    maxLines = 1,
                )
            }
        }

        Text(
            text = "+ 全て表示 →",
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clickable(actionStartActivity(openTodoIntent)),
            style = TextStyle(
                fontWeight = FontWeight.Medium,
                color = ColorProvider(0xFF2A6BC8.toInt()),
            ),
            maxLines = 1,
        )
    }
}

class ToggleTodoAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val itemId = parameters[TodoWidget.ITEM_ID_KEY] ?: return
        val currentChecked = parameters[TodoWidget.ITEM_CHECKED_KEY] ?: false
        widgetEntryPoint(context).todoRepository().updateChecked(itemId, !currentChecked)
    }
}

private fun buildProgressBar(completedCount: Int, totalCount: Int): String {
    if (totalCount <= 0) return "□□□□□"
    val ratio = completedCount.toFloat() / totalCount.toFloat()
    val filled = (ratio * 5f).roundToInt().coerceIn(0, 5)
    return "■".repeat(filled) + "□".repeat(5 - filled)
}
