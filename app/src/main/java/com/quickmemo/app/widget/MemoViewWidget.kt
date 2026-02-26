package com.quickmemo.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.quickmemo.app.MainActivity
import com.quickmemo.app.domain.model.Memo
import com.quickmemo.app.util.QuickMemoIntents
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MemoViewWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val memoId = WidgetConfigStore.getMemoId(context, appWidgetId)
        val repository = widgetEntryPoint(context).memoRepository()
        val memo = memoId?.let { repository.getMemoById(it) }

        provideContent {
            if (memo == null) {
                EmptyMemoContent()
            } else {
                MemoContent(
                    memo = memo,
                    openIntent = buildOpenMemoIntent(context, memo.id),
                )
            }
        }
    }

    private fun buildOpenMemoIntent(context: Context, memoId: Long): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = QuickMemoIntents.ACTION_OPEN_EDITOR
            putExtra(QuickMemoIntents.EXTRA_MEMO_ID, memoId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
}

@Composable
private fun EmptyMemoContent() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetTheme.containerColor)
            .cornerRadius(WidgetTheme.cornerRadius)
            .padding(WidgetTheme.contentPadding),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = "📝 メモが未選択です",
            style = TextStyle(
                fontWeight = FontWeight.Medium,
                color = ColorProvider(0xFF222222.toInt()),
            ),
        )
        Text(
            text = "ウィジェット設定で表示メモを選択してください",
            modifier = GlanceModifier.padding(top = 6.dp),
            style = TextStyle(color = WidgetTheme.subtleTextColor),
        )
    }
}

@Composable
private fun MemoContent(
    memo: Memo,
    openIntent: Intent,
) {
    val preview = memo.contentPlainText
        .replace("\n", " ")
        .trim()
        .ifBlank { "本文がありません" }
        .take(140)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetTheme.containerColor)
            .cornerRadius(WidgetTheme.cornerRadius)
            .padding(WidgetTheme.contentPadding)
            .fillMaxWidth()
            .clickable(actionStartActivity(openIntent)),
    ) {
        Text(
            text = "📝 ${memo.displayTitle}",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                color = ColorProvider(0xFF111111.toInt()),
            ),
            maxLines = 1,
        )

        Text(
            text = preview,
            modifier = GlanceModifier.padding(top = 8.dp),
            style = TextStyle(color = ColorProvider(0xFF333333.toInt())),
            maxLines = 4,
        )

        Text(
            text = "${formatRelativeTime(memo.updatedAt)} 更新",
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            style = TextStyle(color = WidgetTheme.subtleTextColor),
            maxLines = 1,
        )
    }
}

private fun formatRelativeTime(timeMillis: Long): String {
    val diffMillis = (System.currentTimeMillis() - timeMillis).coerceAtLeast(0L)
    val minuteMillis = 60_000L
    val hourMillis = 60 * minuteMillis
    val dayMillis = 24 * hourMillis

    return when {
        diffMillis < minuteMillis -> "今"
        diffMillis < hourMillis -> "${diffMillis / minuteMillis}分前"
        diffMillis < dayMillis -> "${diffMillis / hourMillis}時間前"
        diffMillis < 7 * dayMillis -> "${diffMillis / dayMillis}日前"
        else -> SimpleDateFormat("M/d", Locale.JAPAN).format(Date(timeMillis))
    }
}
