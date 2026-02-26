package com.quickmemo.app.di

import android.content.Context
import androidx.room.Room
import com.quickmemo.app.data.local.dao.MemoDao
import com.quickmemo.app.data.local.dao.TodoDao
import com.quickmemo.app.data.local.database.DatabaseMigrations
import com.quickmemo.app.data.local.database.QuickMemoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideQuickMemoDatabase(
        @ApplicationContext context: Context,
    ): QuickMemoDatabase {
        return Room.databaseBuilder(
            context,
            QuickMemoDatabase::class.java,
            QuickMemoDatabase.DB_NAME,
        )
            .addMigrations(DatabaseMigrations.MIGRATION_1_2)
            .addMigrations(DatabaseMigrations.MIGRATION_2_3)
            .addMigrations(DatabaseMigrations.MIGRATION_3_4)
            .addMigrations(DatabaseMigrations.MIGRATION_4_5)
            .addMigrations(DatabaseMigrations.MIGRATION_5_6)
            .addMigrations(DatabaseMigrations.MIGRATION_6_7)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideMemoDao(database: QuickMemoDatabase): MemoDao = database.memoDao()

    @Provides
    fun provideTodoDao(database: QuickMemoDatabase): TodoDao = database.todoDao()
}
