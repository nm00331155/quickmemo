package com.quickmemo.app.data.repository

import com.quickmemo.app.data.local.entity.MemoEntity
import com.quickmemo.app.data.local.entity.TodoItemEntity
import com.quickmemo.app.domain.model.createDefaultMemoBlocks
import com.quickmemo.app.domain.model.decodeMemoBlocks
import com.quickmemo.app.domain.model.encodeMemoBlocks
import com.quickmemo.app.domain.model.Memo
import com.quickmemo.app.domain.model.TodoItem

fun MemoEntity.toDomain(): Memo {
    val resolvedBlocks = decodeMemoBlocks(
        blocksJson = blocks,
        fallbackHtml = contentHtml,
    )

    return Memo(
        id = id,
        title = title,
        contentHtml = contentHtml,
        contentPlainText = contentPlainText,
        blocks = resolvedBlocks,
        colorLabel = colorLabel,
        isPinned = isPinned,
        isLocked = isLocked,
        isChecklist = isChecklist,
        isDeleted = isDeleted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

fun Memo.toEntity(): MemoEntity {
    val resolvedBlocks = if (blocks.isEmpty()) {
        createDefaultMemoBlocks(contentHtml)
    } else {
        blocks
    }

    return MemoEntity(
        id = id,
        title = title,
        contentHtml = contentHtml,
        contentPlainText = contentPlainText,
        blocks = encodeMemoBlocks(resolvedBlocks),
        colorLabel = colorLabel,
        isPinned = isPinned,
        isLocked = isLocked,
        isChecklist = isChecklist,
        isDeleted = isDeleted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

fun TodoItemEntity.toDomain(): TodoItem {
    return TodoItem(
        id = id,
        tabId = tabId,
        text = text,
        checked = checked,
        dueDate = dueDate,
        sortOrder = sortOrder,
        createdAt = createdAt,
        checkedAt = checkedAt,
    )
}

fun TodoItem.toEntity(): TodoItemEntity {
    return TodoItemEntity(
        id = id,
        tabId = tabId,
        text = text,
        checked = checked,
        dueDate = dueDate,
        sortOrder = sortOrder,
        createdAt = createdAt,
        checkedAt = checkedAt,
    )
}
