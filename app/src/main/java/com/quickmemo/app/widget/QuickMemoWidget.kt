package com.quickmemo.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import com.quickmemo.app.MainActivity
import com.quickmemo.app.util.QuickMemoIntents

class QuickMemoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(android.graphics.Color.parseColor("#FFF4F4F4")))
                    .padding(12.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(text = "QuickMemo")

                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 10.dp)
                        .background(ColorProvider(android.graphics.Color.WHITE))
                        .clickable(actionStartActivity(buildEditorIntent(context))),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = "✏️ メモを書く...",
                        modifier = GlanceModifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                ) {
                    Text(
                        text = "☑",
                        modifier = GlanceModifier
                            .padding(horizontal = 10.dp)
                            .clickable(
                                actionStartActivity(
                                    buildEditorIntent(context, checklist = true)
                                )
                            ),
                    )

                    Text(
                        text = "🧮",
                        modifier = GlanceModifier
                            .padding(horizontal = 10.dp)
                            .clickable(
                                actionStartActivity(
                                    buildEditorIntent(context, prefillText = "計算式: ")
                                )
                            ),
                    )

                    Text(
                        text = "📅",
                        modifier = GlanceModifier
                            .padding(horizontal = 10.dp)
                            .clickable(
                                actionStartActivity(
                                    buildEditorIntent(context, insertToday = true)
                                )
                            ),
                    )
                }
            }
        }
    }

    private fun buildEditorIntent(
        context: Context,
        checklist: Boolean = false,
        prefillText: String = "",
        insertToday: Boolean = false,
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = QuickMemoIntents.ACTION_OPEN_EDITOR
            putExtra(QuickMemoIntents.EXTRA_IS_CHECKLIST, checklist)
            putExtra(QuickMemoIntents.EXTRA_PRE_FILLED_TEXT, prefillText)
            putExtra(QuickMemoIntents.EXTRA_INSERT_TODAY, insertToday)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
}
