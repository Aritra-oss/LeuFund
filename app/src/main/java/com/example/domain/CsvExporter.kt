package com.example.domain

import com.example.data.local.BusinessEntity
import com.example.data.local.EntryEntity
import com.example.data.local.EntryType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

  private const val UTF8_BOM = "\uFEFF"

  fun suggestedFileName(businessName: String, nowMillis: Long = System.currentTimeMillis()): String {
    val sanitized =
      businessName
        .trim()
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifEmpty { "business" }
        .take(32)
    val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(nowMillis))
    return "leufunds_${sanitized}_${dateStamp}.csv"
  }

  fun buildBusinessCsv(business: BusinessEntity, entries: List<EntryEntity>): String {
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    val sb = StringBuilder()
    sb.append(UTF8_BOM)
    sb.appendLine("Business,Type,Name,Amount (INR),Formatted Amount,Date")

    val orderedEntries = entries.sortedWith(compareBy<EntryEntity> { it.createdAt }.thenBy { it.id })
    for (entry in orderedEntries) {
      val typeLabel =
        when (entry.type) {
          EntryType.INVESTMENT -> "Investment"
          EntryType.REVENUE -> "Revenue"
          EntryType.DUE -> "Money due"
        }
      val plainAmount = formatPlainAmount(entry.amount)
      val formattedAmount = CurrencyFormatter.formatInr(entry.amount)
      val formattedDate = dateFormatter.format(Date(entry.createdAt))

      sb.append(escapeCsvField(business.name))
      sb.append(',')
      sb.append(escapeCsvField(typeLabel))
      sb.append(',')
      sb.append(escapeCsvField(entry.name))
      sb.append(',')
      sb.append(escapeCsvField(plainAmount))
      sb.append(',')
      sb.append(escapeCsvField(formattedAmount))
      sb.append(',')
      sb.append(escapeCsvField(formattedDate))
      sb.appendLine()
    }

    return sb.toString()
  }

  internal fun escapeCsvField(value: String): String {
    val needsQuotes =
      value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
    if (!needsQuotes) return value
    return "\"${value.replace("\"", "\"\"")}\""
  }

  private fun formatPlainAmount(amount: Double): String {
    if (amount.isNaN() || amount.isInfinite()) return "0"
    val decimal = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
    return if (decimal.scale() <= 0) {
      decimal.toBigInteger().toString()
    } else {
      decimal.toPlainString()
    }
  }
}
