package com.quickmemo.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object WidgetUpdateDispatcher {
    suspend fun updateMemoWidgets(context: Context) {
        MemoViewWidget().updateAll(context)
    }

    suspend fun updateTodoWidgets(context: Context) {
        TodoWidget().updateAll(context)
    }
}
