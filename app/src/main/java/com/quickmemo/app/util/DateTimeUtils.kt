package com.quickmemo.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private val memoDateFormat = SimpleDateFormat("M月d日", Locale.JAPAN)
    private val insertDateFormat = SimpleDateFormat("M月d日(E)", Locale.JAPAN)
    private val insertTimeFormat = SimpleDateFormat("HH:mm", Locale.JAPAN)

    fun formatCardDate(timestamp: Long): String = memoDateFormat.format(Date(timestamp))

    fun formatInsertDate(timestamp: Long): String = insertDateFormat.format(Date(timestamp))

    fun formatInsertTime(timestamp: Long): String = insertTimeFormat.format(Date(timestamp))
}
