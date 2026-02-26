package com.quickmemo.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickmemo.app.R
import com.quickmemo.app.ads.AdMobBanner
import com.quickmemo.app.domain.model.ListLayoutMode
import com.quickmemo.app.domain.model.Memo
import com.quickmemo.app.presentation.components.ColorFilterRow
import com.quickmemo.app.presentation.components.MemoCard
import com.quickmemo.app.presentation.components.QuickMemoSearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenEditor: (Long?, Int?) -> Unit,
    onOpenSettings: () -> Unit,
    onRequestUnlockMemo: (Memo, (Boolean) -> Unit) -> Unit,
    onOpenTodo: () -> Unit,
    fabExtraBottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val moveToTrashMessage = context.getString(R.string.move_to_trash)
    val undoLabel = context.getString(R.string.undo)

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is HomeEvent.ShowUndoTrash -> {
                    val result = snackbarHostState.showSnackbar(
                        message = moveToTrashMessage,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoTrash(event.memoId)
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.home_title))
                },
                actions = {
                    IconButton(onClick = viewModel::toggleListLayoutMode) {
                        Icon(
                            imageVector = if (uiState.listLayoutMode == ListLayoutMode.GRID) {
                                Icons.Default.ViewList
                            } else {
                                Icons.Default.ViewModule
                            },
                            contentDescription = "list_layout",
                        )
                    }

                    IconButton(onClick = onOpenTodo) {
                        Icon(
                            imageVector = Icons.Outlined.CheckBox,
                            contentDescription = "todo",
                        )
                    }

                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(id = R.string.settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenEditor(null, uiState.selectedColorFilter) },
                modifier = Modifier.padding(bottom = fabExtraBottomPadding),
            ) {
                Text(text = "+")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (!uiState.removeAdsPurchased) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AdMobBanner()
                }
            }
        },
    ) { innerPadding ->
        when (uiState.listLayoutMode) {
            ListLayoutMode.LIST -> {
                HomeListContent(
                    uiState = uiState,
                    onMemoClick = { memo ->
                        if (memo.isLocked) {
                            onRequestUnlockMemo(memo) { unlocked ->
                                if (unlocked) onOpenEditor(memo.id, null)
                            }
                        } else {
                            onOpenEditor(memo.id, null)
                        }
                    },
                    onTogglePin = viewModel::onTogglePin,
                    onMoveToTrash = viewModel::onMoveToTrash,
                    onColorSelected = viewModel::onColorFilterSelected,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            ListLayoutMode.GRID -> {
                HomeGridContent(
                    uiState = uiState,
                    onMemoClick = { memo ->
                        if (memo.isLocked) {
                            onRequestUnlockMemo(memo) { unlocked ->
                                if (unlocked) onOpenEditor(memo.id, null)
                            }
                        } else {
                            onOpenEditor(memo.id, null)
                        }
                    },
                    onTogglePin = viewModel::onTogglePin,
                    onMoveToTrash = viewModel::onMoveToTrash,
                    onColorSelected = viewModel::onColorFilterSelected,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun HomeListContent(
    uiState: HomeUiState,
    onMemoClick: (Memo) -> Unit,
    onTogglePin: (Memo) -> Unit,
    onMoveToTrash: (Memo) -> Unit,
    onColorSelected: (Int?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            QuickMemoSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
            )
        }
        item {
            ColorFilterRow(
                selectedColor = uiState.selectedColorFilter,
                onColorSelected = onColorSelected,
            )
        }

        if (uiState.pinnedMemos.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(id = R.string.pinned),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            items(uiState.pinnedMemos, key = { it.id }) { memo ->
                SwipeableMemoCard(
                    memo = memo,
                    onClick = { onMemoClick(memo) },
                    onPin = { onTogglePin(memo) },
                    onTrash = { onMoveToTrash(memo) },
                )
            }
            item { HorizontalDivider() }
        }

        if (uiState.groupedMemos.isEmpty()) {
            item {
                Text(
                    text = "メモがありません",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        uiState.groupedMemos.forEach { section ->
            item {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            items(section.memos, key = { it.id }) { memo ->
                SwipeableMemoCard(
                    memo = memo,
                    onClick = { onMemoClick(memo) },
                    onPin = { onTogglePin(memo) },
                    onTrash = { onMoveToTrash(memo) },
                )
            }
        }
    }
}

@Composable
private fun HomeGridContent(
    uiState: HomeUiState,
    onMemoClick: (Memo) -> Unit,
    onTogglePin: (Memo) -> Unit,
    onMoveToTrash: (Memo) -> Unit,
    onColorSelected: (Int?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalStaggeredGrid(
        modifier = modifier,
        columns = StaggeredGridCells.Fixed(2),
        verticalItemSpacing = 10.dp,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(12.dp),
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            QuickMemoSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
            )
        }
        item(span = StaggeredGridItemSpan.FullLine) {
            ColorFilterRow(
                selectedColor = uiState.selectedColorFilter,
                onColorSelected = onColorSelected,
            )
        }

        if (uiState.pinnedMemos.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    text = stringResource(id = R.string.pinned),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            items(uiState.pinnedMemos, key = { it.id }) { memo ->
                SwipeableMemoCard(
                    memo = memo,
                    onClick = { onMemoClick(memo) },
                    onPin = { onTogglePin(memo) },
                    onTrash = { onMoveToTrash(memo) },
                )
            }
        }

        if (uiState.groupedMemos.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    text = "メモがありません",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        uiState.groupedMemos.forEach { section ->
            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            items(section.memos, key = { it.id }) { memo ->
                SwipeableMemoCard(
                    memo = memo,
                    onClick = { onMemoClick(memo) },
                    onPin = { onTogglePin(memo) },
                    onTrash = { onMoveToTrash(memo) },
                )
            }
        }
    }
}

@Composable
private fun SwipeableMemoCard(
    memo: Memo,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onTrash: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onPin()
                    false
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    onTrash()
                    false
                }

                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val alignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                Alignment.CenterStart
            } else {
                Alignment.CenterEnd
            }
            val icon = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                Icons.Default.PushPin
            } else {
                Icons.Default.Delete
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentAlignment = alignment,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Gray,
                )
            }
        },
    ) {
        MemoCard(
            memo = memo,
            onClick = onClick,
            onTogglePin = onPin,
        )
    }
}
