package com.quickmemo.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.quickmemo.app.data.local.dao.DictionaryDao
import com.quickmemo.app.data.local.dao.MemoDao
import com.quickmemo.app.data.local.dao.TodoDao
import com.quickmemo.app.data.local.entity.DictionaryEntryEntity
import com.quickmemo.app.data.local.entity.MemoEntity
import com.quickmemo.app.data.local.entity.MemoFtsEntity
import com.quickmemo.app.data.local.entity.TodoItemEntity

@Database(
    entities = [MemoEntity::class, MemoFtsEntity::class, TodoItemEntity::class, DictionaryEntryEntity::class],
    version = 8,
    exportSchema = false,
)
abstract class QuickMemoDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
    abstract fun todoDao(): TodoDao
    abstract fun dictionaryDao(): DictionaryDao

    companion object {
        @Volatile
        private var INSTANCE: QuickMemoDatabase? = null

        const val DB_NAME = "quickmemo.db"

        fun getInstance(context: Context): QuickMemoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuickMemoDatabase::class.java,
                    DB_NAME,
                )
                    .addMigrations(DatabaseMigrations.MIGRATION_1_2)
                    .addMigrations(DatabaseMigrations.MIGRATION_2_3)
                    .addMigrations(DatabaseMigrations.MIGRATION_3_4)
                    .addMigrations(DatabaseMigrations.MIGRATION_4_5)
                    .addMigrations(DatabaseMigrations.MIGRATION_5_6)
                    .addMigrations(DatabaseMigrations.MIGRATION_6_7)
                    .addMigrations(DatabaseMigrations.MIGRATION_7_8)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
