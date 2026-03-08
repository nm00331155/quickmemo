package com.quickmemo.app.data.backup

import com.quickmemo.app.data.local.dao.MemoBackupDao
import com.quickmemo.app.data.local.entity.MemoBackupEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoBackupManagerTest {

    @Test
    fun `既存リアルタイムがある状態_保存を繰り返す_最新1件だけ残る`() = runBlocking {
        val dao = FakeMemoBackupDao()
        val manager = MemoBackupManager(dao)

        manager.saveRealtimeBackup(memoId = 10L, title = "title", contentHtml = "v1")
        manager.saveRealtimeBackup(memoId = 10L, title = "title", contentHtml = "v2")
        manager.saveRealtimeBackup(memoId = 10L, title = "title", contentHtml = "v3")

        val backups = dao.getBackupsByType(10L, MemoBackupManager.TYPE_REALTIME)
        assertEquals(1, backups.size)
        assertEquals("v3", backups.first().contentHtml)
    }

    @Test
    fun `1分バックアップが上限超過した状態_保存を続ける_最新4件だけ残る`() = runBlocking {
        val dao = FakeMemoBackupDao()
        val manager = MemoBackupManager(dao)

        repeat(6) { index ->
            manager.savePeriodicBackup(
                memoId = 11L,
                title = "title",
                contentHtml = "v$index",
                type = MemoBackupManager.TYPE_1MIN,
            )
        }

        val backups = dao.getBackupsByType(11L, MemoBackupManager.TYPE_1MIN)
        assertEquals(4, backups.size)
        assertEquals("v5", backups.first().contentHtml)
        assertEquals("v2", backups.last().contentHtml)
    }

    @Test
    fun `5分バックアップが上限超過した状態_保存を続ける_最新5件だけ残る`() = runBlocking {
        val dao = FakeMemoBackupDao()
        val manager = MemoBackupManager(dao)

        repeat(7) { index ->
            manager.savePeriodicBackup(
                memoId = 12L,
                title = "title",
                contentHtml = "v$index",
                type = MemoBackupManager.TYPE_5MIN,
            )
        }

        val backups = dao.getBackupsByType(12L, MemoBackupManager.TYPE_5MIN)
        assertEquals(5, backups.size)
        assertEquals("v6", backups.first().contentHtml)
        assertEquals("v2", backups.last().contentHtml)
    }

    private class FakeMemoBackupDao : MemoBackupDao {
        private val records = mutableListOf<MemoBackupEntity>()
        private val flow = MutableStateFlow<List<MemoBackupEntity>>(emptyList())
        private var nextId = 1L
        private var nextCreatedAt = 1L

        override suspend fun insert(backup: MemoBackupEntity) {
            records += backup.copy(
                id = nextId++,
                createdAt = nextCreatedAt++,
            )
            publish()
        }

        override fun getBackupsForMemo(memoId: Long): Flow<List<MemoBackupEntity>> {
            return flow.map { all ->
                all.filter { it.memoId == memoId }
                    .sortedByDescending { it.createdAt }
            }
        }

        override suspend fun getBackupsByType(memoId: Long, type: String): List<MemoBackupEntity> {
            return records
                .filter { it.memoId == memoId && it.backupType == type }
                .sortedByDescending { it.createdAt }
        }

        override suspend fun deleteByIds(ids: List<Long>) {
            records.removeAll { it.id in ids.toSet() }
            publish()
        }

        override suspend fun pruneRealtimeBackups(memoId: Long) {
            val realtime = records
                .filter { it.memoId == memoId && it.backupType == MemoBackupManager.TYPE_REALTIME }
                .sortedByDescending { it.createdAt }
            if (realtime.size <= 1) return

            val idsToDelete = realtime.drop(1).map { it.id }.toSet()
            records.removeAll { it.id in idsToDelete }
            publish()
        }

        private fun publish() {
            flow.value = records.toList()
        }
    }
}
