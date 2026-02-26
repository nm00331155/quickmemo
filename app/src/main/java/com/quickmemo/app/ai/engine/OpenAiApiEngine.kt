package com.quickmemo.app.ai.engine

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quickmemo.app.ai.AiEngineInterface
import com.quickmemo.app.data.datastore.settingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class OpenAiApiEngine(
    private val context: Context,
) : AiEngineInterface {

    override suspend fun generate(
        systemPrompt: String,
        userMessage: String,
        maxTokens: Int,
        temperature: Float,
    ): String {
        val prefs = context.settingsDataStore.data.first()
        val apiKey = prefs[API_KEY]?.trim().orEmpty()
        val model = prefs[API_MODEL]?.trim().orEmpty().ifBlank { DEFAULT_MODEL }
        if (apiKey.isBlank()) {
            error("APIキーが設定されていません")
        }

        val payload = JSONObject().apply {
            put("model", model)
            put("max_tokens", maxTokens)
            put("temperature", temperature)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })
            })
        }

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return withContext(Dispatchers.IO) {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("OpenAI APIエラー: ${response.code}")
            }

            JSONObject(responseBody)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        }
    }

    override fun isAvailable(): Boolean {
        val prefs = runCatching {
            runBlocking { context.settingsDataStore.data.first() }
        }.getOrNull() ?: return false
        return !prefs[API_KEY].isNullOrBlank()
    }

    override fun getDisplayName(): String {
        return "OpenAI API（クラウド）"
    }

    override fun getStatusDescription(): String {
        return if (isAvailable()) "✅ APIキー設定済み" else "🔑 APIキー未設定"
    }

    companion object {
        val API_KEY = stringPreferencesKey("openai_api_key")
        val API_MODEL = stringPreferencesKey("openai_model")
        private const val DEFAULT_MODEL = "gpt-4o-mini"
        private const val BASE_URL = "https://api.openai.com/v1/chat/completions"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private val httpClient = OkHttpClient()
    }
}
