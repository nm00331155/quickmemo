package com.quickmemo.app.domain.model

data class DictionaryEntry(
    val id: Long = 0,
    val label: String,
    val content: String,
    val sortOrder: Int = 0,
)
