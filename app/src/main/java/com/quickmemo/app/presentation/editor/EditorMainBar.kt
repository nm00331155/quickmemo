package com.quickmemo.app.presentation.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.quickmemo.app.R

@Composable
fun EditorMainBar(
    onCopyFullText: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onRunOcr: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenDictionary: () -> Unit,
    onOpenTranslation: () -> Unit,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    showFullCopy: Boolean,
    showUndoRedo: Boolean,
    showOcr: Boolean,
    showCalculator: Boolean,
    showDictionary: Boolean,
    showTranslation: Boolean,
    showDateTimeInsert: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showFullCopy) {
                ToolbarActionButton(
                    onClick = onCopyFullText,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "copy",
                        )
                    },
                )
            }

            if (showUndoRedo) {
                ToolbarActionButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "undo",
                        )
                    },
                )
                ToolbarActionButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "redo",
                        )
                    },
                )
            }

            if (showOcr) {
                ToolbarActionButton(
                    onClick = onRunOcr,
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ocr_scan),
                            contentDescription = "OCR テキスト認識",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                )
            }

            if (showCalculator) {
                ToolbarActionButton(
                    onClick = onOpenCalculator,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "calculator",
                        )
                    },
                )
            }

            if (showDictionary) {
                ToolbarActionButton(
                    onClick = onOpenDictionary,
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_dictionary),
                            contentDescription = "単語辞書",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                )
            }

            if (showTranslation) {
                ToolbarActionButton(
                    onClick = onOpenTranslation,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = "translation",
                        )
                    },
                )
            }

            if (showDateTimeInsert) {
                ToolbarActionButton(
                    onClick = onPickDate,
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_calendar_check),
                            contentDescription = "日付挿入",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                )

                ToolbarActionButton(
                    onClick = onPickTime,
                    icon = { Icon(imageVector = Icons.Default.Schedule, contentDescription = "time") },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolbarActionButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    selected: Boolean = false,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .focusProperties { canFocus = false }
            .graphicsLayer {
                alpha = if (enabled) 1f else 0.3f
            }
            .combinedClickable(
                onClick = {
                    if (enabled) {
                        onClick()
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                Color.Transparent
            },
            shape = MaterialTheme.shapes.small,
        ) {
            Box(
                modifier = Modifier.padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
        }
    }
}
