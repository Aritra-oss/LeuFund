package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
  animMemory: DashboardAnimMemory? = null,
) {
  val isNegative = netProfit < 0.0
  val targetFraction = pulseMetrics.recoveryProgress
  val shouldAnimate =
    remember(business.id, targetFraction, netProfit) {
      animMemory?.consumeShouldAnimate("hero_${business.id}", Pair(targetFraction, netProfit))
        ?: true
    }

  // Animate recovery ring smoothly only on initial entry, business switch, or totals update
  val animatedProgress =
    remember(business.id) { Animatable(if (shouldAnimate) 0f else targetFraction) }
  LaunchedEffect(business.id, targetFraction) {
    if (shouldAnimate || animatedProgress.value != targetFraction) {
      animatedProgress.animateTo(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing),
      )
    }
  }

  // Rolling double-precision telemetry interpolation for Net Profit (skipped when scrolling back)
  var startProfit by remember(business.id) { mutableDoubleStateOf(if (shouldAnimate) 0.0 else netProfit) }
  var displayedProfit by remember(business.id) { mutableDoubleStateOf(netProfit) }
  val rollProgress = remember(business.id) { Animatable(1f) }
  LaunchedEffect(business.id, netProfit) {
    if (!shouldAnimate) {
      startProfit = netProfit
      displayedProfit = netProfit
      if (rollProgress.value != 1f) rollProgress.snapTo(1f)
      return@LaunchedEffect
    }
    val initial = if (rollProgress.value < 1f) displayedProfit else startProfit
    startProfit = initial
    rollProgress.snapTo(0f)
    rollProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
    ) {
      displayedProfit =
        if (value >= 0.999f) netProfit
        else startProfit + (netProfit - startProfit) * value.toDouble()
    }
    startProfit = netProfit
    displayedProfit = netProfit
  }

  val infiniteTransition = rememberInfiniteTransition(label = "hero_futuristic")
  val shimmerFraction by
    infiniteTransition.animateFloat(
      initialValue = -0.7f,
      targetValue = 0.7f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 4_600, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "hero_shimmer_sweep",
    )
  val orbitRotation by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 16_000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "hero_orbit_ring",
    )
  val pulseAlpha by
    infiniteTransition.animateFloat(
      initialValue = 0.35f,
      targetValue = 1.0f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1_400, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "hero_beacon_pulse",
    )

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
  val shownAmount = if (rollProgress.value >= 1f) netProfit else displayedProfit
  val formattedHeroAmount =
    remember(shownAmount, isPrivacyMasked, maskedPlaceholder) {
      if (isPrivacyMasked) maskedPlaceholder
      else CurrencyFormatter.formatInr(if (shownAmount < 0.0) abs(shownAmount) else shownAmount)
    }

  val gradientBrush =
    remember(themePalette) {
      Brush.linearGradient(
        colors =
          listOf(
            themePalette.gradientStart.copy(alpha = 0.92f),
            themePalette.gradientEnd.copy(alpha = 0.96f),
          )
      )
    }
  val rimBrush =
    remember(themePalette) {
      Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.50f), themePalette.accentLightTint.copy(alpha = 0.24f))
      )
    }
  val summaryBtnBrush =
    remember {
      Brush.horizontalGradient(
        colors = listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.10f))
      )
    }
  val dashPathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 10f), 0f) }
  val heroShape = RoundedCornerShape(30.dp)

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(heroShape)
        .background(gradientBrush)
        .border(width = 1.dp, brush = rimBrush, shape = heroShape)
        .testTag("hero_profit_card"),
  ) {
    // Static cached HUD contours + radial glow + corner brackets (computed ONCE per size/theme)
    Spacer(
      modifier =
        Modifier.matchParentSize().drawWithCache {
          val w = size.width
          val h = size.height
          val glowCenter = Offset(w * 0.88f, h * 0.14f)
          val glowRadius = w * 0.65f
          val glowBrush =
            Brush.radialGradient(
              colors = listOf(themePalette.accentLightTint.copy(alpha = 0.26f), Color.Transparent),
              center = glowCenter,
              radius = glowRadius,
            )
          val arcColor = Color.White.copy(alpha = 0.065f)
          val arcStroke = Stroke(width = 1.dp.toPx())
          val arcCenter = Offset(w * 0.96f, h * 0.10f)
          val cInset = 14.dp.toPx()
          val cLen = 11.dp.toPx()
          val bracketColor = Color.White.copy(alpha = 0.22f)
          val bw = 1.25.dp.toPx()

          onDrawBehind {
            drawCircle(brush = glowBrush, center = glowCenter, radius = glowRadius)
            drawCircle(color = arcColor, radius = w * 0.50f, center = arcCenter, style = arcStroke)
            drawCircle(color = arcColor, radius = w * 0.72f, center = arcCenter, style = arcStroke)
            drawLine(bracketColor, Offset(cInset, cInset), Offset(cInset + cLen, cInset), strokeWidth = bw)
            drawLine(bracketColor, Offset(cInset, cInset), Offset(cInset, cInset + cLen), strokeWidth = bw)
            drawLine(bracketColor, Offset(w - cInset, h - cInset), Offset(w - cInset - cLen, h - cInset), strokeWidth = bw)
            drawLine(bracketColor, Offset(w - cInset, h - cInset), Offset(w - cInset, h - cInset - cLen), strokeWidth = bw)
          }
        }
    )

    // GPU-only holographic shimmer beam sweep (cached Brush + pure graphicsLayer translationX)
    Spacer(
      modifier =
        Modifier.matchParentSize()
          .graphicsLayer { translationX = shimmerFraction * size.width }
          .drawWithCache {
            val beamW = size.width * 0.56f
            val startX = (size.width - beamW) / 2f
            val beamBrush =
              Brush.linearGradient(
                colors =
                  listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.075f),
                    Color.Transparent,
                  ),
                start = Offset(startX, 0f),
                end = Offset(startX + beamW, size.height),
              )
            onDrawBehind {
              drawRect(brush = beamBrush, topLeft = Offset(startX, 0f), size = Size(beamW, size.height))
            }
          }
    )

    Column(
      modifier = Modifier.fillMaxWidth().padding(22.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
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
                .background(Color.White.copy(alpha = 0.16f))
                .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape),
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
            val beaconColor = if (isNegative) HeroSoftRed else themePalette.accentLightTint
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(7.dp)
                    .graphicsLayer { alpha = pulseAlpha }
                    .clip(CircleShape)
                    .background(beaconColor)
              )
              Text(
                text = healthLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.0.sp),
                color = beaconColor,
              )
            }
          }
        }

        Box(
          modifier =
            Modifier.heightIn(min = 48.dp)
              .clip(RoundedCornerShape(999.dp))
              .background(Color.White.copy(alpha = 0.14f))
              .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)), RoundedCornerShape(999.dp))
              .futuristicClickable(role = Role.Button, onClick = onOpenSettings)
              .testTag("settings_button"),
          contentAlignment = Alignment.Center,
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

      // Asymmetric hero body: giant telemetry figure on left + dual-track orbital gauge on right
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

        // Classic Futuristic Dual-Ring Orbital Telemetry Gauge
        val arcColor = if (isNegative) HeroSoftRed else Color(0xFFA7F3D0)
        val trackColor = Color.White.copy(alpha = 0.16f)
        val outerTickColor = Color.White.copy(alpha = 0.24f)
        Box(modifier = Modifier.size(86.dp), contentAlignment = Alignment.Center) {
          // GPU-rotated cached dashed orbital reticle
          Spacer(
            modifier =
              Modifier.matchParentSize()
                .graphicsLayer { rotationZ = orbitRotation }
                .drawWithCache {
                  val outerRadius = (size.minDimension / 2f) - 1.5.dp.toPx()
                  val stroke = Stroke(width = 1.25.dp.toPx(), pathEffect = dashPathEffect)
                  onDrawBehind {
                    drawCircle(color = outerTickColor, radius = outerRadius, style = stroke)
                  }
                }
          )

          // Recovery arc + beacon (static once settled)
          Spacer(
            modifier =
              Modifier.matchParentSize().drawBehind {
                val insetPad = 7.dp.toPx()
                val strokeWidth = 6.5.dp.toPx()
                val inset = insetPad + strokeWidth / 2f
                val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
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
                  val endRad = ((-90f + sweep) * PI / 180.0)
                  val center = Offset(size.width / 2f, size.height / 2f)
                  val arcRadius = arcSize.width / 2f
                  val dotPos =
                    Offset(
                      x = center.x + (cos(endRad) * arcRadius).toFloat(),
                      y = center.y + (sin(endRad) * arcRadius).toFloat(),
                    )
                  drawCircle(
                    color = Color.White.copy(alpha = 0.92f),
                    radius = 3.dp.toPx(),
                    center = dotPos,
                  )
                }
              }
          )

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

      // Recovery progress strip + narrative + futuristic Summary pill
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val progressFillColor = if (isNegative) HeroSoftRed else Color(0xFFA7F3D0)
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(999.dp))
              .background(Color.White.copy(alpha = 0.16f))
              .drawWithCache {
                val radius = size.height / 2f
                val corner = CornerRadius(radius, radius)
                val fillBrush =
                  Brush.horizontalGradient(
                    colors = listOf(progressFillColor.copy(alpha = 0.78f), Color.White)
                  )
                onDrawBehind {
                  val fraction = animatedProgress.value.coerceIn(0f, 1f)
                  if (fraction > 0f) {
                    drawRoundRect(
                      brush = fillBrush,
                      size = Size(width = size.width * fraction, height = size.height),
                      cornerRadius = corner,
                    )
                  }
                }
              }
        )

        Text(
          text = progressText,
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.92f),
          modifier = Modifier.testTag("hero_progress_text"),
        )

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(min = 48.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(summaryBtnBrush)
              .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
              .futuristicClickable(role = Role.Button, onClick = onOpenSummary)
              .testTag("financial_summary_btn"),
          contentAlignment = Alignment.CenterStart,
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

@Composable
private fun HeroMetricPill(text: String, tint: Color) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(999.dp))
        .background(Color.White.copy(alpha = 0.12f))
        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp)),
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
