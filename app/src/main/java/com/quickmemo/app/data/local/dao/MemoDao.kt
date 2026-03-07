package com.quickmemo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quickmemo.app.data.local.entity.MemoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {
    @Query("SELECT * FROM memos WHERE isPinned = 1 AND isDeleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestPinnedMemoSync(): MemoEntity?

    @Query(
        """
        SELECT * FROM memos
        WHERE isDeleted = 0
        AND (:colorFilter IS NULL OR colorLabel = :colorFilter)
        ORDER BY isPinned DESC, updatedAt DESC
        """
    )
    fun observeActiveMemos(colorFilter: Int?): Flow<List<MemoEntity>>

    @Query(
        """
        SELECT m.* FROM memos AS m
        JOIN memos_fts ON m.rowid = memos_fts.rowid
        WHERE m.isDeleted = 0
        AND memos_fts MATCH :query
        ORDER BY m.updatedAt DESC
        """
    )
    fun searchMemos(query: String): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun observeTrashMemos(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE id = :id LIMIT 1")
    suspend fun getMemoById(id: Long): MemoEntity?

    @Query("SELECT * FROM memos ORDER BY id ASC")
    suspend fun getAllForBackup(): List<MemoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: MemoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllForBackup(memos: List<MemoEntity>)

    @Update
    suspend fun updateMemo(memo: MemoEntity)

    @Query("UPDATE memos SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, updatedAt: Long)

    @Query("UPDATE memos SET isLocked = :locked, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setLocked(id: Long, locked: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE memos
        SET isDeleted = 1,
            deletedAt = :deletedAt,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun moveToTrash(id: Long, deletedAt: Long, updatedAt: Long)

    @Query(
        """
        UPDATE memos
        SET isDeleted = 0,
            deletedAt = NULL,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun restoreFromTrash(id: Long, updatedAt: Long)

    @Query("SELECT * FROM memos WHERE isDeleted = 1")
    suspend fun getAllDeletedMemos(): List<MemoEntity>

    @Query(
        """
        SELECT * FROM memos
        WHERE isDeleted = 1
        AND deletedAt IS NOT NULL
        AND deletedAt <= :threshold
        """
    )
    suspend fun getExpiredDeletedMemos(threshold: Long): List<MemoEntity>

    @Query("DELETE FROM memos WHERE id = :id")
    suspend fun deleteMemoById(id: Long)

    @Query("DELETE FROM memos WHERE isDeleted = 1")
    suspend fun deleteAllDeletedMemos()

    @Query("DELETE FROM memos")
    suspend fun deleteAllForBackup()

    @Query(
        """
        DELETE FROM memos
        WHERE isDeleted = 1
        AND deletedAt IS NOT NULL
        AND deletedAt <= :threshold
        """
    )
    suspend fun deleteExpiredDeletedMemos(threshold: Long): Int
}
