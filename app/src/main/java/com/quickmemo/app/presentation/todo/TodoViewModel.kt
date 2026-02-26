// File: app/src/main/java/com/quickmemo/app/presentation/todo/TodoViewModel.kt
package com.quickmemo.app.presentation.todo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickmemo.app.domain.model.TodoItem
import com.quickmemo.app.domain.repository.SettingsRepository
import com.quickmemo.app.domain.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    var selectedTab by mutableIntStateOf(0)
        private set

    var isCompletedExpanded by mutableStateOf(true)
        private set

    private val events = MutableSharedFlow<TodoEvent>()
    val uiEvents = events.asSharedFlow()

    val tabNames: StateFlow<List<String>> = todoRepository.observeTabNames()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = listOf("Todo 1", "Todo 2", "Todo 3"),
        )

    val removeAdsPurchased: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { settings -> settings.removeAdsPurchased }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    val uncheckedItems: StateFlow<List<TodoItem>> = snapshotFlow { selectedTab }
        .flatMapLatest { tabId ->
            todoRepository.getUncheckedItems(tabId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val checkedItems: StateFlow<List<TodoItem>> = snapshotFlow { selectedTab }
        .flatMapLatest { tabId ->
            todoRepository.getCheckedItems(tabId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun selectTab(index: Int) {
        selectedTab = index.coerceIn(0, 2)
    }

    fun toggleCompletedExpanded() {
        isCompletedExpanded = !isCompletedExpanded
    }

    fun addTodo(text: String) {
        viewModelScope.launch {
            todoRepository.addItem(selectedTab, text)
        }
    }

    fun updateText(id: String, text: String) {
        viewModelScope.launch {
            todoRepository.updateText(id, text)
        }
    }

    fun toggleChecked(id: String, checked: Boolean) {
        viewModelScope.launch {
            todoRepository.updateChecked(id, checked)
        }
    }

    fun updateDueDate(id: String, dueDate: Long?) {
        viewModelScope.launch {
            todoRepository.updateDueDate(id, dueDate)
        }
    }

    fun reorderUncheckedItems(orderedIds: List<String>) {
        viewModelScope.launch {
            todoRepository.reorderUncheckedItems(selectedTab, orderedIds)
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            val deleted = todoRepository.deleteItem(id) ?: return@launch
            events.emit(TodoEvent.ShowUndoDelete(deleted))
        }
    }

    fun undoDelete(item: TodoItem) {
        viewModelScope.launch {
            todoRepository.restoreItem(item)
        }
    }

    fun setTabName(tabId: Int, name: String) {
        viewModelScope.launch {
            todoRepository.setTabName(tabId, name.take(10))
        }
    }
}

sealed interface TodoEvent {
    data class ShowUndoDelete(val item: TodoItem) : TodoEvent
}
