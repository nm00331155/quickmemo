package com.quickmemo.app.data.backup

import com.quickmemo.app.data.local.dao.MemoBackupDao
import com.quickmemo.app.data.local.entity.MemoBackupEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoBackupManager @Inject constructor(
    private val backupDao: MemoBackupDao,
) {
    suspend fun saveRealtimeBackup(memoId: Long, title: String, contentHtml: String) {
        backupDao.insert(
            MemoBackupEntity(
                memoId = memoId,
                title = title,
                contentHtml = contentHtml,
                backupType = TYPE_REALTIME,
            ),
        )
        backupDao.pruneRealtimeBackups(memoId)
    }

    suspend fun savePeriodicBackup(
        memoId: Long,
        title: String,
        contentHtml: String,
        type: String,
    ) {
        val maxGenerations = when (type) {
            TYPE_1MIN -> 4
            TYPE_5MIN -> 5
            else -> return
        }

        backupDao.insert(
            MemoBackupEntity(
                memoId = memoId,
                title = title,
                contentHtml = contentHtml,
                backupType = type,
            ),
        )
        pruneOldBackups(memoId = memoId, type = type, maxGenerations = maxGenerations)
    }

    private suspend fun pruneOldBackups(memoId: Long, type: String, maxGenerations: Int) {
        val backups = backupDao.getBackupsByType(memoId = memoId, type = type)
        if (backups.size <= maxGenerations) return

        val idsToDelete = backups
            .drop(maxGenerations)
            .map { it.id }
        if (idsToDelete.isNotEmpty()) {
            backupDao.deleteByIds(idsToDelete)
        }
    }

    companion object {
        const val TYPE_REALTIME = "REALTIME"
        const val TYPE_1MIN = "1MIN"
        const val TYPE_5MIN = "5MIN"
    }
}
