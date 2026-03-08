package com.quickmemo.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memo_backups",
    indices = [
        Index(value = ["memoId"]),
        Index(value = ["backupType"]),
        Index(value = ["createdAt"]),
    ],
)
data class MemoBackupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memoId: Long,
    val title: String,
    val contentHtml: String,
    val backupType: String,
    val createdAt: Long = System.currentTimeMillis(),
)
