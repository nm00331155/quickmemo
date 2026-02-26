package com.quickmemo.app.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = MemoEntity::class)
@Entity(tableName = "memos_fts")
data class MemoFtsEntity(
    val title: String,
    val contentPlainText: String,
)
