package com.quickmemo.app.data.repository

import com.quickmemo.app.data.local.dao.DictionaryDao
import com.quickmemo.app.domain.model.DictionaryEntry
import com.quickmemo.app.domain.repository.DictionaryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DictionaryRepositoryImpl @Inject constructor(
    private val dictionaryDao: DictionaryDao,
) : DictionaryRepository {

    override fun observeEntries(): Flow<List<DictionaryEntry>> {
        return dictionaryDao.getAllFlow().map { entries ->
            entries.map { it.toDomain() }
        }
    }

    override suspend fun addEntry(label: String, content: String) {
        val normalizedLabel = label.trim()
        val normalizedContent = content.trim()
        if (normalizedLabel.isBlank() || normalizedContent.isBlank()) return

        val nextSortOrder = dictionaryDao.getMaxSortOrder() + 1
        dictionaryDao.insert(
            DictionaryEntry(
                label = normalizedLabel,
                content = normalizedContent,
                sortOrder = nextSortOrder,
            ).toEntity(),
        )
    }

    override suspend fun updateEntry(entry: DictionaryEntry) {
        val normalized = entry.copy(
            label = entry.label.trim(),
            content = entry.content.trim(),
        )
        if (normalized.label.isBlank() || normalized.content.isBlank()) return
        dictionaryDao.update(normalized.toEntity())
    }

    override suspend fun deleteEntry(entry: DictionaryEntry) {
        dictionaryDao.delete(entry.toEntity())
    }
}
