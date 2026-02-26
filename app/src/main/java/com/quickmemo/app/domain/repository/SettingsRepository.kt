package com.quickmemo.app.domain.repository

import com.quickmemo.app.domain.model.AppSettings
import com.quickmemo.app.domain.model.ListLayoutMode
import com.quickmemo.app.domain.model.MemoToolbarFeature
import com.quickmemo.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setListLayoutMode(mode: ListLayoutMode)
    suspend fun setDefaultMemoColor(colorLabel: Int)
    suspend fun setShowCharacterCount(enabled: Boolean)
    suspend fun setInsertCurrentTimeWithDate(enabled: Boolean)
    suspend fun setQuickInputNotificationEnabled(enabled: Boolean)
    suspend fun setTodoReminderEnabled(enabled: Boolean)
    suspend fun setTodoReminderCustomHours(hours: Int?)
    suspend fun setTodoReminderOneDay(enabled: Boolean)
    suspend fun setTodoReminderThreeDays(enabled: Boolean)
    suspend fun setTodoReminderOneWeek(enabled: Boolean)
    suspend fun setAiPolishPreview(enabled: Boolean)
    suspend fun setRequireAuthOnLaunch(enabled: Boolean)
    suspend fun setRemoveAdsPurchased(purchased: Boolean)
    suspend fun setLastBackupDateTime(value: String?)
    suspend fun setMemoToolbarFeature(feature: MemoToolbarFeature, enabled: Boolean)
}
