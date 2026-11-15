package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
) {
  val semanticColors = LocalFundTrackColors.current
  val maskedText = stringResource(R.string.masked_amount)
  val invColor = semanticColors.softRedContent
  val revColor = accentColor
  val dueColor = semanticColors.softAmberContent
  val emptyTrackColor = semanticColors.cardBorder

  GlassCard(
    modifier = modifier.fillMaxWidth().testTag("cashflow_radar_card"),
    shape = RoundedCornerShape(26.dp),
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

        Surface(
          shape = RoundedCornerShape(999.dp),
          color = accentColor.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
          modifier =
            Modifier.heightIn(min = 48.dp)
              .clip(RoundedCornerShape(999.dp))
              .clickable(role = Role.Button, onClick = onOpenSimulator)
              .testTag("open_simulator_btn"),
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
        // Multi-segment Canvas Cashflow Ring
        Box(
          modifier =
            Modifier.size(86.dp).drawBehind {
              val strokeWidth = 10.dp.toPx()
              val inset = strokeWidth / 2f
              val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
              val topLeft = Offset(inset, inset)

              if (pulseMetrics.totalCashflowVolume <= 0.0) {
                drawArc(
                  color = emptyTrackColor,
                  startAngle = -90f,
                  sweepAngle = 360f,
                  useCenter = false,
                  topLeft = topLeft,
                  size = arcSize,
                  style = Stroke(width = strokeWidth),
                )
              } else {
                var currentStart = -90f
                val segments =
                  listOf(
                    pulseMetrics.investmentShare to invColor,
                    pulseMetrics.revenueShare to revColor,
                    pulseMetrics.dueShare to dueColor,
                  ).filter { it.first > 0f }

                val gapAngle = if (segments.size > 1) 4f else 0f
                val availableAngle = 360f - gapAngle * segments.size
                segments.forEach { (share, color) ->
                  val sweep = (availableAngle * share).coerceAtLeast(3f)
                  drawArc(
                    color = color,
                    startAngle = currentStart,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                  )
                  currentStart += sweep + gapAngle
                }
              }
            },
          contentAlignment = Alignment.Center,
        ) {
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
