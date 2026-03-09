package com.quickmemo.app.ocr

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.quickmemo.app.presentation.theme.QuickMemoTheme
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OcrCropActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sourceUri = intent?.getStringExtra(EXTRA_SOURCE_URI)?.let(Uri::parse)
        if (sourceUri == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        setContent {
            QuickMemoTheme {
                OcrCropScreen(
                    sourceUri = sourceUri,
                    onBack = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    onUseOriginal = {
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(EXTRA_RESULT_URI, sourceUri.toString()),
                        )
                        finish()
                    },
                    onUseCrop = { croppedUri ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(EXTRA_RESULT_URI, croppedUri.toString()),
                        )
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_SOURCE_URI = "extra_source_uri"
        const val EXTRA_RESULT_URI = "extra_result_uri"

        fun createIntent(context: Context, sourceUri: Uri): Intent {
            return Intent(context, OcrCropActivity::class.java).apply {
                putExtra(EXTRA_SOURCE_URI, sourceUri.toString())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OcrCropScreen(
    sourceUri: Uri,
    onBack: () -> Unit,
    onUseOriginal: () -> Unit,
    onUseCrop: (Uri) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bitmap by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }
    var loadError by remember(sourceUri) { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var cropRect by remember { mutableStateOf(CropRect.Uninitialized) }
    val density = LocalDensity.current
    val handleTouchRadius = with(density) { 24.dp.toPx() }

    LaunchedEffect(sourceUri) {
        loadError = null
        bitmap = null
        runCatching {
            withContext(Dispatchers.IO) {
                OcrImageUtils.loadBitmapForEditing(context, sourceUri)
            }
        }.onSuccess { loaded ->
            bitmap = loaded
        }.onFailure { throwable ->
            loadError = throwable.message ?: "画像を読み込めませんでした"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OCR 範囲選択") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("戻る")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            bitmap == null && loadError == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            loadError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(loadError.orEmpty())
                        TextButton(onClick = onBack) {
                            Text("閉じる")
                        }
                    }
                }
            }

            else -> {
                val loadedBitmap = bitmap ?: return@Scaffold
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "範囲内だけ OCR するか、全体 OCR を選択できます。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.Black.copy(alpha = 0.04f))
                            .onSizeChanged { size ->
                                containerSize = size
                            },
                    ) {
                        val viewport = remember(loadedBitmap, containerSize) {
                            calculateViewport(
                                imageWidth = loadedBitmap.width,
                                imageHeight = loadedBitmap.height,
                                containerSize = containerSize,
                            )
                        }

                        LaunchedEffect(viewport) {
                            if (viewport.width <= 0f || viewport.height <= 0f) return@LaunchedEffect
                            if (cropRect == CropRect.Uninitialized) {
                                cropRect = CropRect(
                                    left = viewport.left + viewport.width * 0.1f,
                                    top = viewport.top + viewport.height * 0.1f,
                                    right = viewport.right - viewport.width * 0.1f,
                                    bottom = viewport.bottom - viewport.height * 0.1f,
                                )
                            }
                        }

                        val activeViewport = viewport
                        var dragHandle by remember { mutableStateOf<CropHandle?>(null) }

                        androidx.compose.foundation.Image(
                            bitmap = loadedBitmap.asImageBitmap(),
                            contentDescription = "OCR crop preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(activeViewport, cropRect) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            dragHandle = determineDragHandle(
                                                point = offset,
                                                cropRect = cropRect,
                                                viewport = activeViewport,
                                                handleRadius = handleTouchRadius,
                                            )
                                        },
                                        onDragEnd = {
                                            dragHandle = null
                                        },
                                        onDragCancel = {
                                            dragHandle = null
                                        },
                                    ) { change, dragAmount ->
                                        change.consume()
                                        val next = cropRect.update(
                                            handle = dragHandle,
                                            dx = dragAmount.x,
                                            dy = dragAmount.y,
                                            viewport = activeViewport,
                                        )
                                        if (next != null) {
                                            cropRect = next
                                        }
                                    }
                                },
                        ) {
                            if (activeViewport.width <= 0f || activeViewport.height <= 0f) return@Canvas

                            if (cropRect.isInitialized) {
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.38f),
                                    topLeft = Offset.Zero,
                                    size = Size(size.width, cropRect.top),
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.38f),
                                    topLeft = Offset(0f, cropRect.top),
                                    size = Size(cropRect.left, cropRect.height),
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.38f),
                                    topLeft = Offset(cropRect.right, cropRect.top),
                                    size = Size(size.width - cropRect.right, cropRect.height),
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.38f),
                                    topLeft = Offset(0f, cropRect.bottom),
                                    size = Size(size.width, size.height - cropRect.bottom),
                                )
                                drawRect(
                                    color = Color.White.copy(alpha = 0.08f),
                                    topLeft = Offset(cropRect.left, cropRect.top),
                                    size = Size(cropRect.width, cropRect.height),
                                )
                                drawRect(
                                    color = Color.White,
                                    topLeft = Offset(cropRect.left, cropRect.top),
                                    size = Size(cropRect.width, cropRect.height),
                                    style = Stroke(width = 3f),
                                )
                                drawHandle(cropRect.left, cropRect.top)
                                drawHandle(cropRect.right, cropRect.top)
                                drawHandle(cropRect.left, cropRect.bottom)
                                drawHandle(cropRect.right, cropRect.bottom)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("キャンセル")
                        }
                        TextButton(
                            onClick = onUseOriginal,
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving,
                        ) {
                            Text("全体をOCR")
                        }
                        Button(
                            onClick = {
                                if (!cropRect.isInitialized || isSaving) return@Button
                                isSaving = true
                                val viewport = calculateViewport(
                                    imageWidth = loadedBitmap.width,
                                    imageHeight = loadedBitmap.height,
                                    containerSize = containerSize,
                                )
                                val crop = createBitmapCropRect(
                                    imageWidth = loadedBitmap.width,
                                    imageHeight = loadedBitmap.height,
                                    displayWidth = viewport.width,
                                    displayHeight = viewport.height,
                                    cropLeft = cropRect.left - viewport.left,
                                    cropTop = cropRect.top - viewport.top,
                                    cropRight = cropRect.right - viewport.left,
                                    cropBottom = cropRect.bottom - viewport.top,
                                )
                                runCatching {
                                    val croppedBitmap = OcrImageUtils.cropBitmap(loadedBitmap, crop)
                                    OcrImageUtils.saveBitmapToTempFile(context, croppedBitmap)
                                }.onSuccess(onUseCrop).onFailure {
                                    loadError = it.message ?: "トリミングに失敗しました"
                                    isSaving = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = cropRect.isInitialized && !isSaving,
                        ) {
                            Text(if (isSaving) "保存中..." else "この範囲でOCR")
                        }
                    }
                }
            }
        }
    }
}

private data class Viewport(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

private data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val isInitialized: Boolean get() = width > 0f && height > 0f

    companion object {
        val Uninitialized = CropRect(0f, 0f, 0f, 0f)
    }

    fun update(
        handle: CropHandle?,
        dx: Float,
        dy: Float,
        viewport: Viewport,
    ): CropRect? {
        if (!isInitialized || viewport.width <= 0f || viewport.height <= 0f) return null
        val minimumSize = min(viewport.width, viewport.height) * 0.12f

        return when (handle) {
            CropHandle.Move -> copy(
                left = (left + dx).coerceIn(viewport.left, viewport.right - width),
                top = (top + dy).coerceIn(viewport.top, viewport.bottom - height),
                right = (right + dx).coerceIn(viewport.left + width, viewport.right),
                bottom = (bottom + dy).coerceIn(viewport.top + height, viewport.bottom),
            )

            CropHandle.TopLeft -> copy(
                left = (left + dx).coerceIn(viewport.left, right - minimumSize),
                top = (top + dy).coerceIn(viewport.top, bottom - minimumSize),
            )

            CropHandle.TopRight -> copy(
                right = (right + dx).coerceIn(left + minimumSize, viewport.right),
                top = (top + dy).coerceIn(viewport.top, bottom - minimumSize),
            )

            CropHandle.BottomLeft -> copy(
                left = (left + dx).coerceIn(viewport.left, right - minimumSize),
                bottom = (bottom + dy).coerceIn(top + minimumSize, viewport.bottom),
            )

            CropHandle.BottomRight -> copy(
                right = (right + dx).coerceIn(left + minimumSize, viewport.right),
                bottom = (bottom + dy).coerceIn(top + minimumSize, viewport.bottom),
            )

            null -> null
        }
    }
}

private enum class CropHandle {
    Move,
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
}

private fun calculateViewport(
    imageWidth: Int,
    imageHeight: Int,
    containerSize: IntSize,
): Viewport {
    if (imageWidth <= 0 || imageHeight <= 0 || containerSize.width <= 0 || containerSize.height <= 0) {
        return Viewport()
    }

    val scale = min(
        containerSize.width.toFloat() / imageWidth.toFloat(),
        containerSize.height.toFloat() / imageHeight.toFloat(),
    )
    val width = imageWidth * scale
    val height = imageHeight * scale
    val left = (containerSize.width - width) / 2f
    val top = (containerSize.height - height) / 2f
    return Viewport(
        left = left,
        top = top,
        right = left + width,
        bottom = top + height,
    )
}

private fun determineDragHandle(
    point: Offset,
    cropRect: CropRect,
    viewport: Viewport,
    handleRadius: Float,
): CropHandle? {
    if (!cropRect.isInitialized || viewport.width <= 0f || viewport.height <= 0f) return null

    val corners = listOf(
        CropHandle.TopLeft to Offset(cropRect.left, cropRect.top),
        CropHandle.TopRight to Offset(cropRect.right, cropRect.top),
        CropHandle.BottomLeft to Offset(cropRect.left, cropRect.bottom),
        CropHandle.BottomRight to Offset(cropRect.right, cropRect.bottom),
    )
    corners.firstOrNull { (_, handleOffset) ->
        abs(handleOffset.x - point.x) <= handleRadius && abs(handleOffset.y - point.y) <= handleRadius
    }?.let { return it.first }

    return if (
        point.x in cropRect.left..cropRect.right &&
        point.y in cropRect.top..cropRect.bottom
    ) {
        CropHandle.Move
    } else {
        null
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandle(x: Float, y: Float) {
    drawCircle(
        color = Color.White,
        radius = 10f,
        center = Offset(x, y),
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.25f),
        radius = 10f,
        center = Offset(x, y),
        style = Stroke(width = 2f),
    )
}