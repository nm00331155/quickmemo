package com.quickmemo.app.presentation.editor

import com.quickmemo.app.domain.model.MemoBlock
import com.quickmemo.app.domain.model.MemoToolbarSettings

data class EditorUiState(
    val memoId: Long = 0L,
    val title: String = "",
    val initialContentHtml: String = "",
    val initialContentPlainText: String = "",
    val initialBlocks: List<MemoBlock> = emptyList(),
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val colorLabel: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val showFormatBar: Boolean = true,
    val showCharacterCount: Boolean = true,
    val insertCurrentTimeWithDate: Boolean = false,
    val memoToolbarSettings: MemoToolbarSettings = MemoToolbarSettings(),
    val ttsEnabled: Boolean = false,
    val taxRate: Double = 10.0,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
)

data class MemoInfo(
    val createdAt: Long,
    val updatedAt: Long,
    val charCount: Int,
    val wordCount: Int,
)
