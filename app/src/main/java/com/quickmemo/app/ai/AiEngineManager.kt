package com.quickmemo.app.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quickmemo.app.ai.engine.CustomApiEngine
import com.quickmemo.app.ai.engine.GeminiNanoEngine
import com.quickmemo.app.ai.engine.OpenAiApiEngine
import com.quickmemo.app.ai.engine.Qwen3LocalEngine
import com.quickmemo.app.data.datastore.settingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class AiEngineManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelManager: ModelManager,
) {
    private val engines: Map<AiEngineType, AiEngineInterface> = mapOf(
        AiEngineType.GEMINI_NANO to GeminiNanoEngine(context),
        AiEngineType.QWEN3_LOCAL to Qwen3LocalEngine(modelManager),
        AiEngineType.OPENAI_API to OpenAiApiEngine(context),
        AiEngineType.CUSTOM_API to CustomApiEngine(context),
    )

    fun getEngine(type: AiEngineType): AiEngineInterface {
        return engines[type] ?: error("未知のエンジンです: $type")
    }

    fun getAllEngines(): Map<AiEngineType, AiEngineInterface> {
        return engines
    }

    suspend fun getSelectedEngineType(): AiEngineType {
        val prefs = context.settingsDataStore.data.first()
        val raw = prefs[SELECTED_ENGINE] ?: AiEngineType.QWEN3_LOCAL.name
        return runCatching { AiEngineType.valueOf(raw) }.getOrDefault(AiEngineType.QWEN3_LOCAL)
    }

    suspend fun getSelectedEngine(): AiEngineInterface {
        return getEngine(getSelectedEngineType())
    }

    suspend fun setSelectedEngine(type: AiEngineType) {
        context.settingsDataStore.edit { preferences ->
            preferences[SELECTED_ENGINE] = type.name
        }
    }

    fun observePreferences(): Flow<AiEnginePreferences> {
        return context.settingsDataStore.data.map { preferences ->
            val engineType = runCatching {
                AiEngineType.valueOf(preferences[SELECTED_ENGINE] ?: AiEngineType.QWEN3_LOCAL.name)
            }.getOrDefault(AiEngineType.QWEN3_LOCAL)

            AiEnginePreferences(
                selectedEngine = engineType,
                openAiApiKey = preferences[OPENAI_API_KEY].orEmpty(),
                openAiModel = preferences[OPENAI_MODEL].orEmpty().ifBlank { "gpt-4o-mini" },
                customApiEndpoint = preferences[CUSTOM_API_ENDPOINT].orEmpty(),
                customApiKey = preferences[CUSTOM_API_KEY].orEmpty(),
                customApiModel = preferences[CUSTOM_API_MODEL].orEmpty().ifBlank { "default" },
            )
        }
    }

    suspend fun setOpenAiApiKey(value: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[OPENAI_API_KEY] = value.trim()
        }
    }

    suspend fun setOpenAiModel(value: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[OPENAI_MODEL] = value.trim().ifBlank { "gpt-4o-mini" }
        }
    }

    suspend fun setCustomApiEndpoint(value: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[CUSTOM_API_ENDPOINT] = value.trim()
        }
    }

    suspend fun setCustomApiKey(value: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[CUSTOM_API_KEY] = value.trim()
        }
    }

    suspend fun setCustomApiModel(value: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[CUSTOM_API_MODEL] = value.trim().ifBlank { "default" }
        }
    }

    data class AiEnginePreferences(
        val selectedEngine: AiEngineType = AiEngineType.QWEN3_LOCAL,
        val openAiApiKey: String = "",
        val openAiModel: String = "gpt-4o-mini",
        val customApiEndpoint: String = "",
        val customApiKey: String = "",
        val customApiModel: String = "default",
    )

    companion object {
        val SELECTED_ENGINE = stringPreferencesKey("selected_ai_engine")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val OPENAI_MODEL = stringPreferencesKey("openai_model")
        val CUSTOM_API_ENDPOINT = stringPreferencesKey("custom_api_endpoint")
        val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
        val CUSTOM_API_MODEL = stringPreferencesKey("custom_api_model")
    }
}
