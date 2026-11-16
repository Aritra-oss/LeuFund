package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ThemeMode
import com.example.ui.theme.LocalFundTrackColors

@Composable
fun QuickThemeToggleButton(
  isDark: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  val haptics = LocalHapticFeedback.current
  val fillBrush =
    remember(semanticColors.glassElevatedStart, semanticColors.glassElevatedEnd) {
      Brush.linearGradient(
        colors = listOf(semanticColors.glassElevatedStart, semanticColors.glassElevatedEnd)
      )
    }
  val rimBrush =
    remember(semanticColors.glassRimStart, semanticColors.glassRimEnd) {
      Brush.linearGradient(colors = listOf(semanticColors.glassRimStart, semanticColors.glassRimEnd))
    }

  Box(
    modifier =
      modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(fillBrush)
        .border(width = 1.dp, brush = rimBrush, shape = CircleShape)
        .futuristicClickable(role = Role.Switch) {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onToggle()
        }
        .testTag("quick_theme_toggle"),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
      contentDescription = stringResource(R.string.toggle_theme_cd),
      tint = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.size(20.dp),
    )
  }
}

@Composable
fun PrivacyMaskToggleButton(
  isMasked: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  val haptics = LocalHapticFeedback.current
  val primaryColor = MaterialTheme.colorScheme.primary
  val fillBrush =
    remember(
      isMasked,
      primaryColor,
      semanticColors.glassElevatedStart,
      semanticColors.glassElevatedEnd,
    ) {
      if (isMasked) {
        Brush.linearGradient(
          colors = listOf(primaryColor.copy(alpha = 0.26f), primaryColor.copy(alpha = 0.14f))
        )
      } else {
        Brush.linearGradient(
          colors = listOf(semanticColors.glassElevatedStart, semanticColors.glassElevatedEnd)
        )
      }
    }
  val rimBrush =
    remember(isMasked, primaryColor, semanticColors.glassRimStart, semanticColors.glassRimEnd) {
      if (isMasked) {
        Brush.linearGradient(
          colors = listOf(primaryColor.copy(alpha = 0.7f), primaryColor.copy(alpha = 0.3f))
        )
      } else {
        Brush.linearGradient(
          colors = listOf(semanticColors.glassRimStart, semanticColors.glassRimEnd)
        )
      }
    }

  Box(
    modifier =
      modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(fillBrush)
        .border(width = 1.dp, brush = rimBrush, shape = CircleShape)
        .futuristicClickable(role = Role.Switch) {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onToggle()
        }
        .testTag("privacy_mask_toggle"),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = if (isMasked) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
      contentDescription =
        if (isMasked) stringResource(R.string.show_amounts_cd)
        else stringResource(R.string.hide_amounts_cd),
      tint =
        if (isMasked) primaryColor
        else MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.size(20.dp),
    )
  }
}

@Composable
fun AppearanceModeSelector(
  selectedMode: ThemeMode,
  onSelectMode: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  val selectorShape = RoundedCornerShape(18.dp)
  val fillBrush =
    remember(semanticColors.glassFillStart, semanticColors.glassFillEnd) {
      Brush.linearGradient(
        colors = listOf(semanticColors.glassFillStart, semanticColors.glassFillEnd)
      )
    }
  val rimBrush =
    remember(semanticColors.glassRimStart, semanticColors.glassRimEnd) {
      Brush.linearGradient(colors = listOf(semanticColors.glassRimStart, semanticColors.glassRimEnd))
    }

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(selectorShape)
        .background(fillBrush)
        .border(width = 1.dp, brush = rimBrush, shape = selectorShape)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      AppearanceOption(
        label = stringResource(R.string.theme_system),
        icon = Icons.Outlined.BrightnessAuto,
        isSelected = selectedMode == ThemeMode.SYSTEM,
        onClick = { onSelectMode(ThemeMode.SYSTEM) },
        modifier = Modifier.weight(1f).testTag("theme_mode_system"),
      )
      AppearanceOption(
        label = stringResource(R.string.theme_light),
        icon = Icons.Outlined.LightMode,
        isSelected = selectedMode == ThemeMode.LIGHT,
        onClick = { onSelectMode(ThemeMode.LIGHT) },
        modifier = Modifier.weight(1f).testTag("theme_mode_light"),
      )
      AppearanceOption(
        label = stringResource(R.string.theme_dark),
        icon = Icons.Outlined.DarkMode,
        isSelected = selectedMode == ThemeMode.DARK,
        onClick = { onSelectMode(ThemeMode.DARK) },
        modifier = Modifier.weight(1f).testTag("theme_mode_dark"),
      )
    }
  }
}

@Composable
private fun AppearanceOption(
  label: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val optionShape = RoundedCornerShape(14.dp)
  val primaryColor = MaterialTheme.colorScheme.primary
  Box(
    modifier =
      modifier
        .heightIn(min = 48.dp)
        .clip(optionShape)
        .then(
          if (isSelected) {
            Modifier.background(
                Brush.linearGradient(
                  colors = listOf(primaryColor.copy(alpha = 0.94f), primaryColor.copy(alpha = 0.80f))
                )
              )
              .border(
                width = 1.dp,
                brush =
                  Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.12f))
                  ),
                shape = optionShape,
              )
          } else {
            Modifier.background(Color.Transparent)
          }
        )
        .semantics { selected = isSelected }
        .futuristicClickable(role = Role.RadioButton, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
