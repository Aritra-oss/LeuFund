package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * Emits a cohesive rounded-card section directly into the parent [LazyColumn] so each
 * transaction row is individually virtualized and recycled at full display refresh rate.
 */
fun LazyListScope.ledgerSection(
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
  emptyMessage: String?,
  onAddEntry: (name: String, amount: Double) -> Unit,
  onEditEntry: (EntryEntity) -> Unit,
  onDeleteEntry: (EntryEntity) -> Unit,
  onMarkDueReceived: ((EntryEntity) -> Unit)? = null,
) {
  val keyPrefix = type.name.lowercase()

  item(key = "section_header_$keyPrefix", contentType = "section_header") {
    LedgerSectionHeader(
      type = type,
      title = title,
      itemCount = models.size,
      totalAmount = totalAmount,
      markerColor = markerColor,
      isPrivacyMasked = isPrivacyMasked,
    )
  }

  if (models.isEmpty()) {
    item(key = "section_empty_$keyPrefix", contentType = "section_empty") {
      LedgerSectionEmptyRow(type = type, emptyMessage = emptyMessage)
    }
  } else {
    itemsIndexed(
      items = models,
      key = { _, model -> "${keyPrefix}_${model.entry.id}" },
      contentType = { _, _ -> "ledger_entry_row" },
    ) { index, model ->
      LedgerVirtualizedEntryRow(
        model = model,
        type = type,
        showBottomDivider = index < models.lastIndex,
        badgeContainerColor = badgeContainerColor,
        badgeContentColor = badgeContentColor,
        isPrivacyMasked = isPrivacyMasked,
        onEdit = onEditEntry,
        onDelete = onDeleteEntry,
        onMarkReceived = onMarkDueReceived,
      )
    }
  }

  item(key = "section_footer_$keyPrefix", contentType = "section_footer") {
    LedgerSectionFooter(
      businessId = businessId,
      type = type,
      addButtonLabel = addButtonLabel,
      onAddEntry = onAddEntry,
    )
  }
}

@Composable
private fun LedgerSectionHeader(
  type: EntryType,
  title: String,
  itemCount: Int,
  totalAmount: Double,
  markerColor: Color,
  isPrivacyMasked: Boolean,
) {
  val semanticColors = LocalFundTrackColors.current
  val borderColor = semanticColors.glassRimStart
  val maskedText = stringResource(R.string.masked_amount)
  val formattedTotal =
    remember(totalAmount, isPrivacyMasked, maskedText) {
      if (isPrivacyMasked) maskedText else CurrencyFormatter.formatInr(totalAmount)
    }
  val fillBrush =
    remember(semanticColors.glassFillStart, semanticColors.glassFillEnd) {
      Brush.horizontalGradient(
        colors = listOf(semanticColors.glassFillStart, semanticColors.glassFillEnd)
      )
    }

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
        .background(fillBrush)
        .testTag("section_card_${type.name.lowercase()}"),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .drawCardSideAndTopBorders(borderColor = borderColor, cornerRadiusDp = 26f)
          .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier =
            Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(markerColor)
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = semanticColors.glassElevatedStart.copy(alpha = 0.7f),
        ) {
          Text(
            text = itemCount.toString(),
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
  }
}

@Composable
private fun LedgerSectionEmptyRow(type: EntryType, emptyMessage: String?) {
  val semanticColors = LocalFundTrackColors.current
  val fillBrush =
    remember(semanticColors.glassFillStart, semanticColors.glassFillEnd) {
      Brush.horizontalGradient(
        colors = listOf(semanticColors.glassFillStart, semanticColors.glassFillEnd)
      )
    }
  Box(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp)
        .background(fillBrush)
  ) {
    Text(
      text = emptyMessage ?: stringResource(R.string.nothing_here_yet),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier =
        Modifier.fillMaxWidth()
          .drawSideBorders(semanticColors.glassRimStart)
          .padding(horizontal = 20.dp, vertical = 8.dp)
          .testTag("empty_text_${type.name.lowercase()}"),
    )
  }
}

@Composable
private fun LedgerVirtualizedEntryRow(
  model: EntryUiModel,
  type: EntryType,
  showBottomDivider: Boolean,
  badgeContainerColor: Color,
  badgeContentColor: Color,
  isPrivacyMasked: Boolean,
  onEdit: (EntryEntity) -> Unit,
  onDelete: (EntryEntity) -> Unit,
  onMarkReceived: ((EntryEntity) -> Unit)?,
) {
  val semanticColors = LocalFundTrackColors.current
  val haptics = LocalHapticFeedback.current
  val entry = model.entry
  val displayAmount =
    if (isPrivacyMasked) stringResource(R.string.masked_amount) else model.formattedAmount
  val fillBrush =
    remember(semanticColors.glassFillStart, semanticColors.glassFillEnd) {
      Brush.horizontalGradient(
        colors = listOf(semanticColors.glassFillStart, semanticColors.glassFillEnd)
      )
    }

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp)
        .background(fillBrush)
        .clickable { onEdit(entry) }
        .testTag("entry_row_${entry.id}"),
  ) {
    Column(modifier = Modifier.fillMaxWidth().drawSideBorders(semanticColors.glassRimStart)) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier =
            Modifier.size(40.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(badgeContainerColor),
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
            onClick = {
              haptics.performHapticFeedback(HapticFeedbackType.LongPress)
              onMarkReceived(entry)
            },
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
          onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onDelete(entry)
          },
          modifier = Modifier.size(48.dp).testTag("delete_entry_btn_${entry.id}"),
        ) {
          Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = stringResource(R.string.delete_entry_cd, entry.name),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (showBottomDivider) {
        HorizontalDivider(
          color = semanticColors.cardBorder.copy(alpha = 0.6f),
          thickness = 1.dp,
          modifier = Modifier.padding(horizontal = 20.dp),
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LedgerSectionFooter(
  businessId: Long,
  type: EntryType,
  addButtonLabel: String,
  onAddEntry: (name: String, amount: Double) -> Unit,
) {
  val semanticColors = LocalFundTrackColors.current
  val haptics = LocalHapticFeedback.current
  val coroutineScope = rememberCoroutineScope()
  val formBringIntoViewRequester = remember { BringIntoViewRequester() }

  var isFormOpen by rememberSaveable(businessId, type) { mutableStateOf(false) }
  var nameInput by rememberSaveable(businessId, type) { mutableStateOf("") }
  var amountInput by rememberSaveable(businessId, type) { mutableStateOf("") }
  var nameError by rememberSaveable(businessId, type) { mutableStateOf(false) }
  var amountError by rememberSaveable(businessId, type) { mutableStateOf(false) }

  LaunchedEffect(isFormOpen) {
    if (isFormOpen) {
      delay(160)
      formBringIntoViewRequester.bringIntoView()
    }
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
      onAddEntry(cleanName, parsedAmount)
      nameInput = ""
      amountInput = ""
      nameError = false
      amountError = false
      isFormOpen = false
    }
  }

  val footerFillBrush =
    remember(semanticColors.glassFillStart, semanticColors.glassFillEnd) {
      Brush.horizontalGradient(
        colors = listOf(semanticColors.glassFillStart, semanticColors.glassFillEnd)
      )
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

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
        .background(footerFillBrush),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .drawCardSideAndBottomBorders(borderColor = semanticColors.glassRimStart, cornerRadiusDp = 26f)
          .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      AnimatedVisibility(
        visible = isFormOpen,
        enter = fadeIn(tween(140)) + expandVertically(tween(160)),
        exit = fadeOut(tween(120)) + shrinkVertically(tween(140)),
      ) {
        val inlineShape = RoundedCornerShape(18.dp)
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

      val dashColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button) {
              isFormOpen = !isFormOpen
              if (!isFormOpen) {
                nameError = false
                amountError = false
              }
            }
            .drawBehind {
              val radius = 16.dp.toPx()
              drawRoundRect(
                color = dashColor,
                cornerRadius = CornerRadius(radius, radius),
                style =
                  Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                  ),
              )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("toggle_add_btn_${type.name.lowercase()}"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = if (isFormOpen) stringResource(R.string.cancel) else addButtonLabel,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

private fun Modifier.drawSideBorders(borderColor: Color): Modifier =
  this.drawBehind {
    val w = 1.dp.toPx()
    drawLine(borderColor, Offset(w / 2f, 0f), Offset(w / 2f, size.height), strokeWidth = w)
    drawLine(
      borderColor,
      Offset(size.width - w / 2f, 0f),
      Offset(size.width - w / 2f, size.height),
      strokeWidth = w,
    )
  }

private fun Modifier.drawCardSideAndTopBorders(
  borderColor: Color,
  cornerRadiusDp: Float,
): Modifier =
  this.drawBehind {
    val strokeW = 1.dp.toPx()
    val r = cornerRadiusDp.dp.toPx()
    val inset = strokeW / 2f
    drawLine(borderColor, Offset(inset, r), Offset(inset, size.height), strokeWidth = strokeW)
    drawLine(
      borderColor,
      Offset(size.width - inset, r),
      Offset(size.width - inset, size.height),
      strokeWidth = strokeW,
    )
    drawLine(borderColor, Offset(r, inset), Offset(size.width - r, inset), strokeWidth = strokeW)
    drawArc(
      color = borderColor,
      startAngle = 180f,
      sweepAngle = 90f,
      useCenter = false,
      topLeft = Offset(inset, inset),
      size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
      style = Stroke(width = strokeW),
    )
    drawArc(
      color = borderColor,
      startAngle = 270f,
      sweepAngle = 90f,
      useCenter = false,
      topLeft = Offset(size.width - r * 2 - inset, inset),
      size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
      style = Stroke(width = strokeW),
    )
  }

private fun Modifier.drawCardSideAndBottomBorders(
  borderColor: Color,
  cornerRadiusDp: Float,
): Modifier =
  this.drawBehind {
    val strokeW = 1.dp.toPx()
    val r = cornerRadiusDp.dp.toPx()
    val inset = strokeW / 2f
    drawLine(
      borderColor,
      Offset(inset, 0f),
      Offset(inset, size.height - r),
      strokeWidth = strokeW,
    )
    drawLine(
      borderColor,
      Offset(size.width - inset, 0f),
      Offset(size.width - inset, size.height - r),
      strokeWidth = strokeW,
    )
    drawLine(
      borderColor,
      Offset(r, size.height - inset),
      Offset(size.width - r, size.height - inset),
      strokeWidth = strokeW,
    )
    drawArc(
      color = borderColor,
      startAngle = 90f,
      sweepAngle = 90f,
      useCenter = false,
      topLeft = Offset(inset, size.height - r * 2 - inset),
      size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
      style = Stroke(width = strokeW),
    )
    drawArc(
      color = borderColor,
      startAngle = 0f,
      sweepAngle = 90f,
      useCenter = false,
      topLeft = Offset(size.width - r * 2 - inset, size.height - r * 2 - inset),
      size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
      style = Stroke(width = strokeW),
    )
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
