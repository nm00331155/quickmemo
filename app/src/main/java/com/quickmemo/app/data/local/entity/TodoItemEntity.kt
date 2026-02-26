// File: app/src/main/java/com/quickmemo/app/data/local/entity/TodoItemEntity.kt
package com.quickmemo.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todo_items",
    indices = [
        Index(
            value = ["tabId", "checked", "sortOrder"],
            name = "index_todo_items_tab_checked_sort",
        ),
    ],
)
data class TodoItemEntity(
    @PrimaryKey val id: String,
    val tabId: Int,
    val text: String,
    val checked: Boolean,
    val dueDate: Long?,
    val sortOrder: Int,
    val createdAt: Long,
    val checkedAt: Long?,
)
