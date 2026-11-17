package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.EntryEntity
import com.example.data.local.EntryType
import com.example.domain.CurrencyFormatter
import com.example.domain.EntryUiModel
import com.example.ui.theme.LocalFundTrackColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Renders an entire ledger category as a single unified classic frosted [GlassCard] composed once
 * inside the scrollable dashboard [Column], avoiding per-section subcomposition stalls during scroll.
 */
@Composable
fun LedgerSectionCard(
  businessId: Long,
  type: EntryType,
  title: String,
  addButtonLabel: String,
  models: List<EntryUiModel>,
  totalAmount: Double,
  markerColor: Color,
  badgeContainerColor: Color,
  badgeContentColor: Color,
  isPrivacyMasked: Boolean,
  maskedAmountLabel: String,
  emptyMessage: String?,
  onAddEntry: (name: String, amount: Double) -> Unit,
  onEditEntry: (EntryEntity) -> Unit,
  onDeleteEntry: (EntryEntity) -> Unit,
  modifier: Modifier = Modifier,
  onMarkDueReceived: ((EntryEntity) -> Unit)? = null,
) {
  val semanticColors = LocalFundTrackColors.current
  var isFormOpen by rememberSaveable(businessId, type) { mutableStateOf(false) }

  val formattedTotal =
    remember(totalAmount, isPrivacyMasked, maskedAmountLabel) {
      if (isPrivacyMasked) maskedAmountLabel else CurrencyFormatter.formatInr(totalAmount)
    }
  val dividerColor =
    remember(semanticColors.cardBorder) { semanticColors.cardBorder.copy(alpha = 0.55f) }
  val buttonShape = RoundedCornerShape(14.dp)

  GlassCard(
    modifier = modifier.fillMaxWidth().testTag("section_card_${type.name.lowercase()}"),
    shape = RoundedCornerShape(22.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Classic card header
      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(markerColor))
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(999.dp))
                .background(semanticColors.glassElevatedStart.copy(alpha = 0.8f))
                .border(
                  1.dp,
                  semanticColors.glassRimStart.copy(alpha = 0.65f),
                  RoundedCornerShape(999.dp),
                ),
          ) {
            Text(
              text = models.size.toString(),
              style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }

        Text(
          text = formattedTotal,
          style =
            MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontFeatureSettings = "tnum",
            ),
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.testTag("section_total_${type.name.lowercase()}"),
        )
      }

      HorizontalDivider(color = dividerColor)

      if (models.isEmpty()) {
        Text(
          text = emptyMessage ?: stringResource(R.string.nothing_here_yet),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 18.dp, vertical = 14.dp)
              .testTag("empty_text_${type.name.lowercase()}"),
        )
      } else {
        models.forEachIndexed { index, model ->
          key(model.entry.id) {
            LedgerEntryRow(
              model = model,
              type = type,
              badgeContainerColor = badgeContainerColor,
              badgeContentColor = badgeContentColor,
              isPrivacyMasked = isPrivacyMasked,
              maskedAmountLabel = maskedAmountLabel,
              onEdit = onEditEntry,
              onDelete = onDeleteEntry,
              onMarkReceived = onMarkDueReceived,
            )
            if (index < models.lastIndex) {
              HorizontalDivider(
                color = dividerColor,
                modifier = Modifier.padding(horizontal = 18.dp),
              )
            }
          }
        }
      }

      Column(
        modifier =
          Modifier.fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (isFormOpen) {
          InlineEntryFormCard(
            businessId = businessId,
            type = type,
            onSave = { name, amount ->
              onAddEntry(name, amount)
              isFormOpen = false
            },
          )
        }

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(min = 48.dp)
              .clip(buttonShape)
              .background(semanticColors.glassFillEnd.copy(alpha = 0.65f))
              .border(1.dp, semanticColors.glassRimStart.copy(alpha = 0.80f), buttonShape)
              .clickable(role = Role.Button) { isFormOpen = !isFormOpen }
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .testTag("toggle_add_btn_${type.name.lowercase()}"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (isFormOpen) stringResource(R.string.cancel) else addButtonLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun LedgerEntryRow(
  model: EntryUiModel,
  type: EntryType,
  badgeContainerColor: Color,
  badgeContentColor: Color,
  isPrivacyMasked: Boolean,
  maskedAmountLabel: String,
  onEdit: (EntryEntity) -> Unit,
  onDelete: (EntryEntity) -> Unit,
  onMarkReceived: ((EntryEntity) -> Unit)?,
) {
  val semanticColors = LocalFundTrackColors.current
  val haptics = LocalHapticFeedback.current
  val entry = model.entry
  val displayAmount = if (isPrivacyMasked) maskedAmountLabel else model.formattedAmount

  val handleEditClick = remember(entry, onEdit) { { onEdit(entry) } }
  val handleDeleteClick =
    remember(entry, onDelete, haptics) {
      {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onDelete(entry)
      }
    }
  val handleReceivedClick =
    remember(entry, onMarkReceived, haptics) {
      {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onMarkReceived?.invoke(entry)
        Unit
      }
    }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = handleEditClick)
        .padding(start = 18.dp, end = 8.dp, top = 11.dp, bottom = 11.dp)
        .testTag("entry_row_${entry.id}"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier =
        Modifier.size(40.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(badgeContainerColor)
          .border(1.dp, badgeContentColor.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = model.initials,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = badgeContentColor,
      )
    }

    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = entry.name,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = displayAmount,
          style =
            MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.SemiBold,
              fontFeatureSettings = "tnum",
            ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (model.formattedDate.isNotEmpty()) {
          Text(
            text = "· ${model.formattedDate}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            maxLines = 1,
          )
        }
      }
    }

    if (type == EntryType.DUE && onMarkReceived != null) {
      FilledTonalButton(
        onClick = handleReceivedClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors =
          ButtonDefaults.filledTonalButtonColors(
            containerColor = semanticColors.softGreenContainer,
            contentColor = semanticColors.softGreenContent,
          ),
        modifier = Modifier.heightIn(min = 48.dp).testTag("received_btn_${entry.id}"),
      ) {
        Text(
          text = stringResource(R.string.received),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    IconButton(
      onClick = handleDeleteClick,
      modifier = Modifier.size(48.dp).testTag("delete_entry_btn_${entry.id}"),
    ) {
      Icon(
        imageVector = Icons.Outlined.DeleteOutline,
        contentDescription = stringResource(R.string.delete_entry_cd, entry.name),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InlineEntryFormCard(
  businessId: Long,
  type: EntryType,
  onSave: (name: String, amount: Double) -> Unit,
) {
  val semanticColors = LocalFundTrackColors.current
  val haptics = LocalHapticFeedback.current
  val coroutineScope = rememberCoroutineScope()
  val formBringIntoViewRequester = remember { BringIntoViewRequester() }

  var nameInput by rememberSaveable(businessId, type) { mutableStateOf("") }
  var amountInput by rememberSaveable(businessId, type) { mutableStateOf("") }
  var nameError by rememberSaveable(businessId, type) { mutableStateOf(false) }
  var amountError by rememberSaveable(businessId, type) { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    delay(160)
    formBringIntoViewRequester.bringIntoView()
  }

  fun submitEntry() {
    val cleanName = nameInput.trim()
    val parsedAmount = CurrencyFormatter.parseValidAmount(amountInput)
    val invalidName = cleanName.isEmpty()
    val invalidAmount = parsedAmount == null

    nameError = invalidName
    amountError = invalidAmount

    if (!invalidName && !invalidAmount && parsedAmount != null) {
      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
      onSave(cleanName, parsedAmount)
      nameInput = ""
      amountInput = ""
      nameError = false
      amountError = false
    }
  }

  val inlineFormBrush =
    remember(semanticColors.glassElevatedStart, semanticColors.glassElevatedEnd) {
      Brush.linearGradient(
        colors = listOf(semanticColors.glassElevatedStart, semanticColors.glassElevatedEnd)
      )
    }
  val inlineRimBrush =
    remember(semanticColors.glassRimStart, semanticColors.glassRimEnd) {
      Brush.linearGradient(colors = listOf(semanticColors.glassRimStart, semanticColors.glassRimEnd))
    }
  val inlineShape = RoundedCornerShape(16.dp)

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .bringIntoViewRequester(formBringIntoViewRequester)
        .clip(inlineShape)
        .background(inlineFormBrush)
        .border(width = 1.dp, brush = inlineRimBrush, shape = inlineShape)
        .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    OutlinedTextField(
      value = nameInput,
      onValueChange = {
        nameInput = it
        if (nameError && it.trim().isNotEmpty()) nameError = false
      },
      label = {
        Text(
          if (type == EntryType.INVESTMENT) stringResource(R.string.entry_name_hint)
          else stringResource(R.string.entry_client_hint)
        )
      },
      singleLine = true,
      isError = nameError,
      supportingText =
        if (nameError) {
          { Text(stringResource(R.string.error_empty_name)) }
        } else null,
      keyboardOptions =
        KeyboardOptions(
          capitalization = KeyboardCapitalization.Words,
          imeAction = ImeAction.Next,
        ),
      shape = RoundedCornerShape(14.dp),
      modifier =
        Modifier.fillMaxWidth()
          .onFocusChanged { focusState ->
            if (focusState.isFocused) {
              coroutineScope.launch {
                delay(180)
                formBringIntoViewRequester.bringIntoView()
              }
            }
          }
          .testTag("entry_name_input_${type.name.lowercase()}"),
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
        if (amountError) {
          { Text(stringResource(R.string.error_invalid_amount)) }
        } else null,
      keyboardOptions =
        KeyboardOptions(
          keyboardType = KeyboardType.Decimal,
          imeAction = ImeAction.Done,
        ),
      keyboardActions = KeyboardActions(onDone = { submitEntry() }),
      shape = RoundedCornerShape(14.dp),
      modifier =
        Modifier.fillMaxWidth()
          .onFocusChanged { focusState ->
            if (focusState.isFocused) {
              coroutineScope.launch {
                delay(180)
                formBringIntoViewRequester.bringIntoView()
              }
            }
          }
          .testTag("entry_amount_input_${type.name.lowercase()}"),
    )

    Button(
      onClick = { submitEntry() },
      shape = RoundedCornerShape(14.dp),
      colors =
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = Color.White,
        ),
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("save_entry_btn_${type.name.lowercase()}"),
    ) {
      Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
    }
  }
}

internal fun sanitizeDecimalAmount(input: String): String {
  val sb = StringBuilder()
  var hasDot = false
  for (ch in input) {
    if (ch.isDigit()) {
      sb.append(ch)
    } else if (ch == '.' && !hasDot) {
      hasDot = true
      sb.append(ch)
    }
  }
  return sb.toString()
}
