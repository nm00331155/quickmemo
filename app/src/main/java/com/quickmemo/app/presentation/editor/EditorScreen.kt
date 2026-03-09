package com.quickmemo.app.presentation.editor

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.quickmemo.app.domain.model.DictionaryEntry
import com.quickmemo.app.domain.model.MemoBlock
import com.quickmemo.app.domain.model.createDefaultMemoBlocks
import com.quickmemo.app.domain.model.decodeMemoBlocks
import com.quickmemo.app.domain.model.encodeMemoBlocks
import com.quickmemo.app.domain.model.memoBlocksToPlainText
import com.quickmemo.app.domain.model.plainTextToHtml
import com.quickmemo.app.ocr.OcrCropActivity
import com.quickmemo.app.ocr.OcrFormattedText
import com.quickmemo.app.ocr.OcrTextFormatter
import com.quickmemo.app.ocr.OcrTextNormalizationMode
import com.quickmemo.app.ocr.OcrProcessor
import com.quickmemo.app.ocr.OcrResult
import com.quickmemo.app.translation.TranslationManager
import com.quickmemo.app.translation.TranslationResult
import com.quickmemo.app.util.DateTimeUtils
import com.quickmemo.app.util.EditorDebugLog
import com.quickmemo.app.util.memoCardBackgroundColor
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onNavigateBack: () -> Unit,
    onOpenTodo: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestBiometric: (String, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dictionaryEntries by viewModel.dictionaryEntries.collectAsStateWithLifecycle()
    val editorBackgroundColor = memoCardBackgroundColor(uiState.colorLabel)
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var loadedSignature by remember { mutableStateOf("") }
    var memoBlocks by remember { mutableStateOf<List<MemoBlock>>(createDefaultMemoBlocks("")) }
    val richTextStates = remember { mutableStateMapOf<String, RichTextState>() }
    var activeRichBlockId by remember { mutableStateOf<String?>(null) }
    var currentPlainText by remember { mutableStateOf("") }
    var undoSnapshotRaw by remember { mutableStateOf("") }
    var isApplyingSnapshot by remember { mutableStateOf(false) }

    var showCalculatorSheet by remember { mutableStateOf(false) }
    var lastCalcInsertedFlag by remember { mutableStateOf(false) }
    val calcHistory = viewModel.calcHistory
    var showDictionarySheet by remember { mutableStateOf(false) }
    var showDictionaryEditorDialog by remember { mutableStateOf(false) }
    var dictionaryEditTarget by remember { mutableStateOf<DictionaryEntry?>(null) }
    var dictionaryDeleteTarget by remember { mutableStateOf<DictionaryEntry?>(null) }
    var dictionaryLabelInput by remember { mutableStateOf("") }
    var dictionaryContentInput by remember { mutableStateOf("") }
    var dictionaryDialogTitle by remember { mutableStateOf("辞書を追加") }

    var showOcrSourceSheet by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isOcrLoading by remember { mutableStateOf(false) }
    var ocrFormattedText by remember { mutableStateOf<OcrFormattedText?>(null) }
    var ocrDraftMode by remember { mutableStateOf(OcrTextNormalizationMode.Normalized) }
    var ocrDraftText by remember { mutableStateOf("") }
    var showOcrResultDialog by remember { mutableStateOf(false) }

    var isTranslationLoading by remember { mutableStateOf(false) }
    var translationResult by remember { mutableStateOf<TranslationResult?>(null) }
    var showTranslationResultDialog by remember { mutableStateOf(false) }

    var showTtsDialog by remember { mutableStateOf(false) }
    var ttsSpeechRate by remember { mutableStateOf(1.0f) }
    var ttsPitch by remember { mutableStateOf(1.0f) }
    var ttsReady by remember { mutableStateOf(false) }
    var ttsInitStatus by remember { mutableStateOf<Int?>(null) }
    var ttsErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSpeaking by remember { mutableStateOf(false) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    val mainThreadHandler = remember { Handler(Looper.getMainLooper()) }

    val ocrProcessor = remember { OcrProcessor() }
    val translationManager = remember { TranslationManager() }
    val currentMemoId by rememberUpdatedState(uiState.memoId)
    val currentMemoTitle by rememberUpdatedState(uiState.title)

    val selectedTextForDictionary by remember {
        derivedStateOf {
            val activeId = activeRichBlockId ?: return@derivedStateOf ""
            val state = richTextStates[activeId] ?: return@derivedStateOf ""
            val fullText = state.annotatedString.text
            if (fullText.isBlank()) return@derivedStateOf ""

            val selection = state.selection
            val start = selection.min.coerceIn(0, fullText.length)
            val end = selection.max.coerceIn(0, fullText.length)
            if (start >= end) return@derivedStateOf ""
            fullText.substring(start, end).trim()
        }
    }

    val liveCharCount by remember {
        derivedStateOf {
            val richBlockIds = memoBlocks.mapNotNull { block ->
                when (block) {
                    is MemoBlock.RichTextBlock -> block.id
                    is MemoBlock.TableBlock -> null
                }
            }
            if (richBlockIds.isEmpty()) {
                return@derivedStateOf currentPlainText.length
            }

            val blockTexts = richBlockIds.map { id ->
                val state = richTextStates[id] ?: return@derivedStateOf currentPlainText.length
                state.annotatedString.text
            }
            blockTexts.joinToString(separator = "\n").length
        }
    }

    suspend fun showInfoMessage(message: String) {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(
            message = message,
            duration = SnackbarDuration.Short,
        )
    }

    fun activeRichTextState(): RichTextState? {
        val byId = activeRichBlockId?.let { richTextStates[it] }
        if (byId != null) return byId

        val firstRichId = memoBlocks.firstOrNull { it is MemoBlock.RichTextBlock }?.id
        if (firstRichId != null) {
            activeRichBlockId = firstRichId
            return richTextStates[firstRichId]
        }

        return null
    }

    fun convertLegacyTableBlock(block: MemoBlock.TableBlock): MemoBlock.RichTextBlock {
        val plainText = block.cells
            .joinToString(separator = "\n") { row ->
                row.joinToString(separator = "\t").trimEnd()
            }
            .trim()
        return MemoBlock.RichTextBlock(html = plainTextToHtml(plainText))
    }

    fun normalizeDeprecatedBlocks(blocks: List<MemoBlock>): List<MemoBlock> {
        val normalized = blocks.map { block ->
            when (block) {
                is MemoBlock.RichTextBlock -> block
                is MemoBlock.TableBlock -> convertLegacyTableBlock(block)
            }
        }
        return ensureAtLeastOneRichBlock(normalized)
    }

    fun resolveCurrentBlocks(): List<MemoBlock> {
        return normalizeDeprecatedBlocks(
            memoBlocks.map { block ->
                when (block) {
                    is MemoBlock.RichTextBlock -> {
                        val latestHtml = richTextStates[block.id]?.toHtml() ?: block.html
                        block.copy(html = latestHtml)
                    }

                    is MemoBlock.TableBlock -> block
                }
            },
        )
    }

    fun updateBlocks(nextBlocks: List<MemoBlock>, recordHistory: Boolean = true) {
        val ensured = normalizeDeprecatedBlocks(nextBlocks)
        memoBlocks = ensured
        currentPlainText = memoBlocksToPlainText(ensured)
        if (activeRichBlockId == null || ensured.none { it.id == activeRichBlockId }) {
            activeRichBlockId = ensured.firstOrNull { it is MemoBlock.RichTextBlock }?.id
        }
        if (recordHistory) {
            undoSnapshotRaw = encodeMemoBlocks(ensured)
        }
    }

    fun appendTextToEditor(text: String) {
        if (text.isBlank()) return
        val targetState = activeRichTextState()
        if (targetState == null) {
            val newBlock = MemoBlock.RichTextBlock()
            updateBlocks(memoBlocks + newBlock)
            activeRichBlockId = newBlock.id
            scope.launch {
                delay(80)
                richTextStates[newBlock.id]?.addTextAfterSelection(text)
            }
            return
        }
        targetState.addTextAfterSelection(text)
    }

    fun openOcrDraft(text: String) {
        val formatted = OcrTextFormatter.formatAll(text)
        ocrFormattedText = formatted
        ocrDraftMode = OcrTextNormalizationMode.Normalized
        ocrDraftText = formatted.normalized
        showOcrResultDialog = true
    }

    fun logEditorEvent(message: String, throwable: Throwable? = null) {
        EditorDebugLog.log(
            context = context,
            category = "Editor/Persist",
            message = message,
            throwable = throwable,
        )
    }

    fun syncCurrentBlocksFromStates(reason: String, logIfChanged: Boolean = false): Boolean {
        if (isApplyingSnapshot) return false

        var changed = false
        val updatedBlocks = memoBlocks.map { block ->
            when (block) {
                is MemoBlock.RichTextBlock -> {
                    val latestHtml = richTextStates[block.id]?.toHtml() ?: block.html
                    if (latestHtml != block.html) {
                        changed = true
                        block.copy(html = latestHtml)
                    } else {
                        block
                    }
                }

                is MemoBlock.TableBlock -> block
            }
        }

        if (!changed) return false

        memoBlocks = updatedBlocks
        currentPlainText = memoBlocksToPlainText(updatedBlocks)
        undoSnapshotRaw = encodeMemoBlocks(updatedBlocks)
        if (logIfChanged) {
            logEditorEvent("block sync reason=$reason blocks=${updatedBlocks.size}")
        }
        return true
    }

    fun moveCursor(delta: Int) {
        val state = activeRichTextState() ?: return
        val length = state.annotatedString.text.length
        val target = (state.selection.end + delta).coerceIn(0, length)
        state.selection = TextRange(target)
    }

    fun adjustSelection(delta: Int) {
        val state = activeRichTextState() ?: return
        val length = state.annotatedString.text.length
        val selection = state.selection
        val start = selection.start.coerceIn(0, length)
        val nextEnd = (selection.end + delta).coerceIn(start, length)
        state.selection = TextRange(start, nextEnd)
    }

    fun openAddDictionaryDialog() {
        dictionaryEditTarget = null
        dictionaryLabelInput = ""
        dictionaryContentInput = ""
        dictionaryDialogTitle = "辞書を追加"
        showDictionaryEditorDialog = true
    }

    fun openEditDictionaryDialog(entry: DictionaryEntry) {
        dictionaryEditTarget = entry
        dictionaryLabelInput = entry.label
        dictionaryContentInput = entry.content
        dictionaryDialogTitle = "辞書を編集"
        showDictionaryEditorDialog = true
    }

    fun saveDictionaryEntry() {
        val label = dictionaryLabelInput.trim()
        val content = dictionaryContentInput.trim()
        if (label.isBlank() || content.isBlank()) {
            scope.launch {
                showInfoMessage("ラベルと内容を入力してください")
            }
            return
        }

        val editing = dictionaryEditTarget
        if (editing == null) {
            viewModel.addDictionaryEntry(label = label, content = content)
        } else {
            viewModel.updateDictionaryEntry(
                editing.copy(
                    label = label,
                    content = content,
                ),
            )
        }
        showDictionaryEditorDialog = false
        dictionaryEditTarget = null
        dictionaryDialogTitle = "辞書を追加"
    }

    fun copyFullTextToClipboard() {
        val plainText = memoBlocksToPlainText(resolveCurrentBlocks())
        clipboardManager.setText(AnnotatedString(plainText))
        scope.launch {
            showInfoMessage("メモをコピーしました")
        }
    }

    fun shareMemoText() {
        val plainText = memoBlocksToPlainText(resolveCurrentBlocks()).trim()
        val body = buildString {
            if (uiState.title.isNotBlank()) {
                append(uiState.title.trim())
                append("\n\n")
            }
            append(plainText)
        }.trim()

        if (body.isBlank()) {
            scope.launch {
                showInfoMessage("共有する内容がありません")
            }
            return
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(intent, "メモを共有"))
    }

    fun applyUndoRedoSnapshot(snapshot: String) {
        val restored = normalizeDeprecatedBlocks(
            decodeMemoBlocks(blocksJson = snapshot, fallbackHtml = ""),
        )
        isApplyingSnapshot = true
        viewModel.setUndoRedoRestoring(true)
        richTextStates.clear()
        memoBlocks = restored
        currentPlainText = memoBlocksToPlainText(restored)
        activeRichBlockId = restored.firstOrNull { it is MemoBlock.RichTextBlock }?.id
        undoSnapshotRaw = encodeMemoBlocks(restored)
        scope.launch {
            delay(120)
            viewModel.setUndoRedoRestoring(false)
            isApplyingSnapshot = false
        }
    }

    fun performUndo() {
        viewModel.setUndoRedoRestoring(true)
        val snapshot = viewModel.undoSnapshot()
        if (snapshot == null) {
            viewModel.setUndoRedoRestoring(false)
            return
        }
        logEditorEvent("undo memoId=${uiState.memoId}")
        applyUndoRedoSnapshot(snapshot)
    }

    fun performRedo() {
        viewModel.setUndoRedoRestoring(true)
        val snapshot = viewModel.redoSnapshot()
        if (snapshot == null) {
            viewModel.setUndoRedoRestoring(false)
            return
        }
        logEditorEvent("redo memoId=${uiState.memoId}")
        applyUndoRedoSnapshot(snapshot)
    }

    fun startTtsReading() {
        val text = currentPlainText.trim()
        if (text.isBlank()) {
            scope.launch {
                showInfoMessage("読み上げるテキストがありません")
            }
            return
        }

        val engine = ttsEngine
        if (engine == null || !ttsReady) {
            scope.launch {
                showInfoMessage(ttsErrorMessage ?: "読み上げ機能を初期化できませんでした")
            }
            return
        }

        engine.setSpeechRate(ttsSpeechRate)
        engine.setPitch(ttsPitch)
        val speakResult = engine.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "quickmemo_editor_tts",
        )
        if (speakResult == TextToSpeech.ERROR) {
            scope.launch {
                showInfoMessage("読み上げ開始に失敗しました")
            }
            isSpeaking = false
            return
        }
        isSpeaking = true
    }

    fun stopTtsReading() {
        ttsEngine?.stop()
        isSpeaking = false
    }

    suspend fun ensureTranslationReady(): Boolean {
        val statusBefore = translationManager.downloadStatus.value
        if (statusBefore !is TranslationManager.TranslationDownloadStatus.Ready) {
            translationManager.ensureModelsDownloaded()
        }

        return when (val status = translationManager.downloadStatus.value) {
            is TranslationManager.TranslationDownloadStatus.Ready -> true
            is TranslationManager.TranslationDownloadStatus.Error -> {
                showInfoMessage(status.message)
                false
            }

            else -> {
                showInfoMessage("翻訳モデルを準備できませんでした")
                false
            }
        }
    }

    fun runTranslationFlow(sourceText: String, insertImmediately: Boolean) {
        val targetText = sourceText.trim()
        if (targetText.isBlank()) {
            scope.launch {
                showInfoMessage("翻訳対象のテキストがありません")
            }
            return
        }

        scope.launch {
            isTranslationLoading = true
            val ready = ensureTranslationReady()
            if (!ready) {
                isTranslationLoading = false
                return@launch
            }

            val result = runCatching { translationManager.autoTranslate(targetText) }
            isTranslationLoading = false

            result.onSuccess { translated ->
                if (insertImmediately) {
                    appendTextToEditor(translated.translatedText)
                } else {
                    translationResult = translated
                    showTranslationResultDialog = true
                }
            }.onFailure {
                showInfoMessage("翻訳に失敗しました")
            }
        }
    }

    fun processOcrUri(uri: Uri) {
        scope.launch {
            isOcrLoading = true
            val result = ocrProcessor.recognizeFromUri(context, uri)
            when (result) {
                is OcrResult.Success -> {
                    val recognizedText = result.text.trim()
                    if (recognizedText.isBlank()) {
                        showInfoMessage("テキストを認識できませんでした")
                    } else {
                        openOcrDraft(recognizedText)
                        EditorDebugLog.log(
                            context = context,
                            category = "Editor/OCR",
                            message = "ocr success length=${recognizedText.length}",
                        )
                    }
                }

                is OcrResult.Error -> {
                    EditorDebugLog.log(
                        context = context,
                        category = "Editor/OCR",
                        message = "ocr failed",
                    )
                    showInfoMessage(result.message)
                }
            }
            isOcrLoading = false
        }
    }

    suspend fun persistCurrentMemo(reason: String) {
        syncCurrentBlocksFromStates(reason = reason)
        val blocksToSave = resolveCurrentBlocks()
        val mergedHtml = blocksToSave
            .filterIsInstance<MemoBlock.RichTextBlock>()
            .joinToString(separator = "\n") { it.html }
            .trim()

        runCatching {
            viewModel.saveMemo(
                contentHtml = mergedHtml,
                contentPlainText = memoBlocksToPlainText(blocksToSave),
                blocks = blocksToSave,
            )
        }.onSuccess { savedId ->
            val resolvedMemoId = savedId ?: uiState.memoId
            logEditorEvent(
                message = "persist reason=$reason memoId=$resolvedMemoId blocks=${blocksToSave.size} plain=${memoBlocksToPlainText(blocksToSave).length}",
            )
        }.onFailure { throwable ->
            logEditorEvent(
                message = "persist failed reason=$reason memoId=${uiState.memoId}",
                throwable = throwable,
            )
        }
    }

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            showOcrSourceSheet = true
        } else {
            scope.launch {
                showInfoMessage("カメラの権限が必要です")
            }
        }
    }

    val cropImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val resultUri = result.data?.getStringExtra(OcrCropActivity.EXTRA_RESULT_URI)?.let(Uri::parse)
        if (result.resultCode == Activity.RESULT_OK && resultUri != null) {
            processOcrUri(resultUri)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            pendingCameraUri?.let { sourceUri ->
                cropImageLauncher.launch(OcrCropActivity.createIntent(context, sourceUri))
            }
        } else {
            scope.launch {
                showInfoMessage("撮影をキャンセルしました")
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            cropImageLauncher.launch(OcrCropActivity.createIntent(context, uri))
        }
    }

    LaunchedEffect(uiState.memoId, uiState.initialContentHtml, uiState.initialBlocks) {
        val signature = buildString {
            append(uiState.memoId)
            append(':')
            append(uiState.initialContentHtml.hashCode())
            append(':')
            append(uiState.initialBlocks.hashCode())
        }

        if (loadedSignature == signature) return@LaunchedEffect

        loadedSignature = signature
        lastCalcInsertedFlag = false
        isApplyingSnapshot = true
        richTextStates.clear()

        val initialBlocks = normalizeDeprecatedBlocks(
            if (uiState.initialBlocks.isEmpty()) {
                createDefaultMemoBlocks(uiState.initialContentHtml)
            } else {
                uiState.initialBlocks
            },
        )
        memoBlocks = initialBlocks
        currentPlainText = memoBlocksToPlainText(initialBlocks)
        activeRichBlockId = initialBlocks.firstOrNull { it is MemoBlock.RichTextBlock }?.id

        val initialSnapshot = encodeMemoBlocks(initialBlocks)
        undoSnapshotRaw = initialSnapshot
        viewModel.initializeUndoRedo(initialSnapshot)
        logEditorEvent("load memoId=${uiState.memoId} blocks=${initialBlocks.size}")

        delay(120)
        isApplyingSnapshot = false
    }

    LaunchedEffect(loadedSignature) {
        snapshotFlow { undoSnapshotRaw }
            .filter { it.isNotBlank() }
            .debounce(500)
            .collect { snapshot ->
                viewModel.saveUndoSnapshot(snapshot)

                val blocksToBackup = resolveCurrentBlocks()
                val mergedHtml = blocksToBackup
                    .filterIsInstance<MemoBlock.RichTextBlock>()
                    .joinToString(separator = "\n") { it.html }
                    .trim()
                viewModel.onEditorSnapshotChanged(
                    memoId = currentMemoId,
                    title = currentMemoTitle,
                    contentHtml = mergedHtml,
                )
            }
    }

    LaunchedEffect(loadedSignature) {
        while (loadedSignature.isNotBlank()) {
            delay(3_000)
            syncCurrentBlocksFromStates(reason = "poll", logIfChanged = true)
        }
    }

    LaunchedEffect(ttsInitStatus, ttsEngine) {
        when (ttsInitStatus) {
            TextToSpeech.SUCCESS -> {
                val engine = ttsEngine ?: return@LaunchedEffect
                val languageResult = engine.setLanguage(Locale.JAPANESE)
                val available = languageResult != TextToSpeech.LANG_MISSING_DATA &&
                    languageResult != TextToSpeech.LANG_NOT_SUPPORTED
                ttsReady = available
                ttsErrorMessage = if (available) {
                    null
                } else {
                    "日本語の読み上げ音声が利用できません"
                }
            }

            TextToSpeech.ERROR -> {
                ttsReady = false
                ttsErrorMessage = "読み上げ機能を初期化できませんでした"
            }

            else -> Unit
        }
    }

    BackHandler {
        scope.launch {
            logEditorEvent("back pressed memoId=${uiState.memoId}")
            persistCurrentMemo(reason = "back_handler")
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.ttsEnabled) {
        if (!uiState.ttsEnabled) {
            stopTtsReading()
            showTtsDialog = false
            ttsReady = false
            ttsInitStatus = null
            ttsErrorMessage = null
        }
    }

    DisposableEffect(context, uiState.ttsEnabled) {
        if (!uiState.ttsEnabled) {
            ttsEngine = null
            onDispose {
            }
        } else {
            val engine = TextToSpeech(context.applicationContext) { status ->
                mainThreadHandler.post {
                    ttsInitStatus = status
                }
            }
            engine.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        mainThreadHandler.post { isSpeaking = true }
                    }

                    override fun onDone(utteranceId: String?) {
                        mainThreadHandler.post { isSpeaking = false }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        mainThreadHandler.post { isSpeaking = false }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        mainThreadHandler.post { isSpeaking = false }
                    }
                },
            )
            ttsEngine = engine

            onDispose {
                runCatching {
                    engine.stop()
                    engine.shutdown()
                }
                ttsEngine = null
                ttsReady = false
                ttsInitStatus = null
                isSpeaking = false
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                scope.launch {
                    logEditorEvent("lifecycle onStop memoId=${uiState.memoId}")
                    persistCurrentMemo(reason = "lifecycle_on_stop")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ocrProcessor.close()
            translationManager.close()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Scaffold(
            containerColor = editorBackgroundColor,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = {
                TopAppBar(
                    title = { Text("メモ編集") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    logEditorEvent("back icon memoId=${uiState.memoId}")
                                    persistCurrentMemo(reason = "top_bar_back")
                                    onNavigateBack()
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "back",
                            )
                        }
                    },
                    actions = {
                        if (uiState.memoToolbarSettings.share) {
                            IconButton(onClick = ::shareMemoText) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "share",
                                )
                            }
                        }

                        IconButton(onClick = onOpenTodo) {
                            Icon(
                                imageVector = Icons.Outlined.CheckBox,
                                contentDescription = "todo",
                            )
                        }

                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "settings",
                            )
                        }
                    },
                )
            },
            bottomBar = {
                EditorMainBar(
                    onCopyFullText = ::copyFullTextToClipboard,
                    onUndo = ::performUndo,
                    onRedo = ::performRedo,
                    onRunOcr = {
                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA,
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasCameraPermission) {
                            showOcrSourceSheet = true
                        } else {
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onOpenDictionary = {
                        showDictionarySheet = true
                    },
                    onOpenTranslation = {
                        runTranslationFlow(
                            sourceText = currentPlainText,
                            insertImmediately = false,
                        )
                    },
                    onPickDate = {
                        showDatePicker(context) { timestamp ->
                            val dateText = DateTimeUtils.formatInsertDate(timestamp)
                            val resultText = if (uiState.insertCurrentTimeWithDate) {
                                "$dateText ${DateTimeUtils.formatInsertTime(System.currentTimeMillis())}"
                            } else {
                                dateText
                            }
                            appendTextToEditor(resultText)
                        }
                    },
                    onPickTime = {
                        showTimePicker(context) { hour, minute ->
                            appendTextToEditor(String.format("%02d:%02d", hour, minute))
                        }
                    },
                    onOpenTts = {
                        showTtsDialog = true
                    },
                    onOpenCalculator = {
                        showCalculatorSheet = true
                    },
                    showFullCopy = uiState.memoToolbarSettings.fullCopy,
                    showUndoRedo = uiState.memoToolbarSettings.undoRedo,
                    showOcr = uiState.memoToolbarSettings.ocr,
                    showDictionary = true,
                    showTranslation = uiState.memoToolbarSettings.translation,
                    showDateTimeInsert = uiState.memoToolbarSettings.dateTimeInsert,
                    showTts = uiState.ttsEnabled,
                    showCalculator = uiState.memoToolbarSettings.calculator,
                    canUndo = uiState.canUndo,
                    canRedo = uiState.canRedo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding(),
                    containerColor = editorBackgroundColor,
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                HorizontalDivider()

                TextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    placeholder = {
                        Text(text = "タイトル（任意）")
                    },
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true,
                )

                EditorFormatBar(
                    richTextState = activeRichTextState(),
                    showTextSize = uiState.memoToolbarSettings.textSize,
                    showBoldItalic = uiState.memoToolbarSettings.boldItalic,
                    showTextColor = uiState.memoToolbarSettings.textColor,
                    showHighlighter = uiState.memoToolbarSettings.highlighter,
                )

                if (activeRichTextState() != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { moveCursor(-1) }) {
                            Text("←")
                        }
                        TextButton(onClick = { moveCursor(1) }) {
                            Text("→")
                        }
                        TextButton(onClick = { adjustSelection(-1) }) {
                            Text("選択-")
                        }
                        TextButton(onClick = { adjustSelection(1) }) {
                            Text("選択+")
                        }
                    }
                }

                if (selectedTextForDictionary.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = {
                                dictionaryEditTarget = null
                                dictionaryLabelInput = selectedTextForDictionary
                                dictionaryContentInput = selectedTextForDictionary
                                dictionaryDialogTitle = "単語辞書に登録"
                                showDictionaryEditorDialog = true
                            },
                            modifier = Modifier.focusProperties { canFocus = false },
                        ) {
                            Icon(
                                painter = painterResource(id = com.quickmemo.app.R.drawable.ic_dictionary),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(text = "辞書に登録")
                        }
                    }
                }

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(
                        items = memoBlocks,
                        key = { it.id },
                    ) { block ->
                        when (block) {
                            is MemoBlock.RichTextBlock -> {
                                val state = remember(block.id) { RichTextState() }

                                DisposableEffect(block.id) {
                                    richTextStates[block.id] = state
                                    onDispose {
                                        richTextStates.remove(block.id)
                                    }
                                }

                                LaunchedEffect(block.id, block.html, loadedSignature) {
                                    if (state.toHtml() != block.html) {
                                        state.setHtml(block.html)
                                    }
                                }

                                LaunchedEffect(block.id, state, loadedSignature) {
                                    snapshotFlow { state.annotatedString.text }
                                        .collect {
                                            if (isApplyingSnapshot) return@collect
                                            syncCurrentBlocksFromStates(reason = "text_change")
                                        }
                                }

                                RichTextEditor(
                                    state = state,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 180.dp)
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                activeRichBlockId = block.id
                                            }
                                        },
                                )
                            }

                            is MemoBlock.TableBlock -> Unit
                        }
                    }
                }

                if (uiState.showCharacterCount) {
                    Text(
                        text = "${liveCharCount}文字",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        if (isOcrLoading || isTranslationLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = if (isOcrLoading) "テキストを認識中..." else "翻訳中...",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    if (showOcrSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOcrSourceSheet = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "OCR",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                )

                TextButton(
                    onClick = {
                        showOcrSourceSheet = false
                        val uri = createOcrImageUri(context)
                        if (uri == null) {
                            scope.launch {
                                showInfoMessage("カメラ画像の準備に失敗しました")
                            }
                            return@TextButton
                        }
                        pendingCameraUri = uri
                        takePictureLauncher.launch(uri)
                    },
                ) {
                    Text("カメラで撮影")
                }

                TextButton(
                    onClick = {
                        showOcrSourceSheet = false
                        pickImageLauncher.launch("image/*")
                    },
                ) {
                    Text("画像から選択")
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    if (showDictionarySheet) {
        DictionaryBottomSheet(
            entries = dictionaryEntries,
            onInsert = { content ->
                appendTextToEditor(content)
                showDictionarySheet = false
            },
            onAddNew = {
                showDictionarySheet = false
                openAddDictionaryDialog()
            },
            onEdit = { entry ->
                showDictionarySheet = false
                openEditDictionaryDialog(entry)
            },
            onDelete = { entry ->
                showDictionarySheet = false
                dictionaryDeleteTarget = entry
            },
            onDismiss = { showDictionarySheet = false },
        )
    }

    if (showDictionaryEditorDialog) {
        val isEditing = dictionaryEditTarget != null
        AlertDialog(
            onDismissRequest = {
                showDictionaryEditorDialog = false
                dictionaryEditTarget = null
                dictionaryDialogTitle = "辞書を追加"
            },
            title = {
                Text(if (isEditing) "辞書を編集" else dictionaryDialogTitle)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dictionaryLabelInput,
                        onValueChange = { dictionaryLabelInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("ラベル") },
                    )
                    OutlinedTextField(
                        value = dictionaryContentInput,
                        onValueChange = { dictionaryContentInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        label = { Text("内容") },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = ::saveDictionaryEntry) {
                    Text(if (isEditing) "更新" else "追加")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDictionaryEditorDialog = false
                        dictionaryEditTarget = null
                        dictionaryDialogTitle = "辞書を追加"
                    },
                ) {
                    Text("キャンセル")
                }
            },
        )
    }

    dictionaryDeleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { dictionaryDeleteTarget = null },
            title = { Text("辞書を削除") },
            text = { Text("「${target.label}」を削除しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDictionaryEntry(target)
                        dictionaryDeleteTarget = null
                    },
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { dictionaryDeleteTarget = null }) {
                    Text("キャンセル")
                }
            },
        )
    }

    if (showTtsDialog) {
        AlertDialog(
            onDismissRequest = { showTtsDialog = false },
            title = { Text("読み上げ") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("速度: ${String.format(Locale.US, "%.1f", ttsSpeechRate)}x")
                    Slider(
                        value = ttsSpeechRate,
                        onValueChange = { ttsSpeechRate = it },
                        valueRange = 0.5f..2.0f,
                    )

                    Text("ピッチ: ${String.format(Locale.US, "%.1f", ttsPitch)}")
                    Slider(
                        value = ttsPitch,
                        onValueChange = { ttsPitch = it },
                        valueRange = 0.5f..2.0f,
                    )

                    val message = ttsErrorMessage
                    if (!message.isNullOrBlank()) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                Row {
                    if (isSpeaking) {
                        TextButton(onClick = ::stopTtsReading) {
                            Text("停止")
                        }
                    } else {
                        TextButton(onClick = ::startTtsReading) {
                            Text("読み上げ")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTtsDialog = false }) {
                    Text("閉じる")
                }
            },
        )
    }

    if (showCalculatorSheet) {
        CalculatorBottomSheet(
            cursorContextText = buildCursorContextText(activeRichTextState()),
            taxRate = uiState.taxRate,
            history = calcHistory,
            onInsertExpressionAndResult = { expression, result ->
                val prefix = if (lastCalcInsertedFlag) "\n" else ""
                val normalizedExpression = expression
                    .replace("&times;", "×")
                    .replace("&divide;", "÷")
                    .replace("&equals;", "=")
                    .replace("&comma;", ",")
                val normalizedResult = result
                    .replace("&equals;", "=")
                    .replace("&comma;", ",")
                appendTextToEditor("$prefix$normalizedExpression=$normalizedResult")
                lastCalcInsertedFlag = true
                showCalculatorSheet = false
            },
            onInsertResultOnly = { result ->
                val prefix = if (lastCalcInsertedFlag) "\n" else ""
                val normalizedResult = result
                    .replace("&equals;", "=")
                    .replace("&comma;", ",")
                appendTextToEditor("$prefix=$normalizedResult")
                lastCalcInsertedFlag = true
                showCalculatorSheet = false
            },
            onDismiss = { showCalculatorSheet = false },
        )
    }

    if (showOcrResultDialog) {
        val formatted = ocrFormattedText
        AlertDialog(
            onDismissRequest = {
                showOcrResultDialog = false
                ocrFormattedText = null
            },
            title = { Text("OCR結果") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        listOf(
                            OcrTextNormalizationMode.Raw to "Raw",
                            OcrTextNormalizationMode.Normalized to "整形",
                            OcrTextNormalizationMode.Bulletized to "箇条書き",
                        ).forEach { (mode, label) ->
                            TextButton(
                                onClick = {
                                    ocrDraftMode = mode
                                    ocrDraftText = formatted?.textFor(mode).orEmpty()
                                },
                            ) {
                                Text(if (ocrDraftMode == mode) "[$label]" else label)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = ocrDraftText,
                        onValueChange = { ocrDraftText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 6,
                        maxLines = 14,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        appendTextToEditor(ocrDraftText)
                        showOcrResultDialog = false
                        ocrFormattedText = null
                    },
                ) {
                    Text("挿入")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            runTranslationFlow(
                                sourceText = ocrDraftText,
                                insertImmediately = true,
                            )
                            showOcrResultDialog = false
                            ocrFormattedText = null
                        },
                    ) {
                        Text("翻訳して挿入")
                    }

                    TextButton(
                        onClick = {
                            showOcrResultDialog = false
                            ocrFormattedText = null
                        },
                    ) {
                        Text("キャンセル")
                    }
                }
            },
        )
    }

    if (showTranslationResultDialog && translationResult != null) {
        val currentResult = translationResult
        AlertDialog(
            onDismissRequest = { showTranslationResultDialog = false },
            title = {
                Text("翻訳結果（${currentResult?.sourceLanguage}→${currentResult?.targetLanguage}）")
            },
            text = {
                OutlinedTextField(
                    value = currentResult?.translatedText.orEmpty(),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    minLines = 5,
                    maxLines = 12,
                )
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentResult?.translatedText.orEmpty()))
                            scope.launch {
                                showInfoMessage("翻訳結果をコピーしました")
                            }
                        },
                    ) {
                        Text("コピー")
                    }
                    TextButton(
                        onClick = {
                            appendTextToEditor(currentResult?.translatedText.orEmpty())
                            showTranslationResultDialog = false
                        },
                    ) {
                        Text("挿入")
                    }
                    TextButton(
                        onClick = {
                            appendTextToEditor(currentResult?.translatedText.orEmpty())
                            showTranslationResultDialog = false
                        },
                    ) {
                        Text("置換")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTranslationResultDialog = false }) {
                    Text("閉じる")
                }
            },
        )
    }
}

internal fun parseCalculatorExpression(source: String): BigDecimal {
    return CalculatorExpressionParser(source).parse()
}

internal fun formatCalculatorResult(value: BigDecimal): String {
    return value.stripTrailingZeros().toPlainString()
}

internal class CalculatorExpressionParser(
    private val source: String,
) {
    private var index: Int = 0

    fun parse(): BigDecimal {
        val value = parseExpression()
        skipWhitespace()
        if (index != source.length) {
            throw IllegalArgumentException("式に無効な文字があります")
        }
        return value
    }

    private fun parseExpression(): BigDecimal {
        var value = parseTerm()
        while (true) {
            skipWhitespace()
            value = when {
                consume('+') -> value.add(parseTerm())
                consume('-') -> value.subtract(parseTerm())
                else -> return value
            }
        }
    }

    private fun parseTerm(): BigDecimal {
        var value = parseFactor()
        while (true) {
            skipWhitespace()
            value = when {
                consume('*') -> value.multiply(parseFactor())
                consume('/') -> {
                    val divisor = parseFactor()
                    if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                        throw IllegalArgumentException("0で割ることはできません")
                    }
                    value.divide(divisor, 12, RoundingMode.HALF_UP).stripTrailingZeros()
                }

                else -> return value
            }
        }
    }

    private fun parseFactor(): BigDecimal {
        skipWhitespace()
        return when {
            consume('+') -> parseFactor()
            consume('-') -> parseFactor().negate()
            consume('(') -> {
                val value = parseExpression()
                skipWhitespace()
                if (!consume(')')) {
                    throw IllegalArgumentException("閉じ括弧 ')' が不足しています")
                }
                value
            }

            else -> parseNumber()
        }
    }

    private fun parseNumber(): BigDecimal {
        skipWhitespace()
        val start = index
        var hasDot = false

        while (index < source.length) {
            val current = source[index]
            when {
                current.isDigit() -> index += 1
                current == '.' && !hasDot -> {
                    hasDot = true
                    index += 1
                }

                else -> break
            }
        }

        if (start == index) {
            throw IllegalArgumentException("数値の形式が不正です")
        }

        val token = source.substring(start, index)
        if (token == ".") {
            throw IllegalArgumentException("数値の形式が不正です")
        }

        return token.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("数値の形式が不正です")
    }

    private fun consume(expected: Char): Boolean {
        if (index < source.length && source[index] == expected) {
            index += 1
            return true
        }
        return false
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index].isWhitespace()) {
            index += 1
        }
    }
}

private fun buildCursorContextText(richTextState: RichTextState?): String {
    if (richTextState == null) return ""
    val plainText = richTextState.annotatedString.text
    val cursorPos = richTextState.selection.start.coerceIn(0, plainText.length)
    val beforeStart = (cursorPos - 20).coerceAtLeast(0)
    val afterEnd = (cursorPos + 20).coerceAtMost(plainText.length)
    val before = plainText.substring(beforeStart, cursorPos)
    val after = plainText.substring(cursorPos, afterEnd)
    val prefix = if (beforeStart > 0) "…" else ""
    val suffix = if (afterEnd < plainText.length) "…" else ""
    return "$prefix$before【▍】$after$suffix"
}

private fun ensureAtLeastOneRichBlock(blocks: List<MemoBlock>): List<MemoBlock> {
    if (blocks.any { it is MemoBlock.RichTextBlock }) {
        return blocks
    }
    return listOf(MemoBlock.RichTextBlock()) + blocks
}

private fun createOcrImageUri(context: Context): Uri? {
    return runCatching {
        val dir = File(context.cacheDir, "quickmemo_images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val imageFile = File.createTempFile(
            "ocr_capture_",
            ".jpg",
            dir,
        )
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile,
        )
    }.getOrNull()
}

private fun showDatePicker(
    context: Context,
    onPicked: (Long) -> Unit,
) {
    val calendar = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val picked = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onPicked(picked.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH),
    ).show()
}

private fun showTimePicker(
    context: Context,
    onPicked: (Int, Int) -> Unit,
) {
    val calendar = Calendar.getInstance()
    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            onPicked(hourOfDay, minute)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true,
    ).show()
}
