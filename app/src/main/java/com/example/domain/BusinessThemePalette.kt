package com.example.domain

import androidx.compose.ui.graphics.Color

enum class BusinessThemePalette(
  val id: Int,
  val displayName: String,
  val gradientStart: Color,
  val gradientEnd: Color,
  val accent: Color,
  val accentLightTint: Color,
) {
  GREEN(
    id = 0,
    displayName = "Green",
    gradientStart = Color(0xFF14594A),
    gradientEnd = Color(0xFF0A2A22),
    accent = Color(0xFF14594A),
    accentLightTint = Color(0xFF34D399),
  ),
  BLUE(
    id = 1,
    displayName = "Blue",
    gradientStart = Color(0xFF1F5F9F),
    gradientEnd = Color(0xFF0D2440),
    accent = Color(0xFF1F5F9F),
    accentLightTint = Color(0xFF60A5FA),
  ),
  PURPLE(
    id = 2,
    displayName = "Purple",
    gradientStart = Color(0xFF6A45A6),
    gradientEnd = Color(0xFF2A1A45),
    accent = Color(0xFF6A45A6),
    accentLightTint = Color(0xFFA78BFA),
  ),
  ORANGE(
    id = 3,
    displayName = "Orange",
    gradientStart = Color(0xFFB9561A),
    gradientEnd = Color(0xFF3D1A07),
    accent = Color(0xFFB9561A),
    accentLightTint = Color(0xFFFB923C),
  ),
  ROSE(
    id = 4,
    displayName = "Rose",
    gradientStart = Color(0xFFB53660),
    gradientEnd = Color(0xFF40101F),
    accent = Color(0xFFB53660),
    accentLightTint = Color(0xFFF472B6),
  ),
  SLATE(
    id = 5,
    displayName = "Slate",
    gradientStart = Color(0xFF4B5F6E),
    gradientEnd = Color(0xFF151D23),
    accent = Color(0xFF4B5F6E),
    accentLightTint = Color(0xFF94A3B8),
  );

  companion object {
    fun fromId(themeId: Int): BusinessThemePalette =
      entries.firstOrNull { it.id == themeId } ?: GREEN
  }
}
