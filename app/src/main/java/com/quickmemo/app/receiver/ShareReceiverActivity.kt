package com.quickmemo.app.receiver

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.quickmemo.app.MainActivity
import com.quickmemo.app.util.QuickMemoIntents

class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            else -> ""
        }

        startActivity(
            Intent(this, MainActivity::class.java).apply {
                action = QuickMemoIntents.ACTION_OPEN_EDITOR
                putExtra(QuickMemoIntents.EXTRA_PRE_FILLED_TEXT, text)
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        )

        finish()
    }
}
