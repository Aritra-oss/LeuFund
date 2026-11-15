package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.BusinessPulseMetrics
import com.example.domain.CurrencyFormatter
import com.example.ui.theme.LocalFundTrackColors

@Composable
fun SummaryTilesRow(
  totalInvestment: Double,
  totalRevenue: Double,
  totalDue: Double,
  pulseMetrics: BusinessPulseMetrics,
  isPrivacyMasked: Boolean,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  val maskedText = stringResource(R.string.masked_amount)

  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    SummaryTile(
      label = stringResource(R.string.summary_invested),
      amount = totalInvestment,
      shareFraction = pulseMetrics.investmentShare,
      icon = Icons.AutoMirrored.Outlined.TrendingDown,
      containerColor = semanticColors.softRedContainer,
      contentColor = semanticColors.softRedContent,
      isPrivacyMasked = isPrivacyMasked,
      maskedText = maskedText,
      modifier = Modifier.weight(1f).testTag("summary_tile_invested"),
    )
    SummaryTile(
      label = stringResource(R.string.summary_earned),
      amount = totalRevenue,
      shareFraction = pulseMetrics.revenueShare,
      icon = Icons.AutoMirrored.Outlined.TrendingUp,
      containerColor = semanticColors.softGreenContainer,
      contentColor = semanticColors.softGreenContent,
      isPrivacyMasked = isPrivacyMasked,
      maskedText = maskedText,
      modifier = Modifier.weight(1f).testTag("summary_tile_earned"),
    )
    SummaryTile(
      label = stringResource(R.string.summary_still_due),
      amount = totalDue,
      shareFraction = pulseMetrics.dueShare,
      icon = Icons.Outlined.Schedule,
      containerColor = semanticColors.softAmberContainer,
      contentColor = semanticColors.softAmberContent,
      isPrivacyMasked = isPrivacyMasked,
      maskedText = maskedText,
      modifier = Modifier.weight(1f).testTag("summary_tile_due"),
    )
  }
}

@Composable
private fun SummaryTile(
  label: String,
  amount: Double,
  shareFraction: Float,
  icon: ImageVector,
  containerColor: Color,
  contentColor: Color,
  isPrivacyMasked: Boolean,
  maskedText: String,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  val displayText =
    remember(amount, isPrivacyMasked, maskedText) {
      if (isPrivacyMasked) maskedText else CurrencyFormatter.formatInr(amount)
    }
  val trackColor = contentColor.copy(alpha = 0.14f)
  val fillColor = contentColor.copy(alpha = 0.78f)
  val tileShape = RoundedCornerShape(22.dp)
  val fillBrush =
    remember(containerColor) {
      Brush.linearGradient(
        colors = listOf(containerColor.copy(alpha = 0.92f), containerColor.copy(alpha = 0.68f))
      )
    }
  val rimBrush =
    remember(semanticColors.glassRimStart, contentColor) {
      Brush.linearGradient(
        colors = listOf(semanticColors.glassRimStart, contentColor.copy(alpha = 0.22f))
      )
    }

  Box(
    modifier =
      modifier
        .shadow(
          elevation = 6.dp,
          shape = tileShape,
          clip = false,
          ambientColor = semanticColors.glassShadow,
          spotColor = semanticColors.glassShadow,
        )
        .clip(tileShape)
        .background(fillBrush)
        .border(width = 1.dp, brush = rimBrush, shape = tileShape),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Box(
          modifier =
            Modifier.size(24.dp)
              .clip(CircleShape)
              .background(contentColor.copy(alpha = 0.14f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(14.dp),
          )
        }
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = contentColor.copy(alpha = 0.92f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Text(
        text = displayText,
        style =
          MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            fontFeatureSettings = "tnum",
          ),
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      // Micro proportional volume bar drawn in drawBehind
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .padding(top = 2.dp)
            .drawBehind {
              val h = 4.dp.toPx()
              val r = h / 2f
              drawRoundRect(
                color = trackColor,
                topLeft = Offset.Zero,
                size = Size(size.width, h),
                cornerRadius = CornerRadius(r, r),
              )
              val clamped = shareFraction.coerceIn(0f, 1f)
              if (clamped > 0f) {
                drawRoundRect(
                  color = fillColor,
                  topLeft = Offset.Zero,
                  size = Size(size.width * clamped.coerceAtLeast(0.08f), h),
                  cornerRadius = CornerRadius(r, r),
                )
              }
            }
            .padding(bottom = 4.dp)
      )
    }
  }
}
