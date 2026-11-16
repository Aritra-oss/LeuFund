package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BusinessEntity
import com.example.domain.BusinessThemePalette
import com.example.ui.theme.LocalFundTrackColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BusinessTabsRow(
  openBusinesses: List<BusinessEntity>,
  selectedBusinessId: Long,
  isDark: Boolean,
  isPrivacyMasked: Boolean,
  onToggleTheme: () -> Unit,
  onTogglePrivacy: () -> Unit,
  onSelectBusiness: (Long) -> Unit,
  onAddBusiness: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  val haptics = LocalHapticFeedback.current
  val coroutineScope = rememberCoroutineScope()
  val formBringIntoViewRequester = remember { BringIntoViewRequester() }
  var isAddOpen by rememberSaveable { mutableStateOf(false) }
  var newBusinessName by rememberSaveable { mutableStateOf("") }
  var showNameError by rememberSaveable { mutableStateOf(false) }

  fun submitNewBusiness() {
    val trimmed = newBusinessName.trim()
    if (trimmed.isEmpty()) {
      showNameError = true
    } else {
      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
      onAddBusiness(trimmed)
      newBusinessName = ""
      showNameError = false
      isAddOpen = false
    }
  }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // Editorial Top Brand Command Bar
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Image(
          painter = painterResource(R.drawable.leufunds_icon),
          contentDescription = stringResource(R.string.app_name),
          contentScale = ContentScale.Crop,
          modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)),
        )
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PrivacyMaskToggleButton(isMasked = isPrivacyMasked, onToggle = onTogglePrivacy)
        QuickThemeToggleButton(isDark = isDark, onToggle = onToggleTheme)
      }
    }

    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      items(items = openBusinesses, key = { it.id }) { business ->
        val isSelected = business.id == selectedBusinessId
        val palette = BusinessThemePalette.fromId(business.themeId)
        val dotColor = if (isSelected) palette.accentLightTint else palette.accent
        val pillShape = RoundedCornerShape(999.dp)
        val fillBrush =
          remember(
            isSelected,
            palette,
            semanticColors.selectedTabBg,
            semanticColors.glassFillStart,
            semanticColors.glassFillEnd,
          ) {
            if (isSelected) {
              Brush.linearGradient(
                colors =
                  listOf(
                    semanticColors.selectedTabBg,
                    palette.gradientEnd.copy(alpha = 0.90f),
                  )
              )
            } else {
              Brush.linearGradient(
                colors = listOf(semanticColors.glassFillStart, semanticColors.glassFillEnd)
              )
            }
          }
        val rimBrush =
          remember(isSelected, palette, semanticColors.glassRimStart, semanticColors.glassRimEnd) {
            if (isSelected) {
              Brush.linearGradient(
                colors =
                  listOf(
                    Color.White.copy(alpha = 0.40f),
                    palette.accentLightTint.copy(alpha = 0.25f),
                  )
              )
            } else {
              Brush.linearGradient(
                colors = listOf(semanticColors.glassRimStart, semanticColors.glassRimEnd)
              )
            }
          }
        Box(
          modifier =
            Modifier.heightIn(min = 48.dp)
              .clip(pillShape)
              .background(fillBrush)
              .border(width = 1.dp, brush = rimBrush, shape = pillShape)
              .semantics { selected = isSelected }
              .futuristicClickable(role = Role.Tab) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectBusiness(business.id)
              }
              .testTag("business_tab_${business.id}"),
          contentAlignment = Alignment.Center,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(dotColor))
            Text(
              text = business.name,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color =
                if (isSelected) semanticColors.selectedTabText
                else MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }

      item(key = "new_business_pill") {
        val dashColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        val pillShape = RoundedCornerShape(999.dp)
        Box(
          modifier =
            Modifier.heightIn(min = 48.dp)
              .clip(pillShape)
              .background(semanticColors.glassFillEnd.copy(alpha = 0.45f))
              .futuristicClickable(role = Role.Button) {
                isAddOpen = !isAddOpen
                showNameError = false
              }
              .drawWithCache {
                val corner = CornerRadius(size.height / 2f, size.height / 2f)
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
              .testTag("new_business_pill"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = stringResource(R.string.new_business_pill),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    AnimatedVisibility(
      visible = isAddOpen,
      enter = fadeIn(tween(140)) + expandVertically(tween(160)),
      exit = fadeOut(tween(120)) + shrinkVertically(tween(140)),
    ) {
      GlassCard(
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp)
            .bringIntoViewRequester(formBringIntoViewRequester),
        shape = RoundedCornerShape(26.dp),
        elevated = true,
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.new_business_pill).removePrefix("+ "),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(
              onClick = {
                isAddOpen = false
                showNameError = false
              },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.cancel),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedTextField(
              value = newBusinessName,
              onValueChange = {
                newBusinessName = it
                if (showNameError && it.trim().isNotEmpty()) showNameError = false
              },
              label = { Text(stringResource(R.string.business_name_label)) },
              singleLine = true,
              isError = showNameError,
              supportingText =
                if (showNameError) {
                  { Text(stringResource(R.string.error_empty_name)) }
                } else null,
              keyboardOptions =
                KeyboardOptions(
                  capitalization = KeyboardCapitalization.Words,
                  imeAction = ImeAction.Done,
                ),
              keyboardActions = KeyboardActions(onDone = { submitNewBusiness() }),
              shape = RoundedCornerShape(16.dp),
              modifier =
                Modifier.weight(1f)
                  .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                      coroutineScope.launch {
                        delay(180)
                        formBringIntoViewRequester.bringIntoView()
                      }
                    }
                  }
                  .testTag("new_business_name_input"),
            )

            Spacer(modifier = Modifier.width(2.dp))

            Button(
              onClick = { submitNewBusiness() },
              shape = RoundedCornerShape(16.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = Color.White,
                ),
              modifier = Modifier.padding(top = 6.dp).heightIn(min = 52.dp).testTag("add_business_btn"),
            ) {
              Text(stringResource(R.string.add))
            }
          }
        }
      }
    }
  }
}
