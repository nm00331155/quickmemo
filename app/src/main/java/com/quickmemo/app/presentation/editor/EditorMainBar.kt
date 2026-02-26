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
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun EditorMainBar(
    onCopyFullText: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onRunOcr: () -> Unit,
    onInsertNumberedList: () -> Unit,
    onInsertTable: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenTranslation: () -> Unit,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    showFullCopy: Boolean,
    showUndoRedo: Boolean,
    showOcr: Boolean,
    showNumberedList: Boolean,
    showTable: Boolean,
    showCalculator: Boolean,
    showAi: Boolean,
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
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "ocr",
                        )
                    },
                )
            }

            if (showNumberedList) {
                ToolbarActionButton(
                    onClick = onInsertNumberedList,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = "numbered_list",
                        )
                    },
                )
            }

            if (showTable) {
                ToolbarActionButton(
                    onClick = onInsertTable,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.TableChart,
                            contentDescription = "table",
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

            if (showAi) {
                ToolbarActionButton(
                    onClick = onOpenAi,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "ai",
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
                    icon = { Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "date") },
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
