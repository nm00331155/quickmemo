package com.quickmemo.app.ai

import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiService @Inject constructor(
    private val engineManager: AiEngineManager,
) {

    private suspend fun engine(): AiEngineInterface {
        return engineManager.getSelectedEngine()
    }

    suspend fun polish(text: String): String {
        return engine().generate(
            systemPrompt = "あなたは日本語の文章校正アシスタントです。入力されたテキストを自然で読みやすい日本語に整えてください。意味を変えず、誤字脱字を修正し、文体を統一してください。整えた文章のみを出力してください。",
            userMessage = text,
            maxTokens = 1024,
            temperature = 0.2f,
        )
    }

    suspend fun detectEntities(text: String): String {
        return engine().generate(
            systemPrompt = """あなたはテキスト解析アシスタントです。入力テキストから以下を検出し、JSON形式で返してください。
- dates: 日付・時刻の表現（位置情報付き）
- persons: 人名
- places: 場所・地名
出力形式: {\"dates\":[\"2月25日\"],\"persons\":[\"田中\"],\"places\":[\"東京駅\"]}
該当がない場合は空配列にしてください。JSON以外は出力しないでください。""",
            userMessage = text,
            maxTokens = 256,
            temperature = 0.1f,
        )
    }

    suspend fun extractTodos(text: String): String {
        return engine().generate(
            systemPrompt = """あなたはタスク抽出アシスタントです。入力テキストから行動が必要な項目を抽出し、JSON配列で返してください。
出力形式: [\"牛乳を買う\",\"田中さんに電話する\",\"レポートを提出する\"]
行動項目がない場合は空配列[]を返してください。JSON以外は出力しないでください。""",
            userMessage = text,
            maxTokens = 256,
            temperature = 0.1f,
        )
    }

    suspend fun suggestTags(text: String): String {
        return engine().generate(
            systemPrompt = """あなたは分類アシスタントです。入力テキストの内容に最も適したタグを1〜3個選んでください。
選択肢: 仕事, 買い物, アイデア, 日記, 学習, 健康, お金, 旅行, 料理, その他
出力形式: [\"仕事\",\"アイデア\"]
JSON以外は出力しないでください。""",
            userMessage = text,
            maxTokens = 64,
            temperature = 0.1f,
        )
    }

    suspend fun summarize(text: String): String {
        return engine().generate(
            systemPrompt = "あなたは要約アシスタントです。入力テキストを3行以内に要約してください。要約のみを出力してください。",
            userMessage = text,
            maxTokens = 256,
            temperature = 0.2f,
        )
    }

    suspend fun expandKeywords(keywords: String): String {
        return engine().generate(
            systemPrompt = "あなたは文章作成アシスタントです。入力されたキーワードやメモ書きから、自然な日本語の文章を作成してください。文章のみを出力してください。",
            userMessage = keywords,
            maxTokens = 512,
            temperature = 0.5f,
        )
    }

    suspend fun extractCalendarEvents(text: String): String {
        return engine().generate(
            systemPrompt = """あなたはスケジュール抽出アシスタントです。入力テキストからカレンダーに登録すべきイベントを抽出してください。
出力形式: [{\"title\":\"会議\",\"date\":\"2026-03-01\",\"time\":\"14:00\",\"description\":\"プロジェクトA進捗確認\"}]
日時が明示されていない場合やイベントがない場合は空配列[]を返してください。JSON以外は出力しないでください。""",
            userMessage = text,
            maxTokens = 256,
            temperature = 0.1f,
        )
    }

    fun validateJson(output: String): String? {
        val trimmed = output.trim()
        if (trimmed.isEmpty()) return null

        val candidate = if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            trimmed
        } else {
            Regex("(\\[.*]|\\{.*})", RegexOption.DOT_MATCHES_ALL)
                .find(trimmed)
                ?.value
                ?.trim()
                ?: return null
        }

        return runCatching {
            if (candidate.startsWith("[")) {
                JSONArray(candidate)
            } else {
                JSONObject(candidate)
            }
            candidate
        }.getOrNull()
    }
}
