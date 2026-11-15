package com.example.domain

import androidx.compose.runtime.Immutable
import com.example.data.local.EntryEntity
import com.example.data.local.EntryType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

@Immutable
sealed interface SummaryPeriod {
  data object Today : SummaryPeriod

  data object Last7Days : SummaryPeriod

  data object Last30Days : SummaryPeriod

  data object LastYear : SummaryPeriod

  data class CalendarYear(val year: Int) : SummaryPeriod

  data object Lifetime : SummaryPeriod
}

@Immutable
data class PeriodAnalytics(
  val period: SummaryPeriod,
  val entryCount: Int,
  val investmentCount: Int,
  val revenueCount: Int,
  val dueCount: Int,
  val totalInvestment: Double,
  val totalRevenue: Double,
  val totalDue: Double,
  val netProfit: Double,
  val roiPercent: Int?,
  val collectionRatePercent: Int,
  val averageTransactionAmount: Double,
  val topClientName: String?,
  val topClientAmount: Double,
  val largestInvestmentName: String?,
  val largestInvestmentAmount: Double,
  val investmentShare: Float,
  val revenueShare: Float,
  val dueShare: Float,
  val rangeStartMillis: Long?,
  val rangeEndMillis: Long?,
)

object FinancialSummaryCalculator {
  private val SHORT_DATE_YEAR_FORMAT =
    object : ThreadLocal<SimpleDateFormat>() {
      override fun initialValue(): SimpleDateFormat =
        SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    }

  private val SHORT_DAY_MONTH_FORMAT =
    object : ThreadLocal<SimpleDateFormat>() {
      override fun initialValue(): SimpleDateFormat =
        SimpleDateFormat("d MMM", Locale.getDefault())
    }

  fun availablePeriods(
    entries: List<EntryEntity>,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): List<SummaryPeriod> {
    val cal = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
    val currentYear = cal.get(Calendar.YEAR)

    val previousYears =
      entries
        .asSequence()
        .filter { it.createdAt > 0L }
        .map { entry ->
          cal.timeInMillis = entry.createdAt
          cal.get(Calendar.YEAR)
        }
        .filter { year -> year < currentYear }
        .distinct()
        .sortedDescending()
        .map { SummaryPeriod.CalendarYear(it) }
        .toList()

    return buildList {
      add(SummaryPeriod.Today)
      add(SummaryPeriod.Last7Days)
      add(SummaryPeriod.Last30Days)
      add(SummaryPeriod.LastYear)
      addAll(previousYears)
      add(SummaryPeriod.Lifetime)
    }
  }

  fun computeAnalytics(
    entries: List<EntryEntity>,
    period: SummaryPeriod,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): PeriodAnalytics {
    val (rangeStart, rangeEnd) = computeRangeBounds(period, nowMillis, timeZone)
    val matching =
      if (rangeStart == null || rangeEnd == null) {
        entries
      } else {
        entries.filter { it.createdAt in rangeStart..rangeEnd }
      }

    val investments = matching.filter { it.type == EntryType.INVESTMENT }
    val revenues = matching.filter { it.type == EntryType.REVENUE }
    val dues = matching.filter { it.type == EntryType.DUE }

    val totalInvestment = investments.sumOf { it.amount }
    val totalRevenue = revenues.sumOf { it.amount }
    val totalDue = dues.sumOf { it.amount }
    val netProfit = totalRevenue - totalInvestment

    val roiPercent =
      if (totalInvestment > 0.0) {
        ((netProfit / totalInvestment) * 100.0).roundToInt()
      } else {
        null
      }

    val billedTotal = totalRevenue + totalDue
    val collectionRatePercent =
      if (billedTotal > 0.0) {
        ((totalRevenue / billedTotal) * 100.0).roundToInt().coerceIn(0, 100)
      } else {
        100
      }

    val totalVolume = totalInvestment + totalRevenue + totalDue
    val averageTransaction = if (matching.isNotEmpty()) totalVolume / matching.size else 0.0

    val (topClientName, topClientAmount) = resolveTopClient(revenues)
    val largestInvestment = investments.maxByOrNull { it.amount }

    val invShare =
      if (totalVolume > 0.0) (totalInvestment / totalVolume).toFloat().coerceIn(0f, 1f) else 0f
    val revShare =
      if (totalVolume > 0.0) (totalRevenue / totalVolume).toFloat().coerceIn(0f, 1f) else 0f
    val dueShare =
      if (totalVolume > 0.0) (totalDue / totalVolume).toFloat().coerceIn(0f, 1f) else 0f

    val effectiveStart =
      rangeStart ?: matching.filter { it.createdAt > 0L }.minOfOrNull { it.createdAt }
    val effectiveEnd =
      rangeEnd ?: matching.filter { it.createdAt > 0L }.maxOfOrNull { it.createdAt }

    return PeriodAnalytics(
      period = period,
      entryCount = matching.size,
      investmentCount = investments.size,
      revenueCount = revenues.size,
      dueCount = dues.size,
      totalInvestment = totalInvestment,
      totalRevenue = totalRevenue,
      totalDue = totalDue,
      netProfit = netProfit,
      roiPercent = roiPercent,
      collectionRatePercent = collectionRatePercent,
      averageTransactionAmount = averageTransaction,
      topClientName = topClientName,
      topClientAmount = topClientAmount,
      largestInvestmentName = largestInvestment?.name?.trim()?.takeIf { it.isNotEmpty() },
      largestInvestmentAmount = largestInvestment?.amount ?: 0.0,
      investmentShare = invShare,
      revenueShare = revShare,
      dueShare = dueShare,
      rangeStartMillis = effectiveStart,
      rangeEndMillis = effectiveEnd,
    )
  }

  fun formatDateRangeCaption(
    analytics: PeriodAnalytics,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): String {
    val start = analytics.rangeStartMillis ?: return ""
    val end = analytics.rangeEndMillis ?: return ""
    val withYearFmt = SHORT_DATE_YEAR_FORMAT.get()?.apply { this.timeZone = timeZone }
    val dayMonthFmt = SHORT_DAY_MONTH_FORMAT.get()?.apply { this.timeZone = timeZone }
    if (withYearFmt == null || dayMonthFmt == null) return ""

    return when (analytics.period) {
      SummaryPeriod.Today -> withYearFmt.format(Date(start))
      SummaryPeriod.Last7Days,
      SummaryPeriod.Last30Days ->
        "${dayMonthFmt.format(Date(start))} – ${withYearFmt.format(Date(end))}"
      SummaryPeriod.LastYear,
      is SummaryPeriod.CalendarYear,
      SummaryPeriod.Lifetime ->
        "${withYearFmt.format(Date(start))} – ${withYearFmt.format(Date(end))}"
    }
  }

  private fun computeRangeBounds(
    period: SummaryPeriod,
    nowMillis: Long,
    timeZone: TimeZone,
  ): Pair<Long?, Long?> {
    if (period == SummaryPeriod.Lifetime) return null to null

    val endCal =
      Calendar.getInstance(timeZone).apply {
        timeInMillis = nowMillis
        if (period is SummaryPeriod.CalendarYear) {
          set(Calendar.YEAR, period.year)
          set(Calendar.MONTH, Calendar.DECEMBER)
          set(Calendar.DAY_OF_MONTH, 31)
        }
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
      }

    val startCal =
      Calendar.getInstance(timeZone).apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        when (period) {
          SummaryPeriod.Today -> Unit
          SummaryPeriod.Last7Days -> add(Calendar.DAY_OF_YEAR, -6)
          SummaryPeriod.Last30Days -> add(Calendar.DAY_OF_YEAR, -29)
          SummaryPeriod.LastYear -> add(Calendar.DAY_OF_YEAR, -364)
          is SummaryPeriod.CalendarYear -> {
            set(Calendar.YEAR, period.year)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
          }
          SummaryPeriod.Lifetime -> Unit
        }
      }

    return startCal.timeInMillis to endCal.timeInMillis
  }

  private fun resolveTopClient(revenues: List<EntryEntity>): Pair<String?, Double> {
    if (revenues.isEmpty()) return null to 0.0
    data class Acc(val name: String, var total: Double)
    val grouped = linkedMapOf<String, Acc>()
    for (entry in revenues) {
      val clean = entry.name.trim()
      if (clean.isEmpty()) continue
      val key = clean.lowercase(Locale.getDefault())
      val existing = grouped[key]
      if (existing == null) {
        grouped[key] = Acc(clean, entry.amount)
      } else {
        existing.total += entry.amount
      }
    }
    val top = grouped.values.maxByOrNull { it.total }
    return (top?.name to (top?.total ?: 0.0))
  }
}
