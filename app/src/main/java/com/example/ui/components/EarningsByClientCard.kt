package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CurrencyFormatter
import com.example.ui.theme.LocalFundTrackColors
import com.example.ui.viewmodel.ClientEarning
import kotlinx.coroutines.delay

@Composable
fun EarningsByClientCard(
  clientEarnings: List<ClientEarning>,
  accentColor: Color,
  isPrivacyMasked: Boolean,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  val maskedText = stringResource(R.string.masked_amount)
  val trackColor = semanticColors.glassRimEnd

  GlassCard(
    modifier = modifier.fillMaxWidth().graphicsLayer().testTag("earnings_by_client_card"),
    shape = RoundedCornerShape(26.dp),
    shadowTint = accentColor,
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = stringResource(R.string.earnings_by_client),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      if (clientEarnings.isEmpty()) {
        Text(
          text = stringResource(R.string.earnings_by_client_empty),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("earnings_by_client_empty_text"),
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          clientEarnings.forEachIndexed { index, client ->
            val displayTotal =
              if (isPrivacyMasked) maskedText
              else client.formattedAmount.ifEmpty { CurrencyFormatter.formatInr(client.totalAmount) }
            val targetFraction = client.fractionOfMax.coerceAtLeast(0.04f)
            val animatedBar = remember(client.displayName) { Animatable(0f) }

            LaunchedEffect(client.displayName, targetFraction) {
              if (index in 1..6) delay((index * 55).toLong())
              animatedBar.animateTo(
                targetValue = targetFraction,
                animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing),
              )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  modifier = Modifier.weight(1f).padding(end = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Box(
                    modifier =
                      Modifier.size(22.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                          if (index == 0) accentColor.copy(alpha = 0.16f)
                          else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = "${index + 1}",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color =
                        if (index == 0) accentColor
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }

                  Text(
                    text = client.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }

                Text(
                  text = displayTotal,
                  style =
                    MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.SemiBold,
                      fontFeatureSettings = "tnum",
                    ),
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }

              // Cached draw-phase animated luminous progress bar
              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(8.dp)
                    .drawWithCache {
                      val radius = CornerRadius(size.height / 2f, size.height / 2f)
                      val barBrush =
                        Brush.horizontalGradient(
                          colors = listOf(accentColor.copy(alpha = 0.65f), accentColor)
                        )
                      onDrawBehind {
                        drawRoundRect(
                          color = trackColor,
                          topLeft = Offset.Zero,
                          size = size,
                          cornerRadius = radius,
                        )
                        val fillWidth = size.width * animatedBar.value.coerceIn(0f, 1f)
                        if (fillWidth > 0f) {
                          drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset.Zero,
                            size = Size(width = fillWidth, height = size.height),
                            cornerRadius = radius,
                          )
                        }
                      }
                    }
              )
            }
          }
        }
      }
    }
  }
}
