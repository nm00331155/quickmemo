package com.quickmemo.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque

object EditorDebugLog {
    private const val FILE_NAME = "editor_debug.log"
    private const val MAX_LINES = 1_000
    private const val MAX_BYTES = 500 * 1024

    private val lineQueue = ArrayDeque<String>()
    private val lock = Any()
    private var initializedPath: String? = null

    private val formatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())

    fun log(
        context: Context,
        category: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        val appContext = context.applicationContext
        synchronized(lock) {
            ensureLoadedLocked(appContext)

            val line = buildString {
                append(formatter.format(Instant.now()))
                append(" [")
                append(category)
                append("] ")
                append(message.replace('\n', ' ').replace('\r', ' '))

                if (throwable != null) {
                    append(" | ")
                    append(throwable::class.java.simpleName)
                    val throwableMessage = throwable.message.orEmpty().replace('\n', ' ').replace('\r', ' ')
                    if (throwableMessage.isNotBlank()) {
                        append(": ")
                        append(throwableMessage)
                    }
                }
            }

            lineQueue.addLast(line)
            while (lineQueue.size > MAX_LINES) {
                lineQueue.removeFirst()
            }

            trimToByteLimitLocked()
            writeLocked(appContext)
        }
    }

    fun buildShareIntent(context: Context): Intent? {
        val appContext = context.applicationContext
        synchronized(lock) {
            ensureLoadedLocked(appContext)
            val logFile = getLogFile(appContext)
            if (!logFile.exists() || logFile.length() == 0L) {
                return null
            }

            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                logFile,
            )
            return Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "QuickMemo debug log")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    fun clear(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            lineQueue.clear()
            initializedPath = getLogFile(appContext).absolutePath
            getLogFile(appContext).delete()
        }
    }

    private fun ensureLoadedLocked(context: Context) {
        val logFile = getLogFile(context)
        val absolutePath = logFile.absolutePath
        if (initializedPath == absolutePath) {
            return
        }

        lineQueue.clear()
        if (logFile.exists()) {
            logFile.readLines(Charsets.UTF_8)
                .takeLast(MAX_LINES)
                .filter { it.isNotBlank() }
                .forEach(lineQueue::addLast)
            trimToByteLimitLocked()
            writeLocked(context)
        }

        initializedPath = absolutePath
    }

    private fun trimToByteLimitLocked() {
        while (lineQueue.isNotEmpty() && currentByteSizeLocked() > MAX_BYTES) {
            lineQueue.removeFirst()
        }
    }

    private fun currentByteSizeLocked(): Int {
        return lineQueue.joinToString(separator = "\n").toByteArray(Charsets.UTF_8).size
    }

    private fun writeLocked(context: Context) {
        val logFile = getLogFile(context)
        if (!logFile.parentFile.exists()) {
            logFile.parentFile?.mkdirs()
        }
        logFile.writeText(lineQueue.joinToString(separator = "\n"), Charsets.UTF_8)
    }

    private fun getLogFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }
}