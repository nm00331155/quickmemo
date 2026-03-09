package com.quickmemo.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object OcrImageUtils {
    fun loadBitmapForEditing(
        context: Context,
        uri: Uri,
        maxDimension: Int = 2_048,
    ): Bitmap {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } ?: error("画像を読み込めませんでした")

        val sampleSize = calculateInSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            maxDimension = maxDimension,
        )

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = sampleSize
        }

        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: error("画像を読み込めませんでした")
    }

    fun cropBitmap(bitmap: Bitmap, rect: Rect): Bitmap {
        val safeRect = rect.coerceTo(bitmap.width, bitmap.height)
        return Bitmap.createBitmap(
            bitmap,
            safeRect.left,
            safeRect.top,
            safeRect.width(),
            safeRect.height(),
        )
    }

    fun saveBitmapToTempFile(
        context: Context,
        bitmap: Bitmap,
        prefix: String = "ocr_crop_",
    ): Uri {
        val dir = File(context.cacheDir, "quickmemo_images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val outputFile = File.createTempFile(prefix, ".jpg", dir)
        FileOutputStream(outputFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 96, stream)
            stream.flush()
        }
        return outputFile.toUri()
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        maxDimension: Int,
    ): Int {
        if (width <= 0 || height <= 0) return 1

        var sampleSize = 1
        while (max(width / sampleSize, height / sampleSize) > maxDimension) {
            sampleSize *= 2
        }
        return sampleSize.coerceAtLeast(1)
    }
}

private fun Rect.coerceTo(maxWidth: Int, maxHeight: Int): Rect {
    val left = left.coerceIn(0, maxWidth - 1)
    val top = top.coerceIn(0, maxHeight - 1)
    val right = right.coerceIn(left + 1, maxWidth)
    val bottom = bottom.coerceIn(top + 1, maxHeight)
    return Rect(left, top, right, bottom)
}

fun createBitmapCropRect(
    imageWidth: Int,
    imageHeight: Int,
    displayWidth: Float,
    displayHeight: Float,
    cropLeft: Float,
    cropTop: Float,
    cropRight: Float,
    cropBottom: Float,
): Rect {
    val scaleX = imageWidth / displayWidth
    val scaleY = imageHeight / displayHeight

    return Rect(
        (cropLeft * scaleX).roundToInt(),
        (cropTop * scaleY).roundToInt(),
        (cropRight * scaleX).roundToInt(),
        (cropBottom * scaleY).roundToInt(),
    )
}