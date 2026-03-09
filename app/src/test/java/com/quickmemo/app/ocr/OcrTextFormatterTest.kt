package com.quickmemo.app.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrTextFormatterTest {

    @Test
    fun normalized_compacts_spaces_and_blank_lines() {
        val source = "  項目  1  \n\n\n  項目 2 \t です  "

        val result = OcrTextFormatter.format(source, OcrTextNormalizationMode.Normalized)

        assertEquals("項目 1\n\n項目 2 です", result)
    }

    @Test
    fun normalized_preserves_decimal_points() {
        val source = "価格 12 . 5 円"

        val result = OcrTextFormatter.format(source, OcrTextNormalizationMode.Normalized)

        assertEquals("価格 12.5 円", result)
    }

    @Test
    fun bulletized_unifies_existing_bullets() {
        val source = "- りんご\n1. みかん\n● ぶどう"

        val result = OcrTextFormatter.format(source, OcrTextNormalizationMode.Bulletized)

        assertEquals("・ りんご\n・ みかん\n・ ぶどう", result)
    }
}