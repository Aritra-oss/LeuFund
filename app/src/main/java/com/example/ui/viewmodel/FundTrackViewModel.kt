package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BusinessEntity
import com.example.data.local.EntryEntity
import com.example.data.local.EntryType
import com.example.data.repository.FundTrackRepository
import com.example.domain.BackupReminderPolicy
import com.example.domain.BusinessThemePalette
import com.example.domain.CsvExporter
import com.example.domain.CurrencyFormatter
import com.example.domain.DashboardInsights
import com.example.domain.PdfReportGenerator
import com.example.domain.PdfSaveOutcome
import com.example.domain.ThemeMode
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class FundTrackViewModel(private val repository: FundTrackRepository) : ViewModel() {

  private val _events = MutableSharedFlow<FundTrackEvent>()
  val events: SharedFlow<FundTrackEvent> = _events.asSharedFlow()

  private val activeSelectionFlow =
    combine(repository.openBusinesses, repository.selectedBusinessId) { openList, savedId ->
      if (openList.isEmpty()) {
        null
      } else {
        openList.firstOrNull { it.id == savedId } ?: openList.first()
      }
    }

  private val entriesForActiveBusinessFlow =
    activeSelectionFlow.flatMapLatest { selectedBusiness ->
      if (selectedBusiness == null) {
        flowOf(emptyList())
      } else {
        repository.observeEntriesForBusiness(selectedBusiness.id)
      }
    }

  val uiState: StateFlow<FundTrackUiState> =
    combine(
        repository.openBusinesses,
        repository.closedBusinesses,
        activeSelectionFlow,
        entriesForActiveBusinessFlow,
        repository.lastExportMillis,
      ) { openList, closedList, selectedBusiness, entries, lastExportMillis ->
        if (selectedBusiness == null || openList.isEmpty()) {
          FundTrackUiState.FirstRun(closedBusinesses = closedList)
        } else {
          buildDashboardState(
            openBusinesses = openList,
            closedBusinesses = closedList,
            selectedBusiness = selectedBusiness,
            entries = entries,
            lastExportMillis = lastExportMillis,
          )
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = FundTrackUiState.Loading,
      )

  val themeMode: StateFlow<ThemeMode> =
    repository.themeMode.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000L),
      initialValue = ThemeMode.SYSTEM,
    )

  val biometricLockEnabled: StateFlow<Boolean?> =
    repository.biometricLockEnabled.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000L),
      initialValue = null,
    )

  val hasMpin: StateFlow<Boolean?> =
    repository.hasMpin.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000L),
      initialValue = null,
    )

  private val _isSessionUnlocked = MutableStateFlow(false)
  val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

  fun unlockSession() {
    _isSessionUnlocked.value = true
  }

  fun lockSession() {
    _isSessionUnlocked.value = false
  }

  fun setBiometricLockEnabled(enabled: Boolean) {
    // Keep current interactive settings session unlocked; next launch applies updated mode.
    _isSessionUnlocked.value = true
    viewModelScope.launch { repository.setBiometricLockEnabled(enabled) }
  }

  fun setupInitialMpin(pin: String) {
    viewModelScope.launch {
      repository.setupMpin(pin)
      _isSessionUnlocked.value = true
      _events.emit(FundTrackEvent.MpinCreated)
    }
  }

  suspend fun verifyMpinAndUnlock(pin: String): Boolean {
    val isValid = repository.verifyMpin(pin)
    if (isValid) {
      _isSessionUnlocked.value = true
    }
    return isValid
  }

  suspend fun changeMpin(currentPin: String, newPin: String): Boolean {
    val updated = repository.changeMpin(currentPin, newPin)
    if (updated) {
      _events.emit(FundTrackEvent.MpinChanged)
    }
    return updated
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { repository.setThemeMode(mode) }
  }

  fun createBusiness(name: String) {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return
    viewModelScope.launch { repository.createBusiness(trimmed) }
  }

  fun selectBusiness(businessId: Long) {
    viewModelScope.launch { repository.selectBusiness(businessId) }
  }

  fun updateBusinessTheme(businessId: Long, themeId: Int) {
    viewModelScope.launch { repository.updateBusinessTheme(businessId, themeId) }
  }

  fun closeBusiness(businessId: Long) {
    viewModelScope.launch { repository.closeBusiness(businessId) }
  }

  fun reopenBusiness(businessId: Long) {
    viewModelScope.launch { repository.reopenBusiness(businessId) }
  }

  fun deleteBusiness(businessId: Long) {
    viewModelScope.launch { repository.deleteBusiness(businessId) }
  }

  fun deleteAllBusinessData(businessId: Long) {
    viewModelScope.launch { repository.deleteAllBusinessData(businessId) }
  }

  fun addEntry(businessId: Long, type: EntryType, name: String, amount: Double) {
    val trimmed = name.trim()
    if (trimmed.isEmpty() || amount < 0.0 || amount.isNaN() || amount.isInfinite()) return
    viewModelScope.launch { repository.addEntry(businessId, type, trimmed, amount) }
  }

  fun deleteEntry(entry: EntryEntity) {
    viewModelScope.launch {
      repository.deleteEntry(entry.id)
      _events.emit(FundTrackEvent.EntryDeleted(entry))
    }
  }

  fun undoDeleteEntry(entry: EntryEntity) {
    viewModelScope.launch { repository.restoreEntry(entry) }
  }

  fun markDueAsReceived(entry: EntryEntity) {
    viewModelScope.launch {
      repository.markDueAsReceived(entry.id)
      _events.emit(FundTrackEvent.DueMarkedReceived(entry.name))
    }
  }

  fun updateEntry(existing: EntryEntity, newType: EntryType, newName: String, newAmount: Double) {
    val trimmed = newName.trim()
    if (trimmed.isEmpty() || newAmount < 0.0 || newAmount.isNaN() || newAmount.isInfinite()) {
      return
    }
    viewModelScope.launch {
      repository.updateEntry(existing, newType, trimmed, newAmount)
      _events.emit(FundTrackEvent.EntryUpdated(trimmed))
    }
  }

  fun exportCurrentBusinessToCsv(openOutputStream: () -> OutputStream?) {
    val dashboard = uiState.value as? FundTrackUiState.Dashboard
    if (dashboard == null) {
      viewModelScope.launch { _events.emit(FundTrackEvent.ExportError) }
      return
    }
    val allEntries = dashboard.investmentEntries + dashboard.revenueEntries + dashboard.dueEntries
    val csvContent = CsvExporter.buildBusinessCsv(dashboard.selectedBusiness, allEntries)

    viewModelScope.launch {
      val succeeded =
        withContext(Dispatchers.IO) {
          runCatching {
              val stream = openOutputStream() ?: return@runCatching false
              stream.use {
                it.write(csvContent.toByteArray(Charsets.UTF_8))
                it.flush()
              }
              true
            }
            .getOrDefault(false)
        }
      if (succeeded) {
        repository.recordExportCompleted()
        _events.emit(
          FundTrackEvent.ExportSuccess(
            businessName = dashboard.selectedBusiness.name,
            entryCount = allEntries.size,
          )
        )
      } else {
        _events.emit(FundTrackEvent.ExportError)
      }
    }
  }

  fun exportCurrentBusinessPdf(
    persistPdf: (fileName: String, pdfBytes: ByteArray) -> PdfSaveOutcome?,
    onFallbackCreateDocument: (businessName: String, fileName: String, pdfBytes: ByteArray, shareUriString: String) -> Unit,
  ) {
    val dashboard = uiState.value as? FundTrackUiState.Dashboard
    if (dashboard == null) {
      viewModelScope.launch { _events.emit(FundTrackEvent.PdfExportError) }
      return
    }
    val businessName = dashboard.selectedBusiness.name
    val fileName = PdfReportGenerator.suggestedFileName(businessName)

    viewModelScope.launch {
      val result =
        withContext(Dispatchers.IO) {
          runCatching {
              val bytes = PdfReportGenerator.buildBusinessReportPdfBytes(dashboard)
              val outcome = persistPdf(fileName, bytes) ?: return@runCatching null
              Pair(bytes, outcome)
            }
            .getOrNull()
        }

      if (result == null) {
        _events.emit(FundTrackEvent.PdfExportError)
        return@launch
      }

      val (pdfBytes, outcome) = result
      if (outcome.savedToDownloads) {
        repository.recordExportCompleted()
        _events.emit(
          FundTrackEvent.PdfExportSuccess(
            businessName = businessName,
            shareUriString = outcome.shareUri.toString(),
            savedToDownloads = true,
          )
        )
      } else {
        onFallbackCreateDocument(
          businessName,
          fileName,
          pdfBytes,
          outcome.shareUri.toString(),
        )
      }
    }
  }

  fun writePdfBytesToStream(
    businessName: String,
    pdfBytes: ByteArray,
    shareUriString: String?,
    openOutputStream: () -> OutputStream?,
  ) {
    viewModelScope.launch {
      val succeeded =
        withContext(Dispatchers.IO) {
          runCatching {
              val stream = openOutputStream() ?: return@runCatching false
              stream.use {
                it.write(pdfBytes)
                it.flush()
              }
              true
            }
            .getOrDefault(false)
        }
      if (succeeded) {
        repository.recordExportCompleted()
        _events.emit(
          FundTrackEvent.PdfExportSuccess(
            businessName = businessName,
            shareUriString = shareUriString,
            savedToDownloads = false,
          )
        )
      } else {
        _events.emit(FundTrackEvent.PdfExportError)
      }
    }
  }

  fun recordPdfExportCompleted(businessName: String, shareUriString: String?) {
    viewModelScope.launch {
      repository.recordExportCompleted()
      _events.emit(
        FundTrackEvent.PdfExportSuccess(
          businessName = businessName,
          shareUriString = shareUriString,
          savedToDownloads = false,
        )
      )
    }
  }

  fun reportPdfExportError() {
    viewModelScope.launch { _events.emit(FundTrackEvent.PdfExportError) }
  }

  fun exportCurrentBusinessToPdf(openOutputStream: () -> OutputStream?) {
    val dashboard = uiState.value as? FundTrackUiState.Dashboard
    if (dashboard == null) {
      viewModelScope.launch { _events.emit(FundTrackEvent.PdfExportError) }
      return
    }

    viewModelScope.launch {
      val succeeded =
        withContext(Dispatchers.IO) {
          runCatching {
              val stream = openOutputStream() ?: return@runCatching false
              stream.use { PdfReportGenerator.writeBusinessReportPdf(dashboard, it) }
              true
            }
            .getOrDefault(false)
        }
      if (succeeded) {
        repository.recordExportCompleted()
        _events.emit(FundTrackEvent.PdfExportSuccess(dashboard.selectedBusiness.name))
      } else {
        _events.emit(FundTrackEvent.PdfExportError)
      }
    }
  }

  companion object {
    fun buildDashboardState(
      openBusinesses: List<BusinessEntity>,
      closedBusinesses: List<BusinessEntity>,
      selectedBusiness: BusinessEntity,
      entries: List<EntryEntity>,
      lastExportMillis: Long? = null,
    ): FundTrackUiState.Dashboard {
      val investments = entries.filter { it.type == EntryType.INVESTMENT }
      val revenues = entries.filter { it.type == EntryType.REVENUE }
      val dues = entries.filter { it.type == EntryType.DUE }

      val totalInvestment = investments.sumOf { it.amount }
      val totalRevenue = revenues.sumOf { it.amount }
      val totalDue = dues.sumOf { it.amount }
      val netProfit = totalRevenue - totalInvestment

      val clientEarnings = computeClientEarnings(revenues)
      val topClient = clientEarnings.firstOrNull()
      val pulseMetrics =
        DashboardInsights.computePulse(
          totalInvestment = totalInvestment,
          totalRevenue = totalRevenue,
          totalDue = totalDue,
          topClientName = topClient?.displayName,
          topClientAmount = topClient?.totalAmount ?: 0.0,
        )
      val baselineCreatedAt = (openBusinesses + closedBusinesses).minOfOrNull { it.createdAt }
      val backupStatus =
        BackupReminderPolicy.evaluate(
          lastExportMillis = lastExportMillis,
          baselineCreatedAtMillis = baselineCreatedAt,
          hasAnyEntries = entries.isNotEmpty(),
        )

      return FundTrackUiState.Dashboard(
        openBusinesses = openBusinesses,
        closedBusinesses = closedBusinesses,
        selectedBusiness = selectedBusiness,
        themePalette = BusinessThemePalette.fromId(selectedBusiness.themeId),
        investmentEntries = investments,
        revenueEntries = revenues,
        dueEntries = dues,
        investmentModels = DashboardInsights.toEntryUiModels(investments),
        revenueModels = DashboardInsights.toEntryUiModels(revenues),
        dueModels = DashboardInsights.toEntryUiModels(dues),
        totalInvestment = totalInvestment,
        totalRevenue = totalRevenue,
        totalDue = totalDue,
        netProfit = netProfit,
        clientEarnings = clientEarnings,
        pulseMetrics = pulseMetrics,
        backupStatus = backupStatus,
      )
    }

    fun computeClientEarnings(revenueEntries: List<EntryEntity>): List<ClientEarning> {
      if (revenueEntries.isEmpty()) return emptyList()

      data class Accumulator(val displayName: String, var total: Double)

      val grouped = linkedMapOf<String, Accumulator>()
      for (entry in revenueEntries) {
        val cleanName = entry.name.trim()
        val key = cleanName.lowercase()
        val existing = grouped[key]
        if (existing == null) {
          grouped[key] = Accumulator(displayName = cleanName, total = entry.amount)
        } else {
          existing.total += entry.amount
        }
      }

      val sorted = grouped.values.sortedByDescending { it.total }
      val maxAmount = sorted.firstOrNull()?.total ?: 0.0
      return sorted.map { acc ->
        val fraction =
          if (maxAmount > 0.0) {
            (acc.total / maxAmount).toFloat().coerceIn(0f, 1f)
          } else {
            0f
          }
        ClientEarning(
          displayName = acc.displayName,
          totalAmount = acc.total,
          fractionOfMax = fraction,
          formattedAmount = CurrencyFormatter.formatInr(acc.total),
        )
      }
    }
  }

  class Factory(private val repository: FundTrackRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(FundTrackViewModel::class.java)) {
        return FundTrackViewModel(repository) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
