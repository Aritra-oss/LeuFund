package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, semanticColors.cardBorder),
    modifier =
      modifier
        .size(48.dp)
        .clip(CircleShape)
        .clickable(role = Role.Switch, onClick = onToggle)
        .testTag("quick_theme_toggle"),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
        contentDescription = stringResource(R.string.toggle_theme_cd),
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
fun AppearanceModeSelector(
  selectedMode: ThemeMode,
  onSelectMode: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    border = BorderStroke(1.dp, semanticColors.cardBorder),
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
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
    modifier =
      modifier
        .heightIn(min = 48.dp)
        .clip(RoundedCornerShape(14.dp))
        .semantics { selected = isSelected }
        .clickable(role = Role.RadioButton, onClick = onClick),
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
