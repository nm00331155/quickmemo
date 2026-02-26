package com.quickmemo.app.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class TranslationManager {

    private var jaToEnTranslator: Translator? = null
    private var enToJaTranslator: Translator? = null

    private val _downloadStatus = MutableStateFlow<TranslationDownloadStatus>(
        TranslationDownloadStatus.NotChecked,
    )
    val downloadStatus: StateFlow<TranslationDownloadStatus> = _downloadStatus.asStateFlow()

    sealed class TranslationDownloadStatus {
        data object NotChecked : TranslationDownloadStatus()
        data object Downloading : TranslationDownloadStatus()
        data object Ready : TranslationDownloadStatus()
        data class Error(val message: String) : TranslationDownloadStatus()
    }

    suspend fun ensureModelsDownloaded() {
        _downloadStatus.value = TranslationDownloadStatus.Downloading

        runCatching {
            val jaToEnOptions = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.JAPANESE)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()

            val enToJaOptions = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.JAPANESE)
                .build()

            jaToEnTranslator = Translation.getClient(jaToEnOptions)
            enToJaTranslator = Translation.getClient(enToJaOptions)

            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            jaToEnTranslator?.downloadModelIfNeeded(conditions)?.await()
            enToJaTranslator?.downloadModelIfNeeded(conditions)?.await()
        }.onSuccess {
            _downloadStatus.value = TranslationDownloadStatus.Ready
        }.onFailure { throwable ->
            _downloadStatus.value = TranslationDownloadStatus.Error(
                throwable.message ?: "翻訳モデルのダウンロードに失敗しました",
            )
        }
    }

    suspend fun translateJaToEn(text: String): String {
        val translator = jaToEnTranslator ?: error("翻訳モデルが準備されていません")
        return translator.translate(text).await()
    }

    suspend fun translateEnToJa(text: String): String {
        val translator = enToJaTranslator ?: error("翻訳モデルが準備されていません")
        return translator.translate(text).await()
    }

    suspend fun autoTranslate(text: String): TranslationResult {
        val languageIdentifier = LanguageIdentification.getClient()
        val detectedLanguage = suspendCoroutine { continuation ->
            languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { languageCode ->
                    continuation.resume(languageCode)
                }
                .addOnFailureListener {
                    continuation.resume("und")
                }
        }

        return if (detectedLanguage == "ja") {
            TranslationResult(
                translatedText = translateJaToEn(text),
                sourceLanguage = "日本語",
                targetLanguage = "英語",
            )
        } else {
            TranslationResult(
                translatedText = translateEnToJa(text),
                sourceLanguage = "英語",
                targetLanguage = "日本語",
            )
        }
    }

    fun close() {
        jaToEnTranslator?.close()
        enToJaTranslator?.close()
    }
}

data class TranslationResult(
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
)
