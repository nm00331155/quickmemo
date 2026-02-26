package com.quickmemo.app.ai

interface AiEngineInterface {
    suspend fun generate(
        systemPrompt: String,
        userMessage: String,
        maxTokens: Int = 512,
        temperature: Float = 0.3f,
    ): String

    fun isAvailable(): Boolean
    fun getDisplayName(): String
    fun getStatusDescription(): String
}
