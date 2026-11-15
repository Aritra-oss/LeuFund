package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BusinessEntity
import com.example.domain.BusinessThemePalette
import com.example.ui.theme.LocalFundTrackColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FirstRunScreen(
  closedBusinesses: List<BusinessEntity>,
  isDark: Boolean,
  onToggleTheme: () -> Unit,
  onCreateBusiness: (String) -> Unit,
  onReopenBusiness: (Long) -> Unit,
  onDeleteBusiness: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  var businessName by rememberSaveable { mutableStateOf("") }
  var showNameError by rememberSaveable { mutableStateOf(false) }
  var pendingDeleteBusiness by remember { mutableStateOf<BusinessEntity?>(null) }

  val greenTheme = BusinessThemePalette.GREEN
  val semanticColors = LocalFundTrackColors.current
  val coroutineScope = rememberCoroutineScope()
  val formBringIntoViewRequester = remember { BringIntoViewRequester() }
  val gradientBrush =
    remember(greenTheme) {
      Brush.linearGradient(colors = listOf(greenTheme.gradientStart, greenTheme.gradientEnd))
    }

  fun attemptCreate() {
    if (businessName.trim().isEmpty()) {
      showNameError = true
    } else {
      onCreateBusiness(businessName.trim())
      businessName = ""
      showNameError = false
    }
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 600.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 24.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      // App brand header with quick light/dark toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Image(
            painter = painterResource(R.drawable.leufunds_icon),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)),
          )
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }

        QuickThemeToggleButton(isDark = isDark, onToggle = onToggleTheme)
      }

      // Large green gradient card
      Surface(
        modifier =
          Modifier.fillMaxWidth()
            .bringIntoViewRequester(formBringIntoViewRequester)
            .testTag("first_run_card"),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
      ) {
        Box(modifier = Modifier.background(gradientBrush).padding(24.dp)) {
          Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
              text = stringResource(R.string.first_run_title),
              style = MaterialTheme.typography.headlineMedium,
              color = Color.White,
            )

            Text(
              text = stringResource(R.string.first_run_subtitle),
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.85f),
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
              value = businessName,
              onValueChange = {
                businessName = it
                if (showNameError && it.trim().isNotEmpty()) {
                  showNameError = false
                }
              },
              label = { Text(stringResource(R.string.business_name_label)) },
              singleLine = true,
              isError = showNameError,
              supportingText =
                if (showNameError) {
                  {
                    Text(
                      text = stringResource(R.string.error_empty_name),
                      color = Color(0xFFFFC1C1),
                    )
                  }
                } else {
                  null
                },
              keyboardOptions =
                KeyboardOptions(
                  capitalization = KeyboardCapitalization.Words,
                  imeAction = ImeAction.Done,
                ),
              keyboardActions = KeyboardActions(onDone = { attemptCreate() }),
              shape = RoundedCornerShape(16.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White,
                  focusedBorderColor = Color.White,
                  unfocusedBorderColor = Color.White.copy(alpha = 0.45f),
                  focusedLabelColor = Color.White,
                  unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                  cursorColor = Color.White,
                  errorBorderColor = Color(0xFFFFB4B4),
                  errorLabelColor = Color(0xFFFFB4B4),
                  focusedContainerColor = Color.White.copy(alpha = 0.08f),
                  unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                ),
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
                  .testTag("first_run_business_name_input"),
            )

            Button(
              onClick = { attemptCreate() },
              shape = RoundedCornerShape(16.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = Color.White,
                  contentColor = greenTheme.gradientEnd,
                ),
              contentPadding = PaddingValues(vertical = 14.dp),
              modifier =
                Modifier.fillMaxWidth()
                  .heightIn(min = 52.dp)
                  .testTag("create_first_business_button"),
            ) {
              Text(
                text = stringResource(R.string.create_business),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      if (closedBusinesses.isNotEmpty()) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, semanticColors.cardBorder),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = stringResource(R.string.closed_businesses),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )

            closedBusinesses.forEach { business ->
              val palette = BusinessThemePalette.fromId(business.themeId)
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Box(
                  modifier =
                    Modifier.size(12.dp)
                      .clip(CircleShape)
                      .background(palette.accent)
                )
                Text(
                  text = business.name,
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                  onClick = { onReopenBusiness(business.id) },
                  shape = RoundedCornerShape(12.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                  modifier = Modifier.heightIn(min = 48.dp).testTag("reopen_${business.id}"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(stringResource(R.string.reopen))
                }
                TextButton(
                  onClick = { pendingDeleteBusiness = business },
                  colors =
                    ButtonDefaults.textButtonColors(
                      contentColor = semanticColors.softRedContent
                    ),
                  modifier = Modifier.heightIn(min = 48.dp).testTag("delete_closed_${business.id}"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(stringResource(R.string.delete))
                }
              }
            }
          }
        }
      }

      CautionCard()
    }
  }

  pendingDeleteBusiness?.let { target ->
    AlertDialog(
      onDismissRequest = { pendingDeleteBusiness = null },
      title = { Text(stringResource(R.string.confirm_delete_business_title, target.name)) },
      text = { Text(stringResource(R.string.confirm_delete_business_body, target.name)) },
      confirmButton = {
        Button(
          onClick = {
            onDeleteBusiness(target.id)
            pendingDeleteBusiness = null
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = semanticColors.softRedContent,
              contentColor = Color.White,
            ),
        ) {
          Text(stringResource(R.string.delete))
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDeleteBusiness = null }) {
          Text(stringResource(R.string.cancel))
        }
      },
    )
  }
}
