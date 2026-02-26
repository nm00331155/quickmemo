package com.quickmemo.app.domain.model

import androidx.compose.ui.graphics.Color

enum class MemoColor(val index: Int, val lightColor: Color, val darkColor: Color) {
    NONE(0, Color(0xFFF1F3F4), Color(0xFF2C2F31)),
    RED(1, Color(0xFFFFD9DE), Color(0xFF5A3B3F)),
    ORANGE(2, Color(0xFFFFE3CC), Color(0xFF5E4735)),
    YELLOW(3, Color(0xFFFFF3C4), Color(0xFF5A5338)),
    GREEN(4, Color(0xFFDDF5D8), Color(0xFF365343)),
    BLUE(5, Color(0xFFDCEEFF), Color(0xFF334C63)),
    PURPLE(6, Color(0xFFECDDFF), Color(0xFF4B3F64));

    companion object {
        fun fromIndex(index: Int): MemoColor {
            return entries.firstOrNull { it.index == index } ?: NONE
        }
    }
}
