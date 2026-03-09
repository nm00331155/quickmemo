package com.quickmemo.app.widget

import android.content.Context
import com.quickmemo.app.domain.repository.MemoRepository
import com.quickmemo.app.domain.repository.TodoRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun memoRepository(): MemoRepository
    fun todoRepository(): TodoRepository
    fun widgetRefreshCoordinator(): WidgetRefreshCoordinator
}

internal fun widgetEntryPoint(context: Context): WidgetEntryPoint {
    return EntryPointAccessors.fromApplication(
        context.applicationContext,
        WidgetEntryPoint::class.java,
    )
}

internal fun widgetRefreshCoordinator(context: Context): WidgetRefreshCoordinator {
    return widgetEntryPoint(context).widgetRefreshCoordinator()
}
