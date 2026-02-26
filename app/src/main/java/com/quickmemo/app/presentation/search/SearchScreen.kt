package com.quickmemo.app.presentation.search

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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickmemo.app.domain.model.Memo
import com.quickmemo.app.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenEditor: (Long) -> Unit,
    onOpenTodo: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestUnlockMemo: (Memo, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "back",
                    )
                }
                TextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .weight(1f),
                    singleLine = true,
                    placeholder = { Text("検索") },
                )

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
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = "${uiState.results.size}件のメモが見つかりました",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }

            items(uiState.results, key = { it.id }) { memo ->
                Card(
                    onClick = {
                        if (memo.isLocked) {
                            onRequestUnlockMemo(memo) { unlocked ->
                                if (unlocked) onOpenEditor(memo.id)
                            }
                        } else {
                            onOpenEditor(memo.id)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = memo.displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        if (memo.isLocked) {
                            Text(
                                text = "ロックされたメモ",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            Text(
                                text = highlightQuery(
                                    text = memo.contentPlainText,
                                    query = uiState.query,
                                    highlightColor = MaterialTheme.colorScheme.primary,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                            )
                        }

                        Text(
                            text = DateTimeUtils.formatCardDate(memo.updatedAt),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

private fun highlightQuery(
    text: String,
    query: String,
    highlightColor: Color,
): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)

    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var currentIndex = 0

    return buildAnnotatedString {
        while (currentIndex < text.length) {
            val matchStart = lowerText.indexOf(lowerQuery, currentIndex)
            if (matchStart < 0) {
                append(text.substring(currentIndex))
                break
            }

            if (matchStart > currentIndex) {
                append(text.substring(currentIndex, matchStart))
            }

            val end = (matchStart + lowerQuery.length).coerceAtMost(text.length)
            pushStyle(
                SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.Bold,
                )
            )
            append(text.substring(matchStart, end))
            pop()

            currentIndex = end
        }
    }
}
