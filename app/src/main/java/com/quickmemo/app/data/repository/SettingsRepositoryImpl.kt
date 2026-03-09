package com.quickmemo.app.data.repository

import com.quickmemo.app.data.datastore.SettingsDataStore
import com.quickmemo.app.domain.model.AppSettings
import com.quickmemo.app.domain.model.ListLayoutMode
import com.quickmemo.app.domain.model.MemoToolbarFeature
import com.quickmemo.app.domain.model.ThemeMode
import com.quickmemo.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore,
) : SettingsRepository {
    override val settingsFlow: Flow<AppSettings> = dataStore.settingsFlow

    override suspend fun setDeepLApiKey(value: String) {
        dataStore.setDeepLApiKey(value)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.setThemeMode(mode)
    }

    override suspend fun setListLayoutMode(mode: ListLayoutMode) {
        dataStore.setListLayoutMode(mode)
    }

    override suspend fun setDefaultMemoColor(colorLabel: Int) {
        dataStore.setDefaultMemoColor(colorLabel)
    }

    override suspend fun setShowCharacterCount(enabled: Boolean) {
        dataStore.setShowCharacterCount(enabled)
    }

    override suspend fun setInsertCurrentTimeWithDate(enabled: Boolean) {
        dataStore.setInsertCurrentTimeWithDate(enabled)
    }

    override suspend fun setQuickInputNotificationEnabled(enabled: Boolean) {
        dataStore.setQuickInputNotificationEnabled(enabled)
    }

    override fun observeLockscreenGuideShown(): Flow<Boolean> {
        return dataStore.lockscreenGuideShownFlow
    }

    override suspend fun setLockscreenGuideShown() {
        dataStore.setLockscreenGuideShown()
    }

    override suspend fun setLockscreenTodoMaxItems(value: Int) {
        dataStore.setLockscreenTodoMaxItems(value)
    }

    override suspend fun setLockscreenTodoTabId(value: Int) {
        dataStore.setLockscreenTodoTabId(value)
    }

    override suspend fun setTodoReminderEnabled(enabled: Boolean) {
        dataStore.setTodoReminderEnabled(enabled)
    }

    override suspend fun setTodoReminderCustomHours(hours: Int?) {
        dataStore.setTodoReminderCustomHours(hours)
    }

    override suspend fun setTodoReminderOneDay(enabled: Boolean) {
        dataStore.setTodoReminderOneDay(enabled)
    }

    override suspend fun setTodoReminderThreeDays(enabled: Boolean) {
        dataStore.setTodoReminderThreeDays(enabled)
    }

    override suspend fun setTodoReminderOneWeek(enabled: Boolean) {
        dataStore.setTodoReminderOneWeek(enabled)
    }

    override suspend fun setRequireAuthOnLaunch(enabled: Boolean) {
        dataStore.setRequireAuthOnLaunch(enabled)
    }

    override suspend fun setRemoveAdsPurchased(purchased: Boolean) {
        dataStore.setRemoveAdsPurchased(purchased)
    }

    override suspend fun setLastBackupDateTime(value: String?) {
        dataStore.setLastBackupDateTime(value)
    }

    override suspend fun setAppBackupEnabled(enabled: Boolean) {
        dataStore.setAppBackupEnabled(enabled)
    }

    override suspend fun setAppBackupTime(hour: Int, minute: Int) {
        dataStore.setAppBackupTime(hour, minute)
    }

    override suspend fun setAppBackupMaxGenerations(value: Int) {
        dataStore.setAppBackupMaxGenerations(value)
    }

    override suspend fun setTtsEnabled(enabled: Boolean) {
        dataStore.setTtsEnabled(enabled)
    }

    override suspend fun setMemoToolbarFeature(feature: MemoToolbarFeature, enabled: Boolean) {
        dataStore.setMemoToolbarFeature(feature, enabled)
    }

    override fun observeTaxRate(): Flow<Double> {
        return dataStore.taxRateFlow
    }

    override suspend fun setTaxRate(rate: Double) {
        dataStore.setTaxRate(rate)
    }
}
