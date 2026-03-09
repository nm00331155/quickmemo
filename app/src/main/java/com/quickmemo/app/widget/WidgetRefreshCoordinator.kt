package com.quickmemo.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import com.quickmemo.app.util.EditorDebugLog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRefreshCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun refreshAll(reason: String = "manual") {
        EditorDebugLog.log(context, "Widget", "refresh request all reason=$reason")
        refreshMemoWidgets(reason)
        refreshTodoWidgets(reason)
        refreshNewMemoWidgets(reason)
    }

    suspend fun refreshMemoWidgets(reason: String = "manual") {
        refreshWidget(
            reason = reason,
            label = "memo",
            widget = MemoViewWidget(),
            receiverClass = MemoViewWidgetReceiver::class.java,
        )
    }

    suspend fun refreshTodoWidgets(reason: String = "manual") {
        refreshWidget(
            reason = reason,
            label = "todo",
            widget = TodoWidget(),
            receiverClass = TodoWidgetReceiver::class.java,
        )
    }

    suspend fun refreshNewMemoWidgets(reason: String = "manual") {
        refreshWidget(
            reason = reason,
            label = "new_memo",
            widget = NewMemoWidget(),
            receiverClass = NewMemoWidgetReceiver::class.java,
        )
    }

    private suspend fun refreshWidget(
        reason: String,
        label: String,
        widget: GlanceAppWidget,
        receiverClass: Class<*>,
    ) {
        val widgetCount = getWidgetCount(receiverClass)
        EditorDebugLog.log(
            context,
            "Widget",
            "refresh request widget=$label count=$widgetCount reason=$reason",
        )

        runCatching {
            widget.updateAll(context)
        }.onSuccess {
            EditorDebugLog.log(
                context,
                "Widget",
                "refresh executed widget=$label count=$widgetCount reason=$reason",
            )
        }.onFailure { throwable ->
            EditorDebugLog.log(
                context,
                "Widget",
                "refresh failed widget=$label count=$widgetCount reason=$reason",
                throwable,
            )
        }
    }

    private fun getWidgetCount(receiverClass: Class<*>): Int {
        val manager = AppWidgetManager.getInstance(context)
        return manager.getAppWidgetIds(ComponentName(context, receiverClass)).size
    }
}