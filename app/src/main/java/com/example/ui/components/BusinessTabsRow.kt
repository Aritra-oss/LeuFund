package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
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

@Composable
fun BusinessTabsRow(
  openBusinesses: List<BusinessEntity>,
  selectedBusinessId: Long,
  isDark: Boolean,
  onToggleTheme: () -> Unit,
  onSelectBusiness: (Long) -> Unit,
  onAddBusiness: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  var isAddOpen by rememberSaveable { mutableStateOf(false) }
  var newBusinessName by rememberSaveable { mutableStateOf("") }
  var showNameError by rememberSaveable { mutableStateOf(false) }

  fun submitNewBusiness() {
    val trimmed = newBusinessName.trim()
    if (trimmed.isEmpty()) {
      showNameError = true
    } else {
      onAddBusiness(trimmed)
      newBusinessName = ""
      showNameError = false
      isAddOpen = false
    }
  }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      LazyRow(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(start = 16.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
      items(items = openBusinesses, key = { it.id }) { business ->
        val isSelected = business.id == selectedBusinessId
        val palette = BusinessThemePalette.fromId(business.themeId)
        val dotColor = if (isSelected) palette.accentLightTint else palette.accent

        Surface(
          shape = RoundedCornerShape(999.dp),
          color =
            if (isSelected) semanticColors.selectedTabBg else MaterialTheme.colorScheme.surface,
          border =
            BorderStroke(
              width = 1.dp,
              color =
                if (isSelected) semanticColors.selectedTabBorder else semanticColors.cardBorder,
            ),
          modifier =
            Modifier.heightIn(min = 48.dp)
              .clip(RoundedCornerShape(999.dp))
              .semantics { selected = isSelected }
              .clickable(role = Role.Tab) { onSelectBusiness(business.id) }
              .testTag("business_tab_${business.id}"),
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
        Box(
          modifier =
            Modifier.heightIn(min = 48.dp)
              .clip(RoundedCornerShape(999.dp))
              .clickable(role = Role.Button) {
                isAddOpen = !isAddOpen
                showNameError = false
              }
              .drawBehind {
                val stroke =
                  Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                  )
                drawRoundRect(
                  color = dashColor,
                  cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                  style = stroke,
                )
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

      QuickThemeToggleButton(
        isDark = isDark,
        onToggle = onToggleTheme,
        modifier = Modifier.padding(end = 16.dp),
      )
    }

    AnimatedVisibility(visible = isAddOpen) {
      Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, semanticColors.cardBorder),
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
              modifier = Modifier.weight(1f).testTag("new_business_name_input"),
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
