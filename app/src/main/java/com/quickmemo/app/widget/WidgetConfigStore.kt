package com.quickmemo.app.widget

import android.content.Context

object WidgetConfigStore {
    private const val PREFS_NAME = "quickmemo_widget_config"
    private const val KEY_MEMO_ID_PREFIX = "memo_id_"
    private const val KEY_TODO_TAB_PREFIX = "todo_tab_"

    fun saveMemoId(context: Context, appWidgetId: Int, memoId: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong("$KEY_MEMO_ID_PREFIX$appWidgetId", memoId)
            .apply()
    }

    fun getMemoId(context: Context, appWidgetId: Int): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "$KEY_MEMO_ID_PREFIX$appWidgetId"
        if (!prefs.contains(key)) return null
        return prefs.getLong(key, -1L).takeIf { it > 0L }
    }

    fun saveTodoTabId(context: Context, appWidgetId: Int, tabId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt("$KEY_TODO_TAB_PREFIX$appWidgetId", tabId.coerceIn(0, 2))
            .apply()
    }

    fun getTodoTabId(context: Context, appWidgetId: Int): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("$KEY_TODO_TAB_PREFIX$appWidgetId", 0)
            .coerceIn(0, 2)
    }

    fun clear(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove("$KEY_MEMO_ID_PREFIX$appWidgetId")
            .remove("$KEY_TODO_TAB_PREFIX$appWidgetId")
            .apply()
    }
}
