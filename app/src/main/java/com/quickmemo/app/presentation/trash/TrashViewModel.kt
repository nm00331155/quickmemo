package com.quickmemo.app.presentation.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickmemo.app.domain.repository.MemoRepository
import com.quickmemo.app.domain.usecase.ManageTrashUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TrashViewModel @Inject constructor(
    memoRepository: MemoRepository,
    private val manageTrashUseCase: ManageTrashUseCase,
) : ViewModel() {

    val uiState: StateFlow<TrashUiState> = memoRepository.observeTrashMemos()
        .map { TrashUiState(memos = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TrashUiState(),
        )

    fun restore(id: Long) {
        viewModelScope.launch { manageTrashUseCase.restore(id) }
    }

    fun deletePermanently(id: Long) {
        viewModelScope.launch { manageTrashUseCase.deletePermanently(id) }
    }

    fun emptyTrash() {
        viewModelScope.launch { manageTrashUseCase.emptyTrash() }
    }
}
