package com.quickmemo.app.ai

import android.content.Context

object AiCapabilityDetector {
    private val aicorePackages = listOf(
        "com.google.android.aicore",
        "com.samsung.android.aicore",
    )

    fun hasGeminiRuntime(context: Context): Boolean {
        val mlkitGenAiClassesExist = runCatching {
            Class.forName("com.google.mlkit.genai.text.Generation")
            true
        }.getOrDefault(false) || runCatching {
            Class.forName("com.google.mlkit.genai.text.Summarization")
            true
        }.getOrDefault(false)

        val hasAicorePackage = aicorePackages.any { packageName ->
            runCatching {
                context.packageManager.getPackageInfo(packageName, 0)
                true
            }.getOrDefault(false)
        }

        return mlkitGenAiClassesExist || hasAicorePackage
    }
}
