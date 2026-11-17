package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BusinessEntity
import com.example.data.local.EntryEntity
import com.example.domain.BusinessThemePalette
import com.example.domain.CurrencyFormatter
import com.example.domain.FinancialSummaryCalculator
import com.example.domain.PeriodAnalytics
import com.example.domain.SummaryPeriod
import com.example.ui.theme.HeroSoftRed
import com.example.ui.theme.LocalFundTrackColors
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialSummaryBottomSheet(
  business: BusinessEntity,
  themePalette: BusinessThemePalette,
  allEntries: List<EntryEntity>,
  isPrivacyMasked: Boolean,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val semanticColors = LocalFundTrackColors.current
  val maskedText = stringResource(R.string.masked_amount)

  val periods =
    remember(allEntries) { FinancialSummaryCalculator.availablePeriods(allEntries) }
  var selectedPeriod by remember(business.id) { mutableStateOf<SummaryPeriod>(SummaryPeriod.Last30Days) }

  // Ensure selectedPeriod stays valid if entries list changes (e.g., an old year deleted)
  val effectivePeriod = if (selectedPeriod in periods) selectedPeriod else SummaryPeriod.Lifetime

  val analytics =
    remember(allEntries, effectivePeriod) {
      FinancialSummaryCalculator.computeAnalytics(entries = allEntries, period = effectivePeriod)
    }

  val rangeCaption =
    remember(analytics) {
      val caption = FinancialSummaryCalculator.formatDateRangeCaption(analytics)
      caption.ifEmpty { null }
    }

  fun formatAmountOrMask(amount: Double): String =
    if (isPrivacyMasked) maskedText else CurrencyFormatter.formatInr(amount)

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
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = stringResource(R.string.financial_summary_title, business.name),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.testTag("financial_summary_sheet_title"),
        )
        Text(
          text = rangeCaption ?: stringResource(R.string.financial_summary_all_time),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("financial_summary_date_range"),
        )
      }

      // Period selector pills
      val periodsScrollState = rememberScrollState()
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .horizontalScroll(periodsScrollState)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        for (period in periods) {
          val isSelected = period == effectivePeriod
          val label = periodLabel(period)
          val pillShape = RoundedCornerShape(999.dp)
          val fillBrush =
            if (isSelected) {
              Brush.linearGradient(
                listOf(
                  themePalette.accent.copy(alpha = 0.94f),
                  themePalette.accent.copy(alpha = 0.78f),
                )
              )
            } else {
              Brush.linearGradient(
                listOf(semanticColors.glassFillStart, semanticColors.glassFillEnd)
              )
            }
          val rimBrush =
            if (isSelected) {
              Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.14f))
              )
            } else {
              Brush.linearGradient(
                listOf(semanticColors.glassRimStart, semanticColors.glassRimEnd)
              )
            }

          Box(
            modifier =
              Modifier.heightIn(min = 48.dp)
                .clip(pillShape)
                .background(fillBrush)
                .border(width = 1.dp, brush = rimBrush, shape = pillShape)
                .semantics { selected = isSelected }
                .clickable(role = Role.Tab) { selectedPeriod = period }
                .testTag("summary_period_${periodTag(period)}"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
          }
        }
      }

      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Net result vault card
        PeriodHeroCard(
          analytics = analytics,
          themePalette = themePalette,
          isPrivacyMasked = isPrivacyMasked,
          maskedText = maskedText,
        )

        // Three frosted breakdown tiles
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          PeriodBreakdownTile(
            label = stringResource(R.string.summary_invested),
            amountText = formatAmountOrMask(analytics.totalInvestment),
            count = analytics.investmentCount,
            containerColor = semanticColors.softRedContainer,
            contentColor = semanticColors.softRedContent,
            modifier = Modifier.weight(1f).testTag("summary_period_invested"),
          )
          PeriodBreakdownTile(
            label = stringResource(R.string.summary_earned),
            amountText = formatAmountOrMask(analytics.totalRevenue),
            count = analytics.revenueCount,
            containerColor = semanticColors.softGreenContainer,
            contentColor = semanticColors.softGreenContent,
            modifier = Modifier.weight(1f).testTag("summary_period_earned"),
          )
          PeriodBreakdownTile(
            label = stringResource(R.string.summary_still_due),
            amountText = formatAmountOrMask(analytics.totalDue),
            count = analytics.dueCount,
            containerColor = semanticColors.softAmberContainer,
            contentColor = semanticColors.softAmberContent,
            modifier = Modifier.weight(1f).testTag("summary_period_due"),
          )
        }

        if (analytics.entryCount == 0) {
          GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(18.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.Analytics,
                contentDescription = null,
                tint = themePalette.accent,
                modifier = Modifier.size(28.dp),
              )
              Text(
                text = stringResource(R.string.financial_summary_empty_period, periodLabel(effectivePeriod)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        } else {
          PeriodInsightsCard(
            analytics = analytics,
            themePalette = themePalette,
            isPrivacyMasked = isPrivacyMasked,
            maskedText = maskedText,
          )
        }

        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(16.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = themePalette.accent,
              contentColor = Color.White,
            ),
          modifier =
            Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("financial_summary_done_btn"),
        ) {
          Text(text = stringResource(R.string.done), fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun PeriodHeroCard(
  analytics: PeriodAnalytics,
  themePalette: BusinessThemePalette,
  isPrivacyMasked: Boolean,
  maskedText: String,
) {
  val isNegative = analytics.netProfit < 0.0
  val cardShape = RoundedCornerShape(24.dp)
  val fillBrush =
    remember(themePalette) {
      Brush.linearGradient(
        listOf(
          themePalette.gradientStart.copy(alpha = 0.90f),
          themePalette.gradientEnd.copy(alpha = 0.94f),
        )
      )
    }
  val rimBrush =
    remember(themePalette) {
      Brush.linearGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.14f)))
    }
  val displayAmount =
    if (isPrivacyMasked) {
      maskedText
    } else {
      (if (isNegative) "-" else "") + CurrencyFormatter.formatInr(abs(analytics.netProfit))
    }

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .clip(cardShape)
        .background(fillBrush)
        .border(width = 1.dp, brush = rimBrush, shape = cardShape),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text =
            if (isNegative) stringResource(R.string.financial_summary_net_deficit)
            else stringResource(R.string.net_profit),
          style = MaterialTheme.typography.labelLarge,
          color = if (isNegative) HeroSoftRed else Color.White.copy(alpha = 0.88f),
        )

        val entriesLabel =
          pluralStringResource(
            R.plurals.financial_summary_entry_count,
            analytics.entryCount,
            analytics.entryCount,
          )
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(999.dp))
              .background(Color.White.copy(alpha = 0.14f))
              .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = entriesLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            color = Color.White.copy(alpha = 0.92f),
          )
        }
      }

      Text(
        text = displayAmount,
        style = MaterialTheme.typography.displayMedium,
        color = if (isNegative) HeroSoftRed else Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag("summary_period_net_profit"),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        analytics.roiPercent?.let { roi ->
          val roiText =
            if (roi >= 0) stringResource(R.string.roi_badge_positive, roi)
            else stringResource(R.string.roi_badge_negative, roi)
          SummaryHeroPill(
            text = roiText,
            tint = if (roi >= 0) themePalette.accentLightTint else HeroSoftRed,
          )
        }
        if (analytics.revenueCount + analytics.dueCount > 0) {
          SummaryHeroPill(
            text =
              "${stringResource(R.string.collection_rate_label)}: ${analytics.collectionRatePercent}%",
            tint = Color.White.copy(alpha = 0.92f),
          )
        }
      }
    }
  }
}

@Composable
private fun SummaryHeroPill(text: String, tint: Color) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(999.dp))
        .background(Color.White.copy(alpha = 0.12f))
        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
        .padding(horizontal = 10.dp, vertical = 4.dp)
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
    )
  }
}

@Composable
private fun PeriodBreakdownTile(
  label: String,
  amountText: String,
  count: Int,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  val tileShape = RoundedCornerShape(20.dp)
  val fillBrush =
    remember(containerColor) {
      Brush.linearGradient(
        listOf(containerColor.copy(alpha = 0.92f), containerColor.copy(alpha = 0.68f))
      )
    }
  val rimBrush =
    remember(semanticColors.glassRimStart, contentColor) {
      Brush.linearGradient(listOf(semanticColors.glassRimStart, contentColor.copy(alpha = 0.22f)))
    }

  Box(
    modifier =
      modifier
        .clip(tileShape)
        .background(fillBrush)
        .border(width = 1.dp, brush = rimBrush, shape = tileShape)
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = contentColor.copy(alpha = 0.9f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = amountText,
        style =
          MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum",
          ),
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = pluralStringResource(R.plurals.financial_summary_entry_count, count, count),
        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
        color = contentColor.copy(alpha = 0.78f),
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun PeriodInsightsCard(
  analytics: PeriodAnalytics,
  themePalette: BusinessThemePalette,
  isPrivacyMasked: Boolean,
  maskedText: String,
) {
  val semanticColors = LocalFundTrackColors.current
  val invColor = semanticColors.softRedContent
  val revColor = themePalette.accent
  val dueColor = semanticColors.softAmberContent
  val trackColor = semanticColors.glassRimEnd

  GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = stringResource(R.string.financial_summary_mix_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      // Multi-segment horizontal flow bar
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor)
            .drawBehind {
              val h = size.height
              val r = h / 2f
              val segments =
                listOf(
                  analytics.investmentShare to invColor,
                  analytics.revenueShare to revColor,
                  analytics.dueShare to dueColor,
                ).filter { it.first > 0f }

              var xOffset = 0f
              val gapPx = if (segments.size > 1) 3.dp.toPx() else 0f
              val usableWidth = (size.width - gapPx * (segments.size - 1)).coerceAtLeast(0f)

              segments.forEach { (share, color) ->
                val segWidth = (usableWidth * share).coerceAtLeast(4.dp.toPx())
                drawRoundRect(
                  color = color,
                  topLeft = Offset(xOffset, 0f),
                  size = Size(segWidth.coerceAtMost(size.width - xOffset), h),
                  cornerRadius = CornerRadius(r, r),
                )
                xOffset += segWidth + gapPx
              }
            }
      )

      SummaryInsightRow(
        dotColor = revColor,
        label = stringResource(R.string.financial_summary_avg_entry),
        value =
          if (isPrivacyMasked) maskedText
          else CurrencyFormatter.formatInr(analytics.averageTransactionAmount),
      )

      analytics.topClientName?.let { clientName ->
        SummaryInsightRow(
          dotColor = revColor,
          label = stringResource(R.string.financial_summary_top_client),
          value =
            if (isPrivacyMasked) "$clientName ($maskedText)"
            else "$clientName · ${CurrencyFormatter.formatInr(analytics.topClientAmount)}",
        )
      }

      analytics.largestInvestmentName?.let { expenseName ->
        SummaryInsightRow(
          dotColor = invColor,
          label = stringResource(R.string.financial_summary_top_expense),
          value =
            if (isPrivacyMasked) "$expenseName ($maskedText)"
            else "$expenseName · ${CurrencyFormatter.formatInr(analytics.largestInvestmentAmount)}",
        )
      }
    }
  }
}

@Composable
private fun SummaryInsightRow(dotColor: Color, label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
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
      modifier = Modifier.padding(start = 12.dp),
    )
  }
}

@Composable
private fun periodLabel(period: SummaryPeriod): String =
  when (period) {
    SummaryPeriod.Today -> stringResource(R.string.period_today)
    SummaryPeriod.Last7Days -> stringResource(R.string.period_last_7_days)
    SummaryPeriod.Last30Days -> stringResource(R.string.period_last_30_days)
    SummaryPeriod.LastYear -> stringResource(R.string.period_last_year)
    is SummaryPeriod.CalendarYear -> period.year.toString()
    SummaryPeriod.Lifetime -> stringResource(R.string.period_lifetime)
  }

private fun periodTag(period: SummaryPeriod): String =
  when (period) {
    SummaryPeriod.Today -> "today"
    SummaryPeriod.Last7Days -> "last_7_days"
    SummaryPeriod.Last30Days -> "last_30_days"
    SummaryPeriod.LastYear -> "last_year"
    is SummaryPeriod.CalendarYear -> "year_${period.year}"
    SummaryPeriod.Lifetime -> "lifetime"
  }
