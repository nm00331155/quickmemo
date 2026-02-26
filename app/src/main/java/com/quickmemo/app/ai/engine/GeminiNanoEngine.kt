package com.quickmemo.app.ai.engine

import android.content.Context
import com.quickmemo.app.ai.AiCapabilityDetector
import com.quickmemo.app.ai.AiEngineInterface

class GeminiNanoEngine(
    private val context: Context,
) : AiEngineInterface {

    override suspend fun generate(
        systemPrompt: String,
        userMessage: String,
        maxTokens: Int,
        temperature: Float,
    ): String {
        if (!isAvailable()) {
            error("Gemini Nano はこの端末で利用できません")
        }
        return "Gemini Nano のランタイムが未接続です。設定で別エンジンを選択してください。"
    }

    override fun isAvailable(): Boolean {
        return AiCapabilityDetector.hasGeminiRuntime(context)
    }

    override fun getDisplayName(): String {
        return "Gemini Nano（オンデバイス）"
    }

    override fun getStatusDescription(): String {
        return if (isAvailable()) {
            "✅ 利用可能"
        } else {
            "❌ この端末では利用できません"
        }
    }
}
