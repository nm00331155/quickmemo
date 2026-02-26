package com.quickmemo.app.domain.usecase

import com.quickmemo.app.domain.repository.MemoRepository
import javax.inject.Inject

class ManageTrashUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
) {
    suspend fun moveToTrash(id: Long) = memoRepository.moveToTrash(id)

    suspend fun restore(id: Long) = memoRepository.restoreFromTrash(id)

    suspend fun deletePermanently(id: Long) = memoRepository.deletePermanently(id)

    suspend fun emptyTrash() = memoRepository.emptyTrash()

    suspend fun purgeExpired(days: Long = 30): Int = memoRepository.purgeExpiredTrash(days)
}
