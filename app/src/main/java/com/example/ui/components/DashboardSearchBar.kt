package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.DashboardCategoryFilter
import com.example.domain.EntrySortOrder
import com.example.ui.theme.LocalFundTrackColors

@Composable
fun DashboardSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  matchingCount: Int,
  selectedCategory: DashboardCategoryFilter,
  onSelectCategory: (DashboardCategoryFilter) -> Unit,
  sortOrder: EntrySortOrder,
  onToggleSort: () -> Unit,
  investmentCount: Int,
  revenueCount: Int,
  dueCount: Int,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  val focusManager = LocalFocusManager.current
  val trimmedQuery = query.trim()
  val searchShape = RoundedCornerShape(20.dp)

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 16.dp)
          .shadow(
            elevation = 6.dp,
            shape = searchShape,
            clip = false,
            ambientColor = semanticColors.glassShadow,
            spotColor = semanticColors.glassShadow,
          )
    ) {
      OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
          Text(
            text = stringResource(R.string.search_transactions_hint),
            style = MaterialTheme.typography.bodyMedium,
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
        },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(
              onClick = { onQueryChange("") },
              modifier = Modifier.size(48.dp).testTag("clear_search_btn"),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.clear_search_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = searchShape,
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedContainerColor = semanticColors.glassElevatedStart,
            unfocusedContainerColor = semanticColors.glassFillStart,
            focusedBorderColor = accentColor,
            unfocusedBorderColor = semanticColors.glassRimStart,
          ),
        modifier = Modifier.fillMaxWidth().testTag("dashboard_search_input"),
      )
    }

    // Category filter & sort chips row
    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      item(key = "filter_all") {
        FilterChipPill(
          label = stringResource(R.string.filter_all),
          count = investmentCount + revenueCount + dueCount,
          isSelected = selectedCategory == DashboardCategoryFilter.ALL,
          accentColor = accentColor,
          onClick = { onSelectCategory(DashboardCategoryFilter.ALL) },
          testTag = "filter_chip_all",
        )
      }
      item(key = "filter_investment") {
        FilterChipPill(
          label = stringResource(R.string.section_investment),
          count = investmentCount,
          isSelected = selectedCategory == DashboardCategoryFilter.INVESTMENT,
          accentColor = semanticColors.softRedContent,
          onClick = { onSelectCategory(DashboardCategoryFilter.INVESTMENT) },
          testTag = "filter_chip_investment",
        )
      }
      item(key = "filter_revenue") {
        FilterChipPill(
          label = stringResource(R.string.section_revenue),
          count = revenueCount,
          isSelected = selectedCategory == DashboardCategoryFilter.REVENUE,
          accentColor = accentColor,
          onClick = { onSelectCategory(DashboardCategoryFilter.REVENUE) },
          testTag = "filter_chip_revenue",
        )
      }
      item(key = "filter_due") {
        FilterChipPill(
          label = stringResource(R.string.section_money_due),
          count = dueCount,
          isSelected = selectedCategory == DashboardCategoryFilter.DUE,
          accentColor = semanticColors.softAmberContent,
          onClick = { onSelectCategory(DashboardCategoryFilter.DUE) },
          testTag = "filter_chip_due",
        )
      }
      item(key = "sort_toggle") {
        val isSortedHighest = sortOrder == EntrySortOrder.HIGHEST_AMOUNT
        val sortShape = RoundedCornerShape(999.dp)
        val sortFillBrush =
          remember(
            isSortedHighest,
            accentColor,
            semanticColors.glassFillStart,
            semanticColors.glassFillEnd,
          ) {
            if (isSortedHighest) {
              Brush.linearGradient(
                colors = listOf(accentColor.copy(alpha = 0.22f), accentColor.copy(alpha = 0.12f))
              )
            } else {
              Brush.linearGradient(
                colors = listOf(semanticColors.glassFillStart, semanticColors.glassFillEnd)
              )
            }
          }
        val sortRimBrush =
          remember(
            isSortedHighest,
            accentColor,
            semanticColors.glassRimStart,
            semanticColors.glassRimEnd,
          ) {
            if (isSortedHighest) {
              Brush.linearGradient(
                colors = listOf(accentColor.copy(alpha = 0.72f), accentColor.copy(alpha = 0.28f))
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
              .shadow(
                elevation = 4.dp,
                shape = sortShape,
                clip = false,
                ambientColor = semanticColors.glassShadow,
                spotColor = semanticColors.glassShadow,
              )
              .clip(sortShape)
              .background(sortFillBrush)
              .border(width = 1.dp, brush = sortRimBrush, shape = sortShape)
              .clickable(role = Role.Button, onClick = onToggleSort)
              .testTag("sort_order_toggle"),
          contentAlignment = Alignment.Center,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.Sort,
              contentDescription = null,
              tint =
                if (isSortedHighest) accentColor
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text =
                if (isSortedHighest) stringResource(R.string.sort_highest)
                else stringResource(R.string.sort_newest),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color =
                if (isSortedHighest) accentColor
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    AnimatedVisibility(
      visible = trimmedQuery.isNotEmpty(),
      enter = fadeIn(tween(120)) + expandVertically(tween(140)),
      exit = fadeOut(tween(100)) + shrinkVertically(tween(120)),
    ) {
      Text(
        text =
          if (matchingCount == 0) {
            stringResource(R.string.search_no_results, trimmedQuery)
          } else {
            pluralStringResource(R.plurals.search_results_count, matchingCount, matchingCount)
          },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 22.dp).testTag("search_results_caption"),
      )
    }
  }
}

@Composable
private fun FilterChipPill(
  label: String,
  count: Int,
  isSelected: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  val semanticColors = LocalFundTrackColors.current
  val pillShape = RoundedCornerShape(999.dp)
  val fillBrush =
    remember(
      isSelected,
      accentColor,
      semanticColors.glassFillStart,
      semanticColors.glassFillEnd,
    ) {
      if (isSelected) {
        Brush.linearGradient(
          colors = listOf(accentColor.copy(alpha = 0.94f), accentColor.copy(alpha = 0.78f))
        )
      } else {
        Brush.linearGradient(
          colors = listOf(semanticColors.glassFillStart, semanticColors.glassFillEnd)
        )
      }
    }
  val rimBrush =
    remember(isSelected, semanticColors.glassRimStart, semanticColors.glassRimEnd) {
      if (isSelected) {
        Brush.linearGradient(
          colors = listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.14f))
        )
      } else {
        Brush.linearGradient(
          colors = listOf(semanticColors.glassRimStart, semanticColors.glassRimEnd)
        )
      }
    }
  val shadowTint =
    if (isSelected) accentColor.copy(alpha = 0.35f) else semanticColors.glassShadow

  Box(
    modifier =
      Modifier.heightIn(min = 48.dp)
        .shadow(
          elevation = if (isSelected) 6.dp else 4.dp,
          shape = pillShape,
          clip = false,
          ambientColor = shadowTint,
          spotColor = shadowTint,
        )
        .clip(pillShape)
        .background(fillBrush)
        .border(width = 1.dp, brush = rimBrush, shape = pillShape)
        .semantics { selected = isSelected }
        .clickable(role = Role.Tab, onClick = onClick)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
        color =
          if (isSelected) Color.White.copy(alpha = 0.85f)
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
