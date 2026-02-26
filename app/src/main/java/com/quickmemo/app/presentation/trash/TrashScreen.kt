package com.quickmemo.app.presentation.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickmemo.app.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    onOpenTodo: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showConfirmEmpty by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("ゴミ箱") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenTodo) {
                        Icon(
                            imageVector = Icons.Outlined.CheckBox,
                            contentDescription = "todo",
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "settings",
                        )
                    }
                    TextButton(onClick = { showConfirmEmpty = true }) {
                        Text("すべて空にする")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(uiState.memos, key = { it.id }) { memo ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = memo.displayTitle)
                        Text(
                            text = memo.contentPlainText,
                            maxLines = 2,
                        )
                        Text(text = "削除日: ${memo.deletedAt?.let { DateTimeUtils.formatCardDate(it) } ?: "-"}")

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.restore(memo.id) }) {
                                Text("復元")
                            }
                            Button(onClick = { viewModel.deletePermanently(memo.id) }) {
                                Text("完全に削除")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmEmpty) {
        AlertDialog(
            onDismissRequest = { showConfirmEmpty = false },
            title = { Text("ゴミ箱を空にする") },
            text = { Text("すべてのメモを完全に削除します。よろしいですか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmEmpty = false
                        viewModel.emptyTrash()
                    },
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmEmpty = false }) {
                    Text("キャンセル")
                }
            },
        )
    }
}
