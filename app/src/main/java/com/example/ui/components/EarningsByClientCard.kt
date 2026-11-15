package com.example.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CurrencyFormatter
import com.example.ui.theme.LocalFundTrackColors
import com.example.ui.viewmodel.ClientEarning

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
    modifier = modifier.fillMaxWidth().testTag("earnings_by_client_card"),
    shape = RoundedCornerShape(26.dp),
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
            val barFraction = client.fractionOfMax.coerceAtLeast(0.04f)

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

              // Draw-phase horizontal progress bar (zero child Box layout pass)
              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(8.dp)
                    .drawBehind {
                      val radius = size.height / 2f
                      drawRoundRect(
                        color = trackColor,
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = CornerRadius(radius, radius),
                      )
                      drawRoundRect(
                        color = accentColor,
                        topLeft = Offset.Zero,
                        size = Size(width = size.width * barFraction, height = size.height),
                        cornerRadius = CornerRadius(radius, radius),
                      )
                    }
              )
            }
          }
        }
      }
    }
  }
}
