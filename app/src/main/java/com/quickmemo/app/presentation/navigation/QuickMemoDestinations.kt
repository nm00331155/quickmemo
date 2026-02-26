package com.quickmemo.app.presentation.navigation

import android.net.Uri

object QuickMemoDestinations {
    const val HOME = "home"
    const val TODO = "todo"
    const val SEARCH = "search"
    const val SETTINGS = "settings?startTab={startTab}"
    const val TRASH = "trash"
    const val PREMIUM = "premium"

    const val EDITOR =
        "editor?memoId={memoId}&prefillText={prefillText}&prefillChecklist={prefillChecklist}&insertToday={insertToday}&colorLabel={colorLabel}"

    fun editorRoute(
        memoId: Long = 0L,
        prefillText: String = "",
        prefillChecklist: Boolean = false,
        insertToday: Boolean = false,
        colorLabel: Int = 0,
    ): String {
        return "editor?memoId=$memoId" +
            "&prefillText=${Uri.encode(prefillText)}" +
            "&prefillChecklist=$prefillChecklist" +
            "&insertToday=$insertToday" +
            "&colorLabel=$colorLabel"
    }

    fun settingsRoute(startTab: Int = 0): String {
        val normalized = startTab.coerceIn(0, 2)
        return "settings?startTab=$normalized"
    }
}
