// File: app/src/main/java/com/quickmemo/app/domain/model/TodoItem.kt
package com.quickmemo.app.domain.model

import java.util.UUID

data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val tabId: Int = 0,
    val text: String = "",
    val checked: Boolean = false,
    val dueDate: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val checkedAt: Long? = null,
)
