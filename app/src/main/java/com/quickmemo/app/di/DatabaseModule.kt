package com.quickmemo.app.di

import android.content.Context
import com.quickmemo.app.data.local.dao.DictionaryDao
import com.quickmemo.app.data.local.dao.MemoDao
import com.quickmemo.app.data.local.dao.TodoDao
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
        return QuickMemoDatabase.getInstance(context)
    }

    @Provides
    fun provideMemoDao(database: QuickMemoDatabase): MemoDao = database.memoDao()

    @Provides
    fun provideTodoDao(database: QuickMemoDatabase): TodoDao = database.todoDao()

    @Provides
    fun provideDictionaryDao(database: QuickMemoDatabase): DictionaryDao = database.dictionaryDao()
}
