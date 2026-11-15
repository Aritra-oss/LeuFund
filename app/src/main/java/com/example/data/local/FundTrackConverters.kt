package com.example.data.local

import androidx.room.TypeConverter

class FundTrackConverters {
  @TypeConverter
  fun fromEntryType(type: EntryType): String = type.name

  @TypeConverter
  fun toEntryType(value: String): EntryType = EntryType.valueOf(value)
}
