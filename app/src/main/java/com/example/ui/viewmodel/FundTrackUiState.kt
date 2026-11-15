package com.example.ui.viewmodel

import com.example.data.local.BusinessEntity
import com.example.data.local.EntryEntity
import com.example.domain.BackupStatus
import com.example.domain.BusinessThemePalette

data class ClientEarning(
  val displayName: String,
  val totalAmount: Double,
  val fractionOfMax: Float,
)

sealed interface FundTrackUiState {
  data object Loading : FundTrackUiState

  data class FirstRun(val closedBusinesses: List<BusinessEntity>) : FundTrackUiState

  data class Dashboard(
    val openBusinesses: List<BusinessEntity>,
    val closedBusinesses: List<BusinessEntity>,
    val selectedBusiness: BusinessEntity,
    val themePalette: BusinessThemePalette,
    val investmentEntries: List<EntryEntity>,
    val revenueEntries: List<EntryEntity>,
    val dueEntries: List<EntryEntity>,
    val totalInvestment: Double,
    val totalRevenue: Double,
    val totalDue: Double,
    val netProfit: Double,
    val clientEarnings: List<ClientEarning>,
    val backupStatus: BackupStatus = BackupStatus(),
  ) : FundTrackUiState
}

sealed interface FundTrackEvent {
  data class EntryDeleted(val entry: EntryEntity) : FundTrackEvent

  data class DueMarkedReceived(val entryName: String) : FundTrackEvent

  data class ExportSuccess(val businessName: String, val entryCount: Int) : FundTrackEvent

  data object ExportError : FundTrackEvent

  data class PdfExportSuccess(val businessName: String) : FundTrackEvent

  data object PdfExportError : FundTrackEvent
}
