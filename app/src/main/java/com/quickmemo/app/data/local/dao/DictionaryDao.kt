package com.quickmemo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quickmemo.app.data.local.entity.DictionaryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary_entries ORDER BY sortOrder ASC, id ASC")
    fun getAllFlow(): Flow<List<DictionaryEntryEntity>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM dictionary_entries")
    suspend fun getMaxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DictionaryEntryEntity): Long

    @Update
    suspend fun update(entry: DictionaryEntryEntity)

    @Delete
    suspend fun delete(entry: DictionaryEntryEntity)
}
