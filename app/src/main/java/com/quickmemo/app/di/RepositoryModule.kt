package com.quickmemo.app.di

import com.quickmemo.app.data.repository.DictionaryRepositoryImpl
import com.quickmemo.app.data.repository.MemoRepositoryImpl
import com.quickmemo.app.data.repository.SettingsRepositoryImpl
import com.quickmemo.app.data.repository.TodoRepositoryImpl
import com.quickmemo.app.domain.repository.DictionaryRepository
import com.quickmemo.app.domain.repository.MemoRepository
import com.quickmemo.app.domain.repository.SettingsRepository
import com.quickmemo.app.domain.repository.TodoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMemoRepository(impl: MemoRepositoryImpl): MemoRepository

    @Binds
    @Singleton
    abstract fun bindDictionaryRepository(impl: DictionaryRepositoryImpl): DictionaryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindTodoRepository(impl: TodoRepositoryImpl): TodoRepository
}
