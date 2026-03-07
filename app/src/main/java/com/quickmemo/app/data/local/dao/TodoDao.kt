// File: app/src/main/java/com/quickmemo/app/data/local/dao/TodoDao.kt
package com.quickmemo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quickmemo.app.data.local.entity.TodoItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items WHERE checked = 0 ORDER BY sortOrder ASC")
    suspend fun getUncheckedItemsSync(): List<TodoItemEntity>

    @Query("SELECT COUNT(*) FROM todo_items WHERE checked = 1")
    suspend fun getCheckedCountSync(): Int

    @Query(
        """
        SELECT * FROM todo_items
        WHERE tabId = :tabId AND checked = 0
        ORDER BY sortOrder ASC, createdAt ASC
        """
    )
    fun getUncheckedItems(tabId: Int): Flow<List<TodoItemEntity>>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE tabId = :tabId AND checked = 1
        ORDER BY checkedAt DESC, createdAt DESC
        """
    )
    fun getCheckedItems(tabId: Int): Flow<List<TodoItemEntity>>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE tabId = :tabId AND checked = 0
        ORDER BY sortOrder ASC, createdAt ASC
        """
    )
    suspend fun getUncheckedItemsSnapshot(tabId: Int): List<TodoItemEntity>

    @Query("SELECT COUNT(*) FROM todo_items WHERE tabId = :tabId")
    fun getTotalCount(tabId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM todo_items WHERE tabId = :tabId AND checked = 1")
    fun getCompletedCount(tabId: Int): Flow<Int>

    @Query("SELECT * FROM todo_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): TodoItemEntity?

    @Query("SELECT * FROM todo_items ORDER BY tabId ASC, checked ASC, sortOrder ASC, createdAt ASC")
    suspend fun getAllForBackup(): List<TodoItemEntity>

    @Query(
        """
        SELECT COALESCE(MAX(sortOrder), -1)
        FROM todo_items
        WHERE tabId = :tabId AND checked = 0
        """
    )
    suspend fun getMaxSortOrder(tabId: Int): Int

    @Query(
        """
        SELECT * FROM todo_items
        WHERE dueDate IS NOT NULL
          AND checked = 0
          AND dueDate <= :endOfDayMillis
        ORDER BY dueDate ASC, sortOrder ASC
        """
    )
    suspend fun getDueItems(endOfDayMillis: Long): List<TodoItemEntity>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE dueDate IS NOT NULL
          AND checked = 0
        ORDER BY dueDate ASC, sortOrder ASC
        """
    )
    suspend fun getItemsWithDueDate(): List<TodoItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: TodoItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<TodoItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllForBackup(items: List<TodoItemEntity>)

    @Query("UPDATE todo_items SET text = :text WHERE id = :id")
    suspend fun updateText(id: String, text: String)

    @Query("UPDATE todo_items SET dueDate = :dueDate WHERE id = :id")
    suspend fun updateDueDate(id: String, dueDate: Long?)

    @Query(
        """
        UPDATE todo_items
        SET checked = :checked,
            checkedAt = :checkedAt
        WHERE id = :id
        """
    )
    suspend fun updateCheckedState(id: String, checked: Boolean, checkedAt: Long?)

    @Query(
        """
        UPDATE todo_items
        SET checked = 0,
            checkedAt = NULL,
            sortOrder = :sortOrder
        WHERE id = :id
        """
    )
    suspend fun moveToUncheckedTail(id: String, sortOrder: Int)

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM todo_items")
    suspend fun deleteAllForBackup()
}
