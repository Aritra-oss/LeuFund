package com.example.domain

import androidx.compose.runtime.Immutable
import com.example.data.local.EntryEntity
import com.example.data.local.EntryType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class DashboardCategoryFilter {
  ALL,
  INVESTMENT,
  REVENUE,
  DUE,
}

enum class EntrySortOrder {
  NEWEST,
  HIGHEST_AMOUNT,
}

@Immutable
data class EntryUiModel(
  val entry: EntryEntity,
  val initials: String,
  val formattedAmount: String,
  val formattedDate: String,
)

@Immutable
data class BusinessPulseMetrics(
  val projectedNetProfitWithDues: Double = 0.0,
  val collectionRatePercent: Int = 100,
  val roiPercent: Int? = null,
  val recoveryProgress: Float = 0f,
  val topClientName: String? = null,
  val topClientSharePercent: Int? = null,
  val investmentShare: Float = 0f,
  val revenueShare: Float = 0f,
  val dueShare: Float = 0f,
  val totalCashflowVolume: Double = 0.0,
)

@Immutable
data class WhatIfSimulation(
  val simulatedInvestment: Double,
  val simulatedRevenue: Double,
  val simulatedNetProfit: Double,
  val simulatedRecoveryProgress: Float,
  val remainingToBreakEven: Double,
)

object DashboardInsights {
  private val SHORT_DATE_FORMAT =
    object : ThreadLocal<SimpleDateFormat>() {
      override fun initialValue(): SimpleDateFormat =
        SimpleDateFormat("d MMM", Locale.getDefault())
    }

  fun formatShortDate(millis: Long): String {
    if (millis <= 0L) return ""
    return SHORT_DATE_FORMAT.get()?.format(Date(millis)).orEmpty()
  }

  fun toEntryUiModels(entries: List<EntryEntity>): List<EntryUiModel> =
    entries.map { entry ->
      EntryUiModel(
        entry = entry,
        initials = CurrencyFormatter.extractInitials(entry.name),
        formattedAmount = CurrencyFormatter.formatInr(entry.amount),
        formattedDate = formatShortDate(entry.createdAt),
      )
    }

  fun computePulse(
    totalInvestment: Double,
    totalRevenue: Double,
    totalDue: Double,
    topClientName: String? = null,
    topClientAmount: Double = 0.0,
  ): BusinessPulseMetrics {
    val projectedNet = (totalRevenue + totalDue) - totalInvestment
    val billedTotal = totalRevenue + totalDue
    val collectionRate =
      if (billedTotal > 0.0) {
        ((totalRevenue / billedTotal) * 100.0).roundToInt().coerceIn(0, 100)
      } else {
        100
      }

    val roiPercent =
      if (totalInvestment > 0.0) {
        (((totalRevenue - totalInvestment) / totalInvestment) * 100.0).roundToInt()
      } else {
        null
      }

    val recoveryProgress =
      when {
        totalInvestment <= 0.0 && totalRevenue <= 0.0 -> 0f
        totalInvestment <= 0.0 && totalRevenue > 0.0 -> 1f
        else -> (totalRevenue / totalInvestment).toFloat().coerceIn(0f, 1f)
      }

    val topShare =
      if (totalRevenue > 0.0 && topClientAmount > 0.0 && topClientName != null) {
        ((topClientAmount / totalRevenue) * 100.0).roundToInt().coerceIn(1, 100)
      } else {
        null
      }

    val volume = totalInvestment + totalRevenue + totalDue
    val invShare = if (volume > 0.0) (totalInvestment / volume).toFloat().coerceIn(0f, 1f) else 0f
    val revShare = if (volume > 0.0) (totalRevenue / volume).toFloat().coerceIn(0f, 1f) else 0f
    val dueShare = if (volume > 0.0) (totalDue / volume).toFloat().coerceIn(0f, 1f) else 0f

    return BusinessPulseMetrics(
      projectedNetProfitWithDues = projectedNet,
      collectionRatePercent = collectionRate,
      roiPercent = roiPercent,
      recoveryProgress = recoveryProgress,
      topClientName = topClientName,
      topClientSharePercent = topShare,
      investmentShare = invShare,
      revenueShare = revShare,
      dueShare = dueShare,
      totalCashflowVolume = volume,
    )
  }

  fun simulateWhatIf(
    currentInvestment: Double,
    currentRevenue: Double,
    extraInvestment: Double,
    extraRevenue: Double,
    includePendingDue: Double = 0.0,
  ): WhatIfSimulation {
    val simInv = (currentInvestment + extraInvestment).coerceAtLeast(0.0)
    val simRev = (currentRevenue + extraRevenue + includePendingDue).coerceAtLeast(0.0)
    val simNet = simRev - simInv
    val progress =
      when {
        simInv <= 0.0 && simRev <= 0.0 -> 0f
        simInv <= 0.0 && simRev > 0.0 -> 1f
        else -> (simRev / simInv).toFloat().coerceIn(0f, 1f)
      }
    val remaining = (simInv - simRev).coerceAtLeast(0.0)
    return WhatIfSimulation(
      simulatedInvestment = simInv,
      simulatedRevenue = simRev,
      simulatedNetProfit = simNet,
      simulatedRecoveryProgress = progress,
      remainingToBreakEven = remaining,
    )
  }

  fun filterAndSortModels(
    models: List<EntryUiModel>,
    query: String,
    sortOrder: EntrySortOrder,
  ): List<EntryUiModel> {
    val trimmed = query.trim()
    val filtered =
      if (trimmed.isEmpty()) {
        models
      } else {
        models.filter { it.entry.name.contains(trimmed, ignoreCase = true) }
      }
    return when (sortOrder) {
      EntrySortOrder.NEWEST -> filtered
      EntrySortOrder.HIGHEST_AMOUNT -> filtered.sortedByDescending { it.entry.amount }
    }
  }

  fun extractRecentNames(
    investments: List<EntryUiModel>,
    revenues: List<EntryUiModel>,
    dues: List<EntryUiModel>,
    forType: EntryType,
    limit: Int = 6,
  ): List<String> {
    val source =
      when (forType) {
        EntryType.INVESTMENT -> investments
        EntryType.REVENUE -> revenues + dues
        EntryType.DUE -> dues + revenues
      }
    val seen = hashSetOf<String>()
    val result = ArrayList<String>(limit)
    for (item in source) {
      val clean = item.entry.name.trim()
      val key = clean.lowercase(Locale.getDefault())
      if (clean.isNotEmpty() && seen.add(key)) {
        result.add(clean)
        if (result.size >= limit) break
      }
    }
    return result
  }
}
