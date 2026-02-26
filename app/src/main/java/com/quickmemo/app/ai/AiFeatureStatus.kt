package com.quickmemo.app.ai

enum class AiFeatureStatus {
    AVAILABLE,
    DOWNLOADABLE,
    UNAVAILABLE,
}

sealed interface AiStreamResult {
    data class Partial(val text: String) : AiStreamResult
    data class Complete(val text: String) : AiStreamResult
}
