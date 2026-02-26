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

class CustomApiEngine(
    private val context: Context,
) : AiEngineInterface {

    override suspend fun generate(
        systemPrompt: String,
        userMessage: String,
        maxTokens: Int,
        temperature: Float,
    ): String {
        val prefs = context.settingsDataStore.data.first()
        val endpoint = prefs[ENDPOINT_URL]?.trim().orEmpty()
        val apiKey = prefs[API_KEY]?.trim().orEmpty()
        val model = prefs[MODEL_NAME]?.trim().orEmpty().ifBlank { DEFAULT_MODEL }

        if (endpoint.isBlank()) {
            error("エンドポイントが設定されていません")
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

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        return withContext(Dispatchers.IO) {
            val response = httpClient.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("カスタムAPIエラー: ${response.code}")
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
        return !prefs[ENDPOINT_URL].isNullOrBlank()
    }

    override fun getDisplayName(): String {
        return "カスタムAPI（OpenAI互換）"
    }

    override fun getStatusDescription(): String {
        return if (isAvailable()) "✅ エンドポイント設定済み" else "⚙ 未設定"
    }

    companion object {
        val ENDPOINT_URL = stringPreferencesKey("custom_api_endpoint")
        val API_KEY = stringPreferencesKey("custom_api_key")
        val MODEL_NAME = stringPreferencesKey("custom_api_model")
        private const val DEFAULT_MODEL = "default"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private val httpClient = OkHttpClient()
    }
}
