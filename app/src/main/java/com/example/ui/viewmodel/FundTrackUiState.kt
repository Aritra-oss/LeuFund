package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.example.data.local.BusinessEntity
import com.example.data.local.EntryEntity
import com.example.domain.BackupStatus
import com.example.domain.BusinessPulseMetrics
import com.example.domain.BusinessThemePalette
import com.example.domain.EntryUiModel

@Immutable
data class ClientEarning(
  val displayName: String,
  val totalAmount: Double,
  val fractionOfMax: Float,
  val formattedAmount: String = "",
)

@Immutable
sealed interface FundTrackUiState {
  @Immutable
  data object Loading : FundTrackUiState

  @Immutable
  data class FirstRun(val closedBusinesses: List<BusinessEntity>) : FundTrackUiState

  @Immutable
  data class Dashboard(
    val openBusinesses: List<BusinessEntity>,
    val closedBusinesses: List<BusinessEntity>,
    val selectedBusiness: BusinessEntity,
    val themePalette: BusinessThemePalette,
    val investmentEntries: List<EntryEntity>,
    val revenueEntries: List<EntryEntity>,
    val dueEntries: List<EntryEntity>,
    val investmentModels: List<EntryUiModel> = emptyList(),
    val revenueModels: List<EntryUiModel> = emptyList(),
    val dueModels: List<EntryUiModel> = emptyList(),
    val totalInvestment: Double,
    val totalRevenue: Double,
    val totalDue: Double,
    val netProfit: Double,
    val clientEarnings: List<ClientEarning>,
    val pulseMetrics: BusinessPulseMetrics = BusinessPulseMetrics(),
    val backupStatus: BackupStatus = BackupStatus(),
  ) : FundTrackUiState
}

sealed interface FundTrackEvent {
  data class EntryDeleted(val entry: EntryEntity) : FundTrackEvent

  data class DueMarkedReceived(val entryName: String) : FundTrackEvent

  data class EntryUpdated(val entryName: String) : FundTrackEvent

  data class ExportSuccess(val businessName: String, val entryCount: Int) : FundTrackEvent

  data object ExportError : FundTrackEvent

  data class PdfExportSuccess(
    val businessName: String,
    val shareUriString: String? = null,
    val savedToDownloads: Boolean = false,
  ) : FundTrackEvent

  data object PdfExportError : FundTrackEvent

  data object MpinCreated : FundTrackEvent

  data object MpinChanged : FundTrackEvent
}
