package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BusinessEntity
import com.example.domain.BusinessThemePalette
import com.example.domain.CurrencyFormatter
import com.example.ui.theme.HeroSoftRed
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HeroProfitCard(
  business: BusinessEntity,
  themePalette: BusinessThemePalette,
  totalInvestment: Double,
  totalRevenue: Double,
  netProfit: Double,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isNegative = netProfit < 0.0
  val targetFraction =
    when {
      totalInvestment <= 0.0 && totalRevenue <= 0.0 -> 0f
      totalInvestment <= 0.0 && totalRevenue > 0.0 -> 1f
      else -> (totalRevenue / totalInvestment).toFloat().coerceIn(0f, 1f)
    }

  // Animate once on load (and smoothly update if business or values change)
  val animatedProgress = remember(business.id) { Animatable(0f) }
  LaunchedEffect(business.id, targetFraction) {
    animatedProgress.animateTo(
      targetValue = targetFraction,
      animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
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

  val gradientBrush =
    remember(themePalette) {
      Brush.linearGradient(colors = listOf(themePalette.gradientStart, themePalette.gradientEnd))
    }

  Surface(
    modifier = modifier.fillMaxWidth().testTag("hero_profit_card"),
    shape = RoundedCornerShape(28.dp),
    color = Color.Transparent,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
  ) {
    Box(modifier = Modifier.background(gradientBrush).padding(22.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // Top bar: initial badge + business name + Settings pill button
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
                Modifier.size(40.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.18f)),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = CurrencyFormatter.extractInitials(business.name),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
              )
            }
            Text(
              text = business.name,
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = 0.16f),
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

        // Net profit / Still to recover figure
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text =
              if (isNegative) stringResource(R.string.still_to_recover)
              else stringResource(R.string.net_profit),
            style = MaterialTheme.typography.labelLarge,
            color = if (isNegative) HeroSoftRed else Color.White.copy(alpha = 0.85f),
          )

          Text(
            text = CurrencyFormatter.formatInr(if (isNegative) abs(netProfit) else netProfit),
            style = MaterialTheme.typography.displayLarge,
            color = if (isNegative) HeroSoftRed else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("hero_net_profit_amount"),
          )
        }

        // Animated recovery progress bar (drawn in draw phase to avoid layout passes)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          val progressFillColor = if (isNegative) HeroSoftRed else Color(0xFFA7F3D0)
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.18f))
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
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.testTag("hero_progress_text"),
          )
        }
      }
    }
  }
}
