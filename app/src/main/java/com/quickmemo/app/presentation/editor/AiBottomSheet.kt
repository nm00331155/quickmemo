package com.quickmemo.app.presentation.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiBottomSheet(
    uiState: EditorUiState,
    onSummarizeClick: () -> Unit,
    onSuggestTagsClick: () -> Unit,
    onAppendToMemo: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "✨ AIアシスタント",
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedButton(
                onClick = onSummarizeClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "📝 このメモを要約する")
                    Text(
                        text = "長い文章を3行に要約します",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedButton(
                onClick = onSuggestTagsClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "🏷️ タグを提案してもらう")
                    Text(
                        text = "内容に合ったタグを自動推薦します",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = if (uiState.hasUnlimitedAi) {
                    "残り: 無制限（Pro）"
                } else {
                    "残り: ${(5 - uiState.aiUsageCount).coerceAtLeast(0)}/5回（Proで無制限）"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (uiState.isAiLoading) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .alpha(if (it % 2 == 0) 0.6f else 0.35f)
                        )
                    }
                }
            } else {
                Text(
                    text = uiState.aiResultText,
                    style = MaterialTheme.typography.bodyMedium,
                    minLines = 4,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onAppendToMemo,
                    enabled = uiState.aiResultText.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "メモに追記")
                }

                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "閉じる")
                }
            }
        }
    }
}
