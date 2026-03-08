package com.quickmemo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quickmemo.app.data.local.entity.MemoBackupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoBackupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(backup: MemoBackupEntity)

    @Query("SELECT * FROM memo_backups WHERE memoId = :memoId ORDER BY createdAt DESC")
    fun getBackupsForMemo(memoId: Long): Flow<List<MemoBackupEntity>>

    @Query("SELECT * FROM memo_backups WHERE memoId = :memoId AND backupType = :type ORDER BY createdAt DESC")
    suspend fun getBackupsByType(memoId: Long, type: String): List<MemoBackupEntity>

    @Query("DELETE FROM memo_backups WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query(
        """
        DELETE FROM memo_backups
        WHERE memoId = :memoId
          AND backupType = 'REALTIME'
          AND id NOT IN (
              SELECT id
              FROM memo_backups
              WHERE memoId = :memoId
                AND backupType = 'REALTIME'
              ORDER BY createdAt DESC
              LIMIT 1
          )
        """,
    )
    suspend fun pruneRealtimeBackups(memoId: Long)
}
