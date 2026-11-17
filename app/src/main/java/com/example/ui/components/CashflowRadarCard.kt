package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.BusinessPulseMetrics
import com.example.domain.CurrencyFormatter
import com.example.ui.theme.LocalFundTrackColors

@Composable
fun CashflowRadarCard(
  pulseMetrics: BusinessPulseMetrics,
  accentColor: Color,
  isPrivacyMasked: Boolean,
  onOpenSimulator: () -> Unit,
  modifier: Modifier = Modifier,
  animMemory: DashboardAnimMemory? = null,
) {
  val semanticColors = LocalFundTrackColors.current
  val maskedText = stringResource(R.string.masked_amount)
  val invColor = semanticColors.softRedContent
  val revColor = accentColor
  val dueColor = semanticColors.softAmberContent
  val emptyTrackColor = semanticColors.cardBorder

  val radarSignature =
    remember(
      pulseMetrics.investmentShare,
      pulseMetrics.revenueShare,
      pulseMetrics.dueShare,
      pulseMetrics.totalCashflowVolume,
    ) {
      "${pulseMetrics.investmentShare}_${pulseMetrics.revenueShare}_${pulseMetrics.dueShare}_${pulseMetrics.totalCashflowVolume}"
    }
  val shouldAnimate =
    remember(radarSignature) {
      animMemory?.consumeShouldAnimate("cashflow_radar", radarSignature) ?: true
    }

  val sweepAnim = remember { Animatable(if (shouldAnimate) 0f else 1f) }
  LaunchedEffect(radarSignature) {
    if (shouldAnimate) {
      sweepAnim.snapTo(0f)
      sweepAnim.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
      )
    } else if (sweepAnim.value != 1f) {
      sweepAnim.snapTo(1f)
    }
  }

  GlassCard(
    modifier = modifier.fillMaxWidth().testTag("cashflow_radar_card"),
    shape = RoundedCornerShape(22.dp),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = stringResource(R.string.cashflow_radar_title),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )

        Box(
          modifier =
            Modifier.heightIn(min = 48.dp)
              .clip(RoundedCornerShape(999.dp))
              .background(accentColor.copy(alpha = 0.14f))
              .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)), RoundedCornerShape(999.dp))
              .futuristicClickable(role = Role.Button, onClick = onOpenSimulator)
              .testTag("open_simulator_btn"),
          contentAlignment = Alignment.Center,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.AutoGraph,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = stringResource(R.string.what_if_simulator),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = accentColor,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Classic multi-segment cashflow donut ring
        Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
          Spacer(
            modifier =
              Modifier.matchParentSize().drawWithCache {
                val strokeWidth = 9.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(inset, inset)
                val arcStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                val emptyStroke = Stroke(width = strokeWidth)

                val segments = buildList {
                  if (pulseMetrics.investmentShare > 0f) add(pulseMetrics.investmentShare to invColor)
                  if (pulseMetrics.revenueShare > 0f) add(pulseMetrics.revenueShare to revColor)
                  if (pulseMetrics.dueShare > 0f) add(pulseMetrics.dueShare to dueColor)
                }
                val gapAngle = if (segments.size > 1) 5f else 0f
                val availableAngle = 360f - gapAngle * segments.size

                onDrawBehind {
                  if (pulseMetrics.totalCashflowVolume <= 0.0 || segments.isEmpty()) {
                    drawArc(
                      color = emptyTrackColor,
                      startAngle = -90f,
                      sweepAngle = 360f,
                      useCenter = false,
                      topLeft = topLeft,
                      size = arcSize,
                      style = emptyStroke,
                    )
                  } else {
                    var currentStart = -90f
                    val animMultiplier = sweepAnim.value.coerceIn(0.01f, 1f)
                    for (i in segments.indices) {
                      val (share, color) = segments[i]
                      val fullSweep = (availableAngle * share).coerceAtLeast(3f)
                      val sweep = fullSweep * animMultiplier
                      drawArc(
                        color = color,
                        startAngle = currentStart,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = arcStroke,
                      )
                      currentStart += fullSweep + gapAngle
                    }
                  }
                }
              }
          )

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "${pulseMetrics.collectionRatePercent}%",
              style =
                MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontFeatureSettings = "tnum",
                ),
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = stringResource(R.string.collection_rate_label),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          PulseMetricRow(
            dotColor = revColor,
            label = stringResource(R.string.total_volume_label),
            value =
              if (isPrivacyMasked) maskedText
              else CurrencyFormatter.formatInr(pulseMetrics.totalCashflowVolume),
          )
          PulseMetricRow(
            dotColor = dueColor,
            label = stringResource(R.string.if_dues_collected, "").trimEnd(':', ' '),
            value =
              if (isPrivacyMasked) maskedText
              else CurrencyFormatter.formatInr(pulseMetrics.projectedNetProfitWithDues),
          )
          if (pulseMetrics.topClientName != null && pulseMetrics.topClientSharePercent != null) {
            PulseMetricRow(
              dotColor = invColor,
              label = stringResource(R.string.top_client_label),
              value = "${pulseMetrics.topClientName} (${pulseMetrics.topClientSharePercent}%)",
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PulseMetricRow(
  dotColor: Color,
  label: String,
  value: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Text(
      text = value,
      style =
        MaterialTheme.typography.labelLarge.copy(
          fontWeight = FontWeight.SemiBold,
          fontFeatureSettings = "tnum",
        ),
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(start = 8.dp),
    )
  }
}
