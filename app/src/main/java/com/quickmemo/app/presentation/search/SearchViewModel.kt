package com.quickmemo.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickmemo.app.domain.usecase.SearchMemosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class SearchViewModel @Inject constructor(
    searchMemosUseCase: SearchMemosUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<SearchUiState> = query
        .debounce(200)
        .flatMapLatest { currentQuery ->
            searchMemosUseCase(currentQuery).map { results ->
                SearchUiState(
                    query = currentQuery,
                    results = results,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchUiState(),
        )

    fun onQueryChange(value: String) {
        query.update { value }
    }
}
