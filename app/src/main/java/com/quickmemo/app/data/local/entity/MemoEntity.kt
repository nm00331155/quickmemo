package com.quickmemo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val contentHtml: String = "",
    val contentPlainText: String = "",
    val blocks: String = "[]",
    val colorLabel: Int = 0,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val isChecklist: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
)
