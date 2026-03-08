package com.quickmemo.app.domain.repository

import com.quickmemo.app.domain.model.DictionaryEntry
import kotlinx.coroutines.flow.Flow

interface DictionaryRepository {
    fun observeEntries(): Flow<List<DictionaryEntry>>

    suspend fun addEntry(label: String, content: String)
    suspend fun updateEntry(entry: DictionaryEntry)
    suspend fun deleteEntry(entry: DictionaryEntry)
}
