package com.quickmemo.app.ai

import android.content.Context
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val modelDir: File = File(context.filesDir, "ai_models")
    private val modelFile: File = File(modelDir, MODEL_FILENAME)

    private val _status = MutableStateFlow(checkCurrentStatus())
    val status: StateFlow<ModelStatus> = _status.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    enum class ModelStatus {
        NOT_DOWNLOADED,
        DOWNLOADING,
        DOWNLOADED,
        DOWNLOAD_FAILED,
        CORRUPTED,
    }

    fun getModelPath(): String? {
        return if (_status.value == ModelStatus.DOWNLOADED) modelFile.absolutePath else null
    }

    fun getModelFilePath(): String {
        return modelFile.absolutePath
    }

    fun getCurrentStatus(): ModelStatus {
        return _status.value
    }

    fun checkCurrentStatus(): ModelStatus {
        if (!modelFile.exists()) return ModelStatus.NOT_DOWNLOADED
        if (modelFile.length() < (MODEL_SIZE_BYTES * 0.9f).toLong()) return ModelStatus.CORRUPTED
        return ModelStatus.DOWNLOADED
    }

    fun refreshStatus() {
        _status.value = checkCurrentStatus()
    }

    fun getAvailableSpace(): Long {
        val stat = StatFs(context.filesDir.absolutePath)
        return stat.availableBytes
    }

    fun isEnoughSpace(): Boolean {
        return getAvailableSpace() > (MODEL_SIZE_BYTES * 1.2f).toLong()
    }

    suspend fun downloadModel(onProgress: (Float) -> Unit): Boolean {
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }

        _status.value = ModelStatus.DOWNLOADING
        _progress.value = 0f

        return runCatching {
            val connection = URL(MODEL_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            connection.connect()

            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L

            connection.inputStream.use { inputStream ->
                FileOutputStream(modelFile).use { outputStream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var readBytes: Int
                    while (inputStream.read(buffer).also { readBytes = it } != -1) {
                        outputStream.write(buffer, 0, readBytes)
                        downloadedBytes += readBytes

                        val progressValue = if (totalBytes > 0L) {
                            downloadedBytes.toFloat() / totalBytes.toFloat()
                        } else {
                            0f
                        }
                        _progress.value = progressValue
                        onProgress(progressValue)
                    }
                }
            }

            _status.value = checkCurrentStatus()
            _status.value == ModelStatus.DOWNLOADED
        }.getOrElse {
            if (modelFile.exists()) {
                modelFile.delete()
            }
            _status.value = ModelStatus.DOWNLOAD_FAILED
            false
        }
    }

    fun deleteModel() {
        if (modelFile.exists()) {
            modelFile.delete()
        }
        _status.value = ModelStatus.NOT_DOWNLOADED
        _progress.value = 0f
    }

    companion object {
        private const val MODEL_URL = "https://huggingface.co/unsloth/Qwen3-1.7B-GGUF/resolve/main/Qwen3-1.7B-Q4_K_M.gguf"
        private const val MODEL_FILENAME = "Qwen3-1.7B-Q4_K_M.gguf"
        private const val MODEL_SIZE_BYTES: Long = 1_190_000_000L
    }
}
