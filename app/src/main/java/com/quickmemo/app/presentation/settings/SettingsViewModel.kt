package com.quickmemo.app.presentation.settings

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.quickmemo.app.BuildConfig
import com.quickmemo.app.ai.AiEngineManager
import com.quickmemo.app.ai.AiEngineType
import com.quickmemo.app.ai.ModelDownloadWorker
import com.quickmemo.app.ai.ModelManager
import com.quickmemo.app.billing.BillingManager
import com.quickmemo.app.data.local.database.QuickMemoDatabase
import com.quickmemo.app.data.local.entity.MemoEntity
import com.quickmemo.app.data.local.entity.TodoItemEntity
import com.quickmemo.app.domain.model.ListLayoutMode
import com.quickmemo.app.domain.model.MemoToolbarFeature
import com.quickmemo.app.domain.model.ThemeMode
import com.quickmemo.app.domain.repository.SettingsRepository
import com.quickmemo.app.domain.repository.TodoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val billingManager: BillingManager,
    private val todoRepository: TodoRepository,
    private val database: QuickMemoDatabase,
    private val aiEngineManager: AiEngineManager,
    private val modelManager: ModelManager,
) : ViewModel() {

    private val baseSettingsFlow = combine(
        settingsRepository.settingsFlow,
        billingManager.state,
        todoRepository.observeTabNames(),
    ) { settings, billing, tabNames ->
        Triple(settings, billing, tabNames)
    }

    private val aiStateFlow = combine(
        aiEngineManager.observePreferences(),
        modelManager.status,
        modelManager.progress,
    ) { aiPreferences, modelStatus, modelProgress ->
        Triple(aiPreferences, modelStatus, modelProgress)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        baseSettingsFlow,
        aiStateFlow,
    ) { base, ai ->
        val (settings, billing, tabNames) = base
        val (aiPreferences, modelStatus, modelProgress) = ai
        SettingsUiState(
            settings = settings,
            billingState = billing,
            appVersion = BuildConfig.VERSION_NAME,
            todoTabNames = tabNames,
            aiEnginePreferences = aiPreferences,
            aiModelStatus = modelStatus,
            aiModelProgress = modelProgress,
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

    fun setAiPolishPreview(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAiPolishPreview(enabled) }
    }

    fun setMemoToolbarFeature(feature: MemoToolbarFeature, enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMemoToolbarFeature(feature, enabled) }
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
        val aiPreferences = uiState.value.aiEnginePreferences

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
            put("ai_preferences", aiPreferences.toBackupJson())
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
            val aiPreferencesObject = root.optJSONObject("ai_preferences") ?: JSONObject()

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
            applyAiPreferencesFromBackup(aiPreferencesObject)

            for (index in 0 until 3) {
                val name = tabNames.optString(index).trim()
                if (name.isNotBlank()) {
                    todoRepository.setTabName(index, name)
                }
            }
        }
    }

    private suspend fun applyAiPreferencesFromBackup(aiPreferencesObject: JSONObject) {
        if (aiPreferencesObject.length() == 0) return

        val selectedEngine = runCatching {
            AiEngineType.valueOf(
                aiPreferencesObject.optString("selected_engine", AiEngineType.QWEN3_LOCAL.name),
            )
        }.getOrDefault(AiEngineType.QWEN3_LOCAL)

        aiEngineManager.setSelectedEngine(selectedEngine)

        if (aiPreferencesObject.has("openai_api_key")) {
            aiEngineManager.setOpenAiApiKey(aiPreferencesObject.optString("openai_api_key", ""))
        }
        if (aiPreferencesObject.has("openai_model")) {
            aiEngineManager.setOpenAiModel(aiPreferencesObject.optString("openai_model", "gpt-4o-mini"))
        }
        if (aiPreferencesObject.has("custom_api_endpoint")) {
            aiEngineManager.setCustomApiEndpoint(aiPreferencesObject.optString("custom_api_endpoint", ""))
        }
        if (aiPreferencesObject.has("custom_api_key")) {
            aiEngineManager.setCustomApiKey(aiPreferencesObject.optString("custom_api_key", ""))
        }
        if (aiPreferencesObject.has("custom_api_model")) {
            aiEngineManager.setCustomApiModel(aiPreferencesObject.optString("custom_api_model", "default"))
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

    fun setSelectedAiEngine(type: AiEngineType) {
        viewModelScope.launch {
            aiEngineManager.setSelectedEngine(type)
        }
    }

    fun setOpenAiApiKey(value: String) {
        viewModelScope.launch {
            aiEngineManager.setOpenAiApiKey(value)
        }
    }

    fun setOpenAiModel(value: String) {
        viewModelScope.launch {
            aiEngineManager.setOpenAiModel(value)
        }
    }

    fun setCustomApiEndpoint(value: String) {
        viewModelScope.launch {
            aiEngineManager.setCustomApiEndpoint(value)
        }
    }

    fun setCustomApiKey(value: String) {
        viewModelScope.launch {
            aiEngineManager.setCustomApiKey(value)
        }
    }

    fun setCustomApiModel(value: String) {
        viewModelScope.launch {
            aiEngineManager.setCustomApiModel(value)
        }
    }

    fun startAiModelDownload() {
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "ai_model_download",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun deleteAiModel() {
        modelManager.deleteModel()
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
        settingsRepository.setAiPolishPreview(
            settingsObject.optBoolean("ai_polish_preview", true),
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
            MemoToolbarFeature.NUMBERED_LIST,
            settingsObject.optBoolean("memo_toolbar_numbered_list", true),
        )
        settingsRepository.setMemoToolbarFeature(
            MemoToolbarFeature.TABLE,
            settingsObject.optBoolean("memo_toolbar_table", true),
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
            MemoToolbarFeature.AI,
            settingsObject.optBoolean("memo_toolbar_ai", true),
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
        put("ai_polish_preview", aiPolishPreview)
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
        put("memo_toolbar_numbered_list", memoToolbarSettings.numberedList)
        put("memo_toolbar_table", memoToolbarSettings.table)
        put("memo_toolbar_undo_redo", memoToolbarSettings.undoRedo)
        put("memo_toolbar_datetime_insert", memoToolbarSettings.dateTimeInsert)
        put("memo_toolbar_calculator", memoToolbarSettings.calculator)
        put("memo_toolbar_ai", memoToolbarSettings.ai)
        put("memo_toolbar_translation", memoToolbarSettings.translation)
        put("memo_toolbar_ocr", memoToolbarSettings.ocr)
        put("memo_toolbar_share", memoToolbarSettings.share)
        put("memo_toolbar_full_copy", memoToolbarSettings.fullCopy)
    }
}

private fun AiEngineManager.AiEnginePreferences.toBackupJson(): JSONObject {
    return JSONObject().apply {
        put("selected_engine", selectedEngine.name)
        put("openai_api_key", openAiApiKey)
        put("openai_model", openAiModel)
        put("custom_api_endpoint", customApiEndpoint)
        put("custom_api_key", customApiKey)
        put("custom_api_model", customApiModel)
    }
}
