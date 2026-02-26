package com.quickmemo.app.domain.usecase

import com.quickmemo.app.domain.model.Memo
import com.quickmemo.app.domain.repository.MemoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchMemosUseCase @Inject constructor(
    private val memoRepository: MemoRepository,
) {
    operator fun invoke(query: String): Flow<List<Memo>> {
        return memoRepository.searchMemos(query)
    }
}
