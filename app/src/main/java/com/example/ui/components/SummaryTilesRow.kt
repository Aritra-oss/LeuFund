package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.CurrencyFormatter
import com.example.ui.theme.LocalFundTrackColors

@Composable
fun SummaryTilesRow(
  totalInvestment: Double,
  totalRevenue: Double,
  totalDue: Double,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current

  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    SummaryTile(
      label = stringResource(R.string.summary_invested),
      amount = totalInvestment,
      containerColor = semanticColors.softRedContainer,
      contentColor = semanticColors.softRedContent,
      modifier = Modifier.weight(1f).testTag("summary_tile_invested"),
    )
    SummaryTile(
      label = stringResource(R.string.summary_earned),
      amount = totalRevenue,
      containerColor = semanticColors.softGreenContainer,
      contentColor = semanticColors.softGreenContent,
      modifier = Modifier.weight(1f).testTag("summary_tile_earned"),
    )
    SummaryTile(
      label = stringResource(R.string.summary_still_due),
      amount = totalDue,
      containerColor = semanticColors.softAmberContainer,
      contentColor = semanticColors.softAmberContent,
      modifier = Modifier.weight(1f).testTag("summary_tile_due"),
    )
  }
}

@Composable
private fun SummaryTile(
  label: String,
  amount: Double,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(24.dp),
    color = containerColor,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = contentColor.copy(alpha = 0.88f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = CurrencyFormatter.formatInr(amount),
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
    }
  }
}
