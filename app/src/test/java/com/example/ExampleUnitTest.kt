package com.example

import com.example.data.local.BusinessEntity
import com.example.data.local.EntryEntity
import com.example.data.local.EntryType
import com.example.domain.BackupReminderPolicy
import com.example.domain.CsvExporter
import com.example.domain.CurrencyFormatter
import com.example.domain.PdfReportGenerator
import com.example.domain.ThemeMode
import com.example.domain.TransactionFilter
import com.example.ui.viewmodel.FundTrackViewModel
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
}
