package com.quickmemo.app.presentation.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quickmemo.app.domain.model.DictionaryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryBottomSheet(
    entries: List<DictionaryEntry>,
    onInsert: (String) -> Unit,
    onAddNew: () -> Unit,
    onEdit: (DictionaryEntry) -> Unit,
    onDelete: (DictionaryEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("単語辞書", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onAddNew) {
                    Icon(Icons.Default.Add, contentDescription = "追加")
                }
            }

            HorizontalDivider()

            if (entries.isEmpty()) {
                Text(
                    text = "登録された定型文はありません",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn {
                    items(entries, key = { it.id }) { entry ->
                        ListItem(
                            headlineContent = { Text(entry.label) },
                            supportingContent = {
                                Text(
                                    text = entry.content,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { onEdit(entry) }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "編集",
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    IconButton(onClick = { onDelete(entry) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "削除",
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.clickable { onInsert(entry.content) },
                        )
                    }
                }
            }
        }
    }
}
