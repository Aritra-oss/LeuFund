package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
  @Query("SELECT * FROM entries WHERE businessId = :businessId ORDER BY createdAt DESC, id DESC")
  fun observeEntriesForBusiness(businessId: Long): Flow<List<EntryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEntry(entry: EntryEntity): Long

  @Update
  suspend fun updateEntry(entry: EntryEntity)

  @Query("DELETE FROM entries WHERE id = :entryId")
  suspend fun deleteEntryById(entryId: Long)

  @Query("DELETE FROM entries WHERE businessId = :businessId")
  suspend fun deleteAllForBusiness(businessId: Long)

  @Query("UPDATE entries SET type = :newType WHERE id = :entryId")
  suspend fun updateEntryType(entryId: Long, newType: EntryType)
}
