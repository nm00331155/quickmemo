package com.quickmemo.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickmemo.app.domain.model.ListLayoutMode
import com.quickmemo.app.domain.model.Memo
import com.quickmemo.app.domain.repository.MemoRepository
import com.quickmemo.app.domain.repository.SettingsRepository
import com.quickmemo.app.domain.usecase.ManageTrashUseCase
import com.quickmemo.app.domain.usecase.ObserveHomeMemosUseCase
import com.quickmemo.app.domain.usecase.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeHomeMemosUseCase: ObserveHomeMemosUseCase,
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val memoRepository: MemoRepository,
    private val settingsRepository: SettingsRepository,
    private val manageTrashUseCase: ManageTrashUseCase,
) : ViewModel() {

    private val selectedColorFilter = MutableStateFlow<Int?>(null)
    private val searchQuery = MutableStateFlow("")

    private val events = MutableSharedFlow<HomeEvent>()
    val uiEvents = events.asSharedFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        selectedColorFilter,
        searchQuery,
        observeSettingsUseCase(),
        selectedColorFilter.flatMapLatest { color -> observeHomeMemosUseCase(color) },
    ) { colorFilter, query, settings, memos ->
        val filteredMemos = if (query.isBlank()) {
            memos
        } else {
            val normalized = query.trim()
            memos.filter { memo ->
                memo.title.contains(normalized, ignoreCase = true) ||
                    memo.contentPlainText.contains(normalized, ignoreCase = true)
            }
        }

        val pinned = filteredMemos.filter { it.isPinned }
        val others = filteredMemos.filterNot { it.isPinned }

        HomeUiState(
            pinnedMemos = pinned,
            groupedMemos = groupByUpdatedTime(others),
            searchQuery = query,
            selectedColorFilter = colorFilter,
            listLayoutMode = settings.listLayoutMode,
            removeAdsPurchased = settings.removeAdsPurchased,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onColorFilterSelected(color: Int?) {
        selectedColorFilter.value = color
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun toggleListLayoutMode() {
        viewModelScope.launch {
            val currentMode = uiState.value.listLayoutMode
            val nextMode = if (currentMode == ListLayoutMode.GRID) {
                ListLayoutMode.LIST
            } else {
                ListLayoutMode.GRID
            }
            settingsRepository.setListLayoutMode(nextMode)
        }
    }

    fun onTogglePin(memo: Memo) {
        viewModelScope.launch {
            memoRepository.setPinned(memo.id, !memo.isPinned)
        }
    }

    fun onMoveToTrash(memo: Memo) {
        viewModelScope.launch {
            manageTrashUseCase.moveToTrash(memo.id)
            events.emit(HomeEvent.ShowUndoTrash(memo.id))
        }
    }

    fun undoTrash(memoId: Long) {
        viewModelScope.launch {
            manageTrashUseCase.restore(memoId)
        }
    }

    private fun groupByUpdatedTime(memos: List<Memo>): List<HomeMemoSection> {
        val today = mutableListOf<Memo>()
        val yesterday = mutableListOf<Memo>()
        val threeDaysOrMore = mutableListOf<Memo>()
        val oneWeekOrMore = mutableListOf<Memo>()
        val oneMonthOrMore = mutableListOf<Memo>()
        val longAgo = mutableListOf<Memo>()

        val now = System.currentTimeMillis()
        memos.sortedByDescending { it.updatedAt }.forEach { memo ->
            val days = TimeUnit.MILLISECONDS
                .toDays((now - memo.updatedAt).coerceAtLeast(0L))

            when {
                days == 0L -> today += memo
                days == 1L -> yesterday += memo
                days < 7L -> threeDaysOrMore += memo
                days < 30L -> oneWeekOrMore += memo
                days < 180L -> oneMonthOrMore += memo
                else -> longAgo += memo
            }
        }

        return buildList {
            if (today.isNotEmpty()) add(HomeMemoSection(title = "今日", memos = today))
            if (yesterday.isNotEmpty()) add(HomeMemoSection(title = "昨日", memos = yesterday))
            if (threeDaysOrMore.isNotEmpty()) add(HomeMemoSection(title = "3日以上前", memos = threeDaysOrMore))
            if (oneWeekOrMore.isNotEmpty()) add(HomeMemoSection(title = "1週間以上前", memos = oneWeekOrMore))
            if (oneMonthOrMore.isNotEmpty()) add(HomeMemoSection(title = "1か月以上前", memos = oneMonthOrMore))
            if (longAgo.isNotEmpty()) add(HomeMemoSection(title = "かなり前", memos = longAgo))
        }
    }
}

sealed interface HomeEvent {
    data class ShowUndoTrash(val memoId: Long) : HomeEvent
}
