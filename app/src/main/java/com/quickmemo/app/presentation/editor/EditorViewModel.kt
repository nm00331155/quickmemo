// File: app/src/main/java/com/quickmemo/app/presentation/editor/EditorViewModel.kt
package com.quickmemo.app.presentation.editor

import android.app.Activity
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickmemo.app.billing.BillingManager
import com.quickmemo.app.data.backup.MemoBackupManager
import com.quickmemo.app.domain.model.DictionaryEntry
import com.quickmemo.app.domain.model.Memo
import com.quickmemo.app.domain.model.MemoBlock
import com.quickmemo.app.domain.model.createDefaultMemoBlocks
import com.quickmemo.app.domain.model.plainTextToHtml
import com.quickmemo.app.domain.repository.DictionaryRepository
import com.quickmemo.app.domain.repository.MemoRepository
import com.quickmemo.app.domain.repository.SettingsRepository
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dictionaryRepository: DictionaryRepository,
    private val memoRepository: MemoRepository,
    private val saveMemoUseCase: SaveMemoUseCase,
    private val manageTrashUseCase: ManageTrashUseCase,
    private val settingsRepository: SettingsRepository,
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val billingManager: BillingManager,
    private val memoBackupManager: MemoBackupManager,
) : ViewModel() {

    private val memoIdArg: Long = savedStateHandle.get<Long>(ARG_MEMO_ID) ?: 0L
    private val prefillText: String = savedStateHandle.get<String>(ARG_PREFILL_TEXT).orEmpty()
    private val insertToday: Boolean = savedStateHandle.get<Boolean>(ARG_INSERT_TODAY) ?: false
    private val colorLabelArg: Int = savedStateHandle.get<Int>(ARG_COLOR_LABEL) ?: 0

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()
    private val _dictionaryEntries = MutableStateFlow<List<DictionaryEntry>>(emptyList())
    val dictionaryEntries: StateFlow<List<DictionaryEntry>> = _dictionaryEntries.asStateFlow()
    private val undoRedoManager = UndoRedoManager(maxHistory = 50)
    val calcHistory = mutableStateListOf<CalcHistoryEntry>()
    private var periodicBackupJob: Job? = null
    private var periodicBackupMemoId: Long = 0L
    private var latestEditorSnapshot: EditorBackupSnapshot? = null
    private var lastRealtimeSnapshotSignature: String = ""

    init {
        viewModelScope.launch {
            billingManager.state.collect { billingState ->
                _uiState.update {
                    it.copy(
                        hasTranslation = billingState.purchaseState.hasTranslation,
                    )
                }
            }
        }

        viewModelScope.launch {
            dictionaryRepository.observeEntries().collect { entries ->
                _dictionaryEntries.value = entries
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

        viewModelScope.launch {
            settingsRepository.observeTaxRate().collect { rate ->
                _uiState.update { it.copy(taxRate = rate) }
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

                latestEditorSnapshot = EditorBackupSnapshot(
                    memoId = memo.id,
                    title = memo.title,
                    contentHtml = memo.contentHtml,
                )
                startPeriodicBackups(memo.id)
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

    suspend fun saveMemo(
        contentHtml: String,
        contentPlainText: String,
        blocks: List<MemoBlock>,
    ): Long? {
        val current = _uiState.value
        val updatedAt = System.currentTimeMillis()
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
                updatedAt = updatedAt,
            )
        )
        if (savedId != null) {
            val resolvedMemoId = if (current.memoId == 0L) savedId else current.memoId
            if (current.memoId == 0L) {
                _uiState.update { it.copy(memoId = savedId, updatedAt = updatedAt) }
            } else {
                _uiState.update { it.copy(updatedAt = updatedAt) }
            }

            val snapshot = EditorBackupSnapshot(
                memoId = resolvedMemoId,
                title = current.title,
                contentHtml = contentHtml,
            )
            latestEditorSnapshot = snapshot
            startPeriodicBackups(resolvedMemoId)

            val signature = buildSnapshotSignature(snapshot)
            if (signature != lastRealtimeSnapshotSignature) {
                memoBackupManager.saveRealtimeBackup(
                    memoId = resolvedMemoId,
                    title = current.title,
                    contentHtml = contentHtml,
                )
                lastRealtimeSnapshotSignature = signature
            }
        }
        return savedId
    }

    fun onEditorSnapshotChanged(
        memoId: Long,
        title: String,
        contentHtml: String,
    ) {
        if (memoId <= 0L) return
        if (title.isBlank() && contentHtml.isBlank()) return

        val snapshot = EditorBackupSnapshot(
            memoId = memoId,
            title = title,
            contentHtml = contentHtml,
        )
        latestEditorSnapshot = snapshot
        startPeriodicBackups(memoId)

        val signature = buildSnapshotSignature(snapshot)
        if (signature == lastRealtimeSnapshotSignature) return
        lastRealtimeSnapshotSignature = signature

        viewModelScope.launch {
            memoBackupManager.saveRealtimeBackup(
                memoId = memoId,
                title = title,
                contentHtml = contentHtml,
            )
        }
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

    fun addDictionaryEntry(label: String, content: String) {
        viewModelScope.launch {
            dictionaryRepository.addEntry(label = label, content = content)
        }
    }

    fun updateDictionaryEntry(entry: DictionaryEntry) {
        viewModelScope.launch {
            dictionaryRepository.updateEntry(entry)
        }
    }

    fun deleteDictionaryEntry(entry: DictionaryEntry) {
        viewModelScope.launch {
            dictionaryRepository.deleteEntry(entry)
        }
    }

    fun purchaseTranslation(activity: Activity) {
        billingManager.launchPurchase(activity, BillingManager.Products.UNLOCK_TRANSLATION)
    }

    override fun onCleared() {
        periodicBackupJob?.cancel()
        super.onCleared()
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

    private fun startPeriodicBackups(memoId: Long) {
        if (memoId <= 0L) return
        if (periodicBackupMemoId == memoId && periodicBackupJob?.isActive == true) return

        periodicBackupJob?.cancel()
        periodicBackupMemoId = memoId
        periodicBackupJob = viewModelScope.launch {
            launch {
                while (isActive) {
                    delay(60_000)
                    savePeriodicBackupIfNeeded(
                        memoId = memoId,
                        type = MemoBackupManager.TYPE_1MIN,
                    )
                }
            }

            launch {
                while (isActive) {
                    delay(300_000)
                    savePeriodicBackupIfNeeded(
                        memoId = memoId,
                        type = MemoBackupManager.TYPE_5MIN,
                    )
                }
            }
        }
    }

    private suspend fun savePeriodicBackupIfNeeded(
        memoId: Long,
        type: String,
    ) {
        val snapshot = latestEditorSnapshot ?: return
        if (snapshot.memoId != memoId) return
        if (snapshot.title.isBlank() && snapshot.contentHtml.isBlank()) return

        memoBackupManager.savePeriodicBackup(
            memoId = memoId,
            title = snapshot.title,
            contentHtml = snapshot.contentHtml,
            type = type,
        )
    }

    private fun buildSnapshotSignature(snapshot: EditorBackupSnapshot): String {
        return "${snapshot.memoId}:${snapshot.title.hashCode()}:${snapshot.contentHtml.hashCode()}"
    }

    companion object {
        const val ARG_MEMO_ID = "memoId"
        const val ARG_PREFILL_TEXT = "prefillText"
        const val ARG_INSERT_TODAY = "insertToday"
        const val ARG_COLOR_LABEL = "colorLabel"
    }

    private data class EditorBackupSnapshot(
        val memoId: Long,
        val title: String,
        val contentHtml: String,
    )
}
