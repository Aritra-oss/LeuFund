package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.EntryEntity
import com.example.data.local.EntryType
import com.example.domain.CurrencyFormatter
import com.example.domain.DashboardInsights
import com.example.domain.EntryUiModel
import com.example.ui.theme.LocalFundTrackColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickEntryBottomSheet(
  initialEntry: EntryEntity?,
  investmentModels: List<EntryUiModel>,
  revenueModels: List<EntryUiModel>,
  dueModels: List<EntryUiModel>,
  accentColor: Color,
  onSaveNew: (EntryType, String, Double) -> Unit,
  onUpdateExisting: (EntryEntity, EntryType, String, Double) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val semanticColors = LocalFundTrackColors.current
  val haptics = LocalHapticFeedback.current
  val isEditing = initialEntry != null

  var selectedType by rememberSaveable(initialEntry?.id) {
    mutableStateOf(initialEntry?.type ?: EntryType.REVENUE)
  }
  var nameInput by rememberSaveable(initialEntry?.id) {
    mutableStateOf(initialEntry?.name.orEmpty())
  }
  var amountInput by rememberSaveable(initialEntry?.id) {
    mutableStateOf(
      initialEntry?.amount?.let { amt ->
        if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
      }.orEmpty()
    )
  }
  var nameError by remember { mutableStateOf(false) }
  var amountError by remember { mutableStateOf(false) }

  val recentSuggestions =
    remember(investmentModels, revenueModels, dueModels, selectedType) {
      DashboardInsights.extractRecentNames(
        investments = investmentModels,
        revenues = revenueModels,
        dues = dueModels,
        forType = selectedType,
      )
    }

  fun submit() {
    val cleanName = nameInput.trim()
    val parsedAmount = CurrencyFormatter.parseValidAmount(amountInput)
    nameError = cleanName.isEmpty()
    amountError = parsedAmount == null

    if (!nameError && !amountError && parsedAmount != null) {
      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
      if (initialEntry != null) {
        onUpdateExisting(initialEntry, selectedType, cleanName, parsedAmount)
      } else {
        onSaveNew(selectedType, cleanName, parsedAmount)
      }
      onDismiss()
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = semanticColors.glassElevatedStart,
    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .navigationBarsPadding()
          .imePadding()
          .padding(horizontal = 20.dp)
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text =
          if (isEditing) stringResource(R.string.edit_entry_title)
          else stringResource(R.string.quick_log_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("quick_entry_sheet_title"),
      )

      // Category selector pills
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = stringResource(R.string.category_label),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf(
            Triple(EntryType.INVESTMENT, stringResource(R.string.section_investment), semanticColors.softRedContent),
            Triple(EntryType.REVENUE, stringResource(R.string.section_revenue), accentColor),
            Triple(EntryType.DUE, stringResource(R.string.section_money_due), semanticColors.softAmberContent),
          ).forEach { (type, label, color) ->
            val selected = selectedType == type
            val pillShape = RoundedCornerShape(14.dp)
            val pillFill =
              if (selected) {
                Brush.linearGradient(listOf(color.copy(alpha = 0.94f), color.copy(alpha = 0.78f)))
              } else {
                Brush.linearGradient(
                  listOf(semanticColors.glassFillStart, semanticColors.glassFillEnd)
                )
              }
            val pillRim =
              if (selected) {
                Brush.linearGradient(
                  listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.14f))
                )
              } else {
                Brush.linearGradient(
                  listOf(semanticColors.glassRimStart, semanticColors.glassRimEnd)
                )
              }
            Box(
              modifier =
                Modifier.weight(1f)
                  .heightIn(min = 48.dp)
                  .clip(pillShape)
                  .background(pillFill)
                  .border(width = 1.dp, brush = pillRim, shape = pillShape)
                  .clickable(role = Role.RadioButton) { selectedType = type }
                  .testTag("sheet_category_${type.name.lowercase()}"),
              contentAlignment = Alignment.Center,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        }
      }

      // Recent names quick-fill chips
      if (recentSuggestions.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = stringResource(R.string.recent_names_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            recentSuggestions.forEach { suggestion ->
              val chipShape = RoundedCornerShape(999.dp)
              Box(
                modifier =
                  Modifier.heightIn(min = 48.dp)
                    .clip(chipShape)
                    .background(
                      Brush.linearGradient(
                        listOf(semanticColors.glassFillStart, semanticColors.glassFillEnd)
                      )
                    )
                    .border(
                      width = 1.dp,
                      brush =
                        Brush.linearGradient(
                          listOf(semanticColors.glassRimStart, semanticColors.glassRimEnd)
                        ),
                      shape = chipShape,
                    )
                    .clickable(role = Role.Button) {
                      nameInput = suggestion
                      nameError = false
                    },
                contentAlignment = Alignment.Center,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = suggestion,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }
          }
        }
      }

      OutlinedTextField(
        value = nameInput,
        onValueChange = {
          nameInput = it
          if (nameError && it.trim().isNotEmpty()) nameError = false
        },
        label = {
          Text(
            if (selectedType == EntryType.INVESTMENT) stringResource(R.string.entry_name_hint)
            else stringResource(R.string.entry_client_hint)
          )
        },
        singleLine = true,
        isError = nameError,
        supportingText = if (nameError) ({ Text(stringResource(R.string.error_empty_name)) }) else null,
        keyboardOptions =
          KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
          ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("quick_sheet_name_input"),
      )

      OutlinedTextField(
        value = amountInput,
        onValueChange = { raw ->
          val filtered = sanitizeDecimalAmount(raw)
          amountInput = filtered
          if (amountError && CurrencyFormatter.parseValidAmount(filtered) != null) {
            amountError = false
          }
        },
        label = { Text(stringResource(R.string.amount_hint)) },
        singleLine = true,
        isError = amountError,
        supportingText =
          if (amountError) ({ Text(stringResource(R.string.error_invalid_amount)) }) else null,
        keyboardOptions =
          KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
          ),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("quick_sheet_amount_input"),
      )

      // Quick amount booster chips
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(500L, 1000L, 5000L, 10000L).forEach { delta ->
          val chipShape = RoundedCornerShape(12.dp)
          Box(
            modifier =
              Modifier.heightIn(min = 48.dp)
                .clip(chipShape)
                .background(
                  Brush.linearGradient(
                    listOf(accentColor.copy(alpha = 0.16f), accentColor.copy(alpha = 0.08f))
                  )
                )
                .border(
                  width = 1.dp,
                  brush =
                    Brush.linearGradient(
                      listOf(accentColor.copy(alpha = 0.40f), accentColor.copy(alpha = 0.16f))
                    ),
                  shape = chipShape,
                )
                .clickable(role = Role.Button) {
                  val current = CurrencyFormatter.parseValidAmount(amountInput) ?: 0.0
                  val next = current + delta
                  amountInput = if (next % 1.0 == 0.0) next.toLong().toString() else next.toString()
                  amountError = false
                },
            contentAlignment = Alignment.Center,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "+₹${CurrencyFormatter.formatInr(delta.toDouble()).removePrefix("₹")}",
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
              )
            }
          }
        }
      }

      Button(
        onClick = { submit() },
        shape = RoundedCornerShape(16.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = Color.White,
          ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("quick_sheet_submit_btn"),
      ) {
        Text(
          text =
            if (isEditing) stringResource(R.string.update_entry)
            else stringResource(R.string.save),
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}
