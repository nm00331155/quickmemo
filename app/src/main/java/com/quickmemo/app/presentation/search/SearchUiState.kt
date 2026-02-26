package com.quickmemo.app.presentation.search

import com.quickmemo.app.domain.model.Memo

data class SearchUiState(
    val query: String = "",
    val results: List<Memo> = emptyList(),
)
