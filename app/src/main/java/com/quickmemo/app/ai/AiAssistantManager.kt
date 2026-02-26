package com.quickmemo.app.ai

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class AiAssistantManager @Inject constructor(
    private val aiEngineManager: AiEngineManager,
    private val aiService: AiService,
) {

    suspend fun checkStatus(): AiFeatureStatus {
        val selectedType = aiEngineManager.getSelectedEngineType()
        val selectedEngine = aiEngineManager.getEngine(selectedType)
        if (selectedEngine.isAvailable()) {
            return AiFeatureStatus.AVAILABLE
        }

        return if (selectedType == AiEngineType.QWEN3_LOCAL) {
            AiFeatureStatus.DOWNLOADABLE
        } else {
            AiFeatureStatus.AVAILABLE
        }
    }

    fun summarize(content: String): Flow<AiStreamResult> = flow {
        val lines = normalizedLines(content)

        if (lines.isEmpty()) {
            emit(AiStreamResult.Complete("要約対象のテキストがありません。"))
            return@flow
        }

        emit(AiStreamResult.Partial("解析中..."))

        val summary = runCatching {
            aiService.summarize(content).trim()
        }.getOrNull().orEmpty().ifBlank {
            lines
                .take(3)
                .map { "• $it" }
                .joinToString("\n")
        }

        emit(AiStreamResult.Complete(summary))
    }

    suspend fun suggestTags(content: String): String {
        val generated = runCatching {
            aiService.suggestTags(content)
        }.getOrNull()

        val normalizedGenerated = normalizeTagResult(generated)
        if (normalizedGenerated.isNotBlank()) {
            return normalizedGenerated
        }

        val normalized = content
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 2 }

        val top = normalized
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { "#${it.key}" }

        return if (top.isEmpty()) "#メモ" else top.joinToString(", ")
    }

    suspend fun polish(text: String): String {
        val generated = runCatching {
            aiService.polish(text)
        }.getOrNull()
        if (!generated.isNullOrBlank()) {
            return generated.trim()
        }

        return text
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
            .replace(Regex("\\s+"), " ")
            .replace(" ,", "、")
            .replace(" .", "。")
            .trim()
            .ifBlank { text.trim() }
    }

    suspend fun extractTodos(text: String): String {
        val generated = runCatching {
            aiService.extractTodos(text)
        }.getOrNull()
        val validatedTodos = aiService.validateJson(generated.orEmpty())
        if (!validatedTodos.isNullOrBlank()) {
            return validatedTodos
        }

        val candidates = text
            .lineSequence()
            .map { it.trim(' ', '・', '-', '・', '●', '○') }
            .filter { line ->
                line.isNotBlank() && (
                    line.contains("する") ||
                        line.contains("買") ||
                        line.contains("提出") ||
                        line.contains("連絡") ||
                        line.contains("電話") ||
                        line.contains("確認")
                    )
            }
            .distinct()
            .take(8)
            .toList()

        return JSONArray(candidates).toString()
    }

    suspend fun detectEntities(text: String): String {
        val generated = runCatching {
            aiService.detectEntities(text)
        }.getOrNull()
        val validatedEntities = aiService.validateJson(generated.orEmpty())
        if (!validatedEntities.isNullOrBlank()) {
            return validatedEntities
        }

        val dateCandidates = Regex("\\d{1,2}月\\d{1,2}日|\\d{1,2}/\\d{1,2}|\\d{4}-\\d{2}-\\d{2}")
            .findAll(text)
            .map { it.value }
            .distinct()
            .toList()

        val personCandidates = Regex("[一-龥ぁ-んァ-ン]{2,}(さん|様|君)")
            .findAll(text)
            .map { it.value }
            .distinct()
            .toList()

        val placeCandidates = Regex("[一-龥ぁ-んァ-ン]{2,}(駅|市|区|町|村)")
            .findAll(text)
            .map { it.value }
            .distinct()
            .toList()

        val result = JSONObject().apply {
            put("dates", JSONArray(dateCandidates))
            put("persons", JSONArray(personCandidates))
            put("places", JSONArray(placeCandidates))
        }

        return result.toString()
    }

    suspend fun expandKeywords(keywords: String): String {
        val generated = runCatching {
            aiService.expandKeywords(keywords)
        }.getOrNull()
        if (!generated.isNullOrBlank()) {
            return generated.trim()
        }

        val cleaned = keywords.trim().ifBlank { return "" }
        return "${cleaned}についての要点を整理します。必要な背景・目的・次のアクションを明確にして記録してください。"
    }

    suspend fun extractCalendarEvents(text: String): String {
        val generated = runCatching {
            aiService.extractCalendarEvents(text)
        }.getOrNull()
        val validatedEvents = aiService.validateJson(generated.orEmpty())
        if (!validatedEvents.isNullOrBlank()) {
            return validatedEvents
        }

        val date = Regex("(\\d{4}-\\d{2}-\\d{2}|\\d{1,2}月\\d{1,2}日)")
            .find(text)
            ?.value
            ?: return "[]"
        val time = Regex("(\\d{1,2}:\\d{2})")
            .find(text)
            ?.value
            ?: "09:00"

        val event = JSONObject().apply {
            put("title", "メモから抽出した予定")
            put("date", date)
            put("time", time)
            put("description", text.take(120))
        }

        return JSONArray().put(event).toString()
    }

    private fun normalizedLines(content: String): List<String> {
        return content
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun normalizeTagResult(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return ""

        if (trimmed.startsWith("[")) {
            val tags = runCatching {
                JSONArray(trimmed)
            }.getOrNull() ?: return ""

            val normalized = buildList {
                for (index in 0 until tags.length()) {
                    val value = tags.optString(index).trim().trimStart('#')
                    if (value.isNotBlank()) add("#$value")
                }
            }

            return normalized.joinToString(", ")
        }

        return trimmed
    }
}
