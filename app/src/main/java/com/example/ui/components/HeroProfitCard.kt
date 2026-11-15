package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.BusinessEntity
import com.example.domain.BusinessPulseMetrics
import com.example.domain.BusinessThemePalette
import com.example.domain.CurrencyFormatter
import com.example.ui.theme.HeroSoftRed
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroProfitCard(
  business: BusinessEntity,
  themePalette: BusinessThemePalette,
  totalInvestment: Double,
  totalRevenue: Double,
  totalDue: Double,
  netProfit: Double,
  pulseMetrics: BusinessPulseMetrics,
  isPrivacyMasked: Boolean,
  onOpenSettings: () -> Unit,
  onOpenSummary: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isNegative = netProfit < 0.0
  val targetFraction = pulseMetrics.recoveryProgress

  // Animate once on load (and smoothly update if business or values change)
  val animatedProgress = remember(business.id) { Animatable(0f) }
  LaunchedEffect(business.id, targetFraction) {
    animatedProgress.animateTo(
      targetValue = targetFraction,
      animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
    )
  }

  val progressText =
    when {
      totalInvestment <= 0.0 && totalRevenue <= 0.0 -> stringResource(R.string.progress_empty)
      totalInvestment <= 0.0 && totalRevenue > 0.0 -> stringResource(R.string.progress_pure_profit)
      totalRevenue < totalInvestment -> {
        val percent = ((totalRevenue / totalInvestment) * 100.0).roundToInt().coerceIn(0, 99)
        stringResource(R.string.progress_recovered_percent, percent)
      }
      else -> {
        val multiplier = CurrencyFormatter.formatMultiplier(totalRevenue / totalInvestment)
        stringResource(R.string.progress_earned_back, multiplier)
      }
    }

  val gaugeBadgeText =
    when {
      totalInvestment <= 0.0 && totalRevenue <= 0.0 -> "0%"
      totalInvestment <= 0.0 && totalRevenue > 0.0 -> "100%"
      totalRevenue < totalInvestment -> {
        val percent = ((totalRevenue / totalInvestment) * 100.0).roundToInt().coerceIn(0, 99)
        "$percent%"
      }
      else -> "${CurrencyFormatter.formatMultiplier(totalRevenue / totalInvestment)}×"
    }

  val maskedPlaceholder = stringResource(R.string.masked_amount)
  val formattedHeroAmount =
    remember(netProfit, isNegative, isPrivacyMasked, maskedPlaceholder) {
      if (isPrivacyMasked) maskedPlaceholder
      else CurrencyFormatter.formatInr(if (isNegative) abs(netProfit) else netProfit)
    }

  val gradientBrush =
    remember(themePalette) {
      Brush.linearGradient(
        colors =
          listOf(
            themePalette.gradientStart.copy(alpha = 0.88f),
            themePalette.gradientEnd.copy(alpha = 0.92f),
          )
      )
    }
  val rimBrush =
    remember(themePalette) {
      Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.12f))
      )
    }
  val heroShape = RoundedCornerShape(30.dp)

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .shadow(
          elevation = 14.dp,
          shape = heroShape,
          clip = false,
          ambientColor = themePalette.accent.copy(alpha = 0.45f),
          spotColor = themePalette.accent.copy(alpha = 0.45f),
        )
        .clip(heroShape)
        .background(gradientBrush)
        .border(width = 1.dp, brush = rimBrush, shape = heroShape)
        .testTag("hero_profit_card"),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .drawBehind {
            // Luminous radial corner highlight + crisp architectural contour arcs
            val glowCenter = Offset(size.width * 0.88f, size.height * 0.14f)
            drawCircle(
              brush =
                Brush.radialGradient(
                  colors = listOf(themePalette.accentLightTint.copy(alpha = 0.24f), Color.Transparent),
                  center = glowCenter,
                  radius = size.width * 0.65f,
                ),
              center = glowCenter,
              radius = size.width * 0.65f,
            )
            val arcColor = Color.White.copy(alpha = 0.055f)
            drawCircle(
              color = arcColor,
              radius = size.width * 0.52f,
              center = Offset(size.width * 0.96f, size.height * 0.10f),
              style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
              color = arcColor,
              radius = size.width * 0.72f,
              center = Offset(size.width * 0.96f, size.height * 0.10f),
              style = Stroke(width = 1.dp.toPx()),
            )
          }
          .padding(22.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // Top header: initial seal + business name + Settings glass pill
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier =
                Modifier.size(42.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.16f)),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = CurrencyFormatter.extractInitials(business.name),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
              )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = business.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              val healthLabel =
                when {
                  totalInvestment <= 0.0 && totalRevenue <= 0.0 -> stringResource(R.string.status_new)
                  netProfit >= 0.0 -> stringResource(R.string.status_profitable)
                  else -> stringResource(R.string.status_recovering)
                }
              Text(
                text = healthLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                color = themePalette.accentLightTint,
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
            modifier =
              Modifier.heightIn(min = 48.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable(role = Role.Button, onClick = onOpenSettings)
                .testTag("settings_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
              )
            }
          }
        }

        // Asymmetric hero body: giant figure on left + animated radial gauge on right
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text =
                if (isNegative) stringResource(R.string.still_to_recover)
                else stringResource(R.string.net_profit),
              style = MaterialTheme.typography.labelLarge,
              color = if (isNegative) HeroSoftRed else Color.White.copy(alpha = 0.86f),
            )

            Text(
              text = formattedHeroAmount,
              style = MaterialTheme.typography.displayLarge,
              color = if (isNegative) HeroSoftRed else Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag("hero_net_profit_amount"),
            )

            // ROI & Dues projection badges
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              pulseMetrics.roiPercent?.let { roi ->
                val roiText =
                  if (roi >= 0) stringResource(R.string.roi_badge_positive, roi)
                  else stringResource(R.string.roi_badge_negative, roi)
                HeroMetricPill(
                  text = roiText,
                  tint = if (roi >= 0) themePalette.accentLightTint else HeroSoftRed,
                )
              }
              if (totalDue > 0.0) {
                val dueProjectedText =
                  if (isPrivacyMasked) {
                    stringResource(R.string.if_dues_collected, maskedPlaceholder)
                  } else {
                    stringResource(
                      R.string.if_dues_collected,
                      CurrencyFormatter.formatInr(pulseMetrics.projectedNetProfitWithDues),
                    )
                  }
                HeroMetricPill(text = dueProjectedText, tint = Color.White.copy(alpha = 0.9f))
              }
            }
          }

          // Animated Canvas Recovery Ring
          val arcColor = if (isNegative) HeroSoftRed else Color(0xFFA7F3D0)
          val trackColor = Color.White.copy(alpha = 0.16f)
          Box(
            modifier =
              Modifier.size(80.dp).drawBehind {
                val strokeWidth = 7.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(inset, inset)
                drawArc(
                  color = trackColor,
                  startAngle = -90f,
                  sweepAngle = 360f,
                  useCenter = false,
                  topLeft = topLeft,
                  size = arcSize,
                  style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                val sweep = 360f * animatedProgress.value.coerceIn(0f, 1f)
                if (sweep > 0f) {
                  drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                  )
                }
              },
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = gaugeBadgeText,
              style =
                MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontFeatureSettings = "tnum",
                ),
              color = if (isNegative) HeroSoftRed else Color.White,
            )
          }
        }

        // Recovery progress strip + narrative
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          val progressFillColor = if (isNegative) HeroSoftRed else Color(0xFFA7F3D0)
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .drawBehind {
                  val fraction = animatedProgress.value.coerceIn(0f, 1f)
                  if (fraction > 0f) {
                    val radius = size.height / 2f
                    drawRoundRect(
                      color = progressFillColor,
                      size = Size(width = size.width * fraction, height = size.height),
                      cornerRadius = CornerRadius(radius, radius),
                    )
                  }
                }
          )

          Text(
            text = progressText,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.testTag("hero_progress_text"),
          )

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
            modifier =
              Modifier.fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(role = Role.Button, onClick = onOpenSummary)
                .testTag("financial_summary_btn"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Analytics,
                  contentDescription = null,
                  tint = themePalette.accentLightTint,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = stringResource(R.string.financial_summary_button),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  color = Color.White,
                )
              }
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HeroMetricPill(text: String, tint: Color) {
  Surface(
    shape = RoundedCornerShape(999.dp),
    color = Color.White.copy(alpha = 0.12f),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
  ) {
    Text(
      text = text,
      style =
        MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.SemiBold,
          fontFeatureSettings = "tnum",
        ),
      color = tint,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}
