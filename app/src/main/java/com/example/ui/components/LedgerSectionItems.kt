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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
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
 * Emits a cohesive classic-futuristic glass section directly into the parent [LazyColumn] with
 * unified translucent fills, single-pass `drawWithCache` borders/dividers, and zero placement
 * animation churn so scrolling across section boundaries stays smooth.
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
  maskedAmountLabel: String,
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
      maskedAmountLabel = maskedAmountLabel,
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
        maskedAmountLabel = maskedAmountLabel,
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
  maskedAmountLabel: String,
) {
  val semanticColors = LocalFundTrackColors.current
  val borderColor = semanticColors.glassRimStart
  val sectionFillColor =
    remember(semanticColors.glassFillStart, semanticColors.glassFillEnd) {
      lerp(semanticColors.glassFillStart, semanticColors.glassFillEnd, 0.40f)
    }
  val formattedTotal =
    remember(totalAmount, isPrivacyMasked, maskedAmountLabel) {
      if (isPrivacyMasked) maskedAmountLabel else CurrencyFormatter.formatInr(totalAmount)
    }

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp, top = 16.dp)
        .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
        .background(sectionFillColor)
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
        // Futuristic glowing neon vertical telemetry strip
        Box(
          modifier =
            Modifier.width(4.dp)
              .height(18.dp)
              .clip(RoundedCornerShape(999.dp))
              .background(markerColor)
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(999.dp))
              .background(semanticColors.glassElevatedStart.copy(alpha = 0.72f))
              .border(1.dp, borderColor.copy(alpha = 0.45f), RoundedCornerShape(999.dp)),
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
  val sectionFillColor =
    remember(semanticColors.glassFillStart, semanticColors.glassFillEnd) {
      lerp(semanticColors.glassFillStart, semanticColors.glassFillEnd, 0.40f)
    }
  Box(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp)
        .background(sectionFillColor)
  ) {
    Text(
      text = emptyMessage ?: stringResource(R.string.nothing_here_yet),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier =
        Modifier.fillMaxWidth()
          .drawRowBordersAndDivider(
            borderColor = semanticColors.glassRimStart,
            dividerColor = Color.Transparent,
            showDivider = false,
          )
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
  maskedAmountLabel: String,
  onEdit: (EntryEntity) -> Unit,
  onDelete: (EntryEntity) -> Unit,
  onMarkReceived: ((EntryEntity) -> Unit)?,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  val haptics = LocalHapticFeedback.current
  val entry = model.entry
  val displayAmount = if (isPrivacyMasked) maskedAmountLabel else model.formattedAmount
  val sectionFillColor =
    remember(semanticColors.glassFillStart, semanticColors.glassFillEnd) {
      lerp(semanticColors.glassFillStart, semanticColors.glassFillEnd, 0.40f)
    }
  val dividerColor =
    remember(semanticColors.cardBorder) { semanticColors.cardBorder.copy(alpha = 0.6f) }

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
      modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .background(sectionFillColor)
        .drawRowBordersAndDivider(
          borderColor = semanticColors.glassRimStart,
          dividerColor = dividerColor,
          showDivider = showBottomDivider,
        )
        .clickable(onClick = handleEditClick)
        .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
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

@Composable
private fun LedgerSectionFooter(
  businessId: Long,
  type: EntryType,
  addButtonLabel: String,
  onAddEntry: (name: String, amount: Double) -> Unit,
) {
  val semanticColors = LocalFundTrackColors.current
  var isFormOpen by rememberSaveable(businessId, type) { mutableStateOf(false) }

  val sectionFillColor =
    remember(semanticColors.glassFillStart, semanticColors.glassFillEnd) {
      lerp(semanticColors.glassFillStart, semanticColors.glassFillEnd, 0.40f)
    }
  val dashColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
        .background(sectionFillColor),
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
            .clip(RoundedCornerShape(16.dp))
            .futuristicClickable(role = Role.Button) {
              isFormOpen = !isFormOpen
            }
            .drawWithCache {
              val radius = 16.dp.toPx()
              val corner = CornerRadius(radius, radius)
              val stroke =
                Stroke(
                  width = 1.5.dp.toPx(),
                  pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                )
              onDrawBehind {
                drawRoundRect(color = dashColor, cornerRadius = corner, style = stroke)
              }
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

private fun Modifier.drawRowBordersAndDivider(
  borderColor: Color,
  dividerColor: Color,
  showDivider: Boolean,
): Modifier =
  this.drawWithCache {
    val w = 1.dp.toPx()
    val halfW = w / 2f
    val divPad = 20.dp.toPx()
    val divY = size.height - halfW
    onDrawBehind {
      drawLine(borderColor, Offset(halfW, 0f), Offset(halfW, size.height), strokeWidth = w)
      drawLine(
        borderColor,
        Offset(size.width - halfW, 0f),
        Offset(size.width - halfW, size.height),
        strokeWidth = w,
      )
      if (showDivider) {
        drawLine(dividerColor, Offset(divPad, divY), Offset(size.width - divPad, divY), strokeWidth = w)
      }
    }
  }

private fun Modifier.drawCardSideAndTopBorders(
  borderColor: Color,
  cornerRadiusDp: Float,
): Modifier =
  this.drawWithCache {
    val strokeW = 1.dp.toPx()
    val r = cornerRadiusDp.dp.toPx()
    val inset = strokeW / 2f
    val strokeStyle = Stroke(width = strokeW)
    val arcSize = Size(r * 2f, r * 2f)
    onDrawBehind {
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
        size = arcSize,
        style = strokeStyle,
      )
      drawArc(
        color = borderColor,
        startAngle = 270f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(size.width - r * 2f - inset, inset),
        size = arcSize,
        style = strokeStyle,
      )
    }
  }

private fun Modifier.drawCardSideAndBottomBorders(
  borderColor: Color,
  cornerRadiusDp: Float,
): Modifier =
  this.drawWithCache {
    val strokeW = 1.dp.toPx()
    val r = cornerRadiusDp.dp.toPx()
    val inset = strokeW / 2f
    val strokeStyle = Stroke(width = strokeW)
    val arcSize = Size(r * 2f, r * 2f)
    onDrawBehind {
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
        topLeft = Offset(inset, size.height - r * 2f - inset),
        size = arcSize,
        style = strokeStyle,
      )
      drawArc(
        color = borderColor,
        startAngle = 0f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(size.width - r * 2f - inset, size.height - r * 2f - inset),
        size = arcSize,
        style = strokeStyle,
      )
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
