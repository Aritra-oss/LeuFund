package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
  @Query("SELECT * FROM businesses WHERE isClosed = 0 ORDER BY createdAt ASC, id ASC")
  fun observeOpenBusinesses(): Flow<List<BusinessEntity>>

  @Query("SELECT * FROM businesses WHERE isClosed = 1 ORDER BY createdAt ASC, id ASC")
  fun observeClosedBusinesses(): Flow<List<BusinessEntity>>

  @Query("SELECT COUNT(*) FROM businesses")
  suspend fun getTotalBusinessCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBusiness(business: BusinessEntity): Long

  @Query("UPDATE businesses SET themeId = :themeId WHERE id = :businessId")
  suspend fun updateTheme(businessId: Long, themeId: Int)

  @Query("UPDATE businesses SET isClosed = :isClosed WHERE id = :businessId")
  suspend fun setClosed(businessId: Long, isClosed: Boolean)

  @Query("DELETE FROM businesses WHERE id = :businessId")
  suspend fun deleteBusinessById(businessId: Long)
}
