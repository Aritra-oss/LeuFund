package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.EntryType
import com.example.domain.BusinessThemePalette
import com.example.domain.CsvExporter
import com.example.domain.PdfReportGenerator
import com.example.domain.ThemeMode
import com.example.domain.TransactionFilter
import com.example.ui.components.BusinessTabsRow
import com.example.ui.components.CautionCard
import com.example.ui.components.DashboardSearchBar
import com.example.ui.components.EarningsByClientCard
import com.example.ui.components.EntrySectionCard
import com.example.ui.components.FirstRunScreen
import com.example.ui.components.HeroProfitCard
import com.example.ui.components.SettingsBottomSheet
import com.example.ui.components.SummaryTilesRow
import com.example.ui.theme.FundTrackTheme
import com.example.ui.theme.LocalFundTrackColors
import com.example.ui.viewmodel.FundTrackEvent
import com.example.ui.viewmodel.FundTrackUiState
import com.example.ui.viewmodel.FundTrackViewModel

@Composable
fun FundTrackApp(viewModel: FundTrackViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
  val isSystemDark = isSystemInDarkTheme()
  val isEffectiveDark =
    when (themeMode) {
      ThemeMode.SYSTEM -> isSystemDark
      ThemeMode.LIGHT -> false
      ThemeMode.DARK -> true
    }
  val onQuickToggleTheme = {
    viewModel.setThemeMode(if (isEffectiveDark) ThemeMode.LIGHT else ThemeMode.DARK)
  }

  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  var isSettingsOpen by rememberSaveable { mutableStateOf(false) }

  val activeAccentColor =
    when (val state = uiState) {
      is FundTrackUiState.Dashboard -> state.themePalette.accent
      else -> BusinessThemePalette.GREEN.accent
    }

  val exportCsvLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
      if (uri != null) {
        viewModel.exportCurrentBusinessToCsv { context.contentResolver.openOutputStream(uri) }
      }
    }

  val exportPdfLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
      if (uri != null) {
        viewModel.exportCurrentBusinessToPdf { context.contentResolver.openOutputStream(uri) }
      }
    }

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        is FundTrackEvent.EntryDeleted -> {
          snackbarHostState.currentSnackbarData?.dismiss()
          val result =
            snackbarHostState.showSnackbar(
              message = context.getString(R.string.entry_deleted_snackbar, event.entry.name),
              actionLabel = context.getString(R.string.undo),
              duration = SnackbarDuration.Short,
            )
          if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoDeleteEntry(event.entry)
          }
        }

        is FundTrackEvent.DueMarkedReceived -> {
          snackbarHostState.currentSnackbarData?.dismiss()
          snackbarHostState.showSnackbar(
            message = context.getString(R.string.moved_to_revenue_snackbar, event.entryName),
            duration = SnackbarDuration.Short,
          )
        }

        is FundTrackEvent.ExportSuccess -> {
          snackbarHostState.currentSnackbarData?.dismiss()
          snackbarHostState.showSnackbar(
            message =
              context.getString(
                R.string.export_csv_success,
                event.entryCount,
                event.businessName,
              ),
            duration = SnackbarDuration.Short,
          )
        }

        FundTrackEvent.ExportError -> {
          snackbarHostState.currentSnackbarData?.dismiss()
          snackbarHostState.showSnackbar(
            message = context.getString(R.string.export_csv_error),
            duration = SnackbarDuration.Short,
          )
        }

        is FundTrackEvent.PdfExportSuccess -> {
          snackbarHostState.currentSnackbarData?.dismiss()
          snackbarHostState.showSnackbar(
            message = context.getString(R.string.export_pdf_success, event.businessName),
            duration = SnackbarDuration.Short,
          )
        }

        FundTrackEvent.PdfExportError -> {
          snackbarHostState.currentSnackbarData?.dismiss()
          snackbarHostState.showSnackbar(
            message = context.getString(R.string.export_pdf_error),
            duration = SnackbarDuration.Short,
          )
        }
      }
    }
  }

  FundTrackTheme(accentColor = activeAccentColor, darkTheme = isEffectiveDark) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
      when (val state = uiState) {
        FundTrackUiState.Loading -> {
          Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
        }

        is FundTrackUiState.FirstRun -> {
          if (isSettingsOpen) {
            isSettingsOpen = false
          }
          FirstRunScreen(
            closedBusinesses = state.closedBusinesses,
            isDark = isEffectiveDark,
            onToggleTheme = onQuickToggleTheme,
            onCreateBusiness = viewModel::createBusiness,
            onReopenBusiness = viewModel::reopenBusiness,
            onDeleteBusiness = viewModel::deleteBusiness,
            modifier = Modifier.padding(innerPadding),
          )
        }

        is FundTrackUiState.Dashboard -> {
          DashboardContent(
            state = state,
            isDark = isEffectiveDark,
            onToggleTheme = onQuickToggleTheme,
            onSelectBusiness = viewModel::selectBusiness,
            onAddBusiness = viewModel::createBusiness,
            onOpenSettings = { isSettingsOpen = true },
            onAddEntry = { type, name, amount ->
              viewModel.addEntry(state.selectedBusiness.id, type, name, amount)
            },
            onDeleteEntry = viewModel::deleteEntry,
            onMarkDueReceived = viewModel::markDueAsReceived,
            modifier = Modifier.padding(innerPadding),
          )

          if (isSettingsOpen) {
            SettingsBottomSheet(
              selectedBusiness = state.selectedBusiness,
              closedBusinesses = state.closedBusinesses,
              backupStatus = state.backupStatus,
              themeMode = themeMode,
              onSelectThemeMode = viewModel::setThemeMode,
              onSelectTheme = { themeId ->
                viewModel.updateBusinessTheme(state.selectedBusiness.id, themeId)
              },
              onExportCsv = {
                isSettingsOpen = false
                exportCsvLauncher.launch(CsvExporter.suggestedFileName(state.selectedBusiness.name))
              },
              onExportPdf = {
                isSettingsOpen = false
                exportPdfLauncher.launch(
                  PdfReportGenerator.suggestedFileName(state.selectedBusiness.name)
                )
              },
              onDeleteAllData = {
                viewModel.deleteAllBusinessData(state.selectedBusiness.id)
              },
              onCloseBusiness = {
                viewModel.closeBusiness(state.selectedBusiness.id)
              },
              onReopenBusiness = viewModel::reopenBusiness,
              onDeleteClosedBusiness = viewModel::deleteBusiness,
              onDismiss = { isSettingsOpen = false },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DashboardContent(
  state: FundTrackUiState.Dashboard,
  isDark: Boolean,
  onToggleTheme: () -> Unit,
  onSelectBusiness: (Long) -> Unit,
  onAddBusiness: (String) -> Unit,
  onOpenSettings: () -> Unit,
  onAddEntry: (EntryType, String, Double) -> Unit,
  onDeleteEntry: (com.example.data.local.EntryEntity) -> Unit,
  onMarkDueReceived: (com.example.data.local.EntryEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  var searchQuery by rememberSaveable(state.selectedBusiness.id) { mutableStateOf("") }
  val isFiltering = searchQuery.trim().isNotEmpty()
  val filtered =
    remember(state.investmentEntries, state.revenueEntries, state.dueEntries, searchQuery) {
      TransactionFilter.filterByName(
        investments = state.investmentEntries,
        revenues = state.revenueEntries,
        dues = state.dueEntries,
        query = searchQuery,
      )
    }
  val filteredEmptyMsg = if (isFiltering) stringResource(R.string.no_matching_entries) else null

  Box(modifier = modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
      contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item(key = "business_tabs") {
        BusinessTabsRow(
          openBusinesses = state.openBusinesses,
          selectedBusinessId = state.selectedBusiness.id,
          isDark = isDark,
          onToggleTheme = onToggleTheme,
          onSelectBusiness = onSelectBusiness,
          onAddBusiness = onAddBusiness,
        )
      }

      item(key = "search_bar") {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          DashboardSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            matchingCount = filtered.totalCount,
          )
        }
      }

      item(key = "hero_card") {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          HeroProfitCard(
            business = state.selectedBusiness,
            themePalette = state.themePalette,
            totalInvestment = state.totalInvestment,
            totalRevenue = state.totalRevenue,
            netProfit = state.netProfit,
            onOpenSettings = onOpenSettings,
          )
        }
      }

      item(key = "summary_tiles") {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          SummaryTilesRow(
            totalInvestment = state.totalInvestment,
            totalRevenue = state.totalRevenue,
            totalDue = state.totalDue,
          )
        }
      }

      item(key = "earnings_by_client") {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          EarningsByClientCard(
            clientEarnings = state.clientEarnings,
            accentColor = state.themePalette.accent,
          )
        }
      }

      item(key = "investment_section") {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          EntrySectionCard(
            businessId = state.selectedBusiness.id,
            type = EntryType.INVESTMENT,
            title = stringResource(R.string.section_investment),
            addButtonLabel = stringResource(R.string.add_investment),
            entries = filtered.investments,
            totalAmount =
              if (isFiltering) filtered.investments.sumOf { it.amount } else state.totalInvestment,
            markerColor = semanticColors.softRedContent,
            badgeContainerColor = semanticColors.softRedContainer,
            badgeContentColor = semanticColors.softRedContent,
            onAddEntry = { name, amount -> onAddEntry(EntryType.INVESTMENT, name, amount) },
            onDeleteEntry = onDeleteEntry,
            emptyMessage = filteredEmptyMsg,
          )
        }
      }

      item(key = "revenue_section") {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          EntrySectionCard(
            businessId = state.selectedBusiness.id,
            type = EntryType.REVENUE,
            title = stringResource(R.string.section_revenue),
            addButtonLabel = stringResource(R.string.add_revenue),
            entries = filtered.revenues,
            totalAmount =
              if (isFiltering) filtered.revenues.sumOf { it.amount } else state.totalRevenue,
            markerColor = state.themePalette.accent,
            badgeContainerColor = semanticColors.softGreenContainer,
            badgeContentColor = semanticColors.softGreenContent,
            onAddEntry = { name, amount -> onAddEntry(EntryType.REVENUE, name, amount) },
            onDeleteEntry = onDeleteEntry,
            emptyMessage = filteredEmptyMsg,
          )
        }
      }

      item(key = "due_section") {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          EntrySectionCard(
            businessId = state.selectedBusiness.id,
            type = EntryType.DUE,
            title = stringResource(R.string.section_money_due),
            addButtonLabel = stringResource(R.string.add_money_due),
            entries = filtered.dues,
            totalAmount = if (isFiltering) filtered.dues.sumOf { it.amount } else state.totalDue,
            markerColor = semanticColors.softAmberContent,
            badgeContainerColor = semanticColors.softAmberContainer,
            badgeContentColor = semanticColors.softAmberContent,
            onAddEntry = { name, amount -> onAddEntry(EntryType.DUE, name, amount) },
            onDeleteEntry = onDeleteEntry,
            onMarkDueReceived = onMarkDueReceived,
            emptyMessage = filteredEmptyMsg,
          )
        }
      }

      item(key = "caution_card") {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) { CautionCard() }
      }
    }
  }
}
