package com.quickmemo.app.presentation.components

import android.os.Build
import android.text.Html
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quickmemo.app.domain.model.Memo
import com.quickmemo.app.util.DateTimeUtils
import com.quickmemo.app.util.memoCardBackgroundColor

@Composable
fun MemoCard(
    memo: Memo,
    onClick: () -> Unit,
    onTogglePin: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val decodedPreviewText = if (memo.isLocked) "ロックされたメモ" else decodeHtmlEntities(memo.contentPlainText)
    val previewText = if (memo.isLocked) decodedPreviewText else formatCalcForPreview(decodedPreviewText)
    val previewTtsText = if (memo.isLocked) decodedPreviewText else formatCalcForTts(decodedPreviewText)

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = memoCardBackgroundColor(memo.colorLabel),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (memo.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    if (memo.isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        text = decodeHtmlEntities(memo.displayTitle),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (onTogglePin != null) {
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "pin",
                            tint = if (memo.isPinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

            Text(
                text = previewText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics {
                    contentDescription = previewTtsText
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = DateTimeUtils.formatCardDate(memo.updatedAt),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun decodeHtmlEntities(text: String): String {
    if (!text.contains("&")) return text

    val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(text)
    }
    return spanned.toString()
}

private fun formatCalcForPreview(text: String): String {
    val calcPattern = Regex("""[\d,]+[×÷+\-][\d,×÷+\-\s]*\n?=\s*([\d,]+(?:\.\d+)?)""")
    return calcPattern.replace(text) { match ->
        "計算: ${match.groupValues[1]}"
    }
}

private fun formatCalcForTts(text: String): String {
    val calcPattern = Regex("""[\d,]+[×÷+\-][\d,×÷+\-\s]*\n?=\s*([\d,]+(?:\.\d+)?)""")
    return calcPattern.replace(text) { match ->
        "計算結果 ${match.groupValues[1]}"
    }
}
