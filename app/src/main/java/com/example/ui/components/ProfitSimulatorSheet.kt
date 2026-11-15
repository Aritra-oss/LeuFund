package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.BusinessThemePalette
import com.example.domain.CurrencyFormatter
import com.example.domain.DashboardInsights
import com.example.ui.theme.HeroSoftRed
import com.example.ui.theme.LocalFundTrackColors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitSimulatorSheet(
  themePalette: BusinessThemePalette,
  currentInvestment: Double,
  currentRevenue: Double,
  currentDue: Double,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val semanticColors = LocalFundTrackColors.current

  val sliderScaleMax =
    remember(currentInvestment, currentRevenue) {
      max(100_000.0, max(currentInvestment, currentRevenue) * 1.5).toFloat()
    }

  var extraRevenue by remember { mutableFloatStateOf(0f) }
  var extraInvestment by remember { mutableFloatStateOf(0f) }
  var includePendingDues by remember { mutableStateOf(currentDue > 0.0) }

  val simulation =
    remember(
      currentInvestment,
      currentRevenue,
      currentDue,
      extraInvestment,
      extraRevenue,
      includePendingDues,
    ) {
      DashboardInsights.simulateWhatIf(
        currentInvestment = currentInvestment,
        currentRevenue = currentRevenue,
        extraInvestment = extraInvestment.toDouble(),
        extraRevenue = extraRevenue.toDouble(),
        includePendingDue = if (includePendingDues) currentDue else 0.0,
      )
    }

  val isNegative = simulation.simulatedNetProfit < 0.0

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = semanticColors.glassElevatedStart,
    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .navigationBarsPadding()
          .padding(horizontal = 20.dp)
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = stringResource(R.string.simulator_title),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = stringResource(R.string.simulator_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Live Projection Vault Card
      val vaultShape = RoundedCornerShape(24.dp)
      val vaultFillBrush =
        remember(themePalette) {
          Brush.linearGradient(
            listOf(
              themePalette.gradientStart.copy(alpha = 0.90f),
              themePalette.gradientEnd.copy(alpha = 0.94f),
            )
          )
        }
      val vaultRimBrush =
        remember(themePalette) {
          Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.14f))
          )
        }
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .shadow(
              elevation = 12.dp,
              shape = vaultShape,
              clip = false,
              ambientColor = themePalette.accent.copy(alpha = 0.42f),
              spotColor = themePalette.accent.copy(alpha = 0.42f),
            )
            .clip(vaultShape)
            .background(vaultFillBrush)
            .border(width = 1.dp, brush = vaultRimBrush, shape = vaultShape),
      ) {
        Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
              text = stringResource(R.string.simulator_projected_net),
              style = MaterialTheme.typography.labelLarge,
              color = if (isNegative) HeroSoftRed else Color.White.copy(alpha = 0.85f),
            )
            Text(
              text =
                (if (isNegative) "-" else "") +
                  CurrencyFormatter.formatInr(abs(simulation.simulatedNetProfit)),
              style = MaterialTheme.typography.displayMedium,
              color = if (isNegative) HeroSoftRed else Color.White,
              modifier = Modifier.testTag("simulator_projected_net_text"),
            )

            val fillColor = if (isNegative) HeroSoftRed else Color(0xFFA7F3D0)
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(8.dp)
                  .clip(RoundedCornerShape(999.dp))
                  .background(Color.White.copy(alpha = 0.16f))
                  .drawBehind {
                    val f = simulation.simulatedRecoveryProgress.coerceIn(0f, 1f)
                    if (f > 0f) {
                      val r = size.height / 2f
                      drawRoundRect(
                        color = fillColor,
                        size = Size(size.width * f, size.height),
                        cornerRadius = CornerRadius(r, r),
                      )
                    }
                  }
            )

            val breakEvenMessage =
              if (simulation.remainingToBreakEven > 0.0) {
                stringResource(
                  R.string.simulator_break_even_gap,
                  CurrencyFormatter.formatInr(simulation.remainingToBreakEven),
                )
              } else {
                val ratio =
                  if (simulation.simulatedInvestment > 0.0) {
                    CurrencyFormatter.formatMultiplier(
                      simulation.simulatedRevenue / simulation.simulatedInvestment
                    ) + "×"
                  } else {
                    "100%"
                  }
                stringResource(R.string.simulator_break_even_done, ratio)
              }
            Text(
              text = breakEvenMessage,
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.9f),
            )
          }
        }
      }

      if (currentDue > 0.0) {
        val duesShape = RoundedCornerShape(18.dp)
        val duesFillBrush =
          remember(semanticColors.softAmberContainer) {
            Brush.linearGradient(
              listOf(
                semanticColors.softAmberContainer.copy(alpha = 0.92f),
                semanticColors.softAmberContainer.copy(alpha = 0.72f),
              )
            )
          }
        val duesRimBrush =
          remember(semanticColors.glassRimStart, semanticColors.softAmberBorder) {
            Brush.linearGradient(
              listOf(semanticColors.glassRimStart, semanticColors.softAmberBorder)
            )
          }
        GlassCard(
          modifier = Modifier.fillMaxWidth(),
          shape = duesShape,
          elevation = 4.dp,
          customFillBrush = duesFillBrush,
          customRimBrush = duesRimBrush,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text =
                stringResource(
                  R.string.simulator_include_dues,
                  CurrencyFormatter.formatInr(currentDue),
                ),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              color = semanticColors.softAmberContent,
              modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Switch(
              checked = includePendingDues,
              onCheckedChange = { includePendingDues = it },
              modifier = Modifier.testTag("simulator_dues_switch"),
            )
          }
        }
      }

      SimulatorSliderBlock(
        label = stringResource(R.string.simulator_extra_revenue),
        value = extraRevenue,
        max = sliderScaleMax,
        accentColor = themePalette.accent,
        onValueChange = { extraRevenue = (it / 500f).roundToLong() * 500f },
      )

      SimulatorSliderBlock(
        label = stringResource(R.string.simulator_extra_investment),
        value = extraInvestment,
        max = sliderScaleMax,
        accentColor = semanticColors.softRedContent,
        onValueChange = { extraInvestment = (it / 500f).roundToLong() * 500f },
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = {
            extraRevenue = 0f
            extraInvestment = 0f
            includePendingDues = currentDue > 0.0
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        ) {
          Text(stringResource(R.string.simulator_reset))
        }
        androidx.compose.material3.Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        ) {
          Text(stringResource(R.string.done))
        }
      }
    }
  }
}

@Composable
private fun SimulatorSliderBlock(
  label: String,
  value: Float,
  max: Float,
  accentColor: Color,
  onValueChange: (Float) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "+" + CurrencyFormatter.formatInr(value.toDouble()),
        style =
          MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum",
          ),
        color = accentColor,
      )
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = 0f..max,
      colors =
        SliderDefaults.colors(
          thumbColor = accentColor,
          activeTrackColor = accentColor,
        ),
    )
  }
}
