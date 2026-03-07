package com.quickmemo.app.presentation.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickmemo.app.BuildConfig
import com.quickmemo.app.billing.BillingManager
import com.quickmemo.app.data.local.database.QuickMemoDatabase
import com.quickmemo.app.data.local.entity.MemoEntity
import com.quickmemo.app.data.local.entity.TodoItemEntity
import com.quickmemo.app.domain.model.ListLayoutMode
import com.quickmemo.app.domain.model.MemoToolbarFeature
import com.quickmemo.app.domain.model.ThemeMode
import com.quickmemo.app.domain.repository.SettingsRepository
import com.quickmemo.app.domain.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val billingManager: BillingManager,
    private val todoRepository: TodoRepository,
    private val database: QuickMemoDatabase,
) : ViewModel() {

    private val baseSettingsFlow = combine(
        settingsRepository.settingsFlow,
        billingManager.state,
        todoRepository.observeTabNames(),
    ) { settings, billing, tabNames ->
        Triple(settings, billing, tabNames)
    }

    val uiState: StateFlow<SettingsUiState> = baseSettingsFlow.map { (settings, billing, tabNames) ->
        SettingsUiState(
            settings = settings,
            billingState = billing,
            appVersion = BuildConfig.VERSION_NAME,
            todoTabNames = tabNames,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setListLayoutMode(mode: ListLayoutMode) {
        viewModelScope.launch { settingsRepository.setListLayoutMode(mode) }
    }

    fun setDefaultMemoColor(colorLabel: Int) {
        viewModelScope.launch { settingsRepository.setDefaultMemoColor(colorLabel) }
    }

    fun setShowCharacterCount(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowCharacterCount(enabled) }
    }

    fun setInsertCurrentTimeWithDate(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setInsertCurrentTimeWithDate(enabled) }
    }

    fun setQuickInputNotification(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setQuickInputNotificationEnabled(enabled) }
    }

    fun setTodoReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTodoReminderEnabled(enabled) }
    }

    fun setTodoReminderCustomHours(hours: Int?) {
        viewModelScope.launch { settingsRepository.setTodoReminderCustomHours(hours) }
    }

    fun setTodoReminderOneDay(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTodoReminderOneDay(enabled) }
    }

    fun setTodoReminderThreeDays(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTodoReminderThreeDays(enabled) }
    }

    fun setTodoReminderOneWeek(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTodoReminderOneWeek(enabled) }
    }

    fun setRequireAuthOnLaunch(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRequireAuthOnLaunch(enabled) }
    }

    fun setMemoToolbarFeature(feature: MemoToolbarFeature, enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMemoToolbarFeature(feature, enabled) }
    }

    fun setTaxRate(rate: Double) {
        viewModelScope.launch { settingsRepository.setTaxRate(rate) }
    }

    fun setTodoTabName(tabId: Int, name: String) {
        viewModelScope.launch {
            todoRepository.setTabName(tabId, name.take(10))
        }
    }

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now()
        val memos = database.memoDao().getAllForBackup()
        val todos = database.todoDao().getAllForBackup()
        val settings = uiState.value.settings
        val tabNames = uiState.value.todoTabNames

        val json = JSONObject().apply {
            put("version", 1)
            put("created_at", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            put(
                "memos",
                JSONArray().apply {
                    memos.forEach { memo ->
                        put(memo.toBackupJson())
                    }
                },
            )
            put(
                "todo_items",
                JSONArray().apply {
                    todos.forEach { todo ->
                        put(todo.toBackupJson())
                    }
                },
            )
            put("todo_tab_names", JSONArray(tabNames))
            put("settings", settings.toBackupJson())
        }

        json.toString(2)
    }

    suspend fun restoreFromBackupJson(rawJson: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(rawJson)

            val memoArray = root.optJSONArray("memos") ?: JSONArray()
            val todoArray = root.optJSONArray("todo_items") ?: JSONArray()
            val tabNames = root.optJSONArray("todo_tab_names") ?: JSONArray()
            val settingsObject = root.optJSONObject("settings") ?: JSONObject()

            val restoredMemos = mutableListOf<MemoEntity>()
            for (i in 0 until memoArray.length()) {
                memoArray.optJSONObject(i)?.let { restoredMemos += it.toMemoEntity() }
            }

            val restoredTodos = mutableListOf<TodoItemEntity>()
            for (i in 0 until todoArray.length()) {
                todoArray.optJSONObject(i)?.let { restoredTodos += it.toTodoEntity() }
            }

            database.memoDao().deleteAllForBackup()
            database.todoDao().deleteAllForBackup()
            if (restoredMemos.isNotEmpty()) {
                database.memoDao().insertAllForBackup(restoredMemos)
            }
            if (restoredTodos.isNotEmpty()) {
                database.todoDao().insertAllForBackup(restoredTodos)
            }

            applySettingsFromBackup(settingsObject)

            for (index in 0 until 3) {
                val name = tabNames.optString(index).trim()
                if (name.isNotBlank()) {
                    todoRepository.setTabName(index, name)
                }
            }
        }
    }

    fun setLastBackupDateTime(value: String?) {
        viewModelScope.launch {
            settingsRepository.setLastBackupDateTime(value)
        }
    }

    fun setLastBackupDateTimeNow() {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
        setLastBackupDateTime(now)
    }

    fun purchaseRemoveAds(activity: Activity) {
        billingManager.launchPurchase(activity)
    }

    fun refreshPurchases() {
        billingManager.queryPurchases()
    }

    private suspend fun applySettingsFromBackup(settingsObject: JSONObject) {
        val themeRaw = settingsObject.optString("theme_mode", "system")
        val themeMode = when (themeRaw.lowercase()) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
        settingsRepository.setThemeMode(themeMode)

        settingsRepository.setQuickInputNotificationEnabled(
            settingsObject.optBoolean("notification_quick_input", true),
        )
        settingsRepository.setRequireAuthOnLaunch(
            settingsObject.optBoolean("require_auth_on_launch", false),
        )
        settingsRepository.setDefaultMemoColor(
            settingsObject.optInt("default_memo_color", 0),
        )
        settingsRepository.setShowCharacterCount(
            settingsObject.optBoolean("show_char_count", true),
        )
        settingsRepository.setInsertCurrentTimeWithDate(
            settingsObject.optBoolean("date_include_time", false),
        )

        settingsRepository.setTodoReminderEnabled(
            settingsObject.optBoolean("todo_reminder_enabled", true),
        )
        val customHours = if (settingsObject.has("todo_reminder_custom_hours")) {
            settingsObject.optInt("todo_reminder_custom_hours", 2)
        } else {
            null
        }
        settingsRepository.setTodoReminderCustomHours(customHours)
        settingsRepository.setTodoReminderOneDay(
            settingsObject.optBoolean("todo_reminder_1day", true),
        )
        settingsRepository.setTodoReminderThreeDays(
            settingsObject.optBoolean("todo_reminder_3days", false),
        )
        settingsRepository.setTodoReminderOneWeek(
            settingsObject.optBoolean("todo_reminder_1week", false),
        )

        settingsRepository.setMemoToolbarFeature(
            MemoToolbarFeature.BOLD_ITALIC,
            settingsObject.optBoolean("memo_toolbar_bold", true),
        )
        settingsRepository.setMemoToolbarFeature(
            MemoToolbarFeature.TEXT_COLOR,
            settingsObject.optBoolean("memo_toolbar_text_color", true),
        )
        settingsRepository.setMemoToolbarFeature(
            MemoToolbarFeature.HIGHLIGHTER,
            settingsObject.optBoolean("memo_toolbar_highlighter", true),
        )
        settingsRepository.setMemoToolbarFeature(
            MemoToolbarFeature.TEXT_SIZE,
            settingsObject.optBoolean("memo_toolbar_text_size", true),
        )
        settingsRepository.setMemoToolbarFeature(
            MemoToolbarFeature.UNDO_REDO,
            settingsObject.optBoolean("memo_toolbar_undo_redo", true),
        )
        settingsRepository.setMemoToolbarFeature(
            MemoToolbarFeature.DATETIME_INSERT,
            settingsObject.optBoolean("memo_toolbar_datetime_insert", true),
        )
        settingsRepository.setMemoToolbarFeature(
            MemoToolbarFeature.CALCULATOR,
            settingsObject.optBoolean("memo_toolbar_calculator", true),
        )
        settingsRepository.setMemoToolbarFeature(
            MemoToolbarFeature.TRANSLATION,
            settingsObject.optBoolean("memo_toolbar_translation", true),
        )
        settingsRepository.setMemoToolbarFeature(
            MemoToolbarFeature.OCR,
            settingsObject.optBoolean("memo_toolbar_ocr", true),
        )
        settingsRepository.setMemoToolbarFeature(
            MemoToolbarFeature.SHARE,
            settingsObject.optBoolean("memo_toolbar_share", true),
        )
        settingsRepository.setMemoToolbarFeature(
            MemoToolbarFeature.FULL_COPY,
            settingsObject.optBoolean("memo_toolbar_full_copy", true),
        )

        settingsRepository.setTaxRate(
            settingsObject.optDouble("calculator_tax_rate", 10.0),
        )
    }
}

private fun MemoEntity.toBackupJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("title", title)
        put("contentHtml", contentHtml)
        put("contentPlainText", contentPlainText)
        put("blocks", blocks)
        put("colorLabel", colorLabel)
        put("isPinned", isPinned)
        put("isLocked", isLocked)
        put("isChecklist", isChecklist)
        put("isDeleted", isDeleted)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        if (deletedAt == null) {
            put("deletedAt", JSONObject.NULL)
        } else {
            put("deletedAt", deletedAt)
        }
    }
}

private fun JSONObject.toMemoEntity(): MemoEntity {
    return MemoEntity(
        id = optLong("id", 0L),
        title = optString("title", ""),
        contentHtml = optString("contentHtml", ""),
        contentPlainText = optString("contentPlainText", ""),
        blocks = optString("blocks", "[]"),
        colorLabel = optInt("colorLabel", 0),
        isPinned = optBoolean("isPinned", false),
        isLocked = optBoolean("isLocked", false),
        isChecklist = optBoolean("isChecklist", false),
        isDeleted = optBoolean("isDeleted", false),
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        updatedAt = optLong("updatedAt", System.currentTimeMillis()),
        deletedAt = if (isNull("deletedAt")) null else optLong("deletedAt"),
    )
}

private fun TodoItemEntity.toBackupJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("tabId", tabId)
        put("text", text)
        put("checked", checked)
        if (dueDate == null) {
            put("dueDate", JSONObject.NULL)
        } else {
            put("dueDate", dueDate)
        }
        put("sortOrder", sortOrder)
        put("createdAt", createdAt)
        if (checkedAt == null) {
            put("checkedAt", JSONObject.NULL)
        } else {
            put("checkedAt", checkedAt)
        }
    }
}

private fun JSONObject.toTodoEntity(): TodoItemEntity {
    return TodoItemEntity(
        id = optString("id"),
        tabId = optInt("tabId", 0),
        text = optString("text"),
        checked = optBoolean("checked", false),
        dueDate = if (isNull("dueDate")) null else optLong("dueDate"),
        sortOrder = optInt("sortOrder", 0),
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        checkedAt = if (isNull("checkedAt")) null else optLong("checkedAt"),
    )
}

private fun com.quickmemo.app.domain.model.AppSettings.toBackupJson(): JSONObject {
    return JSONObject().apply {
        put("theme_mode", when (themeMode) {
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK -> "dark"
            ThemeMode.SYSTEM -> "system"
        })
        put("notification_quick_input", quickInputNotificationEnabled)
        put("require_auth_on_launch", requireAuthOnLaunch)
        put("default_memo_color", defaultMemoColor)
        put("show_char_count", showCharacterCount)
        put("date_include_time", insertCurrentTimeWithDate)
        put("todo_reminder_enabled", todoReminderEnabled)
        if (reminderSettings.customHoursBefore == null) {
            put("todo_reminder_custom_hours", JSONObject.NULL)
        } else {
            put("todo_reminder_custom_hours", reminderSettings.customHoursBefore)
        }
        put("todo_reminder_1day", reminderSettings.oneDayBefore)
        put("todo_reminder_3days", reminderSettings.threeDaysBefore)
        put("todo_reminder_1week", reminderSettings.oneWeekBefore)

        put("memo_toolbar_bold", memoToolbarSettings.boldItalic)
        put("memo_toolbar_text_color", memoToolbarSettings.textColor)
        put("memo_toolbar_highlighter", memoToolbarSettings.highlighter)
        put("memo_toolbar_text_size", memoToolbarSettings.textSize)
        put("memo_toolbar_undo_redo", memoToolbarSettings.undoRedo)
        put("memo_toolbar_datetime_insert", memoToolbarSettings.dateTimeInsert)
        put("memo_toolbar_calculator", memoToolbarSettings.calculator)
        put("memo_toolbar_translation", memoToolbarSettings.translation)
        put("memo_toolbar_ocr", memoToolbarSettings.ocr)
        put("memo_toolbar_share", memoToolbarSettings.share)
        put("memo_toolbar_full_copy", memoToolbarSettings.fullCopy)
        put("calculator_tax_rate", calculatorTaxRate)
    }
}
