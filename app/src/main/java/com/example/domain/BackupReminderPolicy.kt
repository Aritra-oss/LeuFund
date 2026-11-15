package com.example.domain

import androidx.compose.runtime.Immutable

@Immutable
data class BackupStatus(
  val lastExportMillis: Long? = null,
  val shouldRemind: Boolean = false,
  val daysSinceLastExport: Int? = null,
)

object BackupReminderPolicy {
  private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
  const val REMINDER_THRESHOLD_DAYS = 30L
  const val REMINDER_THRESHOLD_MILLIS = REMINDER_THRESHOLD_DAYS * DAY_MILLIS

  fun evaluate(
    lastExportMillis: Long?,
    baselineCreatedAtMillis: Long?,
    hasAnyEntries: Boolean,
    nowMillis: Long = System.currentTimeMillis(),
  ): BackupStatus {
    val daysSinceLastExport =
      lastExportMillis?.let { ((nowMillis - it).coerceAtLeast(0L) / DAY_MILLIS).toInt() }

    if (!hasAnyEntries) {
      return BackupStatus(
        lastExportMillis = lastExportMillis,
        shouldRemind = false,
        daysSinceLastExport = daysSinceLastExport,
      )
    }

    val referenceMillis = lastExportMillis ?: baselineCreatedAtMillis
    val shouldRemind =
      if (referenceMillis == null) {
        false
      } else {
        (nowMillis - referenceMillis).coerceAtLeast(0L) > REMINDER_THRESHOLD_MILLIS
      }

    return BackupStatus(
      lastExportMillis = lastExportMillis,
      shouldRemind = shouldRemind,
      daysSinceLastExport = daysSinceLastExport,
    )
  }
}
