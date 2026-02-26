package com.quickmemo.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.quickmemo.app.MainActivity
import com.quickmemo.app.util.QuickMemoIntents

class NewMemoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            NewMemoContent(
                intent = Intent(context, MainActivity::class.java).apply {
                    action = QuickMemoIntents.ACTION_NEW_MEMO
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
            )
        }
    }
}

@Composable
private fun NewMemoContent(intent: Intent) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetTheme.containerColor)
            .cornerRadius(WidgetTheme.cornerRadius)
            .clickable(actionStartActivity(intent))
            .padding(WidgetTheme.contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "📝 + 新規メモ",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                color = ColorProvider(0xFF111111.toInt()),
            ),
        )
    }
}
