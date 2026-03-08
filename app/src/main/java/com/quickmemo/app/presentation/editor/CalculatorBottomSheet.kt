package com.quickmemo.app.presentation.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CalcHistoryEntry(
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorBottomSheet(
    cursorContextText: String,
    taxRate: Double,
    history: SnapshotStateList<CalcHistoryEntry>,
    onInsertExpressionAndResult: (expression: String, result: String) -> Unit,
    onInsertResultOnly: (result: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expression by remember { mutableStateOf("") }
    var displayResult by remember { mutableStateOf("0") }
    var hasError by remember { mutableStateOf(false) }
    val historyListState = rememberLazyListState()

    fun formatWithCommas(value: String): String {
        return try {
            val bd = BigDecimal(value)
            val formatter = DecimalFormat("#,###.##########")
            formatter.format(bd)
        } catch (_: Exception) {
            value
        }
    }

    fun convertDisplayToCalc(display: String): String {
        return display
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(",", "")
    }

    fun evaluate() {
        if (expression.isBlank()) return
        try {
            val calcExpr = convertDisplayToCalc(expression)
            val result = parseCalculatorExpression(calcExpr)
            val resultStr = formatCalculatorResult(result)
            val formattedResult = formatWithCommas(resultStr)
            displayResult = formattedResult
            hasError = false
            history.add(
                CalcHistoryEntry(
                    expression = expression,
                    result = formattedResult,
                ),
            )
        } catch (_: Exception) {
            displayResult = "エラー"
            hasError = true
        }
    }

    fun appendToExpression(token: String) {
        expression += token
        hasError = false
        try {
            val calcExpr = convertDisplayToCalc(expression)
            val result = parseCalculatorExpression(calcExpr)
            val resultStr = formatCalculatorResult(result)
            displayResult = formatWithCommas(resultStr)
        } catch (_: Exception) {
            // 式が不完全な間は前回の結果表示を維持する。
        }
    }

    fun backspace() {
        if (expression.isNotEmpty()) {
            expression = expression.dropLast(1)
            hasError = false
            if (expression.isBlank()) {
                displayResult = "0"
            } else {
                try {
                    val calcExpr = convertDisplayToCalc(expression)
                    val result = parseCalculatorExpression(calcExpr)
                    displayResult = formatWithCommas(formatCalculatorResult(result))
                } catch (_: Exception) {
                    // 式が不完全な間は前回の結果表示を維持する。
                }
            }
        }
    }

    fun clearAll() {
        expression = ""
        displayResult = "0"
        hasError = false
    }

    fun formatTaxRate(rate: Double): String {
        return if (rate == rate.toLong().toDouble()) {
            rate.toLong().toString()
        } else {
            rate.toString()
        }
    }

    fun normalizeInsertText(source: String): String {
        return source
            .replace("&times;", "×")
            .replace("&divide;", "÷")
            .replace("&equals;", "=")
            .replace("&comma;", ",")
    }

    fun applyTaxIncluded() {
        if (displayResult == "0" || displayResult == "エラー") return
        try {
            val current = BigDecimal(displayResult.replace(",", ""))
            val taxMultiplier = BigDecimal.ONE + BigDecimal.valueOf(taxRate / 100.0)
            val taxIncluded = current.multiply(taxMultiplier)
                .setScale(0, java.math.RoundingMode.HALF_UP)
            val formatted = formatWithCommas(taxIncluded.toPlainString())
            val taxExpr = "$displayResult×${formatTaxRate(taxRate)}%税込"
            expression = taxExpr
            displayResult = formatted
            hasError = false
            history.add(CalcHistoryEntry(expression = taxExpr, result = formatted))
        } catch (_: Exception) {
            displayResult = "エラー"
            hasError = true
        }
    }

    fun applyTaxExcluded() {
        if (displayResult == "0" || displayResult == "エラー") return
        try {
            val current = BigDecimal(displayResult.replace(",", ""))
            val taxMultiplier = BigDecimal.ONE + BigDecimal.valueOf(taxRate / 100.0)
            val taxExcluded = current.divide(taxMultiplier, 0, java.math.RoundingMode.HALF_UP)
            val formatted = formatWithCommas(taxExcluded.toPlainString())
            val taxExpr = "$displayResult÷${formatTaxRate(taxRate)}%税抜"
            expression = taxExpr
            displayResult = formatted
            hasError = false
            history.add(CalcHistoryEntry(expression = taxExpr, result = formatted))
        } catch (_: Exception) {
            displayResult = "エラー"
            hasError = true
        }
    }

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            historyListState.animateScrollToItem(history.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) {
            if (cursorContextText.isNotBlank()) {
                Text(
                    text = "挿入位置: $cursorContextText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }

            Text(
                text = "税率: ${formatTaxRate(taxRate)}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )

            LazyColumn(
                state = historyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(
                    items = history,
                    key = { "${it.timestamp}-${it.expression}-${it.result}" },
                ) { entry ->
                    val dateStr = SimpleDateFormat("yyyy/M/d", Locale.JAPAN)
                        .format(Date(entry.timestamp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .combinedClickable(
                                onClick = {
                                    expression = entry.expression
                                    displayResult = entry.result
                                    hasError = false
                                },
                                onLongClick = {},
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = entry.expression,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "=${entry.result}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = expression.ifBlank { " " },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = displayResult,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                        ),
                        color = if (hasError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val buttonRows = listOf(
                listOf(
                    CalcButton("✕", CalcButtonType.FUNCTION),
                    CalcButton("税込", CalcButtonType.TAX),
                    CalcButton("税抜", CalcButtonType.TAX),
                    CalcButton("÷", CalcButtonType.OPERATOR),
                ),
                listOf(
                    CalcButton("7", CalcButtonType.NUMBER),
                    CalcButton("8", CalcButtonType.NUMBER),
                    CalcButton("9", CalcButtonType.NUMBER),
                    CalcButton("×", CalcButtonType.OPERATOR),
                ),
                listOf(
                    CalcButton("4", CalcButtonType.NUMBER),
                    CalcButton("5", CalcButtonType.NUMBER),
                    CalcButton("6", CalcButtonType.NUMBER),
                    CalcButton("−", CalcButtonType.OPERATOR),
                ),
                listOf(
                    CalcButton("1", CalcButtonType.NUMBER),
                    CalcButton("2", CalcButtonType.NUMBER),
                    CalcButton("3", CalcButtonType.NUMBER),
                    CalcButton("+", CalcButtonType.OPERATOR),
                ),
                listOf(
                    CalcButton("0", CalcButtonType.NUMBER),
                    CalcButton("00", CalcButtonType.NUMBER),
                    CalcButton(".", CalcButtonType.NUMBER),
                    CalcButton("=", CalcButtonType.EQUALS),
                ),
            )

            buttonRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    row.forEach { button ->
                        CalcKeyButton(
                            button = button,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                when (button.label) {
                                    "✕" -> backspace()
                                    "=" -> evaluate()
                                    "−" -> appendToExpression("-")
                                    "税込" -> applyTaxIncluded()
                                    "税抜" -> applyTaxExcluded()
                                    else -> appendToExpression(button.label)
                                }
                            },
                            onLongClick = {
                                if (button.label == "✕") {
                                    clearAll()
                                }
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = {
                        if (!hasError && displayResult != "0") {
                            onInsertExpressionAndResult(
                                normalizeInsertText(expression),
                                normalizeInsertText(displayResult),
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !hasError && displayResult != "0" && expression.isNotBlank(),
                ) {
                    Text("式+結果を挿入")
                }

                TextButton(
                    onClick = {
                        if (!hasError && displayResult != "0") {
                            onInsertResultOnly(normalizeInsertText(displayResult))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !hasError && displayResult != "0",
                ) {
                    Text("結果のみ挿入")
                }
            }
        }
    }
}

private enum class CalcButtonType {
    NUMBER,
    OPERATOR,
    FUNCTION,
    EQUALS,
    TAX,
}

private data class CalcButton(
    val label: String,
    val type: CalcButtonType,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalcKeyButton(
    button: CalcButton,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val bgColor = when (button.type) {
        CalcButtonType.NUMBER -> MaterialTheme.colorScheme.surfaceContainerHigh
        CalcButtonType.OPERATOR -> Color(0xFF26808F)
        CalcButtonType.FUNCTION -> Color(0xFFE53935)
        CalcButtonType.EQUALS -> Color(0xFF1565C0)
        CalcButtonType.TAX -> Color(0xFF7B1FA2)
    }
    val textColor = when (button.type) {
        CalcButtonType.NUMBER -> MaterialTheme.colorScheme.onSurface
        else -> Color.White
    }

    Surface(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp)),
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        ) {
            Text(
                text = button.label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
            )
        }
    }
}
