package com.quickmemo.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.quickmemo.app.data.local.dao.MemoDao
import com.quickmemo.app.data.local.dao.TodoDao
import com.quickmemo.app.data.local.entity.MemoEntity
import com.quickmemo.app.data.local.entity.MemoFtsEntity
import com.quickmemo.app.data.local.entity.TodoItemEntity

@Database(
    entities = [MemoEntity::class, MemoFtsEntity::class, TodoItemEntity::class],
    version = 7,
    exportSchema = false,
)
abstract class QuickMemoDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
    abstract fun todoDao(): TodoDao

    companion object {
        const val DB_NAME = "quickmemo.db"
    }
}
