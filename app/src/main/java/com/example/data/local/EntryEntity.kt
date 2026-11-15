package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "entries",
  foreignKeys =
    [
      ForeignKey(
        entity = BusinessEntity::class,
        parentColumns = ["id"],
        childColumns = ["businessId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index(value = ["businessId"])],
)
data class EntryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val businessId: Long,
  val type: EntryType,
  val name: String,
  val amount: Double,
  val createdAt: Long = System.currentTimeMillis(),
)
