package com.quickmemo.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OcrProcessor {

    private val recognizer = TextRecognition.getClient(
        JapaneseTextRecognizerOptions.Builder().build(),
    )

    suspend fun recognizeFromUri(context: Context, uri: Uri): OcrResult {
        return suspendCoroutine { continuation ->
            runCatching {
                InputImage.fromFilePath(context, uri)
            }.onSuccess { image ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        continuation.resume(OcrResult.Success(visionText.text))
                    }
                    .addOnFailureListener { error ->
                        continuation.resume(OcrResult.Error(error.message ?: "認識に失敗しました"))
                    }
            }.onFailure { throwable ->
                continuation.resume(OcrResult.Error(throwable.message ?: "画像の読み込みに失敗しました"))
            }
        }
    }

    suspend fun recognizeFromBitmap(bitmap: Bitmap): OcrResult {
        return suspendCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(OcrResult.Success(visionText.text))
                }
                .addOnFailureListener { error ->
                    continuation.resume(OcrResult.Error(error.message ?: "認識に失敗しました"))
                }
        }
    }

    fun close() {
        recognizer.close()
    }
}

sealed class OcrResult {
    data class Success(val text: String) : OcrResult()
    data class Error(val message: String) : OcrResult()
}
