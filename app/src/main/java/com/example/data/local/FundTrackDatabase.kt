package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
  entities = [BusinessEntity::class, EntryEntity::class],
  version = 1,
  exportSchema = false,
)
@TypeConverters(FundTrackConverters::class)
abstract class FundTrackDatabase : RoomDatabase() {
  abstract fun businessDao(): BusinessDao

  abstract fun entryDao(): EntryDao

  companion object {
    @Volatile private var instance: FundTrackDatabase? = null

    fun getInstance(context: Context): FundTrackDatabase {
      return instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                FundTrackDatabase::class.java,
                "fund_track_db",
              )
              .build()
              .also { instance = it }
        }
    }
  }
}
