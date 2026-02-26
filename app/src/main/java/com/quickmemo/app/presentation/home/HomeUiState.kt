package com.quickmemo.app.presentation.home

import com.quickmemo.app.domain.model.ListLayoutMode
import com.quickmemo.app.domain.model.Memo

data class HomeUiState(
    val pinnedMemos: List<Memo> = emptyList(),
    val groupedMemos: List<HomeMemoSection> = emptyList(),
    val searchQuery: String = "",
    val selectedColorFilter: Int? = null,
    val listLayoutMode: ListLayoutMode = ListLayoutMode.GRID,
    val removeAdsPurchased: Boolean = false,
)

data class HomeMemoSection(
    val title: String,
    val memos: List<Memo>,
)
