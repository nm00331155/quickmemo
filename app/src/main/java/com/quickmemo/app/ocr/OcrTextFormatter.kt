package com.quickmemo.app.ocr

enum class OcrTextNormalizationMode {
    Raw,
    Normalized,
    Bulletized,
}

data class OcrFormattedText(
    val raw: String,
    val normalized: String,
    val bulletized: String,
) {
    fun textFor(mode: OcrTextNormalizationMode): String {
        return when (mode) {
            OcrTextNormalizationMode.Raw -> raw
            OcrTextNormalizationMode.Normalized -> normalized
            OcrTextNormalizationMode.Bulletized -> bulletized
        }
    }
}

object OcrTextFormatter {
    private val bulletPrefixRegex = Regex("""^(?:[・●•*]|[-–—]+|\d+[.)])\s*""")
    private val asciiPunctuationRegex = Regex("""\s*([,.:;!?])\s*""")
    private val japanesePunctuationRegex = Regex("""\s*([、。！？：；])\s*""")
    private val openBracketRegex = Regex("""([\(\[\{「『【])\s+""")
    private val closeBracketRegex = Regex("""\s+([\)\]\}」』】])""")
    private val multiSpaceRegex = Regex("""[ \u3000]{2,}""")

    fun formatAll(source: String): OcrFormattedText {
        val raw = normalizeLineEndings(source).trim()
        val normalized = format(raw, OcrTextNormalizationMode.Normalized)
        val bulletized = format(raw, OcrTextNormalizationMode.Bulletized)
        return OcrFormattedText(
            raw = raw,
            normalized = normalized,
            bulletized = bulletized,
        )
    }

    fun format(source: String, mode: OcrTextNormalizationMode): String {
        val normalizedSource = normalizeLineEndings(source)
        return when (mode) {
            OcrTextNormalizationMode.Raw -> normalizedSource.trim()
            OcrTextNormalizationMode.Normalized -> normalize(normalizedSource)
            OcrTextNormalizationMode.Bulletized -> bulletize(normalizedSource)
        }
    }

    private fun normalize(source: String): String {
        val normalizedLines = source
            .replace('\t', ' ')
            .lines()
            .map { rawLine -> normalizeLine(rawLine) }

        return joinWithLimitedBlankLines(normalizedLines)
    }

    private fun bulletize(source: String): String {
        val normalizedLines = normalize(source).lines()
        val meaningfulLines = normalizedLines.filter { it.isNotBlank() }
        val convertLooseLines = meaningfulLines.size >= 3 &&
            meaningfulLines.count(::isLooseBulletCandidate) >= meaningfulLines.size * 2 / 3

        val bulletizedLines = normalizedLines.map { line ->
            when {
                line.isBlank() -> ""
                bulletPrefixRegex.containsMatchIn(line) -> "・ ${line.replace(bulletPrefixRegex, "").trim()}".trim()
                convertLooseLines && isLooseBulletCandidate(line) -> "・ ${line.trim()}"
                else -> line
            }
        }

        return joinWithLimitedBlankLines(bulletizedLines)
    }

    private fun normalizeLine(line: String): String {
        if (line.isBlank()) {
            return ""
        }

        val trimmed = line.trim()
        val bulletNormalized = trimmed.replace(bulletPrefixRegex) { "・ " }
        return bulletNormalized
            .replace(multiSpaceRegex, " ")
            .replace(japanesePunctuationRegex, "$1")
            .replace(asciiPunctuationRegex) { match ->
                val punctuation = match.groupValues[1]
                if (punctuation == "." && looksLikeDecimal(trimmed, match.range.first)) {
                    punctuation
                } else {
                    "$punctuation "
                }
            }
            .replace(openBracketRegex, "$1")
            .replace(closeBracketRegex, "$1")
            .replace(multiSpaceRegex, " ")
            .trim()
    }

    private fun joinWithLimitedBlankLines(lines: List<String>): String {
        val result = mutableListOf<String>()
        var previousBlank = false
        lines.forEach { line ->
            if (line.isBlank()) {
                if (!previousBlank && result.isNotEmpty()) {
                    result += ""
                }
                previousBlank = true
            } else {
                result += line.trim()
                previousBlank = false
            }
        }
        return result.joinToString(separator = "\n").trim()
    }

    private fun isLooseBulletCandidate(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isBlank()) return false
        if (trimmed.contains("://")) return false
        if (trimmed.endsWith("。") || trimmed.endsWith(".") || trimmed.endsWith("!")) return false
        return trimmed.length <= 32 && trimmed.split(Regex("""\s+""")).size <= 8
    }

    private fun looksLikeDecimal(source: String, punctuationIndex: Int): Boolean {
        val before = source.getOrNull(punctuationIndex - 1)
        val after = source.getOrNull(punctuationIndex + 1)
        return before?.isDigit() == true && after?.isDigit() == true
    }

    private fun normalizeLineEndings(source: String): String {
        return source.replace("\r\n", "\n").replace('\r', '\n')
    }
}