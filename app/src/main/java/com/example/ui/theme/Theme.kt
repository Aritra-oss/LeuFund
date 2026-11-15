package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class FundTrackSemanticColors(
  val softRedContainer: Color,
  val softRedContent: Color,
  val softGreenContainer: Color,
  val softGreenContent: Color,
  val softAmberContainer: Color,
  val softAmberContent: Color,
  val softAmberBorder: Color,
  val cardBorder: Color,
  val selectedTabBg: Color,
  val selectedTabText: Color,
  val selectedTabBorder: Color,
  val canvasTop: Color,
  val canvasBottom: Color,
  val glassFillStart: Color,
  val glassFillEnd: Color,
  val glassElevatedStart: Color,
  val glassElevatedEnd: Color,
  val glassRimStart: Color,
  val glassRimEnd: Color,
  val glassShadow: Color,
)

private val LightSemanticColors =
  FundTrackSemanticColors(
    softRedContainer = SoftRedBgLight,
    softRedContent = SoftRedFgLight,
    softGreenContainer = SoftGreenBgLight,
    softGreenContent = SoftGreenFgLight,
    softAmberContainer = SoftAmberBgLight,
    softAmberContent = SoftAmberFgLight,
    softAmberBorder = SoftAmberBorderLight,
    cardBorder = LightGlassRimStart,
    selectedTabBg = SelectedTabDarkFill,
    selectedTabText = Color.White,
    selectedTabBorder = SelectedTabDarkFill,
    canvasTop = LightCanvasTop,
    canvasBottom = LightCanvasBottom,
    glassFillStart = LightGlassFillStart,
    glassFillEnd = LightGlassFillEnd,
    glassElevatedStart = LightGlassElevatedStart,
    glassElevatedEnd = LightGlassElevatedEnd,
    glassRimStart = LightGlassRimStart,
    glassRimEnd = LightGlassRimEnd,
    glassShadow = LightGlassShadow,
  )

private val DarkSemanticColors =
  FundTrackSemanticColors(
    softRedContainer = SoftRedBgDark,
    softRedContent = SoftRedFgDark,
    softGreenContainer = SoftGreenBgDark,
    softGreenContent = SoftGreenFgDark,
    softAmberContainer = SoftAmberBgDark,
    softAmberContent = SoftAmberFgDark,
    softAmberBorder = SoftAmberBorderDark,
    cardBorder = DarkGlassRimStart,
    selectedTabBg = SelectedTabDarkFill,
    selectedTabText = Color.White,
    selectedTabBorder = SelectedTabDarkBorder,
    canvasTop = DarkCanvasTop,
    canvasBottom = DarkCanvasBottom,
    glassFillStart = DarkGlassFillStart,
    glassFillEnd = DarkGlassFillEnd,
    glassElevatedStart = DarkGlassElevatedStart,
    glassElevatedEnd = DarkGlassElevatedEnd,
    glassRimStart = DarkGlassRimStart,
    glassRimEnd = DarkGlassRimEnd,
    glassShadow = DarkGlassShadow,
  )

val FundTrackGlassShapes =
  Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
  )

val LocalFundTrackColors = staticCompositionLocalOf { LightSemanticColors }

@Composable
fun FundTrackTheme(
  accentColor: Color = Color(0xFF14594A),
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme =
    if (darkTheme) {
      darkColorScheme(
        primary = accentColor,
        onPrimary = Color.White,
        background = DarkBackground,
        onBackground = DarkOnSurface,
        surface = Color(0xFF111916),
        onSurface = DarkOnSurface,
        surfaceVariant = Color(0xFF1B2622),
        onSurfaceVariant = DarkOnSurfaceVariant,
        outline = DarkOutline,
        error = SoftRedFgDark,
        onError = Color.Black,
      )
    } else {
      lightColorScheme(
        primary = accentColor,
        onPrimary = Color.White,
        background = LightBackground,
        onBackground = LightOnSurface,
        surface = Color(0xFFF9FCFA),
        onSurface = LightOnSurface,
        surfaceVariant = Color(0xFFE7EFEA),
        onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline,
        error = SoftRedFgLight,
        onError = Color.White,
      )
    }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

  CompositionLocalProvider(LocalFundTrackColors provides semanticColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = FundTrackTypography,
      shapes = FundTrackGlassShapes,
      content = content,
    )
  }
}
