// File: app/src/main/java/com/quickmemo/app/presentation/editor/EditorFormatBar.kt
package com.quickmemo.app.presentation.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState

@Composable
fun EditorFormatBar(
    richTextState: RichTextState?,
    showTextSize: Boolean,
    showBoldItalic: Boolean,
    showTextColor: Boolean,
    showHighlighter: Boolean,
    modifier: Modifier = Modifier,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val defaultTextColor = if (dark) Color.White else Color.Black
    val highlightColor = Color(0xFFFFFF8D)
    val currentSpanStyle = richTextState?.currentSpanStyle
    val selectedColor = richTextState?.currentSpanStyle?.color
    var showColorMenu by remember { mutableStateOf(false) }

    val currentFontSize = currentSpanStyle?.fontSize
    val isSmallSelected = currentFontSize.isAlmost(14.sp)
    val isMediumSelected = currentFontSize.isAlmost(18.sp)
    val isLargeSelected = currentFontSize.isAlmost(24.sp)

    val isBoldSelected = currentSpanStyle?.fontWeight == FontWeight.Bold
    val isItalicSelected = currentSpanStyle?.fontStyle == FontStyle.Italic
    val isHighlightSelected = currentSpanStyle?.background == highlightColor

    val palette = listOf(
        "黒" to defaultTextColor,
        "赤" to Color(0xFFD32F2F),
        "青" to Color(0xFF1976D2),
        "緑" to Color(0xFF388E3C),
        "橙" to Color(0xFFFF8F00),
        "紫" to Color(0xFF7B1FA2),
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showTextSize) {
                FormatButton(label = "小", selected = isSmallSelected) {
                    richTextState?.toggleSpanStyle(SpanStyle(fontSize = 14.sp))
                }
                FormatButton(label = "中", selected = isMediumSelected) {
                    richTextState?.toggleSpanStyle(SpanStyle(fontSize = 18.sp))
                }
                FormatButton(label = "大", selected = isLargeSelected) {
                    richTextState?.toggleSpanStyle(SpanStyle(fontSize = 24.sp))
                }
            }

            if (showBoldItalic) {
                FormatButton(label = "B", selected = isBoldSelected) {
                    richTextState?.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
                }
                FormatButton(label = "I", selected = isItalicSelected) {
                    richTextState?.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
                }
            }

            if (showHighlighter) {
                FormatButton(label = "蛍", selected = isHighlightSelected) {
                    val state = richTextState ?: return@FormatButton
                    val style = SpanStyle(background = highlightColor)
                    state.toggleSpanStyle(style)
                }
            }

            if (showTextColor) {
                Box {
                    FormatButton(label = "色", selected = selectedColor != null) {
                        showColorMenu = true
                    }
                    DropdownMenu(
                        expanded = showColorMenu,
                        onDismissRequest = { showColorMenu = false },
                    ) {
                        palette.forEach { (label, color) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(color, shape = CircleShape)
                                            .border(1.dp, Color.Gray, CircleShape),
                                    )
                                },
                                onClick = {
                                    val state = richTextState
                                    if (state != null) {
                                        val isSelected = selectedColor == color
                                        if (isSelected) {
                                            state.removeSpanStyle(SpanStyle(color = color))
                                        } else {
                                            state.toggleSpanStyle(SpanStyle(color = color))
                                        }
                                    }
                                    showColorMenu = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        modifier = Modifier
            .focusProperties { canFocus = false }
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

private fun TextUnit?.isAlmost(target: TextUnit): Boolean {
    if (this == null || this == TextUnit.Unspecified || target == TextUnit.Unspecified) return false
    return kotlin.math.abs(this.value - target.value) < 0.1f
}
