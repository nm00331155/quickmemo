package com.quickmemo.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.quickmemo.app.data.local.dao.MemoDao
import com.quickmemo.app.domain.model.Memo
import com.quickmemo.app.domain.repository.MemoRepository
import com.quickmemo.app.widget.WidgetRefreshCoordinator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoRepositoryImpl @Inject constructor(
    private val memoDao: MemoDao,
    private val widgetRefreshCoordinator: WidgetRefreshCoordinator,
    @ApplicationContext private val context: Context,
) : MemoRepository {

    override fun observeActiveMemos(colorFilter: Int?): Flow<List<Memo>> {
        return memoDao.observeActiveMemos(colorFilter).map { list -> list.map { it.toDomain() } }
    }

    override fun observeTrashMemos(): Flow<List<Memo>> {
        return memoDao.observeTrashMemos().map { list -> list.map { it.toDomain() } }
    }

    override fun searchMemos(query: String): Flow<List<Memo>> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return flowOf(emptyList())

        val safeQuery = buildString {
            normalizedQuery.split(" ").filter { it.isNotBlank() }.forEachIndexed { index, part ->
                if (index > 0) append(" ")
                append(part.replace('"', ' '))
                append("*")
            }
        }

        return memoDao.searchMemos(safeQuery).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getMemoById(id: Long): Memo? {
        return memoDao.getMemoById(id)?.toDomain()
    }

    override suspend fun saveMemo(memo: Memo): Long {
        val now = System.currentTimeMillis()
        val savedId = if (memo.id == 0L) {
            memoDao.insertMemo(
                memo.copy(
                    createdAt = now,
                    updatedAt = now,
                ).toEntity()
            )
        } else {
            memoDao.updateMemo(
                memo.copy(updatedAt = now).toEntity()
            )
            memo.id
        }
        widgetRefreshCoordinator.refreshMemoWidgets(reason = "memo_save")
        return savedId
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        memoDao.setPinned(id, pinned, System.currentTimeMillis())
    }

    override suspend fun setLocked(id: Long, locked: Boolean) {
        memoDao.setLocked(id, locked, System.currentTimeMillis())
    }

    override suspend fun moveToTrash(id: Long) {
        val now = System.currentTimeMillis()
        memoDao.moveToTrash(id, now, now)
        widgetRefreshCoordinator.refreshMemoWidgets(reason = "memo_move_to_trash")
    }

    override suspend fun restoreFromTrash(id: Long) {
        memoDao.restoreFromTrash(id, System.currentTimeMillis())
        widgetRefreshCoordinator.refreshMemoWidgets(reason = "memo_restore_from_trash")
    }

    override suspend fun deletePermanently(id: Long) {
        val memo = memoDao.getMemoById(id)
        memoDao.deleteMemoById(id)
        memo?.let { deleteImagesFromHtml(it.contentHtml) }
        widgetRefreshCoordinator.refreshMemoWidgets(reason = "memo_delete_permanently")
    }

    override suspend fun emptyTrash() {
        val deleted = memoDao.getAllDeletedMemos()
        memoDao.deleteAllDeletedMemos()
        deleted.forEach { deleteImagesFromHtml(it.contentHtml) }
        widgetRefreshCoordinator.refreshMemoWidgets(reason = "memo_empty_trash")
    }

    override suspend fun purgeExpiredTrash(days: Long): Int {
        val threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days)
        val expired = memoDao.getExpiredDeletedMemos(threshold)
        val deletedCount = memoDao.deleteExpiredDeletedMemos(threshold)
        expired.forEach { deleteImagesFromHtml(it.contentHtml) }
        if (deletedCount > 0) {
            widgetRefreshCoordinator.refreshMemoWidgets(reason = "memo_purge_expired")
        }
        return deletedCount
    }

    private fun deleteImagesFromHtml(html: String) {
        val regex = Regex("""<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>""")
        regex.findAll(html).forEach { match ->
            val raw = match.groupValues.getOrNull(1).orEmpty()
            if (raw.startsWith("content://") || raw.startsWith("file://")) {
                deleteUri(raw.toUri())
            }
        }
    }

    private fun deleteUri(uri: Uri) {
        kotlin.runCatching {
            if (uri.scheme == "file") {
                uri.path?.let { path ->
                    File(path).delete()
                }
            } else {
                context.contentResolver.delete(uri, null, null)
            }
        }
    }
}
