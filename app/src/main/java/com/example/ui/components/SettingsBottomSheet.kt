package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BusinessEntity
import com.example.domain.BackupStatus
import com.example.domain.BusinessThemePalette
import com.example.domain.ThemeMode
import com.example.ui.theme.LocalFundTrackColors

private sealed interface SettingsConfirmation {
  data object DeleteAllData : SettingsConfirmation

  data object CloseBusiness : SettingsConfirmation

  data class DeleteClosedBusiness(val business: BusinessEntity) : SettingsConfirmation
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
  selectedBusiness: BusinessEntity,
  closedBusinesses: List<BusinessEntity>,
  backupStatus: BackupStatus,
  themeMode: ThemeMode,
  biometricLockEnabled: Boolean,
  onSelectThemeMode: (ThemeMode) -> Unit,
  onToggleBiometricLock: (Boolean) -> Unit,
  onOpenChangeMpin: () -> Unit,
  onLockNow: () -> Unit,
  onSelectTheme: (Int) -> Unit,
  onExportCsv: () -> Unit,
  onExportPdf: () -> Unit,
  onDeleteAllData: () -> Unit,
  onCloseBusiness: () -> Unit,
  onReopenBusiness: (Long) -> Unit,
  onDeleteClosedBusiness: (Long) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val semanticColors = LocalFundTrackColors.current
  var confirmation by remember { mutableStateOf<SettingsConfirmation?>(null) }

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
          .padding(horizontal = 20.dp)
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Text(
        text = stringResource(R.string.settings_title, selectedBusiness.name),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("settings_sheet_title"),
      )

      if (backupStatus.shouldRemind) {
        BackupReminderBanner(
          daysSinceLastExport = backupStatus.daysSinceLastExport,
          onExportCsv = onExportCsv,
          onExportPdf = onExportPdf,
        )
      }

      // Appearance Mode (System / Light / Dark)
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = stringResource(R.string.appearance),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppearanceModeSelector(
          selectedMode = themeMode,
          onSelectMode = onSelectThemeMode,
        )
      }

      HorizontalDivider(color = semanticColors.cardBorder)

      // 6 Theme Swatches
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = stringResource(R.string.colour_theme),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          BusinessThemePalette.entries.forEach { palette ->
            val isSelected = palette.id == selectedBusiness.themeId
            Box(
              modifier =
                Modifier.size(48.dp)
                  .clip(CircleShape)
                  .semantics {
                    selected = isSelected
                    contentDescription = "${palette.displayName} theme"
                  }
                  .clickable(role = Role.RadioButton) { onSelectTheme(palette.id) }
                  .testTag("theme_swatch_${palette.displayName.lowercase()}"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.size(40.dp)
                    .clip(CircleShape)
                    .background(
                      Brush.linearGradient(listOf(palette.gradientStart, palette.gradientEnd))
                    )
                    .then(
                      if (isSelected) {
                        Modifier.border(
                          width = 2.5.dp,
                          color = MaterialTheme.colorScheme.onSurface,
                          shape = CircleShape,
                        )
                      } else {
                        Modifier
                      }
                    ),
                contentAlignment = Alignment.Center,
              ) {
                if (isSelected) {
                  Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }
        }
      }

      HorizontalDivider(color = semanticColors.cardBorder)

      // App Security (Biometric Lock)
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = stringResource(R.string.security_section_title),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val securityCardShape = RoundedCornerShape(18.dp)
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(securityCardShape)
              .background(
                Brush.linearGradient(
                  colors = listOf(semanticColors.glassFillStart, semanticColors.glassFillEnd)
                )
              )
              .border(
                width = 1.dp,
                brush =
                  Brush.linearGradient(
                    colors = listOf(semanticColors.glassRimStart, semanticColors.glassRimEnd)
                  ),
                shape = securityCardShape,
              )
              .clickable(role = Role.Switch) { onToggleBiometricLock(!biometricLockEnabled) },
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Fingerprint,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp),
            )
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text(
                text = stringResource(R.string.biometric_setting_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = stringResource(R.string.biometric_setting_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = biometricLockEnabled,
              onCheckedChange = onToggleBiometricLock,
              modifier = Modifier.testTag("biometric_lock_switch"),
            )
          }
        }

        SettingsActionTile(
          icon = Icons.Outlined.Pin,
          title = stringResource(R.string.change_mpin_title),
          subtitle = stringResource(R.string.change_mpin_desc),
          containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
          contentColor = MaterialTheme.colorScheme.onSurface,
          onClick = {
            onDismiss()
            onOpenChangeMpin()
          },
          testTag = "change_mpin_btn",
        )

        SettingsActionTile(
          icon = Icons.Outlined.Lock,
          title = stringResource(R.string.lock_app_now),
          subtitle = stringResource(R.string.lock_app_now_desc),
          containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
          contentColor = MaterialTheme.colorScheme.onSurface,
          onClick = {
            onDismiss()
            onLockNow()
          },
          testTag = "lock_app_now_btn",
        )
      }

      HorizontalDivider(color = semanticColors.cardBorder)

      // Actions: Export CSV, Delete all data & Close business
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsActionTile(
          icon = Icons.Outlined.PictureAsPdf,
          title = stringResource(R.string.export_pdf_title),
          subtitle = stringResource(R.string.export_pdf_desc, selectedBusiness.name),
          containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
          contentColor = MaterialTheme.colorScheme.onSurface,
          onClick = onExportPdf,
          testTag = "export_pdf_btn",
        )

        SettingsActionTile(
          icon = Icons.Outlined.Download,
          title = stringResource(R.string.export_csv_title),
          subtitle = stringResource(R.string.export_csv_desc, selectedBusiness.name),
          containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
          contentColor = MaterialTheme.colorScheme.onSurface,
          onClick = onExportCsv,
          testTag = "export_csv_btn",
        )

        val backupCaption =
          when (val days = backupStatus.daysSinceLastExport) {
            null -> stringResource(R.string.backup_never)
            0 -> stringResource(R.string.backup_last_today)
            1 -> stringResource(R.string.backup_last_one_day)
            else -> stringResource(R.string.backup_last_days, days)
          }
        Text(
          text = backupCaption,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp).testTag("backup_status_caption"),
        )

        SettingsActionTile(
          icon = Icons.Outlined.DeleteSweep,
          title = stringResource(R.string.delete_all_data),
          subtitle = stringResource(R.string.delete_all_data_desc),
          containerColor = semanticColors.softRedContainer.copy(alpha = 0.7f),
          contentColor = semanticColors.softRedContent,
          onClick = { confirmation = SettingsConfirmation.DeleteAllData },
          testTag = "delete_all_data_btn",
        )

        SettingsActionTile(
          icon = Icons.Outlined.Archive,
          title = stringResource(R.string.close_business),
          subtitle = stringResource(R.string.close_business_desc),
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          contentColor = MaterialTheme.colorScheme.onSurface,
          onClick = { confirmation = SettingsConfirmation.CloseBusiness },
          testTag = "close_business_btn",
        )
      }

      // Closed businesses list
      if (closedBusinesses.isNotEmpty()) {
        HorizontalDivider(color = semanticColors.cardBorder)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = stringResource(R.string.closed_businesses),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          closedBusinesses.forEach { closed ->
            val palette = BusinessThemePalette.fromId(closed.themeId)
            val cardShape = RoundedCornerShape(18.dp)
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(cardShape)
                  .background(
                    Brush.linearGradient(
                      colors = listOf(semanticColors.glassFillStart, semanticColors.glassFillEnd)
                    )
                  )
                  .border(
                    width = 1.dp,
                    brush =
                      Brush.linearGradient(
                        colors = listOf(semanticColors.glassRimStart, semanticColors.glassRimEnd)
                      ),
                    shape = cardShape,
                  ),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(palette.accent))
                Text(
                  text = closed.name,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                  onClick = { onReopenBusiness(closed.id) },
                  shape = RoundedCornerShape(12.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.heightIn(min = 48.dp).testTag("sheet_reopen_${closed.id}"),
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
                  onClick = { confirmation = SettingsConfirmation.DeleteClosedBusiness(closed) },
                  colors =
                    ButtonDefaults.textButtonColors(contentColor = semanticColors.softRedContent),
                  modifier = Modifier.heightIn(min = 48.dp).testTag("sheet_delete_${closed.id}"),
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

      Spacer(modifier = Modifier.height(4.dp))

      Button(
        onClick = onDismiss,
        shape = RoundedCornerShape(16.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
          ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("settings_done_btn"),
      ) {
        Text(text = stringResource(R.string.done), fontWeight = FontWeight.SemiBold)
      }
    }
  }

  when (val activeConfirm = confirmation) {
    SettingsConfirmation.DeleteAllData -> {
      AlertDialog(
        onDismissRequest = { confirmation = null },
        title = { Text(stringResource(R.string.confirm_delete_all_title)) },
        text = { Text(stringResource(R.string.confirm_delete_all_body, selectedBusiness.name)) },
        confirmButton = {
          Button(
            onClick = {
              onDeleteAllData()
              confirmation = null
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
          TextButton(onClick = { confirmation = null }) {
            Text(stringResource(R.string.cancel))
          }
        },
      )
    }

    SettingsConfirmation.CloseBusiness -> {
      AlertDialog(
        onDismissRequest = { confirmation = null },
        title = { Text(stringResource(R.string.confirm_close_title, selectedBusiness.name)) },
        text = { Text(stringResource(R.string.confirm_close_body, selectedBusiness.name)) },
        confirmButton = {
          Button(
            onClick = {
              onCloseBusiness()
              confirmation = null
              onDismiss()
            }
          ) {
            Text(stringResource(R.string.close_business))
          }
        },
        dismissButton = {
          TextButton(onClick = { confirmation = null }) {
            Text(stringResource(R.string.cancel))
          }
        },
      )
    }

    is SettingsConfirmation.DeleteClosedBusiness -> {
      AlertDialog(
        onDismissRequest = { confirmation = null },
        title = {
          Text(stringResource(R.string.confirm_delete_business_title, activeConfirm.business.name))
        },
        text = {
          Text(stringResource(R.string.confirm_delete_business_body, activeConfirm.business.name))
        },
        confirmButton = {
          Button(
            onClick = {
              onDeleteClosedBusiness(activeConfirm.business.id)
              confirmation = null
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
          TextButton(onClick = { confirmation = null }) {
            Text(stringResource(R.string.cancel))
          }
        },
      )
    }

    null -> Unit
  }
}

@Composable
private fun SettingsActionTile(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  containerColor: Color,
  contentColor: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  val semanticColors = LocalFundTrackColors.current
  val tileShape = RoundedCornerShape(18.dp)
  val fillBrush =
    remember(containerColor) {
      Brush.linearGradient(
        colors = listOf(containerColor.copy(alpha = 0.92f), containerColor.copy(alpha = 0.65f))
      )
    }
  val rimBrush =
    remember(semanticColors.glassRimStart, semanticColors.glassRimEnd) {
      Brush.linearGradient(colors = listOf(semanticColors.glassRimStart, semanticColors.glassRimEnd))
    }

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 56.dp)
        .clip(tileShape)
        .background(fillBrush)
        .border(width = 1.dp, brush = rimBrush, shape = tileShape)
        .clickable(role = Role.Button, onClick = onClick)
        .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(22.dp),
      )
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = contentColor,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = contentColor.copy(alpha = 0.85f),
        )
      }
    }
  }
}

@Composable
private fun BackupReminderBanner(
  daysSinceLastExport: Int?,
  onExportCsv: () -> Unit,
  onExportPdf: () -> Unit,
) {
  val semanticColors = LocalFundTrackColors.current
  val bannerFillBrush =
    remember(semanticColors.softAmberContainer) {
      Brush.linearGradient(
        colors =
          listOf(
            semanticColors.softAmberContainer.copy(alpha = 0.92f),
            semanticColors.softAmberContainer.copy(alpha = 0.72f),
          )
      )
    }
  val bannerRimBrush =
    remember(semanticColors.glassRimStart, semanticColors.softAmberBorder) {
      Brush.linearGradient(
        colors = listOf(semanticColors.glassRimStart, semanticColors.softAmberBorder)
      )
    }

  GlassCard(
    modifier = Modifier.fillMaxWidth().testTag("backup_reminder_banner"),
    shape = RoundedCornerShape(22.dp),
    elevation = 6.dp,
    customFillBrush = bannerFillBrush,
    customRimBrush = bannerRimBrush,
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.NotificationsNone,
          contentDescription = stringResource(R.string.backup_prompt_title),
          tint = semanticColors.softAmberContent,
          modifier = Modifier.size(22.dp),
        )
        Text(
          text = stringResource(R.string.backup_prompt_title),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = semanticColors.softAmberContent,
        )
      }

      Text(
        text =
          if (daysSinceLastExport == null) {
            stringResource(R.string.backup_prompt_body_never)
          } else {
            stringResource(R.string.backup_prompt_body_days, daysSinceLastExport)
          },
        style = MaterialTheme.typography.bodySmall,
        color = semanticColors.softAmberContent.copy(alpha = 0.92f),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onExportCsv,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, semanticColors.softAmberContent.copy(alpha = 0.45f)),
          colors =
            ButtonDefaults.outlinedButtonColors(contentColor = semanticColors.softAmberContent),
          modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("reminder_export_csv_btn"),
        ) {
          Text(
            text = stringResource(R.string.backup_cta_csv),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
        }

        Button(
          onClick = onExportPdf,
          shape = RoundedCornerShape(12.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = semanticColors.softAmberContent,
              contentColor = Color.White,
            ),
          modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("reminder_export_pdf_btn"),
        ) {
          Text(
            text = stringResource(R.string.backup_cta_pdf),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}
