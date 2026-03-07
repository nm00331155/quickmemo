// File: app/src/main/java/com/quickmemo/app/presentation/settings/SettingsScreen.kt
package com.quickmemo.app.presentation.settings

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickmemo.app.domain.model.MemoColor
import com.quickmemo.app.domain.model.MemoToolbarFeature
import com.quickmemo.app.domain.model.ThemeMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initialTab: Int,
    onBack: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenTodo: () -> Unit,
    onToggleQuickService: (Boolean) -> Unit,
    onToggleTodoReminder: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val pagerState = rememberPagerState(
        initialPage = initialTab.coerceIn(0, 2),
        pageCount = { 3 },
    )

    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshPurchases()
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val backup = viewModel.createBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(backup.toByteArray(Charsets.UTF_8))
                } ?: error("バックアップファイルを開けませんでした")
                viewModel.setLastBackupDateTimeNow()
            }.onSuccess {
                snackbarHostState.showSnackbar("バックアップを作成しました")
            }.onFailure {
                snackbarHostState.showSnackbar("バックアップ作成に失敗しました")
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.onSuccess { content ->
                if (!content.isNullOrBlank()) {
                    pendingRestoreJson = content
                } else {
                    snackbarHostState.showSnackbar("バックアップファイルが空です")
                }
            }.onFailure {
                snackbarHostState.showSnackbar("バックアップ読込に失敗しました")
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenTodo) {
                        Icon(
                            imageVector = Icons.Outlined.CheckBox,
                            contentDescription = "todo",
                        )
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                listOf("全般", "メモ", "Todo").forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title) },
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> {
                        GeneralSettingsTab(
                            uiState = uiState,
                            onThemeSelected = viewModel::setThemeMode,
                            onQuickInputChanged = {
                                viewModel.setQuickInputNotification(it)
                                onToggleQuickService(it)
                            },
                            onRequireAuthChanged = viewModel::setRequireAuthOnLaunch,
                            onCreateBackup = {
                                val filename = "quickmemo_backup_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.json"
                                createBackupLauncher.launch(filename)
                            },
                            onOpenBackup = {
                                openBackupLauncher.launch(arrayOf("application/json", "text/plain"))
                            },
                            onOpenTrash = onOpenTrash,
                            onOpenPremium = onOpenPremium,
                            onPurchaseRemoveAds = {
                                activity?.let { viewModel.purchaseRemoveAds(it) }
                            },
                            onOpenContact = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:support@quickmemo.app")
                                    putExtra(Intent.EXTRA_SUBJECT, "QuickMemo お問い合わせ")
                                }
                                context.startActivity(intent)
                            },
                        )
                    }

                    1 -> {
                        MemoSettingsTab(
                            uiState = uiState,
                            onDefaultColorSelected = viewModel::setDefaultMemoColor,
                            onShowCharCountChanged = viewModel::setShowCharacterCount,
                            onDateIncludeTimeChanged = viewModel::setInsertCurrentTimeWithDate,
                            onToolbarFeatureChanged = viewModel::setMemoToolbarFeature,
                            onTaxRateChanged = viewModel::setTaxRate,
                        )
                    }

                    2 -> {
                        TodoSettingsTab(
                            uiState = uiState,
                            onReminderEnabledChanged = {
                                viewModel.setTodoReminderEnabled(it)
                                onToggleTodoReminder(it)
                            },
                            onReminderCustomCheckedChanged = { enabled ->
                                if (enabled) {
                                    val current = uiState.settings.reminderSettings.customHoursBefore ?: 2
                                    viewModel.setTodoReminderCustomHours(current)
                                    showHoursPicker(
                                        context = context,
                                        initialHours = current,
                                        onSelected = { hours ->
                                            viewModel.setTodoReminderCustomHours(hours)
                                        },
                                    )
                                } else {
                                    viewModel.setTodoReminderCustomHours(null)
                                }
                            },
                            onEditCustomHours = {
                                val current = uiState.settings.reminderSettings.customHoursBefore ?: 2
                                showHoursPicker(
                                    context = context,
                                    initialHours = current,
                                    onSelected = { hours ->
                                        viewModel.setTodoReminderCustomHours(hours)
                                    },
                                )
                            },
                            onReminderOneDayChanged = viewModel::setTodoReminderOneDay,
                            onReminderThreeDaysChanged = viewModel::setTodoReminderThreeDays,
                            onReminderOneWeekChanged = viewModel::setTodoReminderOneWeek,
                            onTabNameChanged = viewModel::setTodoTabName,
                        )
                    }
                }
            }
        }
    }

    val restoreJson = pendingRestoreJson
    if (restoreJson != null) {
        AlertDialog(
            onDismissRequest = { pendingRestoreJson = null },
            title = { Text("バックアップを復元") },
            text = { Text("現在のデータを上書きしますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRestoreJson = null
                        scope.launch {
                            val result = viewModel.restoreFromBackupJson(restoreJson)
                            if (result.isSuccess) {
                                snackbarHostState.showSnackbar("バックアップを復元しました")
                            } else {
                                snackbarHostState.showSnackbar("バックアップ復元に失敗しました")
                            }
                        }
                    },
                ) {
                    Text("復元")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreJson = null }) {
                    Text("キャンセル")
                }
            },
        )
    }
}

@Composable
private fun GeneralSettingsTab(
    uiState: SettingsUiState,
    onThemeSelected: (ThemeMode) -> Unit,
    onQuickInputChanged: (Boolean) -> Unit,
    onRequireAuthChanged: (Boolean) -> Unit,
    onCreateBackup: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenPremium: () -> Unit,
    onPurchaseRemoveAds: () -> Unit,
    onOpenContact: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionTitle("表示")
            ThemeRadioRow(
                selected = uiState.settings.themeMode,
                onSelected = onThemeSelected,
            )
        }

        item {
            SectionTitle("通知")
            SettingSwitchRow(
                title = "通知バーにクイック入力を表示",
                checked = uiState.settings.quickInputNotificationEnabled,
                onCheckedChange = onQuickInputChanged,
            )
        }

        item {
            SectionTitle("セキュリティ")
            SettingSwitchRow(
                title = "アプリ起動時に認証を要求",
                checked = uiState.settings.requireAuthOnLaunch,
                onCheckedChange = onRequireAuthChanged,
            )
        }

        item {
            SectionTitle("バックアップ")
            TextButton(onClick = onCreateBackup) { Text("バックアップ作成") }
            TextButton(onClick = onOpenBackup) { Text("バックアップ復元") }
            Text(
                text = uiState.settings.lastBackupDateTime?.let { "最終バックアップ: $it" } ?: "バックアップ未実施",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            SectionTitle("ゴミ箱")
            TextButton(onClick = onOpenTrash) { Text("ゴミ箱を開く") }
        }

        item {
            SectionTitle("課金")
            TextButton(onClick = onOpenPremium) { Text("プレミアム機能") }
            TextButton(onClick = onPurchaseRemoveAds) { Text("広告非表示を購入") }
        }

        item {
            SectionTitle("その他")
            Text("バージョン: ${uiState.appVersion}")
            Text("ライセンス情報")
            TextButton(onClick = onOpenContact) { Text("お問い合わせ") }
        }
    }
}

@Composable
private fun MemoSettingsTab(
    uiState: SettingsUiState,
    onDefaultColorSelected: (Int) -> Unit,
    onShowCharCountChanged: (Boolean) -> Unit,
    onDateIncludeTimeChanged: (Boolean) -> Unit,
    onToolbarFeatureChanged: (MemoToolbarFeature, Boolean) -> Unit,
    onTaxRateChanged: (Double) -> Unit,
) {
    val toolbar = uiState.settings.memoToolbarSettings
    var taxRateInput by remember(uiState.settings.calculatorTaxRate) {
        mutableStateOf(formatTaxRateText(uiState.settings.calculatorTaxRate))
    }
    var taxRateError by remember { mutableStateOf<String?>(null) }

    fun commitTaxRate() {
        val parsed = taxRateInput.toDoubleOrNull()
        if (parsed == null || parsed !in 0.0..100.0) {
            taxRateError = "0〜100 の範囲で入力してください"
            return
        }
        onTaxRateChanged(parsed)
        taxRateError = null
        taxRateInput = formatTaxRateText(parsed)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionTitle("デフォルトメモ色")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MemoColor.entries.forEach { memoColor ->
                    val isSelected = uiState.settings.defaultMemoColor == memoColor.index
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(memoColor.lightColor, shape = CircleShape)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape,
                            )
                            .clickable { onDefaultColorSelected(memoColor.index) },
                    )
                }
            }
        }

        item {
            SettingSwitchRow(
                title = "文字カウントを表示",
                checked = uiState.settings.showCharacterCount,
                onCheckedChange = onShowCharCountChanged,
            )
        }

        item {
            SettingSwitchRow(
                title = "日付挿入時に現在時刻も追加",
                checked = uiState.settings.insertCurrentTimeWithDate,
                onCheckedChange = onDateIncludeTimeChanged,
            )
        }

        item {
            SectionTitle("電卓")
            OutlinedTextField(
                value = taxRateInput,
                onValueChange = {
                    taxRateInput = it
                    taxRateError = null
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("電卓の税率") },
                placeholder = { Text("10") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { commitTaxRate() },
                ),
                supportingText = {
                    Text(
                        taxRateError
                            ?: "0〜100。現在 ${formatTaxRateText(uiState.settings.calculatorTaxRate)}%",
                    )
                },
                suffix = { Text("%") },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { commitTaxRate() }) {
                    Text("保存")
                }
            }
        }

        item {
            SectionTitle("メモ機能のオン/オフ")
        }

        items(
            items = listOf(
                "太字/斜体" to (MemoToolbarFeature.BOLD_ITALIC to toolbar.boldItalic),
                "文字色" to (MemoToolbarFeature.TEXT_COLOR to toolbar.textColor),
                "蛍光ペン" to (MemoToolbarFeature.HIGHLIGHTER to toolbar.highlighter),
                "文字サイズ" to (MemoToolbarFeature.TEXT_SIZE to toolbar.textSize),
                "Undo/Redo" to (MemoToolbarFeature.UNDO_REDO to toolbar.undoRedo),
                "日付時刻挿入" to (MemoToolbarFeature.DATETIME_INSERT to toolbar.dateTimeInsert),
                "電卓" to (MemoToolbarFeature.CALCULATOR to toolbar.calculator),
                "翻訳" to (MemoToolbarFeature.TRANSLATION to toolbar.translation),
                "OCR" to (MemoToolbarFeature.OCR to toolbar.ocr),
                "共有" to (MemoToolbarFeature.SHARE to toolbar.share),
                "全文コピー" to (MemoToolbarFeature.FULL_COPY to toolbar.fullCopy),
            ),
        ) { (title, pair) ->
            val feature = pair.first
            val checked = pair.second
            SettingSwitchRow(
                title = title,
                checked = checked,
                onCheckedChange = { onToolbarFeatureChanged(feature, it) },
            )
        }
    }
}

@Composable
private fun TodoSettingsTab(
    uiState: SettingsUiState,
    onReminderEnabledChanged: (Boolean) -> Unit,
    onReminderCustomCheckedChanged: (Boolean) -> Unit,
    onEditCustomHours: () -> Unit,
    onReminderOneDayChanged: (Boolean) -> Unit,
    onReminderThreeDaysChanged: (Boolean) -> Unit,
    onReminderOneWeekChanged: (Boolean) -> Unit,
    onTabNameChanged: (Int, String) -> Unit,
) {
    val reminder = uiState.settings.reminderSettings
    val customEnabled = reminder.customHoursBefore != null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionTitle("リマインド通知")
            SettingSwitchRow(
                title = "通知ON/OFF",
                checked = uiState.settings.todoReminderEnabled,
                onCheckedChange = onReminderEnabledChanged,
            )
        }

        item {
            ReminderCheckRow(
                title = "任意の時間前",
                checked = customEnabled,
                onCheckedChange = onReminderCustomCheckedChanged,
                trailing = if (customEnabled) {
                    "${reminder.customHoursBefore ?: 2}時間前"
                } else {
                    null
                },
                onTrailingClick = if (customEnabled) onEditCustomHours else null,
            )
            ReminderCheckRow(
                title = "1日前",
                checked = reminder.oneDayBefore,
                onCheckedChange = onReminderOneDayChanged,
            )
            ReminderCheckRow(
                title = "3日前",
                checked = reminder.threeDaysBefore,
                onCheckedChange = onReminderThreeDaysChanged,
            )
            ReminderCheckRow(
                title = "1週間前",
                checked = reminder.oneWeekBefore,
                onCheckedChange = onReminderOneWeekChanged,
            )
        }

        item {
            SectionTitle("Todoタブ名称")
        }

        items(3) { index ->
            OutlinedTextField(
                value = uiState.todoTabNames.getOrElse(index) { "Todo ${index + 1}" },
                onValueChange = { onTabNameChanged(index, it.take(10)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Tab ${index + 1}") },
                supportingText = { Text("最大10文字") },
            )
        }
    }
}

@Composable
private fun ThemeRadioRow(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(
            ThemeMode.LIGHT to "ライト",
            ThemeMode.DARK to "ダーク",
            ThemeMode.SYSTEM to "システム準拠",
        ).forEach { (mode, label) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected == mode,
                    onClick = { onSelected(mode) },
                )
                Text(text = label)
            }
        }
    }
}

@Composable
private fun ReminderCheckRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
            Text(title)
        }

        if (trailing != null) {
            if (onTrailingClick != null) {
                TextButton(onClick = onTrailingClick) {
                    Text(trailing)
                }
            } else {
                Text(trailing)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun showHoursPicker(
    context: Context,
    initialHours: Int,
    onSelected: (Int) -> Unit,
) {
    val initial = initialHours.coerceIn(0, 23)
    TimePickerDialog(
        context,
        { _, hour, minute ->
            val resolved = (hour + if (minute >= 30) 1 else 0).coerceAtLeast(1)
            onSelected(resolved)
        },
        initial,
        0,
        true,
    ).show()
}

private fun formatTaxRateText(rate: Double): String {
    return if (rate == rate.toLong().toDouble()) {
        rate.toLong().toString()
    } else {
        rate.toString()
    }
}

