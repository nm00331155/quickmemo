// File: app/src/main/java/com/quickmemo/app/presentation/todo/TodoScreen.kt
package com.quickmemo.app.presentation.todo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickmemo.app.ads.AdMobBanner
import com.quickmemo.app.domain.model.TodoItem
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodoScreen(
    paddingValues: PaddingValues,
    openAddComposer: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: TodoViewModel = hiltViewModel(),
) {
    val selectedTab = viewModel.selectedTab
    val tabNames by viewModel.tabNames.collectAsStateWithLifecycle()
    val uncheckedItems by viewModel.uncheckedItems.collectAsStateWithLifecycle()
    val checkedItems by viewModel.checkedItems.collectAsStateWithLifecycle()
    val removeAdsPurchased by viewModel.removeAdsPurchased.collectAsStateWithLifecycle()

    var displayUncheckedItems by remember(selectedTab) { mutableStateOf(uncheckedItems) }
    var renameTabIndex by remember { mutableStateOf<Int?>(null) }
    var renameTabText by remember { mutableStateOf("") }
    var dueDateTarget by remember { mutableStateOf<TodoItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var isAdding by remember(selectedTab) { mutableStateOf(false) }
    var newItemText by remember(selectedTab) { mutableStateOf("") }
    val addFieldFocusRequester = remember { FocusRequester() }

    LaunchedEffect(uncheckedItems) {
        displayUncheckedItems = uncheckedItems
    }

    LaunchedEffect(isAdding) {
        if (isAdding) {
            addFieldFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(openAddComposer) {
        if (openAddComposer) {
            isAdding = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is TodoEvent.ShowUndoDelete -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Todoを削除しました",
                        actionLabel = "元に戻す",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete(event.item)
                    }
                }
            }
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (displayUncheckedItems.isEmpty()) {
            return@rememberReorderableLazyListState
        }

        val fromIndex = from.index.coerceIn(0, displayUncheckedItems.lastIndex)
        val toIndex = to.index.coerceIn(0, displayUncheckedItems.lastIndex)
        if (fromIndex == toIndex) return@rememberReorderableLazyListState

        val reordered = displayUncheckedItems.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        displayUncheckedItems = reordered
    }

    fun submitNewItem() {
        val normalized = newItemText.trim()
        if (normalized.isBlank()) return
        viewModel.addTodo(normalized)
        newItemText = ""
        isAdding = false
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            Column {
                TodoTabBar(
                    tabNames = tabNames,
                    selectedTab = selectedTab,
                    onSelectTab = viewModel::selectTab,
                    onLongPressTab = { index ->
                        renameTabIndex = index
                        renameTabText = tabNames.getOrElse(index) { "" }
                    },
                )
                if (!removeAdsPurchased) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AdMobBanner()
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            items(displayUncheckedItems, key = { it.id }) { item ->
                ReorderableItem(
                    state = reorderState,
                    key = item.id,
                ) { isDragging ->
                    val reorderableScope = this
                    TodoUncheckedRow(
                        item = item,
                        isDragging = isDragging,
                        onToggleChecked = { checked ->
                            viewModel.toggleChecked(item.id, checked)
                        },
                        onTextChange = { newText ->
                            viewModel.updateText(item.id, newText)
                        },
                        onClickDueDate = {
                            dueDateTarget = item
                        },
                        dragHandle = {
                            IconButton(
                                modifier = with(reorderableScope) {
                                    Modifier
                                        .size(20.dp)
                                        .draggableHandle(
                                            onDragStopped = {
                                                viewModel.reorderUncheckedItems(
                                                    displayUncheckedItems.map { it.id },
                                                )
                                            },
                                        )
                                },
                                onClick = {},
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "並べ替え",
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        modifier = Modifier,
                    )
                }
            }

            item(key = "new_add_row") {
                TodoAddRow(
                    text = newItemText,
                    isEditing = isAdding,
                    focusRequester = addFieldFocusRequester,
                    onStartEdit = { isAdding = true },
                    onTextChange = { newItemText = it },
                    onSubmit = { submitNewItem() },
                    modifier = Modifier,
                )
            }

            if (checkedItems.isNotEmpty()) {
                item(key = "completed_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .combinedClickable(
                                onClick = viewModel::toggleCompletedExpanded,
                                onLongClick = {},
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "── 完了 (${checkedItems.size}) ──",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = if (viewModel.isCompletedExpanded) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = null,
                        )
                    }
                }

                if (viewModel.isCompletedExpanded) {
                    items(checkedItems, key = { "checked_${it.id}" }) { item ->
                        TodoCompletedRow(
                            item = item,
                            onToggleChecked = { checked ->
                                viewModel.toggleChecked(item.id, checked)
                            },
                            onDelete = {
                                viewModel.deleteItem(item.id)
                            },
                            modifier = Modifier,
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (renameTabIndex != null) {
        AlertDialog(
            onDismissRequest = { renameTabIndex = null },
            title = { Text("タブ名を変更") },
            text = {
                BasicTextField(
                    value = renameTabText,
                    onValueChange = { renameTabText = it.take(10) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        renameTabIndex?.let { tabId ->
                            viewModel.setTabName(tabId, renameTabText)
                        }
                        renameTabIndex = null
                    },
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTabIndex = null }) {
                    Text("キャンセル")
                }
            },
        )
    }

    val target = dueDateTarget
    if (target != null) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = target.dueDate,
        )
        DatePickerDialog(
            onDismissRequest = { dueDateTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis
                        val normalizedDate = selectedDate?.let(::normalizeToLocalStartOfDay)
                        viewModel.updateDueDate(target.id, normalizedDate)
                        dueDateTarget = null
                    },
                ) {
                    Text("設定")
                }
            },
            dismissButton = {
                Row {
                    if (target.dueDate != null) {
                        TextButton(
                            onClick = {
                                viewModel.updateDueDate(target.id, null)
                                dueDateTarget = null
                            },
                        ) {
                            Text("解除")
                        }
                    }
                    TextButton(onClick = { dueDateTarget = null }) {
                        Text("キャンセル")
                    }
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TodoUncheckedRow(
    item: TodoItem,
    isDragging: Boolean,
    onToggleChecked: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onClickDueDate: () -> Unit,
    dragHandle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 12.dp)
                .alpha(if (isDragging) 0.7f else 1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Checkbox(
                    checked = item.checked,
                    onCheckedChange = onToggleChecked,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = item.text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                singleLine = true,
                maxLines = 1,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (item.text.isBlank()) {
                            Text(
                                text = "",
                                fontSize = 14.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )

            Spacer(modifier = Modifier.width(8.dp))

            DueDateAction(
                dueDate = item.dueDate,
                isCompleted = false,
                onClick = onClickDueDate,
            )

            Spacer(modifier = Modifier.width(8.dp))

            dragHandle()
        }
    }
}

@Composable
private fun TodoCompletedRow(
    item: TodoItem,
    onToggleChecked: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Checkbox(
                    checked = item.checked,
                    onCheckedChange = onToggleChecked,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = item.text,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                color = Color.Gray,
                textDecoration = TextDecoration.LineThrough,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(20.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun TodoAddRow(
    text: String,
    isEditing: Boolean,
    focusRequester: FocusRequester,
    onStartEdit: () -> Unit,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .combinedClickable(
                    onClick = onStartEdit,
                    onLongClick = {},
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "+",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (isEditing) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent {
                            if (it.type == KeyEventType.KeyDown && it.key == Key.Enter) {
                                onSubmit()
                                true
                            } else {
                                false
                            }
                        },
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onSubmit()
                        },
                    ),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (text.isBlank()) {
                                Text(
                                    text = "新しいアイテムを追加...",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            } else {
                Text(
                    text = "新しいアイテムを追加...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DueDateAction(
    dueDate: Long?,
    isCompleted: Boolean,
    onClick: () -> Unit,
) {
    val todayStart = LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val isOverdue = !isCompleted && dueDate != null && dueDate < todayStart
    val iconTint = when {
        dueDate == null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        isOverdue -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(20.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Event,
                contentDescription = "期限",
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }

        if (dueDate != null) {
            Text(
                text = dateFormatter.format(Date(dueDate)),
                fontSize = 10.sp,
                color = iconTint,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TodoTabBar(
    tabNames: List<String>,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onLongPressTab: (Int) -> Unit,
) {
    Surface(
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 4.dp),
        ) {
            repeat(3) { index ->
                val isSelected = index == selectedTab
                val style = if (isSelected) {
                    MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.titleSmall
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = { onSelectTab(index) },
                            onLongClick = { onLongPressTab(index) },
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tabNames.getOrElse(index) { "Todo ${index + 1}" },
                            style = style,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun normalizeToLocalStartOfDay(pickerUtcMillis: Long): Long {
    val localDate = Instant.ofEpochMilli(pickerUtcMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return localDate
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

private val dateFormatter = SimpleDateFormat("M/d", Locale.JAPAN)
