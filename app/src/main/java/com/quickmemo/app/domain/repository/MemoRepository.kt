package com.quickmemo.app.domain.repository

import com.quickmemo.app.domain.model.Memo
import kotlinx.coroutines.flow.Flow

interface MemoRepository {
    fun observeActiveMemos(colorFilter: Int? = null): Flow<List<Memo>>
    fun observeTrashMemos(): Flow<List<Memo>>
    fun searchMemos(query: String): Flow<List<Memo>>
    suspend fun getMemoById(id: Long): Memo?
    suspend fun saveMemo(memo: Memo): Long
    suspend fun setPinned(id: Long, pinned: Boolean)
    suspend fun setLocked(id: Long, locked: Boolean)
    suspend fun moveToTrash(id: Long)
    suspend fun restoreFromTrash(id: Long)
    suspend fun deletePermanently(id: Long)
    suspend fun emptyTrash()
    suspend fun purgeExpiredTrash(days: Long = 30): Int
}
