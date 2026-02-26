package com.quickmemo.app.ai.engine

import com.quickmemo.app.ai.AiEngineInterface
import com.quickmemo.app.ai.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Qwen3LocalEngine(
    private val modelManager: ModelManager,
) : AiEngineInterface {

    private var modelInstance: Any? = null

    override suspend fun generate(
        systemPrompt: String,
        userMessage: String,
        maxTokens: Int,
        temperature: Float,
    ): String = withContext(Dispatchers.Default) {
        if (!isAvailable()) {
            error("Qwen3モデルが未ダウンロードです")
        }

        val loaded = ensureLoaded()
        if (!loaded || modelInstance == null) {
            return@withContext "Qwen3 ローカル推論エンジンの初期化に失敗しました。"
        }

        val prompt = buildString {
            append("<|im_start|>system\n")
            append(systemPrompt)
            append("<|im_end|>\n")
            append("<|im_start|>user\n")
            append(userMessage)
            append("<|im_end|>\n")
            append("<|im_start|>assistant\n")
        }

        return@withContext runCatching {
            val inferenceParametersClass = Class.forName("de.kherud.llama.InferenceParameters")
            val inferenceParameters = inferenceParametersClass.getDeclaredConstructor().newInstance()
            inferenceParametersClass.getMethod("setTemperature", Float::class.javaPrimitiveType)
                .invoke(inferenceParameters, temperature)
            inferenceParametersClass.getMethod("setNPredict", Int::class.javaPrimitiveType)
                .invoke(inferenceParameters, maxTokens)

            val output = StringBuilder()
            val callbackClass = Class.forName("de.kherud.llama.InferenceCallback")
            val callback = java.lang.reflect.Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass),
            ) { _, _, args ->
                val token = args?.firstOrNull()?.toString().orEmpty()
                output.append(token)
                true
            }

            modelInstance!!::class.java.getMethod(
                "generate",
                String::class.java,
                inferenceParametersClass,
                callbackClass,
            ).invoke(modelInstance, prompt, inferenceParameters, callback)

            output.toString().trim().ifBlank {
                "Qwen3 から応答を取得できませんでした。"
            }
        }.getOrElse {
            "Qwen3 ローカル推論でエラーが発生しました。"
        }
    }

    override fun isAvailable(): Boolean {
        return modelManager.getCurrentStatus() == ModelManager.ModelStatus.DOWNLOADED
    }

    override fun getDisplayName(): String {
        return "Qwen3 1.7B（ローカル）"
    }

    override fun getStatusDescription(): String {
        return when (modelManager.getCurrentStatus()) {
            ModelManager.ModelStatus.DOWNLOADED -> "✅ 利用可能"
            ModelManager.ModelStatus.DOWNLOADING -> "⏳ ダウンロード中"
            ModelManager.ModelStatus.NOT_DOWNLOADED -> "📦 要ダウンロード (1.1GB)"
            ModelManager.ModelStatus.DOWNLOAD_FAILED -> "❌ ダウンロード失敗"
            ModelManager.ModelStatus.CORRUPTED -> "⚠ ファイル破損（再DL必要）"
        }
    }

    private fun ensureLoaded(): Boolean {
        if (modelInstance != null) return true

        val modelPath = modelManager.getModelPath() ?: return false

        return runCatching {
            val modelParametersClass = Class.forName("de.kherud.llama.ModelParameters")
            val modelParameters = modelParametersClass.getDeclaredConstructor().newInstance()
            modelParametersClass.getMethod("setModelFilePath", String::class.java)
                .invoke(modelParameters, modelPath)
            modelParametersClass.getMethod("setNGpuLayers", Int::class.javaPrimitiveType)
                .invoke(modelParameters, 0)
            modelParametersClass.getMethod("setNCtx", Int::class.javaPrimitiveType)
                .invoke(modelParameters, 4096)
            modelParametersClass.getMethod("setNThreads", Int::class.javaPrimitiveType)
                .invoke(modelParameters, 4)

            val llamaModelClass = Class.forName("de.kherud.llama.LlamaModel")
            modelInstance = llamaModelClass
                .getDeclaredConstructor(modelParametersClass)
                .newInstance(modelParameters)
            true
        }.getOrElse {
            modelInstance = null
            false
        }
    }
}
