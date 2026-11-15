package com.example.domain

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import com.example.data.local.EntryEntity
import com.example.ui.viewmodel.FundTrackUiState
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object PdfReportGenerator {

  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842
  private const val MARGIN_H = 36f
  private const val MARGIN_TOP = 36f
  private const val MARGIN_BOTTOM = 44f
  private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_H * 2f)

  fun suggestedFileName(businessName: String, nowMillis: Long = System.currentTimeMillis()): String {
    val sanitized =
      businessName
        .trim()
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifEmpty { "business" }
        .take(32)
    val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(nowMillis))
    return "leufunds_${sanitized}_report_${dateStamp}.pdf"
  }

  fun writeBusinessReportPdf(
    dashboard: FundTrackUiState.Dashboard,
    outputStream: OutputStream,
    generatedAtMillis: Long = System.currentTimeMillis(),
  ) {
    val document = PdfDocument()
    try {
      val composer = PdfComposer(document, dashboard, generatedAtMillis)
      composer.render()
      document.writeTo(outputStream)
    } finally {
      document.close()
    }
  }

  private class PdfComposer(
    private val document: PdfDocument,
    private val dashboard: FundTrackUiState.Dashboard,
    private val generatedAtMillis: Long,
  ) {
    private var pageNumber = 0
    private var currentPage: PdfDocument.Page? = null
    private var canvas: Canvas = Canvas()
    private var cursorY = MARGIN_TOP

    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
    private val entryDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

    private val titlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }

    private val subtitlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 255, 255, 255)
        textSize = 10.5f
      }

    private val sectionTitlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#131A17")
        textSize = 13.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }

    private val bodyPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E2623")
        textSize = 10.5f
      }

    private val bodyBoldPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#131A17")
        textSize = 10.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }

    private val mutedPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5A6660")
        textSize = 9.5f
      }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.parseColor("#E0E5DF")
      }

    fun render() {
      startNewPage()
      drawHeroHeader()
      cursorY += 16f
      drawSummaryCards()
      cursorY += 18f

      if (dashboard.clientEarnings.isNotEmpty()) {
        drawClientEarningsSection()
        cursorY += 18f
      }

      drawEntryCategorySection(
        title = "Investment",
        total = dashboard.totalInvestment,
        markerColor = Color.parseColor("#C43B3B"),
        entries = dashboard.investmentEntries,
      )
      cursorY += 16f

      drawEntryCategorySection(
        title = "Revenue",
        total = dashboard.totalRevenue,
        markerColor = dashboard.themePalette.accent.toArgb(),
        entries = dashboard.revenueEntries,
      )
      cursorY += 16f

      drawEntryCategorySection(
        title = "Money due",
        total = dashboard.totalDue,
        markerColor = Color.parseColor("#B36B00"),
        entries = dashboard.dueEntries,
      )

      finishCurrentPage()
    }

    private fun startNewPage() {
      finishCurrentPage()
      pageNumber += 1
      val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
      val page = document.startPage(pageInfo)
      currentPage = page
      canvas = page.canvas
      cursorY = MARGIN_TOP
    }

    private fun finishCurrentPage() {
      val page = currentPage ?: return
      // Draw subtle page footer
      val footerY = PAGE_HEIGHT - 22f
      canvas.drawLine(MARGIN_H, footerY - 12f, PAGE_WIDTH - MARGIN_H, footerY - 12f, strokePaint)
      canvas.drawText(
        "Generated offline by LeuFunds • ${dateTimeFormat.format(Date(generatedAtMillis))}",
        MARGIN_H,
        footerY,
        mutedPaint,
      )
      val pageLabel = "Page $pageNumber"
      val labelW = mutedPaint.measureText(pageLabel)
      canvas.drawText(pageLabel, PAGE_WIDTH - MARGIN_H - labelW, footerY, mutedPaint)

      document.finishPage(page)
      currentPage = null
    }

    private fun ensureSpace(requiredHeight: Float) {
      if (cursorY + requiredHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
        startNewPage()
      }
    }

    private fun drawHeroHeader() {
      val headerHeight = 96f
      ensureSpace(headerHeight)

      val rect = RectF(MARGIN_H, cursorY, PAGE_WIDTH - MARGIN_H, cursorY + headerHeight)
      fillPaint.shader =
        LinearGradient(
          rect.left,
          rect.top,
          rect.right,
          rect.bottom,
          dashboard.themePalette.gradientStart.toArgb(),
          dashboard.themePalette.gradientEnd.toArgb(),
          Shader.TileMode.CLAMP,
        )
      canvas.drawRoundRect(rect, 16f, 16f, fillPaint)
      fillPaint.shader = null

      val padX = rect.left + 20f
      canvas.drawText("LeuFunds Financial Summary", padX, rect.top + 26f, subtitlePaint)

      val clippedBizName =
        TextUtils.ellipsize(
            dashboard.selectedBusiness.name,
            titlePaint,
            CONTENT_WIDTH - 190f,
            TextUtils.TruncateAt.END,
          )
          .toString()
      canvas.drawText(clippedBizName, padX, rect.top + 52f, titlePaint)

      canvas.drawText(
        "Generated ${dateTimeFormat.format(Date(generatedAtMillis))}",
        padX,
        rect.top + 74f,
        subtitlePaint,
      )

      // Right side recovery status badge
      val statusText =
        when {
          dashboard.totalInvestment <= 0.0 && dashboard.totalRevenue <= 0.0 -> "No activity yet"
          dashboard.totalInvestment <= 0.0 && dashboard.totalRevenue > 0.0 -> "Pure profit"
          dashboard.totalRevenue < dashboard.totalInvestment -> {
            val pct =
              ((dashboard.totalRevenue / dashboard.totalInvestment) * 100.0)
                .roundToInt()
                .coerceIn(0, 99)
            "$pct% recovered"
          }
          else -> {
            val mult =
              CurrencyFormatter.formatMultiplier(
                dashboard.totalRevenue / dashboard.totalInvestment
              )
            "${mult}x earned back"
          }
        }
      val badgeTextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.WHITE
          textSize = 10.5f
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
      val badgeTextWidth = badgeTextPaint.measureText(statusText)
      val badgeWidth = badgeTextWidth + 24f
      val badgeRect =
        RectF(rect.right - 20f - badgeWidth, rect.top + 32f, rect.right - 20f, rect.top + 60f)
      fillPaint.color = Color.argb(46, 255, 255, 255)
      canvas.drawRoundRect(badgeRect, 14f, 14f, fillPaint)
      canvas.drawText(statusText, badgeRect.left + 12f, badgeRect.top + 18f, badgeTextPaint)

      cursorY += headerHeight
    }

    private fun drawSummaryCards() {
      val cardHeight = 64f
      ensureSpace(cardHeight)

      val gap = 10f
      val cardWidth = (CONTENT_WIDTH - gap * 3f) / 4f
      val isNegativeProfit = dashboard.netProfit < 0.0

      val cards =
        listOf(
          Triple(
            if (isNegativeProfit) "Still to recover" else "Net profit",
            CurrencyFormatter.formatInr(
              if (isNegativeProfit) abs(dashboard.netProfit) else dashboard.netProfit
            ),
            if (isNegativeProfit) Pair(Color.parseColor("#FDECEC"), Color.parseColor("#C43B3B"))
            else Pair(Color.parseColor("#E7F6EE"), Color.parseColor("#157A4A")),
          ),
          Triple(
            "Invested",
            CurrencyFormatter.formatInr(dashboard.totalInvestment),
            Pair(Color.parseColor("#FDECEC"), Color.parseColor("#C43B3B")),
          ),
          Triple(
            "Earned",
            CurrencyFormatter.formatInr(dashboard.totalRevenue),
            Pair(Color.parseColor("#E7F6EE"), Color.parseColor("#157A4A")),
          ),
          Triple(
            "Still due",
            CurrencyFormatter.formatInr(dashboard.totalDue),
            Pair(Color.parseColor("#FEF4E1"), Color.parseColor("#B36B00")),
          ),
        )

      cards.forEachIndexed { index, (label, value, colors) ->
        val left = MARGIN_H + index * (cardWidth + gap)
        val rect = RectF(left, cursorY, left + cardWidth, cursorY + cardHeight)

        fillPaint.color = colors.first
        canvas.drawRoundRect(rect, 12f, 12f, fillPaint)

        val labelPaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.second
            textSize = 9.5f
          }
        val valuePaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.second
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          }

        canvas.drawText(label, left + 10f, cursorY + 22f, labelPaint)
        val clippedVal =
          TextUtils.ellipsize(value, valuePaint, cardWidth - 20f, TextUtils.TruncateAt.END)
            .toString()
        canvas.drawText(clippedVal, left + 10f, cursorY + 45f, valuePaint)
      }

      cursorY += cardHeight
    }

    private fun drawClientEarningsSection() {
      ensureSpace(44f)
      canvas.drawText("Earnings by client", MARGIN_H, cursorY + 14f, sectionTitlePaint)
      cursorY += 24f

      val accentInt = dashboard.themePalette.accent.toArgb()
      val trackColor = Color.parseColor("#ECEFE9")

      for (client in dashboard.clientEarnings) {
        ensureSpace(28f)
        val clippedClient =
          TextUtils.ellipsize(
              client.displayName,
              bodyPaint,
              CONTENT_WIDTH - 110f,
              TextUtils.TruncateAt.END,
            )
            .toString()
        canvas.drawText(clippedClient, MARGIN_H, cursorY + 11f, bodyPaint)

        val amountStr = CurrencyFormatter.formatInr(client.totalAmount)
        val amountW = bodyBoldPaint.measureText(amountStr)
        canvas.drawText(amountStr, PAGE_WIDTH - MARGIN_H - amountW, cursorY + 11f, bodyBoldPaint)

        val barTop = cursorY + 16f
        val barRect = RectF(MARGIN_H, barTop, PAGE_WIDTH - MARGIN_H, barTop + 6f)
        fillPaint.color = trackColor
        canvas.drawRoundRect(barRect, 3f, 3f, fillPaint)

        val fillWidth = (CONTENT_WIDTH * client.fractionOfMax.coerceAtLeast(0.03f))
        val activeRect = RectF(MARGIN_H, barTop, MARGIN_H + fillWidth, barTop + 6f)
        fillPaint.color = accentInt
        canvas.drawRoundRect(activeRect, 3f, 3f, fillPaint)

        cursorY += 28f
      }
    }

    private fun drawEntryCategorySection(
      title: String,
      total: Double,
      markerColor: Int,
      entries: List<EntryEntity>,
    ) {
      ensureSpace(48f)

      // Colored square marker + section title + right-aligned total
      val markerRect = RectF(MARGIN_H, cursorY + 3f, MARGIN_H + 10f, cursorY + 13f)
      fillPaint.color = markerColor
      canvas.drawRoundRect(markerRect, 3f, 3f, fillPaint)

      canvas.drawText(title, MARGIN_H + 18f, cursorY + 13f, sectionTitlePaint)

      val totalStr = CurrencyFormatter.formatInr(total)
      val totalW = sectionTitlePaint.measureText(totalStr)
      canvas.drawText(totalStr, PAGE_WIDTH - MARGIN_H - totalW, cursorY + 13f, sectionTitlePaint)
      cursorY += 22f

      if (entries.isEmpty()) {
        canvas.drawText("Nothing recorded yet.", MARGIN_H, cursorY + 12f, mutedPaint)
        cursorY += 20f
        return
      }

      drawTableHeader()

      val ordered = entries.sortedWith(compareByDescending<EntryEntity> { it.createdAt }.thenByDescending { it.id })
      ordered.forEachIndexed { idx, entry ->
        if (cursorY + 22f > PAGE_HEIGHT - MARGIN_BOTTOM) {
          startNewPage()
          canvas.drawText("$title (continued)", MARGIN_H, cursorY + 13f, sectionTitlePaint)
          cursorY += 22f
          drawTableHeader()
        }

        val rowRect = RectF(MARGIN_H, cursorY, PAGE_WIDTH - MARGIN_H, cursorY + 22f)
        if (idx % 2 == 0) {
          fillPaint.color = Color.parseColor("#F7F8F7")
          canvas.drawRect(rowRect, fillPaint)
        }

        val nameMaxW = CONTENT_WIDTH - 190f
        val clippedName =
          TextUtils.ellipsize(entry.name, bodyPaint, nameMaxW, TextUtils.TruncateAt.END).toString()
        canvas.drawText(clippedName, MARGIN_H + 8f, cursorY + 15f, bodyPaint)

        val dateStr = entryDateFormat.format(Date(entry.createdAt))
        canvas.drawText(dateStr, PAGE_WIDTH - MARGIN_H - 165f, cursorY + 15f, mutedPaint)

        val amtStr = CurrencyFormatter.formatInr(entry.amount)
        val amtW = bodyBoldPaint.measureText(amtStr)
        canvas.drawText(amtStr, PAGE_WIDTH - MARGIN_H - 8f - amtW, cursorY + 15f, bodyBoldPaint)

        cursorY += 22f
      }
    }

    private fun drawTableHeader() {
      val headerRect = RectF(MARGIN_H, cursorY, PAGE_WIDTH - MARGIN_H, cursorY + 20f)
      fillPaint.color = Color.parseColor("#ECEFE9")
      canvas.drawRoundRect(headerRect, 6f, 6f, fillPaint)

      canvas.drawText("Name", MARGIN_H + 8f, cursorY + 14f, mutedPaint)
      canvas.drawText("Date", PAGE_WIDTH - MARGIN_H - 165f, cursorY + 14f, mutedPaint)
      val amountHeader = "Amount"
      val headerW = mutedPaint.measureText(amountHeader)
      canvas.drawText(amountHeader, PAGE_WIDTH - MARGIN_H - 8f - headerW, cursorY + 14f, mutedPaint)
      cursorY += 22f
    }
  }
}
