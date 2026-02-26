package com.quickmemo.app.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import com.quickmemo.app.domain.model.MemoColor

@Composable
fun memoCardBackgroundColor(colorLabel: Int): Color {
    val color = MemoColor.fromIndex(colorLabel)
    return if (isSystemInDarkTheme()) color.darkColor else color.lightColor
}
