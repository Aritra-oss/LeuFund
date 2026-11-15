package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current

  Card(
    modifier = modifier.fillMaxWidth().testTag("earnings_by_client_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, semanticColors.cardBorder),
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
          clientEarnings.forEach { client ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = client.displayName,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
                Text(
                  text = CurrencyFormatter.formatInr(client.totalAmount),
                  style =
                    MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.SemiBold,
                      fontFeatureSettings = "tnum",
                    ),
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }

              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
              ) {
                Box(
                  modifier =
                    Modifier.fillMaxWidth(client.fractionOfMax.coerceAtLeast(0.04f))
                      .fillMaxHeight()
                      .clip(RoundedCornerShape(999.dp))
                      .background(accentColor)
                )
              }
            }
          }
        }
      }
    }
  }
}
