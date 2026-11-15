package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
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
    cardBorder = LightOutline,
    selectedTabBg = SelectedTabDarkFill,
    selectedTabText = Color.White,
    selectedTabBorder = SelectedTabDarkFill,
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
    cardBorder = DarkOutline,
    selectedTabBg = SelectedTabDarkFill,
    selectedTabText = Color.White,
    selectedTabBorder = SelectedTabDarkBorder,
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
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
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
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
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
      content = content,
    )
  }
}
