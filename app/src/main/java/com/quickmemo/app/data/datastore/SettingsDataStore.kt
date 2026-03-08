// File: app/src/main/java/com/quickmemo/app/data/datastore/SettingsDataStore.kt
package com.quickmemo.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.quickmemo.app.domain.model.AppSettings
import com.quickmemo.app.domain.model.ListLayoutMode
import com.quickmemo.app.domain.model.MemoToolbarFeature
import com.quickmemo.app.domain.model.MemoToolbarSettings
import com.quickmemo.app.domain.model.ReminderSettings
import com.quickmemo.app.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

const val QUICKMEMO_SETTINGS_DATASTORE_NAME = "quickmemo_settings"

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = QUICKMEMO_SETTINGS_DATASTORE_NAME)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LIST_LAYOUT_MODE = stringPreferencesKey("list_layout_mode")

        val DEFAULT_MEMO_COLOR = intPreferencesKey("default_memo_color")

        val SHOW_CHAR_COUNT = booleanPreferencesKey("show_char_count")
        val LEGACY_SHOW_CHAR_COUNT = booleanPreferencesKey("show_character_count")

        val DATE_INCLUDE_TIME = booleanPreferencesKey("date_include_time")
        val LEGACY_DATE_INCLUDE_TIME = booleanPreferencesKey("insert_current_time_with_date")

        val NOTIFICATION_QUICK_INPUT = booleanPreferencesKey("notification_quick_input")
        val LEGACY_NOTIFICATION_QUICK_INPUT = booleanPreferencesKey("quick_input_notification")
        val LOCKSCREEN_GUIDE_SHOWN = booleanPreferencesKey("lockscreen_guide_shown")

        val TODO_REMINDER_ENABLED = booleanPreferencesKey("todo_reminder_enabled")
        val TODO_REMINDER_CUSTOM_HOURS = intPreferencesKey("todo_reminder_custom_hours")
        val TODO_REMINDER_1DAY = booleanPreferencesKey("todo_reminder_1day")
        val TODO_REMINDER_3DAYS = booleanPreferencesKey("todo_reminder_3days")
        val TODO_REMINDER_1WEEK = booleanPreferencesKey("todo_reminder_1week")

        val REQUIRE_AUTH_ON_LAUNCH = booleanPreferencesKey("require_auth_on_launch")
        val REMOVE_ADS_PURCHASED = booleanPreferencesKey("remove_ads_purchased")
        val LAST_BACKUP_DATETIME = stringPreferencesKey("last_backup_datetime")
        val APP_BACKUP_ENABLED = booleanPreferencesKey("app_backup_enabled")
        val APP_BACKUP_HOUR = intPreferencesKey("app_backup_hour")
        val APP_BACKUP_MINUTE = intPreferencesKey("app_backup_minute")
        val APP_BACKUP_MAX_GENERATIONS = intPreferencesKey("app_backup_max_gen")

        val MEMO_TOOLBAR_BOLD = booleanPreferencesKey("memo_toolbar_bold")
        val MEMO_TOOLBAR_TEXT_COLOR = booleanPreferencesKey("memo_toolbar_text_color")
        val MEMO_TOOLBAR_HIGHLIGHTER = booleanPreferencesKey("memo_toolbar_highlighter")
        val MEMO_TOOLBAR_TEXT_SIZE = booleanPreferencesKey("memo_toolbar_text_size")
        val MEMO_TOOLBAR_UNDO_REDO = booleanPreferencesKey("memo_toolbar_undo_redo")
        val MEMO_TOOLBAR_DATETIME_INSERT = booleanPreferencesKey("memo_toolbar_datetime_insert")
        val MEMO_TOOLBAR_CALCULATOR = booleanPreferencesKey("memo_toolbar_calculator")
        val MEMO_TOOLBAR_TRANSLATION = booleanPreferencesKey("memo_toolbar_translation")
        val MEMO_TOOLBAR_OCR = booleanPreferencesKey("memo_toolbar_ocr")
        val MEMO_TOOLBAR_SHARE = booleanPreferencesKey("memo_toolbar_share")
        val MEMO_TOOLBAR_FULL_COPY = booleanPreferencesKey("memo_toolbar_full_copy")

        val CALCULATOR_TAX_RATE = doublePreferencesKey("calculator_tax_rate")
    }

    val taxRateFlow: Flow<Double> = context.settingsDataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            (preferences[Keys.CALCULATOR_TAX_RATE] ?: 10.0).coerceIn(0.0, 100.0)
        }

    val lockscreenGuideShownFlow: Flow<Boolean> = context.settingsDataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            preferences[Keys.LOCKSCREEN_GUIDE_SHOWN] ?: false
        }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            val themeMode = parseThemeMode(preferences[Keys.THEME_MODE])
            val layoutMode = runCatching {
                ListLayoutMode.valueOf(preferences[Keys.LIST_LAYOUT_MODE] ?: ListLayoutMode.GRID.name)
            }.getOrDefault(ListLayoutMode.GRID)

            val showCharCount = preferences[Keys.SHOW_CHAR_COUNT]
                ?: preferences[Keys.LEGACY_SHOW_CHAR_COUNT]
                ?: true

            val includeTime = preferences[Keys.DATE_INCLUDE_TIME]
                ?: preferences[Keys.LEGACY_DATE_INCLUDE_TIME]
                ?: false

            val quickInput = preferences[Keys.NOTIFICATION_QUICK_INPUT]
                ?: preferences[Keys.LEGACY_NOTIFICATION_QUICK_INPUT]
                ?: true

            val reminderSettings = ReminderSettings(
                customHoursBefore = preferences[Keys.TODO_REMINDER_CUSTOM_HOURS],
                oneDayBefore = preferences[Keys.TODO_REMINDER_1DAY] ?: true,
                threeDaysBefore = preferences[Keys.TODO_REMINDER_3DAYS] ?: false,
                oneWeekBefore = preferences[Keys.TODO_REMINDER_1WEEK] ?: false,
            )

            val toolbarSettings = MemoToolbarSettings(
                boldItalic = preferences[Keys.MEMO_TOOLBAR_BOLD] ?: true,
                textColor = preferences[Keys.MEMO_TOOLBAR_TEXT_COLOR] ?: true,
                highlighter = preferences[Keys.MEMO_TOOLBAR_HIGHLIGHTER] ?: true,
                textSize = preferences[Keys.MEMO_TOOLBAR_TEXT_SIZE] ?: true,
                undoRedo = preferences[Keys.MEMO_TOOLBAR_UNDO_REDO] ?: true,
                dateTimeInsert = preferences[Keys.MEMO_TOOLBAR_DATETIME_INSERT] ?: true,
                calculator = preferences[Keys.MEMO_TOOLBAR_CALCULATOR] ?: true,
                translation = preferences[Keys.MEMO_TOOLBAR_TRANSLATION] ?: true,
                ocr = preferences[Keys.MEMO_TOOLBAR_OCR] ?: true,
                share = preferences[Keys.MEMO_TOOLBAR_SHARE] ?: true,
                fullCopy = preferences[Keys.MEMO_TOOLBAR_FULL_COPY] ?: true,
            )

            val appBackupHour = (preferences[Keys.APP_BACKUP_HOUR] ?: 0).coerceIn(0, 23)
            val appBackupMinute = (preferences[Keys.APP_BACKUP_MINUTE] ?: 0).coerceIn(0, 59)
            val appBackupMaxGenerations = (preferences[Keys.APP_BACKUP_MAX_GENERATIONS] ?: 10)
                .coerceIn(1, 10)

            AppSettings(
                themeMode = themeMode,
                listLayoutMode = layoutMode,
                defaultMemoColor = preferences[Keys.DEFAULT_MEMO_COLOR] ?: 0,
                showCharacterCount = showCharCount,
                insertCurrentTimeWithDate = includeTime,
                quickInputNotificationEnabled = quickInput,
                lockscreenGuideShown = preferences[Keys.LOCKSCREEN_GUIDE_SHOWN] ?: false,
                todoReminderEnabled = preferences[Keys.TODO_REMINDER_ENABLED] ?: true,
                reminderSettings = reminderSettings,
                requireAuthOnLaunch = preferences[Keys.REQUIRE_AUTH_ON_LAUNCH] ?: false,
                removeAdsPurchased = preferences[Keys.REMOVE_ADS_PURCHASED] ?: false,
                lastBackupDateTime = preferences[Keys.LAST_BACKUP_DATETIME],
                appBackupEnabled = preferences[Keys.APP_BACKUP_ENABLED] ?: true,
                appBackupHour = appBackupHour,
                appBackupMinute = appBackupMinute,
                appBackupMaxGenerations = appBackupMaxGenerations,
                memoToolbarSettings = toolbarSettings,
                calculatorTaxRate = (preferences[Keys.CALCULATOR_TAX_RATE] ?: 10.0).coerceIn(0.0, 100.0),
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name.lowercase() }
    }

    suspend fun setListLayoutMode(mode: ListLayoutMode) {
        context.settingsDataStore.edit { it[Keys.LIST_LAYOUT_MODE] = mode.name }
    }

    suspend fun setDefaultMemoColor(colorLabel: Int) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_MEMO_COLOR] = colorLabel }
    }

    suspend fun setShowCharacterCount(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SHOW_CHAR_COUNT] = enabled }
    }

    suspend fun setInsertCurrentTimeWithDate(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DATE_INCLUDE_TIME] = enabled }
    }

    suspend fun setQuickInputNotificationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.NOTIFICATION_QUICK_INPUT] = enabled }
    }

    suspend fun setLockscreenGuideShown() {
        context.settingsDataStore.edit { it[Keys.LOCKSCREEN_GUIDE_SHOWN] = true }
    }

    suspend fun setTodoReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.TODO_REMINDER_ENABLED] = enabled }
    }

    suspend fun setTodoReminderCustomHours(hours: Int?) {
        context.settingsDataStore.edit { preferences ->
            if (hours == null) {
                preferences.remove(Keys.TODO_REMINDER_CUSTOM_HOURS)
            } else {
                preferences[Keys.TODO_REMINDER_CUSTOM_HOURS] = hours
            }
        }
    }

    suspend fun setTodoReminderOneDay(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.TODO_REMINDER_1DAY] = enabled }
    }

    suspend fun setTodoReminderThreeDays(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.TODO_REMINDER_3DAYS] = enabled }
    }

    suspend fun setTodoReminderOneWeek(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.TODO_REMINDER_1WEEK] = enabled }
    }

    suspend fun setRequireAuthOnLaunch(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.REQUIRE_AUTH_ON_LAUNCH] = enabled }
    }

    suspend fun setRemoveAdsPurchased(purchased: Boolean) {
        context.settingsDataStore.edit { it[Keys.REMOVE_ADS_PURCHASED] = purchased }
    }

    suspend fun setLastBackupDateTime(value: String?) {
        context.settingsDataStore.edit { preferences ->
            if (value.isNullOrBlank()) {
                preferences.remove(Keys.LAST_BACKUP_DATETIME)
            } else {
                preferences[Keys.LAST_BACKUP_DATETIME] = value
            }
        }
    }

    suspend fun setAppBackupEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.APP_BACKUP_ENABLED] = enabled }
    }

    suspend fun setAppBackupTime(hour: Int, minute: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.APP_BACKUP_HOUR] = hour.coerceIn(0, 23)
            preferences[Keys.APP_BACKUP_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setAppBackupMaxGenerations(value: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.APP_BACKUP_MAX_GENERATIONS] = value.coerceIn(1, 10)
        }
    }

    suspend fun setMemoToolbarFeature(feature: MemoToolbarFeature, enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            when (feature) {
                MemoToolbarFeature.BOLD_ITALIC -> preferences[Keys.MEMO_TOOLBAR_BOLD] = enabled
                MemoToolbarFeature.TEXT_COLOR -> preferences[Keys.MEMO_TOOLBAR_TEXT_COLOR] = enabled
                MemoToolbarFeature.HIGHLIGHTER -> preferences[Keys.MEMO_TOOLBAR_HIGHLIGHTER] = enabled
                MemoToolbarFeature.TEXT_SIZE -> preferences[Keys.MEMO_TOOLBAR_TEXT_SIZE] = enabled
                MemoToolbarFeature.UNDO_REDO -> preferences[Keys.MEMO_TOOLBAR_UNDO_REDO] = enabled
                MemoToolbarFeature.DATETIME_INSERT -> preferences[Keys.MEMO_TOOLBAR_DATETIME_INSERT] = enabled
                MemoToolbarFeature.CALCULATOR -> preferences[Keys.MEMO_TOOLBAR_CALCULATOR] = enabled
                MemoToolbarFeature.TRANSLATION -> preferences[Keys.MEMO_TOOLBAR_TRANSLATION] = enabled
                MemoToolbarFeature.OCR -> preferences[Keys.MEMO_TOOLBAR_OCR] = enabled
                MemoToolbarFeature.SHARE -> preferences[Keys.MEMO_TOOLBAR_SHARE] = enabled
                MemoToolbarFeature.FULL_COPY -> preferences[Keys.MEMO_TOOLBAR_FULL_COPY] = enabled
            }
        }
    }

    suspend fun setTaxRate(rate: Double) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.CALCULATOR_TAX_RATE] = rate.coerceIn(0.0, 100.0)
        }
    }

    private fun parseThemeMode(raw: String?): ThemeMode {
        return when (raw?.lowercase()) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            "system" -> ThemeMode.SYSTEM
            else -> runCatching { ThemeMode.valueOf(raw ?: ThemeMode.SYSTEM.name) }
                .getOrDefault(ThemeMode.SYSTEM)
        }
    }
}
