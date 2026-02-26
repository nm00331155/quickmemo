package com.quickmemo.app.domain.usecase

import com.quickmemo.app.domain.model.Memo
import com.quickmemo.app.domain.repository.MemoRepository
import javax.inject.Inject

class SaveMemoUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
) {
    suspend operator fun invoke(memo: Memo): Long? {
        val isNew = memo.id == 0L
        val isEmpty = memo.title.isBlank() && memo.contentPlainText.isBlank()
        if (isNew && isEmpty) return null
        return memoRepository.saveMemo(memo)
    }
}
