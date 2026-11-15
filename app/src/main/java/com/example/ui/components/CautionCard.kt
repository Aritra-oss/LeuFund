package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalFundTrackColors

@Composable
fun CautionCard(modifier: Modifier = Modifier) {
  val semanticColors = LocalFundTrackColors.current
  val fillBrush =
    remember(semanticColors.softAmberContainer) {
      Brush.linearGradient(
        colors =
          listOf(
            semanticColors.softAmberContainer.copy(alpha = 0.92f),
            semanticColors.softAmberContainer.copy(alpha = 0.68f),
          )
      )
    }
  val rimBrush =
    remember(semanticColors.glassRimStart, semanticColors.softAmberBorder) {
      Brush.linearGradient(colors = listOf(semanticColors.glassRimStart, semanticColors.softAmberBorder))
    }

  GlassCard(
    modifier = modifier.fillMaxWidth().testTag("caution_card"),
    shape = RoundedCornerShape(24.dp),
    elevation = 6.dp,
    customFillBrush = fillBrush,
    customRimBrush = rimBrush,
  ) {
    Row(
      modifier = Modifier.padding(18.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = Icons.Outlined.WarningAmber,
        contentDescription = stringResource(R.string.caution_title),
        tint = semanticColors.softAmberContent,
        modifier = Modifier.size(22.dp).padding(top = 2.dp),
      )

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = stringResource(R.string.caution_title),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = semanticColors.softAmberContent,
        )
        Text(
          text = stringResource(R.string.caution_body),
          style = MaterialTheme.typography.bodySmall,
          color = semanticColors.softAmberContent.copy(alpha = 0.92f),
        )
      }
    }
  }
}
