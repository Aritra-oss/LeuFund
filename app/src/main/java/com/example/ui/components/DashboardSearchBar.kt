package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalFundTrackColors

@Composable
fun DashboardSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  matchingCount: Int,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  val focusManager = LocalFocusManager.current
  val trimmedQuery = query.trim()

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
      shape = RoundedCornerShape(20.dp),
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface,
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = semanticColors.cardBorder,
        ),
      modifier = Modifier.fillMaxWidth().testTag("dashboard_search_input"),
    )

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
        modifier = Modifier.padding(horizontal = 6.dp).testTag("search_results_caption"),
      )
    }
  }
}
