// File: app/src/main/java/com/quickmemo/app/domain/model/MemoBlockCodec.kt
package com.quickmemo.app.domain.model

import android.os.Build
import android.text.Html
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

sealed interface MemoBlock {
    val id: String

    data class RichTextBlock(
        override val id: String = UUID.randomUUID().toString(),
        val html: String = "",
    ) : MemoBlock

    data class TableBlock(
        override val id: String = UUID.randomUUID().toString(),
        val rows: Int = 3,
        val cols: Int = 3,
        val cells: List<List<String>> = List(rows) { List(cols) { "" } },
    ) : MemoBlock
}

fun createDefaultMemoBlocks(contentHtml: String): List<MemoBlock> {
    return listOf(
        MemoBlock.RichTextBlock(
            html = contentHtml,
        ),
    )
}

fun decodeMemoBlocks(blocksJson: String?, fallbackHtml: String = ""): List<MemoBlock> {
    if (blocksJson.isNullOrBlank()) {
        return createDefaultMemoBlocks(fallbackHtml)
    }

    val parsed = runCatching {
        val result = mutableListOf<MemoBlock>()
        val array = JSONArray(blocksJson)
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            when (obj.optString("type", "")) {
                "rich_text" -> {
                    result += MemoBlock.RichTextBlock(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        html = obj.optString("html", ""),
                    )
                }

                "table" -> {
                    val rows = obj.optInt("rows", 3).coerceIn(1, 10)
                    val cols = obj.optInt("cols", 3).coerceIn(1, 10)
                    val cellsJson = obj.optJSONArray("cells") ?: JSONArray()
                    result += MemoBlock.TableBlock(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        rows = rows,
                        cols = cols,
                        cells = normalizeTableCells(cellsJson, rows, cols),
                    )
                }
            }
        }
        result.toList()
    }.getOrDefault(emptyList())

    if (parsed.isEmpty()) {
        return createDefaultMemoBlocks(fallbackHtml)
    }

    if (parsed.none { it is MemoBlock.RichTextBlock }) {
        return listOf(MemoBlock.RichTextBlock(html = fallbackHtml)) + parsed
    }

    return parsed
}

fun encodeMemoBlocks(blocks: List<MemoBlock>): String {
    val normalized = if (blocks.isEmpty()) {
        createDefaultMemoBlocks("")
    } else {
        blocks
    }

    val array = JSONArray()
    normalized.forEach { block ->
        when (block) {
            is MemoBlock.RichTextBlock -> {
                array.put(
                    JSONObject().apply {
                        put("type", "rich_text")
                        put("id", block.id)
                        put("html", block.html)
                    },
                )
            }

            is MemoBlock.TableBlock -> {
                val rows = block.rows.coerceIn(1, 10)
                val cols = block.cols.coerceIn(1, 10)
                val cells = normalizeTableCells(block.cells, rows, cols)
                array.put(
                    JSONObject().apply {
                        put("type", "table")
                        put("id", block.id)
                        put("rows", rows)
                        put("cols", cols)
                        put(
                            "cells",
                            JSONArray().apply {
                                cells.forEach { row ->
                                    put(
                                        JSONArray().apply {
                                            row.forEach { cell -> put(cell) }
                                        },
                                    )
                                }
                            },
                        )
                    },
                )
            }
        }
    }

    return array.toString()
}

fun memoBlocksToPlainText(blocks: List<MemoBlock>): String {
    if (blocks.isEmpty()) return ""

    return blocks.joinToString(separator = "\n") { block ->
        when (block) {
            is MemoBlock.RichTextBlock -> htmlToPlainText(block.html)
            is MemoBlock.TableBlock -> {
                block.cells.joinToString(separator = "\n") { row ->
                    row.joinToString(separator = "\t")
                }
            }
        }
    }.trim()
}

fun MemoBlock.TableBlock.withUpdatedCell(
    rowIndex: Int,
    colIndex: Int,
    text: String,
): MemoBlock.TableBlock {
    val safeRows = rows.coerceIn(1, 10)
    val safeCols = cols.coerceIn(1, 10)
    val normalized = normalizeTableCells(cells, safeRows, safeCols)
    val updatedCells = normalized.mapIndexed { r, row ->
        row.mapIndexed { c, cell ->
            if (r == rowIndex && c == colIndex) text else cell
        }
    }

    return copy(
        rows = safeRows,
        cols = safeCols,
        cells = updatedCells,
    )
}

private fun normalizeTableCells(
    source: List<List<String>>,
    rows: Int,
    cols: Int,
): List<List<String>> {
    return List(rows) { rowIndex ->
        List(cols) { colIndex ->
            source.getOrNull(rowIndex)?.getOrNull(colIndex).orEmpty()
        }
    }
}

private fun normalizeTableCells(
    source: JSONArray,
    rows: Int,
    cols: Int,
): List<List<String>> {
    val mapped = List(rows) { rowIndex ->
        val row = source.optJSONArray(rowIndex)
        List(cols) { colIndex ->
            row?.optString(colIndex, "").orEmpty()
        }
    }
    return normalizeTableCells(mapped, rows, cols)
}

fun plainTextToHtml(source: String): String {
    if (source.isBlank()) return ""

    return source
        .lines()
        .joinToString(separator = "") { line ->
            if (line.isBlank()) {
                "<p><br></p>"
            } else {
                "<p>${escapeHtmlText(line)}</p>"
            }
        }
}

fun htmlToPlainText(html: String): String {
    if (html.isBlank()) return ""

    val normalizedHtml = html
        .replace(Regex("(?i)<br\\s*/?>"), "<br/>")
        .replace(Regex("(?i)</p>"), "</p>\n")

    val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(normalizedHtml, Html.FROM_HTML_MODE_COMPACT)
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(normalizedHtml)
    }

    return spanned.toString()
        .replace('\u00A0', ' ')
        .trim()
}

private fun escapeHtmlText(source: String): String {
    return source
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
