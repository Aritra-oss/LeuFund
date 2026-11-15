package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "businesses")
data class BusinessEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val themeId: Int,
  val isClosed: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
)
