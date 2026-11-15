package com.example.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {

  /**
   * Formats an amount in Indian Rupees with Indian digit grouping (e.g., ₹1,50,000).
   * Preserves up to 2 decimal places only when fractional paise exist.
   */
  fun formatInr(amount: Double): String {
    if (amount.isNaN() || amount.isInfinite()) return "₹0"

    val isNegative = amount < 0.0
    val absDecimal = BigDecimal.valueOf(abs(amount)).setScale(2, RoundingMode.HALF_UP)

    // Normalize -0.00 to 0
    if (absDecimal.compareTo(BigDecimal.ZERO) == 0) {
      return "₹0"
    }

    val stripped = absDecimal.stripTrailingZeros()
    val plain =
      if (stripped.scale() <= 0) {
        absDecimal.toBigInteger().toString()
      } else {
        absDecimal.toPlainString()
      }

    val parts = plain.split(".")
    val intPart = parts[0]
    val fracPart = parts.getOrNull(1)

    val groupedInt = groupIndianDigits(intPart)
    val prefix = if (isNegative) "-₹" else "₹"
    return if (fracPart != null) {
      "$prefix$groupedInt.$fracPart"
    } else {
      "$prefix$groupedInt"
    }
  }

  private fun groupIndianDigits(digits: String): String {
    if (digits.length <= 3) return digits
    val lastThree = digits.takeLast(3)
    val remaining = digits.dropLast(3)
    val sb = StringBuilder()
    var index = remaining.length
    while (index > 0) {
      val start = (index - 2).coerceAtLeast(0)
      if (sb.isNotEmpty()) sb.insert(0, ',')
      sb.insert(0, remaining.substring(start, index))
      index = start
    }
    return "$sb,$lastThree"
  }

  fun formatMultiplier(ratio: Double): String {
    return String.format(Locale.US, "%.1f", ratio)
  }

  fun extractInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
      words.isEmpty() -> "?"
      words.size == 1 -> words[0].take(2).uppercase(Locale.getDefault())
      else ->
        (words[0].first().toString() + words[1].first().toString()).uppercase(Locale.getDefault())
    }
  }

  fun parseValidAmount(raw: String): Double? {
    val trimmed = raw.trim().replace(",", "")
    if (trimmed.isEmpty()) return null
    val parsed = trimmed.toDoubleOrNull() ?: return null
    if (parsed.isNaN() || parsed.isInfinite() || parsed < 0.0) return null
    return parsed
  }
}
