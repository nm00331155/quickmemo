// File: app/src/main/java/com/quickmemo/app/presentation/editor/EditorViewModel.kt
package com.quickmemo.app.presentation.editor

import com.quickmemo.app.ai.AiUsageManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickmemo.app.ai.AiAssistantManager
import com.quickmemo.app.billing.BillingManager
import com.quickmemo.app.domain.model.Memo
import com.quickmemo.app.domain.model.MemoBlock
import com.quickmemo.app.domain.model.createDefaultMemoBlocks
import com.quickmemo.app.domain.model.plainTextToHtml
import com.quickmemo.app.domain.repository.MemoRepository
import com.quickmemo.app.domain.usecase.ManageTrashUseCase
import com.quickmemo.app.domain.usecase.ObserveSettingsUseCase
import com.quickmemo.app.domain.usecase.SaveMemoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memoRepository: MemoRepository,
    private val saveMemoUseCase: SaveMemoUseCase,
    private val manageTrashUseCase: ManageTrashUseCase,
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val aiAssistantManager: AiAssistantManager,
    private val billingManager: BillingManager,
    private val aiUsageManager: AiUsageManager,
) : ViewModel() {

    private val memoIdArg: Long = savedStateHandle.get<Long>(ARG_MEMO_ID) ?: 0L
    private val prefillText: String = savedStateHandle.get<String>(ARG_PREFILL_TEXT).orEmpty()
    private val insertToday: Boolean = savedStateHandle.get<Boolean>(ARG_INSERT_TODAY) ?: false
    private val colorLabelArg: Int = savedStateHandle.get<Int>(ARG_COLOR_LABEL) ?: 0

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()
    private val undoRedoManager = UndoRedoManager(maxHistory = 50)

    init {
        viewModelScope.launch {
            val aiStatus = aiAssistantManager.checkStatus()
            _uiState.update { it.copy(aiFeatureStatus = aiStatus) }
            refreshAiUsage()
        }

        viewModelScope.launch {
            billingManager.state.collect { billingState ->
                _uiState.update {
                    it.copy(
                        hasTranslation = billingState.purchaseState.hasTranslation,
                        hasUnlimitedAi = billingState.purchaseState.hasUnlimitedAi,
                    )
                }
                refreshAiUsage()
            }
        }

        viewModelScope.launch {
            observeSettingsUseCase().collect { settings ->
                _uiState.update { current ->
                    val resolvedColor =
                        if (colorLabelArg > 0 && current.memoId == 0L) {
                            colorLabelArg
                        } else if (current.memoId == 0L &&
                            current.initialContentHtml.isBlank() &&
                            current.title.isBlank()
                        ) {
                            settings.defaultMemoColor
                        } else {
                            current.colorLabel
                        }

                    current.copy(
                        showCharacterCount = settings.showCharacterCount,
                        insertCurrentTimeWithDate = settings.insertCurrentTimeWithDate,
                        colorLabel = resolvedColor,
                        memoToolbarSettings = settings.memoToolbarSettings,
                    )
                }
            }
        }

        if (memoIdArg > 0L) {
            loadMemo(memoIdArg)
        } else {
            createPrefilledDraft()
        }
    }

    private fun createPrefilledDraft() {
        val textPart = buildString {
            if (insertToday) {
                append(com.quickmemo.app.util.DateTimeUtils.formatInsertDate(System.currentTimeMillis()))
                append("\n")
            }
            if (prefillText.isNotBlank()) {
                append(prefillText)
            }
        }.trim()

        _uiState.update {
            it.copy(
                initialContentHtml = plainTextToHtml(textPart),
                initialContentPlainText = textPart,
                initialBlocks = createDefaultMemoBlocks(plainTextToHtml(textPart)),
                colorLabel = if (colorLabelArg > 0) colorLabelArg else it.colorLabel,
            )
        }
    }

    private fun loadMemo(id: Long) {
        viewModelScope.launch {
            memoRepository.getMemoById(id)?.let { memo ->
                _uiState.update {
                    it.copy(
                        memoId = memo.id,
                        title = memo.title,
                        initialContentHtml = memo.contentHtml,
                        initialContentPlainText = memo.contentPlainText,
                        initialBlocks = if (memo.blocks.isEmpty()) {
                            createDefaultMemoBlocks(memo.contentHtml)
                        } else {
                            memo.blocks
                        },
                        isPinned = memo.isPinned,
                        isLocked = memo.isLocked,
                        colorLabel = memo.colorLabel,
                        createdAt = memo.createdAt,
                        updatedAt = memo.updatedAt,
                    )
                }
            }
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun setPinned(value: Boolean) {
        _uiState.update { it.copy(isPinned = value) }
    }

    fun setLocked(value: Boolean) {
        _uiState.update { it.copy(isLocked = value) }
    }

    fun setColor(color: Int) {
        _uiState.update { it.copy(colorLabel = color) }
    }

    fun openAiSheet() {
        _uiState.update { it.copy(showAiSheet = true) }
    }

    fun closeAiSheet() {
        _uiState.update { it.copy(showAiSheet = false) }
    }

    fun clearAiResult() {
        _uiState.update { it.copy(aiResultText = "", isAiLoading = false) }
    }

    fun summarizeWithAi(plainText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true, aiResultText = "") }
            var consumed = false
            aiAssistantManager.summarize(plainText).collect { stream ->
                when (stream) {
                    is com.quickmemo.app.ai.AiStreamResult.Partial -> {
                        _uiState.update {
                            it.copy(
                                aiResultText = stream.text,
                                isAiLoading = true,
                            )
                        }
                    }

                    is com.quickmemo.app.ai.AiStreamResult.Complete -> {
                        _uiState.update {
                            it.copy(
                                aiResultText = stream.text,
                                isAiLoading = false,
                            )
                        }
                        if (!consumed) {
                            consumeAiUsageOnSuccess()
                            consumed = true
                        }
                    }
                }
            }
        }
    }

    fun suggestTagsWithAi(plainText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true, aiResultText = "") }
            val result = aiAssistantManager.suggestTags(plainText)
            consumeAiUsageOnSuccess()
            _uiState.update {
                it.copy(
                    aiResultText = result,
                    isAiLoading = false,
                )
            }
        }
    }

    suspend fun polishTextWithAi(text: String): String {
        val result = aiAssistantManager.polish(text)
        consumeAiUsageOnSuccess()
        return result
    }

    suspend fun extractTodosWithAi(text: String): String {
        val result = aiAssistantManager.extractTodos(text)
        consumeAiUsageOnSuccess()
        return result
    }

    suspend fun detectEntitiesWithAi(text: String): String {
        val result = aiAssistantManager.detectEntities(text)
        consumeAiUsageOnSuccess()
        return result
    }

    suspend fun expandKeywordsWithAi(text: String): String {
        val result = aiAssistantManager.expandKeywords(text)
        consumeAiUsageOnSuccess()
        return result
    }

    suspend fun extractCalendarEventsWithAi(text: String): String {
        val result = aiAssistantManager.extractCalendarEvents(text)
        consumeAiUsageOnSuccess()
        return result
    }

    suspend fun canUseAiFeature(): Boolean {
        return aiUsageManager.canUse(_uiState.value.hasUnlimitedAi)
    }

    private suspend fun consumeAiUsageOnSuccess() {
        aiUsageManager.consumeOnSuccess(_uiState.value.hasUnlimitedAi)
        refreshAiUsage()
    }

    private suspend fun refreshAiUsage() {
        val usage = aiUsageManager.getUsage()
        _uiState.update {
            it.copy(aiUsageCount = usage.count)
        }
    }

    suspend fun saveMemo(
        contentHtml: String,
        contentPlainText: String,
        blocks: List<MemoBlock>,
    ): Long? {
        val current = _uiState.value
        val savedId = saveMemoUseCase(
            Memo(
                id = current.memoId,
                title = current.title,
                contentHtml = contentHtml,
                contentPlainText = contentPlainText,
                blocks = blocks,
                colorLabel = current.colorLabel,
                isPinned = current.isPinned,
                isLocked = current.isLocked,
                createdAt = current.createdAt,
                updatedAt = System.currentTimeMillis(),
            )
        )
        if (savedId != null && current.memoId == 0L) {
            _uiState.update { it.copy(memoId = savedId) }
        }
        return savedId
    }

    fun initializeUndoRedo(initialSnapshot: String) {
        undoRedoManager.reset(initialSnapshot)
        publishUndoRedoAvailability()
    }

    fun saveUndoSnapshot(snapshot: String) {
        undoRedoManager.save(snapshot)
        publishUndoRedoAvailability()
    }

    fun undoSnapshot(): String? {
        val result = undoRedoManager.undo()
        publishUndoRedoAvailability()
        return result
    }

    fun redoSnapshot(): String? {
        val result = undoRedoManager.redo()
        publishUndoRedoAvailability()
        return result
    }

    fun setUndoRedoRestoring(value: Boolean) {
        undoRedoManager.setRestoring(value)
    }

    private fun publishUndoRedoAvailability() {
        _uiState.update {
            it.copy(
                canUndo = undoRedoManager.canUndo(),
                canRedo = undoRedoManager.canRedo(),
            )
        }
    }

    fun moveToTrash(onDone: () -> Unit) {
        viewModelScope.launch {
            val id = _uiState.value.memoId
            if (id > 0L) {
                manageTrashUseCase.moveToTrash(id)
            }
            onDone()
        }
    }

    fun getMemoInfo(plainText: String): MemoInfo {
        val words = plainText
            .trim()
            .split(Regex("\\s+"))
            .count { it.isNotBlank() }

        return MemoInfo(
            createdAt = _uiState.value.createdAt,
            updatedAt = _uiState.value.updatedAt,
            charCount = plainText.length,
            wordCount = words,
        )
    }

    companion object {
        const val ARG_MEMO_ID = "memoId"
        const val ARG_PREFILL_TEXT = "prefillText"
        const val ARG_PREFILL_CHECKLIST = "prefillChecklist"
        const val ARG_INSERT_TODAY = "insertToday"
        const val ARG_COLOR_LABEL = "colorLabel"
    }
}
