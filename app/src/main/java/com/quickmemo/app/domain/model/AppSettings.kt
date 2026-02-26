package com.quickmemo.app.domain.model

data class ReminderSettings(
    val customHoursBefore: Int? = null,
    val oneDayBefore: Boolean = true,
    val threeDaysBefore: Boolean = false,
    val oneWeekBefore: Boolean = false,
)

data class MemoToolbarSettings(
    val boldItalic: Boolean = true,
    val textColor: Boolean = true,
    val highlighter: Boolean = true,
    val textSize: Boolean = true,
    val numberedList: Boolean = true,
    val table: Boolean = true,
    val undoRedo: Boolean = true,
    val dateTimeInsert: Boolean = true,
    val calculator: Boolean = true,
    val ai: Boolean = true,
    val translation: Boolean = true,
    val ocr: Boolean = true,
    val share: Boolean = true,
    val fullCopy: Boolean = true,
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val listLayoutMode: ListLayoutMode = ListLayoutMode.GRID,
    val defaultMemoColor: Int = 0,
    val showCharacterCount: Boolean = true,
    val insertCurrentTimeWithDate: Boolean = false,
    val quickInputNotificationEnabled: Boolean = true,
    val todoReminderEnabled: Boolean = true,
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val aiPolishPreview: Boolean = true,
    val requireAuthOnLaunch: Boolean = false,
    val removeAdsPurchased: Boolean = false,
    val lastBackupDateTime: String? = null,
    val memoToolbarSettings: MemoToolbarSettings = MemoToolbarSettings(),
)
