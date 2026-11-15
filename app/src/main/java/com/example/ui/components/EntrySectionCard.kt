package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.theme.LocalFundTrackColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntrySectionCard(
  businessId: Long,
  type: EntryType,
  title: String,
  addButtonLabel: String,
  entries: List<EntryEntity>,
  totalAmount: Double,
  markerColor: Color,
  badgeContainerColor: Color,
  badgeContentColor: Color,
  onAddEntry: (name: String, amount: Double) -> Unit,
  onDeleteEntry: (EntryEntity) -> Unit,
  onMarkDueReceived: ((EntryEntity) -> Unit)? = null,
  emptyMessage: String? = null,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
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
      onAddEntry(cleanName, parsedAmount)
      nameInput = ""
      amountInput = ""
      nameError = false
      amountError = false
      isFormOpen = false
    }
  }

  Card(
    modifier = modifier.fillMaxWidth().testTag("section_card_${type.name.lowercase()}"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, semanticColors.cardBorder),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Section header: square color marker + title + right-aligned total
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier =
              Modifier.size(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(markerColor)
          )
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        Text(
          text = CurrencyFormatter.formatInr(totalAmount),
          style =
            MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontFeatureSettings = "tnum",
            ),
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.testTag("section_total_${type.name.lowercase()}"),
        )
      }

      // Entries list or empty placeholder
      if (entries.isEmpty()) {
        Text(
          text = emptyMessage ?: stringResource(R.string.nothing_here_yet),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("empty_text_${type.name.lowercase()}"),
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          entries.forEachIndexed { index, entry ->
            key(entry.id) {
              EntryRow(
                entry = entry,
                badgeContainerColor = badgeContainerColor,
                badgeContentColor = badgeContentColor,
                receivedContainerColor = semanticColors.softGreenContainer,
                receivedContentColor = semanticColors.softGreenContent,
                onDelete = { onDeleteEntry(entry) },
                onMarkReceived =
                  if (type == EntryType.DUE && onMarkDueReceived != null) {
                    { onMarkDueReceived(entry) }
                  } else {
                    null
                  },
              )
              if (index < entries.lastIndex) {
                HorizontalDivider(
                  color = semanticColors.cardBorder.copy(alpha = 0.6f),
                  thickness = 1.dp,
                )
              }
            }
          }
        }
      }

      // Inline add form
      AnimatedVisibility(
        visible = isFormOpen,
        enter = fadeIn(tween(140)) + expandVertically(tween(160)),
        exit = fadeOut(tween(120)) + shrinkVertically(tween(140)),
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .bringIntoViewRequester(formBringIntoViewRequester)
              .clip(RoundedCornerShape(18.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
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
              // Allow digits and at most one decimal point
              val filtered = sanitizeDecimalInput(raw)
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

      // Dashed toggle button: "+ Add ..." / "Cancel"
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

@Composable
private fun EntryRow(
  entry: EntryEntity,
  badgeContainerColor: Color,
  badgeContentColor: Color,
  receivedContainerColor: Color,
  receivedContentColor: Color,
  onDelete: () -> Unit,
  onMarkReceived: (() -> Unit)?,
) {
  val initials = remember(entry.name) { CurrencyFormatter.extractInitials(entry.name) }
  val formattedAmount = remember(entry.amount) { CurrencyFormatter.formatInr(entry.amount) }

  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("entry_row_${entry.id}"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Rounded-square initial badge
    Box(
      modifier =
        Modifier.size(40.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(badgeContainerColor),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = initials,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = badgeContentColor,
      )
    }

    // Name & formatted amount
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = entry.name,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = formattedAmount,
        style =
          MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum",
          ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    if (onMarkReceived != null) {
      FilledTonalButton(
        onClick = onMarkReceived,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors =
          ButtonDefaults.filledTonalButtonColors(
            containerColor = receivedContainerColor,
            contentColor = receivedContentColor,
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
      onClick = onDelete,
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

private fun sanitizeDecimalInput(input: String): String {
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
