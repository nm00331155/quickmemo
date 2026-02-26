package com.quickmemo.app.presentation.trash

import com.quickmemo.app.domain.model.Memo

data class TrashUiState(
    val memos: List<Memo> = emptyList(),
)
