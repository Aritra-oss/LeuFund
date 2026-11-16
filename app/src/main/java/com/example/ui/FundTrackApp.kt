package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.R
import com.example.data.local.EntryEntity
import com.example.data.local.EntryType
import com.example.domain.BiometricAuthStrategy
import com.example.domain.BiometricLockPolicy
import com.example.domain.BusinessThemePalette
import com.example.domain.CsvExporter
import com.example.domain.DashboardCategoryFilter
import com.example.domain.DashboardInsights
import com.example.domain.EntrySortOrder
import com.example.domain.PdfReportGenerator
import com.example.domain.ThemeMode
import com.example.security.BiometricAuthenticator
import com.example.ui.components.BiometricLockScreen
import com.example.ui.components.BusinessTabsRow
import com.example.ui.components.CashflowRadarCard
import com.example.ui.components.CautionCard
import com.example.ui.components.ChangeMpinBottomSheet
import com.example.ui.components.DashboardSearchBar
import com.example.ui.components.EarningsByClientCard
import com.example.ui.components.FinancialSummaryBottomSheet
import com.example.ui.components.FirstRunScreen
import com.example.ui.components.GlassAmbientBackdrop
import com.example.ui.components.HeroProfitCard
import com.example.ui.components.MpinSetupScreen
import com.example.ui.components.ProfitSimulatorSheet
import com.example.ui.components.QuickEntryBottomSheet
import com.example.ui.components.SettingsBottomSheet
import com.example.ui.components.SummaryTilesRow
import com.example.ui.components.futuristicPressScale
import com.example.ui.components.ledgerSection
import com.example.ui.theme.FundTrackTheme
import com.example.ui.theme.LocalFundTrackColors
import com.example.ui.viewmodel.FundTrackEvent
import com.example.ui.viewmodel.FundTrackUiState
import com.example.ui.viewmodel.FundTrackViewModel

@Composable
fun FundTrackApp(viewModel: FundTrackViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
  val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsStateWithLifecycle()
  val hasMpin by viewModel.hasMpin.collectAsStateWithLifecycle()
  val isSessionUnlocked by viewModel.isSessionUnlocked.collectAsStateWithLifecycle()
  val isSystemDark = isSystemInDarkTheme()
  val isEffectiveDark =
    when (themeMode) {
      ThemeMode.SYSTEM -> isSystemDark
      ThemeMode.LIGHT -> false
      ThemeMode.DARK -> true
    }
  val onQuickToggleTheme =
    remember(isEffectiveDark, viewModel) {
      { viewModel.setThemeMode(if (isEffectiveDark) ThemeMode.LIGHT else ThemeMode.DARK) }
    }

  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val fragmentActivity = remember(context) { context.findFragmentActivity() }

  var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
  var isChangeMpinOpen by rememberSaveable { mutableStateOf(false) }
  var isPrivacyMasked by rememberSaveable { mutableStateOf(false) }
  var isSimulatorOpen by rememberSaveable { mutableStateOf(false) }
  var isSummaryOpen by rememberSaveable { mutableStateOf(false) }
  var isQuickEntryOpen by rememberSaveable { mutableStateOf(false) }
  var editingEntry by remember { mutableStateOf<EntryEntity?>(null) }

  var authStrategy by
    remember(context) { mutableStateOf(BiometricAuthenticator.evaluateStrategy(context)) }
  var authStatusMessage by remember { mutableStateOf<String?>(null) }
  var forceMpinSignal by remember { mutableIntStateOf(0) }
  var hasAutoPromptedThisLock by remember { mutableStateOf(false) }

  val isSecurityPrefsLoaded = biometricLockEnabled != null && hasMpin != null
  val needsMandatoryMpinSetup = hasMpin == false
  val isVaultLockActive = hasMpin == true && !isSessionUnlocked
  val shouldAutoPromptBiometrics =
    isVaultLockActive &&
      biometricLockEnabled == true &&
      BiometricLockPolicy.requiresSystemAuthentication(authStrategy)

  val keyguardCredentialLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        authStatusMessage = null
        viewModel.unlockSession()
      } else {
        authStatusMessage = context.getString(R.string.biometric_canceled_hint)
      }
    }

  DisposableEffect(lifecycleOwner, context) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        authStrategy = BiometricAuthenticator.evaluateStrategy(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  fun launchUnlockPrompt() {
    val currentStrategy = BiometricAuthenticator.evaluateStrategy(context)
    authStrategy = currentStrategy
    authStatusMessage = null

    when (currentStrategy) {
      BiometricAuthStrategy.BIOMETRIC_OR_CREDENTIAL_PROMPT,
      BiometricAuthStrategy.BIOMETRIC_ONLY_PROMPT -> {
        val activity = fragmentActivity
        if (activity != null) {
          BiometricAuthenticator.authenticateWithBiometricPrompt(
            activity = activity,
            strategy = currentStrategy,
            onSucceeded = {
              authStatusMessage = null
              viewModel.unlockSession()
            },
            onUseMpinSelected = {
              authStatusMessage = null
              forceMpinSignal += 1
            },
            onCanceled = {
              authStatusMessage = context.getString(R.string.biometric_canceled_hint)
            },
            onError = { message ->
              authStatusMessage = context.getString(R.string.biometric_error_hint, message)
            },
          )
        }
      }

      BiometricAuthStrategy.KEYGUARD_CREDENTIAL_FALLBACK -> {
        val intent = BiometricAuthenticator.createKeyguardCredentialIntent(context)
        if (intent != null) {
          keyguardCredentialLauncher.launch(intent)
        }
      }

      BiometricAuthStrategy.NOT_ENROLLED_NO_LOCK,
      BiometricAuthStrategy.UNAVAILABLE_NO_LOCK -> {
        forceMpinSignal += 1
      }
    }
  }

  LaunchedEffect(isVaultLockActive, shouldAutoPromptBiometrics, lifecycleOwner) {
    if (!isVaultLockActive) {
      hasAutoPromptedThisLock = false
      authStatusMessage = null
      forceMpinSignal = 0
      return@LaunchedEffect
    }
    isSettingsOpen = false
    isChangeMpinOpen = false
    isSimulatorOpen = false
    isSummaryOpen = false
    isQuickEntryOpen = false
    editingEntry = null

    if (!shouldAutoPromptBiometrics) {
      return@LaunchedEffect
    }

    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      if (!hasAutoPromptedThisLock) {
        val refreshed = BiometricAuthenticator.evaluateStrategy(context)
        authStrategy = refreshed
        if (biometricLockEnabled == true && BiometricLockPolicy.requiresSystemAuthentication(refreshed)) {
          hasAutoPromptedThisLock = true
          launchUnlockPrompt()
        }
      }
    }
  }

  val activeThemePalette =
    when (val state = uiState) {
      is FundTrackUiState.Dashboard -> state.themePalette
      else -> BusinessThemePalette.GREEN
    }
  val activeAccentColor = activeThemePalette.accent

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

        is FundTrackEvent.EntryUpdated -> {
          snackbarHostState.currentSnackbarData?.dismiss()
          snackbarHostState.showSnackbar(
            message = context.getString(R.string.entry_updated_snackbar, event.entryName),
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

        FundTrackEvent.MpinCreated -> {
          snackbarHostState.currentSnackbarData?.dismiss()
          snackbarHostState.showSnackbar(
            message = context.getString(R.string.mpin_created_snackbar),
            duration = SnackbarDuration.Short,
          )
        }

        FundTrackEvent.MpinChanged -> {
          snackbarHostState.currentSnackbarData?.dismiss()
          snackbarHostState.showSnackbar(
            message = context.getString(R.string.mpin_changed_snackbar),
            duration = SnackbarDuration.Short,
          )
        }
      }
    }
  }

  FundTrackTheme(accentColor = activeAccentColor, darkTheme = isEffectiveDark) {
    Box(modifier = Modifier.fillMaxSize()) {
      GlassAmbientBackdrop(themePalette = activeThemePalette, isDark = isEffectiveDark)

      Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
          if (
            isSecurityPrefsLoaded &&
              !needsMandatoryMpinSetup &&
              !isVaultLockActive &&
              uiState is FundTrackUiState.Dashboard
          ) {
            val fabShape = RoundedCornerShape(999.dp)
            val fabInteraction = remember { MutableInteractionSource() }
            val fabRimBrush =
              remember(activeThemePalette) {
                Brush.linearGradient(
                  colors =
                    listOf(Color.White.copy(alpha = 0.50f), activeThemePalette.accentLightTint.copy(alpha = 0.25f))
                )
              }
            ExtendedFloatingActionButton(
              onClick = {
                editingEntry = null
                isQuickEntryOpen = true
              },
              interactionSource = fabInteraction,
              shape = fabShape,
              containerColor = activeAccentColor.copy(alpha = 0.94f),
              contentColor = Color.White,
              icon = {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
              },
              text = {
                Text(
                  text = stringResource(R.string.quick_log_fab),
                  fontWeight = FontWeight.Bold,
                )
              },
              modifier =
                Modifier.futuristicPressScale(fabInteraction, pressedScale = 0.95f)
                  .shadow(
                    elevation = 10.dp,
                    shape = fabShape,
                    clip = false,
                    ambientColor = activeAccentColor.copy(alpha = 0.45f),
                    spotColor = activeAccentColor.copy(alpha = 0.45f),
                  )
                  .border(width = 1.dp, brush = fabRimBrush, shape = fabShape)
                  .testTag("quick_log_fab"),
            )
          }
        },
      ) { innerPadding ->
      val contentModifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
      if (!isSecurityPrefsLoaded) {
        Box(
          modifier = Modifier.fillMaxSize().then(contentModifier),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
      } else if (needsMandatoryMpinSetup) {
        MpinSetupScreen(
          themePalette = activeThemePalette,
          isDark = isEffectiveDark,
          onToggleTheme = onQuickToggleTheme,
          onCompleteSetup = viewModel::setupInitialMpin,
          modifier = contentModifier,
        )
      } else if (isVaultLockActive) {
        BiometricLockScreen(
          themePalette = activeThemePalette,
          strategy = authStrategy,
          biometricLockEnabled = biometricLockEnabled == true,
          statusMessage = authStatusMessage,
          forceMpinSignal = forceMpinSignal,
          isDark = isEffectiveDark,
          onToggleTheme = onQuickToggleTheme,
          onUnlockClick = ::launchUnlockPrompt,
          onVerifyMpin = viewModel::verifyMpinAndUnlock,
          modifier = contentModifier,
        )
      } else {
      when (val state = uiState) {
        FundTrackUiState.Loading -> {
          Box(
            modifier = Modifier.fillMaxSize().then(contentModifier),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
        }

        is FundTrackUiState.FirstRun -> {
          if (isSettingsOpen) isSettingsOpen = false
          if (isSimulatorOpen) isSimulatorOpen = false
          if (isSummaryOpen) isSummaryOpen = false
          if (isQuickEntryOpen) isQuickEntryOpen = false
          editingEntry = null

          FirstRunScreen(
            closedBusinesses = state.closedBusinesses,
            isDark = isEffectiveDark,
            onToggleTheme = onQuickToggleTheme,
            onCreateBusiness = viewModel::createBusiness,
            onReopenBusiness = viewModel::reopenBusiness,
            onDeleteBusiness = viewModel::deleteBusiness,
            modifier = contentModifier,
          )
        }

        is FundTrackUiState.Dashboard -> {
          val openSettings = remember { { isSettingsOpen = true } }
          val openSimulator = remember { { isSimulatorOpen = true } }
          val openSummary = remember { { isSummaryOpen = true } }
          val togglePrivacy = remember { { isPrivacyMasked = !isPrivacyMasked } }
          val startEditEntry =
            remember {
              { entry: EntryEntity ->
                editingEntry = entry
                isQuickEntryOpen = true
              }
            }
          val addEntryForSelected =
            remember(state.selectedBusiness.id, viewModel) {
              { type: EntryType, name: String, amount: Double ->
                viewModel.addEntry(state.selectedBusiness.id, type, name, amount)
              }
            }

          DashboardContent(
            state = state,
            isDark = isEffectiveDark,
            isPrivacyMasked = isPrivacyMasked,
            onToggleTheme = onQuickToggleTheme,
            onTogglePrivacy = togglePrivacy,
            onSelectBusiness = viewModel::selectBusiness,
            onAddBusiness = viewModel::createBusiness,
            onOpenSettings = openSettings,
            onOpenSimulator = openSimulator,
            onOpenSummary = openSummary,
            onAddEntry = addEntryForSelected,
            onEditEntry = startEditEntry,
            onDeleteEntry = viewModel::deleteEntry,
            onMarkDueReceived = viewModel::markDueAsReceived,
            modifier = contentModifier,
          )

          if (isSettingsOpen) {
            SettingsBottomSheet(
              selectedBusiness = state.selectedBusiness,
              closedBusinesses = state.closedBusinesses,
              backupStatus = state.backupStatus,
              themeMode = themeMode,
              biometricLockEnabled = biometricLockEnabled == true,
              onSelectThemeMode = viewModel::setThemeMode,
              onToggleBiometricLock = viewModel::setBiometricLockEnabled,
              onOpenChangeMpin = { isChangeMpinOpen = true },
              onLockNow = viewModel::lockSession,
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

          if (isChangeMpinOpen) {
            ChangeMpinBottomSheet(
              accentColor = state.themePalette.accent,
              onVerifyCurrentPin = viewModel::verifyMpinAndUnlock,
              onChangeMpin = viewModel::changeMpin,
              onDismiss = { isChangeMpinOpen = false },
            )
          }

          if (isSimulatorOpen) {
            ProfitSimulatorSheet(
              themePalette = state.themePalette,
              currentInvestment = state.totalInvestment,
              currentRevenue = state.totalRevenue,
              currentDue = state.totalDue,
              onDismiss = { isSimulatorOpen = false },
            )
          }

          if (isSummaryOpen) {
            val allBusinessEntries =
              remember(state.investmentEntries, state.revenueEntries, state.dueEntries) {
                state.investmentEntries + state.revenueEntries + state.dueEntries
              }
            FinancialSummaryBottomSheet(
              business = state.selectedBusiness,
              themePalette = state.themePalette,
              allEntries = allBusinessEntries,
              isPrivacyMasked = isPrivacyMasked,
              onDismiss = { isSummaryOpen = false },
            )
          }

          if (isQuickEntryOpen) {
            QuickEntryBottomSheet(
              initialEntry = editingEntry,
              investmentModels = state.investmentModels,
              revenueModels = state.revenueModels,
              dueModels = state.dueModels,
              accentColor = state.themePalette.accent,
              onSaveNew = addEntryForSelected,
              onUpdateExisting = viewModel::updateEntry,
              onDismiss = {
                isQuickEntryOpen = false
                editingEntry = null
              },
            )
          }
        }
      }
      }
      }
    }
  }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? =
  when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
  }

@Composable
private fun DashboardContent(
  state: FundTrackUiState.Dashboard,
  isDark: Boolean,
  isPrivacyMasked: Boolean,
  onToggleTheme: () -> Unit,
  onTogglePrivacy: () -> Unit,
  onSelectBusiness: (Long) -> Unit,
  onAddBusiness: (String) -> Unit,
  onOpenSettings: () -> Unit,
  onOpenSimulator: () -> Unit,
  onOpenSummary: () -> Unit,
  onAddEntry: (EntryType, String, Double) -> Unit,
  onEditEntry: (EntryEntity) -> Unit,
  onDeleteEntry: (EntryEntity) -> Unit,
  onMarkDueReceived: (EntryEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  var searchQuery by rememberSaveable(state.selectedBusiness.id) { mutableStateOf("") }
  var selectedCategory by
    rememberSaveable(state.selectedBusiness.id) { mutableStateOf(DashboardCategoryFilter.ALL) }
  var sortOrder by rememberSaveable(state.selectedBusiness.id) { mutableStateOf(EntrySortOrder.NEWEST) }

  val isFiltering = searchQuery.trim().isNotEmpty()

  val filteredInvestments =
    remember(state.investmentModels, searchQuery, sortOrder) {
      DashboardInsights.filterAndSortModels(state.investmentModels, searchQuery, sortOrder)
    }
  val filteredRevenues =
    remember(state.revenueModels, searchQuery, sortOrder) {
      DashboardInsights.filterAndSortModels(state.revenueModels, searchQuery, sortOrder)
    }
  val filteredDues =
    remember(state.dueModels, searchQuery, sortOrder) {
      DashboardInsights.filterAndSortModels(state.dueModels, searchQuery, sortOrder)
    }

  val totalFilteredCount =
    filteredInvestments.size + filteredRevenues.size + filteredDues.size
  val filteredEmptyMsg = if (isFiltering) stringResource(R.string.no_matching_entries) else null

  val onSearchQueryChange = remember(state.selectedBusiness.id) { { next: String -> searchQuery = next } }
  val onSelectCategory =
    remember(state.selectedBusiness.id) {
      { category: DashboardCategoryFilter -> selectedCategory = category }
    }
  val onToggleSort =
    remember(state.selectedBusiness.id) {
      {
        sortOrder =
          if (sortOrder == EntrySortOrder.NEWEST) EntrySortOrder.HIGHEST_AMOUNT
          else EntrySortOrder.NEWEST
      }
    }
  val onAddInvestment =
    remember(onAddEntry) { { name: String, amount: Double -> onAddEntry(EntryType.INVESTMENT, name, amount) } }
  val onAddRevenue =
    remember(onAddEntry) { { name: String, amount: Double -> onAddEntry(EntryType.REVENUE, name, amount) } }
  val onAddDue =
    remember(onAddEntry) { { name: String, amount: Double -> onAddEntry(EntryType.DUE, name, amount) } }

  val investmentTitle = stringResource(R.string.section_investment)
  val addInvestmentLabel = stringResource(R.string.add_investment)
  val revenueTitle = stringResource(R.string.section_revenue)
  val addRevenueLabel = stringResource(R.string.add_revenue)
  val dueTitle = stringResource(R.string.section_money_due)
  val addDueLabel = stringResource(R.string.add_money_due)
  val maskedAmountLabel = stringResource(R.string.masked_amount)
  val listState = rememberLazyListState()

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      state = listState,
      modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
      contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
    ) {
      item(key = "business_tabs", contentType = "business_tabs") {
        BusinessTabsRow(
          openBusinesses = state.openBusinesses,
          selectedBusinessId = state.selectedBusiness.id,
          isDark = isDark,
          isPrivacyMasked = isPrivacyMasked,
          onToggleTheme = onToggleTheme,
          onTogglePrivacy = onTogglePrivacy,
          onSelectBusiness = onSelectBusiness,
          onAddBusiness = onAddBusiness,
        )
      }

      item(key = "search_bar", contentType = "search_bar") {
        Column(modifier = Modifier.padding(top = 14.dp)) {
          DashboardSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            matchingCount = totalFilteredCount,
            selectedCategory = selectedCategory,
            onSelectCategory = onSelectCategory,
            sortOrder = sortOrder,
            onToggleSort = onToggleSort,
            investmentCount = filteredInvestments.size,
            revenueCount = filteredRevenues.size,
            dueCount = filteredDues.size,
            accentColor = state.themePalette.accent,
          )
        }
      }

      item(key = "hero_card", contentType = "hero_card") {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
          HeroProfitCard(
            business = state.selectedBusiness,
            themePalette = state.themePalette,
            totalInvestment = state.totalInvestment,
            totalRevenue = state.totalRevenue,
            totalDue = state.totalDue,
            netProfit = state.netProfit,
            pulseMetrics = state.pulseMetrics,
            isPrivacyMasked = isPrivacyMasked,
            onOpenSettings = onOpenSettings,
            onOpenSummary = onOpenSummary,
          )
        }
      }

      item(key = "summary_tiles", contentType = "summary_tiles") {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
          SummaryTilesRow(
            totalInvestment = state.totalInvestment,
            totalRevenue = state.totalRevenue,
            totalDue = state.totalDue,
            pulseMetrics = state.pulseMetrics,
            isPrivacyMasked = isPrivacyMasked,
          )
        }
      }

      item(key = "cashflow_radar", contentType = "cashflow_radar") {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
          CashflowRadarCard(
            pulseMetrics = state.pulseMetrics,
            accentColor = state.themePalette.accent,
            isPrivacyMasked = isPrivacyMasked,
            onOpenSimulator = onOpenSimulator,
          )
        }
      }

      item(key = "earnings_by_client", contentType = "earnings_by_client") {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
          EarningsByClientCard(
            clientEarnings = state.clientEarnings,
            accentColor = state.themePalette.accent,
            isPrivacyMasked = isPrivacyMasked,
          )
        }
      }

      if (
        selectedCategory == DashboardCategoryFilter.ALL ||
          selectedCategory == DashboardCategoryFilter.INVESTMENT
      ) {
        ledgerSection(
          businessId = state.selectedBusiness.id,
          type = EntryType.INVESTMENT,
          title = investmentTitle,
          addButtonLabel = addInvestmentLabel,
          models = filteredInvestments,
          totalAmount =
            if (isFiltering) filteredInvestments.sumOf { it.entry.amount }
            else state.totalInvestment,
          markerColor = semanticColors.softRedContent,
          badgeContainerColor = semanticColors.softRedContainer,
          badgeContentColor = semanticColors.softRedContent,
          isPrivacyMasked = isPrivacyMasked,
          maskedAmountLabel = maskedAmountLabel,
          emptyMessage = filteredEmptyMsg,
          onAddEntry = onAddInvestment,
          onEditEntry = onEditEntry,
          onDeleteEntry = onDeleteEntry,
        )
      }

      if (
        selectedCategory == DashboardCategoryFilter.ALL ||
          selectedCategory == DashboardCategoryFilter.REVENUE
      ) {
        ledgerSection(
          businessId = state.selectedBusiness.id,
          type = EntryType.REVENUE,
          title = revenueTitle,
          addButtonLabel = addRevenueLabel,
          models = filteredRevenues,
          totalAmount =
            if (isFiltering) filteredRevenues.sumOf { it.entry.amount } else state.totalRevenue,
          markerColor = state.themePalette.accent,
          badgeContainerColor = semanticColors.softGreenContainer,
          badgeContentColor = semanticColors.softGreenContent,
          isPrivacyMasked = isPrivacyMasked,
          maskedAmountLabel = maskedAmountLabel,
          emptyMessage = filteredEmptyMsg,
          onAddEntry = onAddRevenue,
          onEditEntry = onEditEntry,
          onDeleteEntry = onDeleteEntry,
        )
      }

      if (
        selectedCategory == DashboardCategoryFilter.ALL ||
          selectedCategory == DashboardCategoryFilter.DUE
      ) {
        ledgerSection(
          businessId = state.selectedBusiness.id,
          type = EntryType.DUE,
          title = dueTitle,
          addButtonLabel = addDueLabel,
          models = filteredDues,
          totalAmount =
            if (isFiltering) filteredDues.sumOf { it.entry.amount } else state.totalDue,
          markerColor = semanticColors.softAmberContent,
          badgeContainerColor = semanticColors.softAmberContainer,
          badgeContentColor = semanticColors.softAmberContent,
          isPrivacyMasked = isPrivacyMasked,
          maskedAmountLabel = maskedAmountLabel,
          emptyMessage = filteredEmptyMsg,
          onAddEntry = onAddDue,
          onEditEntry = onEditEntry,
          onDeleteEntry = onDeleteEntry,
          onMarkDueReceived = onMarkDueReceived,
        )
      }

      item(key = "caution_card", contentType = "caution_card") {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
          CautionCard()
        }
      }
    }
  }
}
