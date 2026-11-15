package com.example

import com.example.data.local.BusinessEntity
import com.example.data.local.EntryEntity
import com.example.data.local.EntryType
import com.example.domain.BackupReminderPolicy
import com.example.domain.BiometricAuthStrategy
import com.example.domain.BiometricLockPolicy
import com.example.domain.CsvExporter
import com.example.domain.CurrencyFormatter
import com.example.domain.DashboardInsights
import com.example.domain.EntrySortOrder
import com.example.domain.FinancialSummaryCalculator
import com.example.domain.MpinManager
import com.example.domain.PdfReportGenerator
import com.example.domain.SummaryPeriod
import com.example.domain.ThemeMode
import com.example.domain.TransactionFilter
import com.example.ui.viewmodel.FundTrackViewModel
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun formatInr_usesIndianDigitGrouping() {
    assertEquals("₹0", CurrencyFormatter.formatInr(0.0))
    assertEquals("₹500", CurrencyFormatter.formatInr(500.0))
    assertEquals("₹1,500", CurrencyFormatter.formatInr(1500.0))
    assertEquals("₹15,000", CurrencyFormatter.formatInr(15000.0))
    assertEquals("₹1,50,000", CurrencyFormatter.formatInr(150000.0))
    assertEquals("₹1,23,45,678", CurrencyFormatter.formatInr(12345678.0))
    assertEquals("₹1,50,000.50", CurrencyFormatter.formatInr(150000.5))
    assertEquals("-₹45,000", CurrencyFormatter.formatInr(-45000.0))
  }

  @Test
  fun parseValidAmount_rejectsNegativeAndNonNumeric() {
    assertEquals(0.0, CurrencyFormatter.parseValidAmount("0")!!, 0.001)
    assertEquals(1250.75, CurrencyFormatter.parseValidAmount("1250.75")!!, 0.001)
    assertNull(CurrencyFormatter.parseValidAmount(""))
    assertNull(CurrencyFormatter.parseValidAmount("-100"))
    assertNull(CurrencyFormatter.parseValidAmount("abc"))
  }

  @Test
  fun computeClientEarnings_groupsIgnoringCaseAndSortsDescending() {
    val entries =
      listOf(
        EntryEntity(id = 1, businessId = 1, type = EntryType.REVENUE, name = "Acme Corp", amount = 5000.0),
        EntryEntity(id = 2, businessId = 1, type = EntryType.REVENUE, name = "beta studio", amount = 12000.0),
        EntryEntity(id = 3, businessId = 1, type = EntryType.REVENUE, name = "acme corp ", amount = 15000.0),
      )

    val result = FundTrackViewModel.computeClientEarnings(entries)
    assertEquals(2, result.size)
    assertEquals("Acme Corp", result[0].displayName)
    assertEquals(20000.0, result[0].totalAmount, 0.001)
    assertEquals(1.0f, result[0].fractionOfMax, 0.001f)
    assertEquals("beta studio", result[1].displayName)
    assertEquals(12000.0, result[1].totalAmount, 0.001)
    assertEquals(0.6f, result[1].fractionOfMax, 0.001f)
  }

  @Test
  fun csvExporter_escapesFieldsAndFormatsRows() {
    val business = BusinessEntity(id = 1, name = "Tea, \"Co\"", themeId = 0)
    val entries =
      listOf(
        EntryEntity(
          id = 1,
          businessId = 1,
          type = EntryType.INVESTMENT,
          name = "Equipment, Stove",
          amount = 150000.0,
          createdAt = 1700000000000L,
        ),
        EntryEntity(
          id = 2,
          businessId = 1,
          type = EntryType.DUE,
          name = "Rohan \"VIP\"",
          amount = 2500.5,
          createdAt = 1700000005000L,
        ),
      )

    val csv = CsvExporter.buildBusinessCsv(business, entries)
    assertTrue(csv.startsWith("\uFEFFBusiness,Type,Name,Amount (INR),Formatted Amount,Date"))
    assertTrue(csv.contains("\"Tea, \"\"Co\"\"\",Investment,\"Equipment, Stove\",150000,\"₹1,50,000\","))
    assertTrue(csv.contains("\"Tea, \"\"Co\"\"\",Money due,\"Rohan \"\"VIP\"\"\",2500.5,\"₹2,500.50\","))
  }

  @Test
  fun csvExporter_generatesSanitizedFilename() {
    val filename = CsvExporter.suggestedFileName("Maity's Bakery & Cafe!", 1700000000000L)
    assertTrue(filename.startsWith("leufunds_maity_s_bakery_cafe_"))
    assertTrue(filename.endsWith(".csv"))
  }

  @Test
  fun themeMode_fromStorageParsesAndFallsBackToSystem() {
    assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(null))
    assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("INVALID"))
    assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorage("LIGHT"))
    assertEquals(ThemeMode.DARK, ThemeMode.fromStorage("DARK"))
  }

  @Test
  fun pdfReportGenerator_generatesSanitizedFilename() {
    val filename = PdfReportGenerator.suggestedFileName("Maity's Bakery & Cafe!", 1700000000000L)
    assertTrue(filename.startsWith("leufunds_maity_s_bakery_cafe_report_"))
    assertTrue(filename.endsWith(".pdf"))
  }

  @Test
  fun backupReminderPolicy_triggersOnlyWhenHasEntriesAndOver30Days() {
    val dayMs = 24L * 60L * 60L * 1000L
    val now = 100L * dayMs

    // Empty business should not trigger reminder
    val emptyBiz =
      BackupReminderPolicy.evaluate(
        lastExportMillis = now - 45L * dayMs,
        baselineCreatedAtMillis = now - 45L * dayMs,
        hasAnyEntries = false,
        nowMillis = now,
      )
    assertFalse(emptyBiz.shouldRemind)

    // Exported within 30 days should not trigger
    val recentExport =
      BackupReminderPolicy.evaluate(
        lastExportMillis = now - 15L * dayMs,
        baselineCreatedAtMillis = now - 60L * dayMs,
        hasAnyEntries = true,
        nowMillis = now,
      )
    assertFalse(recentExport.shouldRemind)
    assertEquals(15, recentExport.daysSinceLastExport)

    // Exported 31 days ago should trigger
    val staleExport =
      BackupReminderPolicy.evaluate(
        lastExportMillis = now - 31L * dayMs,
        baselineCreatedAtMillis = now - 60L * dayMs,
        hasAnyEntries = true,
        nowMillis = now,
      )
    assertTrue(staleExport.shouldRemind)
    assertEquals(31, staleExport.daysSinceLastExport)

    // Never exported, business created 5 days ago should not trigger
    val freshNeverExported =
      BackupReminderPolicy.evaluate(
        lastExportMillis = null,
        baselineCreatedAtMillis = now - 5L * dayMs,
        hasAnyEntries = true,
        nowMillis = now,
      )
    assertFalse(freshNeverExported.shouldRemind)

    // Never exported, business created 31 days ago should trigger
    val oldNeverExported =
      BackupReminderPolicy.evaluate(
        lastExportMillis = null,
        baselineCreatedAtMillis = now - 31L * dayMs,
        hasAnyEntries = true,
        nowMillis = now,
      )
    assertTrue(oldNeverExported.shouldRemind)
    assertNull(oldNeverExported.daysSinceLastExport)
  }

  @Test
  fun transactionFilter_filtersAcrossAllCategoriesIgnoringCase() {
    val inv = listOf(EntryEntity(id = 1, businessId = 1, type = EntryType.INVESTMENT, name = "Rohan Packaging", amount = 2000.0))
    val rev = listOf(
      EntryEntity(id = 2, businessId = 1, type = EntryType.REVENUE, name = "Rohan Traders", amount = 5000.0),
      EntryEntity(id = 3, businessId = 1, type = EntryType.REVENUE, name = "Maya Boutique", amount = 7000.0),
    )
    val due = listOf(EntryEntity(id = 4, businessId = 1, type = EntryType.DUE, name = "rohan retail", amount = 1500.0))

    val matched = TransactionFilter.filterByName(inv, rev, due, "  ROHAN ")
    assertEquals(1, matched.investments.size)
    assertEquals(1, matched.revenues.size)
    assertEquals(1, matched.dues.size)
    assertEquals(3, matched.totalCount)

    val blankQuery = TransactionFilter.filterByName(inv, rev, due, "   ")
    assertEquals(4, blankQuery.totalCount)
  }

  @Test
  fun dashboardInsights_computesPulseAndWhatIfSimulation() {
    val pulse =
      DashboardInsights.computePulse(
        totalInvestment = 100_000.0,
        totalRevenue = 75_000.0,
        totalDue = 25_000.0,
        topClientName = "Acme Corp",
        topClientAmount = 45_000.0,
      )
    assertEquals(0.0, pulse.projectedNetProfitWithDues, 0.001)
    assertEquals(75, pulse.collectionRatePercent)
    assertEquals(-25, pulse.roiPercent)
    assertEquals(0.75f, pulse.recoveryProgress, 0.001f)
    assertEquals(60, pulse.topClientSharePercent)

    val sim =
      DashboardInsights.simulateWhatIf(
        currentInvestment = 100_000.0,
        currentRevenue = 75_000.0,
        extraInvestment = 10_000.0,
        extraRevenue = 25_000.0,
        includePendingDue = 25_000.0,
      )
    assertEquals(110_000.0, sim.simulatedInvestment, 0.001)
    assertEquals(125_000.0, sim.simulatedRevenue, 0.001)
    assertEquals(15_000.0, sim.simulatedNetProfit, 0.001)
    assertEquals(0.0, sim.remainingToBreakEven, 0.001)
  }

  @Test
  fun dashboardInsights_sortsByAmountAndDeduplicatesRecentNames() {
    val models =
      DashboardInsights.toEntryUiModels(
        listOf(
          EntryEntity(id = 1, businessId = 1, type = EntryType.REVENUE, name = "Alpha", amount = 2_000.0),
          EntryEntity(id = 2, businessId = 1, type = EntryType.REVENUE, name = "Beta", amount = 9_000.0),
          EntryEntity(id = 3, businessId = 1, type = EntryType.REVENUE, name = "alpha ", amount = 5_000.0),
        )
      )
    val sorted = DashboardInsights.filterAndSortModels(models, "", EntrySortOrder.HIGHEST_AMOUNT)
    assertEquals(listOf(9_000.0, 5_000.0, 2_000.0), sorted.map { it.entry.amount })

    val recent = DashboardInsights.extractRecentNames(emptyList(), models, emptyList(), EntryType.REVENUE)
    assertEquals(listOf("Alpha", "Beta"), recent)
  }

  @Test
  fun biometricLockPolicy_resolvesSafeStrategyAcrossApiLevels() {
    // Android 11+ (API 30) with biometric + credential available
    val api30Strategy =
      BiometricLockPolicy.resolveStrategy(
        sdkInt = 30,
        canAuthenticateCombinedStatus = BiometricLockPolicy.STATUS_SUCCESS,
        canAuthenticateWeakOnlyStatus = BiometricLockPolicy.STATUS_SUCCESS,
        isDeviceSecure = true,
      )
    assertEquals(BiometricAuthStrategy.BIOMETRIC_OR_CREDENTIAL_PROMPT, api30Strategy)
    assertTrue(BiometricLockPolicy.requiresSystemAuthentication(api30Strategy))

    // Android 10 (API 29) with weak biometric enrolled uses BIOMETRIC_ONLY_PROMPT
    val api29Biometric =
      BiometricLockPolicy.resolveStrategy(
        sdkInt = 29,
        canAuthenticateCombinedStatus = BiometricLockPolicy.STATUS_ERROR_NO_HARDWARE,
        canAuthenticateWeakOnlyStatus = BiometricLockPolicy.STATUS_SUCCESS,
        isDeviceSecure = true,
      )
    assertEquals(BiometricAuthStrategy.BIOMETRIC_ONLY_PROMPT, api29Biometric)
    assertTrue(BiometricLockPolicy.requiresSystemAuthentication(api29Biometric))

    // Android 10 (API 29) without biometric enrolled, but with PIN/Pattern set -> Keyguard fallback
    val api29PinOnly =
      BiometricLockPolicy.resolveStrategy(
        sdkInt = 29,
        canAuthenticateCombinedStatus = BiometricLockPolicy.STATUS_ERROR_NONE_ENROLLED,
        canAuthenticateWeakOnlyStatus = BiometricLockPolicy.STATUS_ERROR_NONE_ENROLLED,
        isDeviceSecure = true,
      )
    assertEquals(BiometricAuthStrategy.KEYGUARD_CREDENTIAL_FALLBACK, api29PinOnly)
    assertTrue(BiometricLockPolicy.requiresSystemAuthentication(api29PinOnly))

    // Device with neither biometric enrollment nor screen lock
    val noLock =
      BiometricLockPolicy.resolveStrategy(
        sdkInt = 33,
        canAuthenticateCombinedStatus = BiometricLockPolicy.STATUS_ERROR_NONE_ENROLLED,
        canAuthenticateWeakOnlyStatus = BiometricLockPolicy.STATUS_ERROR_NONE_ENROLLED,
        isDeviceSecure = false,
      )
    assertEquals(BiometricAuthStrategy.NOT_ENROLLED_NO_LOCK, noLock)
    assertFalse(BiometricLockPolicy.requiresSystemAuthentication(noLock))
  }

  @Test
  fun financialSummaryCalculator_aggregatesTimeframesAndDiscoversPreviousYears() {
    val utc = TimeZone.getTimeZone("UTC")
    fun millisFor(year: Int, month: Int, day: Int, hour: Int = 12): Long =
      Calendar.getInstance(utc).apply {
        set(year, month, day, hour, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }.timeInMillis

    val now = millisFor(2026, Calendar.NOVEMBER, 15, 14)

    val entries =
      listOf(
        // Today (Nov 15, 2026)
        EntryEntity(id = 1, businessId = 1, type = EntryType.REVENUE, name = "Acme Corp", amount = 5000.0, createdAt = millisFor(2026, Calendar.NOVEMBER, 15, 9)),
        // 3 days ago (Nov 12, 2026 -> in Last 7 Days, Last 30 Days, Last Year, Lifetime)
        EntryEntity(id = 2, businessId = 1, type = EntryType.INVESTMENT, name = "Packaging", amount = 2000.0, createdAt = millisFor(2026, Calendar.NOVEMBER, 12, 10)),
        // 20 days ago (Oct 26, 2026 -> in Last 30 Days, Last Year, Lifetime)
        EntryEntity(id = 3, businessId = 1, type = EntryType.DUE, name = "Beta Co", amount = 3000.0, createdAt = millisFor(2026, Calendar.OCTOBER, 26, 10)),
        // 150 days ago (Jun 18, 2026 -> in Last Year, Lifetime)
        EntryEntity(id = 4, businessId = 1, type = EntryType.REVENUE, name = "Acme Corp", amount = 10000.0, createdAt = millisFor(2026, Calendar.JUNE, 18, 10)),
        // Previous year 2025 (Mar 10, 2025 -> in Year 2025 and Lifetime, outside Last Year 365d)
        EntryEntity(id = 5, businessId = 1, type = EntryType.INVESTMENT, name = "Oven", amount = 25000.0, createdAt = millisFor(2025, Calendar.MARCH, 10, 10)),
        // Older previous year 2023
        EntryEntity(id = 6, businessId = 1, type = EntryType.REVENUE, name = "Legacy Client", amount = 40000.0, createdAt = millisFor(2023, Calendar.MAY, 5, 10)),
      )

    val periods = FinancialSummaryCalculator.availablePeriods(entries, nowMillis = now, timeZone = utc)
    assertEquals(
      listOf(
        SummaryPeriod.Today,
        SummaryPeriod.Last7Days,
        SummaryPeriod.Last30Days,
        SummaryPeriod.LastYear,
        SummaryPeriod.CalendarYear(2025),
        SummaryPeriod.CalendarYear(2023),
        SummaryPeriod.Lifetime,
      ),
      periods,
    )

    // Without prior-year entries, previous years are omitted
    val currentYearOnlyPeriods =
      FinancialSummaryCalculator.availablePeriods(entries.take(2), nowMillis = now, timeZone = utc)
    assertEquals(
      listOf(
        SummaryPeriod.Today,
        SummaryPeriod.Last7Days,
        SummaryPeriod.Last30Days,
        SummaryPeriod.LastYear,
        SummaryPeriod.Lifetime,
      ),
      currentYearOnlyPeriods,
    )

    val todayStats = FinancialSummaryCalculator.computeAnalytics(entries, SummaryPeriod.Today, nowMillis = now, timeZone = utc)
    assertEquals(1, todayStats.entryCount)
    assertEquals(5000.0, todayStats.totalRevenue, 0.001)
    assertEquals(5000.0, todayStats.netProfit, 0.001)

    val sevenDayStats = FinancialSummaryCalculator.computeAnalytics(entries, SummaryPeriod.Last7Days, nowMillis = now, timeZone = utc)
    assertEquals(2, sevenDayStats.entryCount)
    assertEquals(2000.0, sevenDayStats.totalInvestment, 0.001)
    assertEquals(5000.0, sevenDayStats.totalRevenue, 0.001)
    assertEquals(3000.0, sevenDayStats.netProfit, 0.001)

    val thirtyDayStats = FinancialSummaryCalculator.computeAnalytics(entries, SummaryPeriod.Last30Days, nowMillis = now, timeZone = utc)
    assertEquals(3, thirtyDayStats.entryCount)
    assertEquals(3000.0, thirtyDayStats.totalDue, 0.001)

    val lastYearStats = FinancialSummaryCalculator.computeAnalytics(entries, SummaryPeriod.LastYear, nowMillis = now, timeZone = utc)
    assertEquals(4, lastYearStats.entryCount)
    assertEquals(15000.0, lastYearStats.totalRevenue, 0.001)
    assertEquals("Acme Corp", lastYearStats.topClientName)
    assertEquals(15000.0, lastYearStats.topClientAmount, 0.001)

    val year2025Stats = FinancialSummaryCalculator.computeAnalytics(entries, SummaryPeriod.CalendarYear(2025), nowMillis = now, timeZone = utc)
    assertEquals(1, year2025Stats.entryCount)
    assertEquals(25000.0, year2025Stats.totalInvestment, 0.001)
    assertEquals(-25000.0, year2025Stats.netProfit, 0.001)

    val lifetimeStats = FinancialSummaryCalculator.computeAnalytics(entries, SummaryPeriod.Lifetime, nowMillis = now, timeZone = utc)
    assertEquals(6, lifetimeStats.entryCount)
    assertEquals(27000.0, lifetimeStats.totalInvestment, 0.001)
    assertEquals(55000.0, lifetimeStats.totalRevenue, 0.001)
    assertEquals(3000.0, lifetimeStats.totalDue, 0.001)
    assertEquals(28000.0, lifetimeStats.netProfit, 0.001)
  }

  @Test
  fun mpinManager_validatesHashesVerifiesAndTracksLockout() {
    assertTrue(MpinManager.isValidFormat("0429"))
    assertFalse(MpinManager.isValidFormat("042"))
    assertFalse(MpinManager.isValidFormat("04291"))
    assertFalse(MpinManager.isValidFormat("12a4"))

    val hash1 = MpinManager.hashPin("2580")
    val hash2 = MpinManager.hashPin("2580")
    // Different random salts must produce distinct records for the same PIN
    assertTrue(hash1 != hash2)
    assertTrue(MpinManager.verifyPin("2580", hash1))
    assertTrue(MpinManager.verifyPin("2580", hash2))
    assertFalse(MpinManager.verifyPin("2581", hash1))
    assertFalse(MpinManager.verifyPin("123", hash1))
    assertFalse(MpinManager.verifyPin("2580", null))
    assertFalse(MpinManager.verifyPin("2580", "malformed:record"))

    val t0 = 1_000_000L
    val attempt1 = MpinManager.recordFailedAttempt(0, nowMillis = t0)
    assertEquals(1, attempt1.failedAttempts)
    assertEquals(4, attempt1.remainingAttemptsBeforeLockout)
    assertEquals(0L, attempt1.lockoutUntilMillis)

    val attempt5 = MpinManager.recordFailedAttempt(4, nowMillis = t0)
    assertEquals(5, attempt5.failedAttempts)
    assertEquals(0, attempt5.remainingAttemptsBeforeLockout)
    assertEquals(t0 + MpinManager.LOCKOUT_DURATION_MS, attempt5.lockoutUntilMillis)
    assertEquals(30, MpinManager.remainingLockoutSeconds(attempt5.lockoutUntilMillis, nowMillis = t0))
    assertEquals(10, MpinManager.remainingLockoutSeconds(attempt5.lockoutUntilMillis, nowMillis = t0 + 20_000L))
    assertEquals(0, MpinManager.remainingLockoutSeconds(attempt5.lockoutUntilMillis, nowMillis = t0 + 30_000L))
  }
}
