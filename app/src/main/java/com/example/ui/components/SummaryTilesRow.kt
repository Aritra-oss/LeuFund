package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
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
  animMemory: DashboardAnimMemory? = null,
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
      animMemory = animMemory,
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
      animMemory = animMemory,
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
      animMemory = animMemory,
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
  animMemory: DashboardAnimMemory?,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  val displayText =
    remember(amount, isPrivacyMasked, maskedText) {
      if (isPrivacyMasked) maskedText else CurrencyFormatter.formatInr(amount)
    }
  val targetShare = shareFraction.coerceIn(0f, 1f)
  val shouldAnimate =
    remember(label, targetShare) {
      animMemory?.consumeShouldAnimate("tile_$label", targetShare) ?: true
    }
  val animatedShare = remember(label) { Animatable(if (shouldAnimate) 0f else targetShare) }
  LaunchedEffect(targetShare) {
    if (shouldAnimate || animatedShare.value != targetShare) {
      animatedShare.animateTo(
        targetValue = targetShare,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
      )
    }
  }

  val trackColor = contentColor.copy(alpha = 0.14f)
  val fillColor = contentColor.copy(alpha = 0.85f)
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
        colors = listOf(semanticColors.glassRimStart, contentColor.copy(alpha = 0.25f))
      )
    }

  Box(
    modifier =
      modifier
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

      // Cached draw-phase proportional volume bar
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .padding(top = 2.dp)
            .drawWithCache {
              val h = 4.dp.toPx()
              val r = CornerRadius(h / 2f, h / 2f)
              val barBrush =
                Brush.horizontalGradient(
                  colors = listOf(fillColor.copy(alpha = 0.55f), fillColor)
                )
              onDrawBehind {
                drawRoundRect(
                  color = trackColor,
                  topLeft = Offset.Zero,
                  size = Size(size.width, h),
                  cornerRadius = r,
                )
                val clamped = animatedShare.value.coerceIn(0f, 1f)
                if (clamped > 0.005f) {
                  drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset.Zero,
                    size = Size(size.width * clamped.coerceAtLeast(0.08f), h),
                    cornerRadius = r,
                  )
                }
              }
            }
            .padding(bottom = 4.dp)
      )
    }
  }
}
