package com.quickmemo.app.domain.model

data class Memo(
    val id: Long = 0,
    val title: String = "",
    val contentHtml: String = "",
    val contentPlainText: String = "",
    val blocks: List<MemoBlock> = emptyList(),
    val colorLabel: Int = 0,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val isChecklist: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
) {
    val displayTitle: String
        get() = title.ifBlank {
            contentPlainText
                .lineSequence()
                .firstOrNull()
                .orEmpty()
                .ifBlank { "(無題)" }
        }
}
