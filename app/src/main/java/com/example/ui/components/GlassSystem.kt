package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.BusinessThemePalette
import com.example.ui.theme.LocalFundTrackColors

/**
 * Full-bleed blurred ambient backdrop rendered behind the translucent glass UI.
 * Uses soft multi-stop radial orbs plus optical hardware blur on Android 12+.
 */
@Composable
fun GlassAmbientBackdrop(
  themePalette: BusinessThemePalette,
  isDark: Boolean,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  val baseGradient =
    remember(semanticColors.canvasTop, semanticColors.canvasBottom) {
      Brush.verticalGradient(colors = listOf(semanticColors.canvasTop, semanticColors.canvasBottom))
    }

  val primaryOrbColor =
    if (isDark) themePalette.accentLightTint.copy(alpha = 0.28f)
    else themePalette.accentLightTint.copy(alpha = 0.34f)
  val secondaryOrbColor =
    if (isDark) themePalette.gradientStart.copy(alpha = 0.42f)
    else themePalette.accent.copy(alpha = 0.22f)
  val tertiaryOrbColor =
    if (isDark) Color(0xFF0E7490).copy(alpha = 0.22f)
    else Color(0xFF38BDF8).copy(alpha = 0.16f)

  val blurModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      Modifier.blur(radius = 52.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    } else {
      Modifier
    }

  Box(modifier = modifier.fillMaxSize().background(baseGradient)) {
    Box(
      modifier =
        Modifier.fillMaxSize().then(blurModifier).drawBehind {
          val w = size.width
          val h = size.height

          // Top-right luminous theme halo
          val topRightCenter = Offset(w * 0.88f, h * 0.12f)
          drawCircle(
            brush =
              Brush.radialGradient(
                colors = listOf(primaryOrbColor, primaryOrbColor.copy(alpha = 0.08f), Color.Transparent),
                center = topRightCenter,
                radius = w * 0.72f,
              ),
            center = topRightCenter,
            radius = w * 0.72f,
          )

          // Mid-left rich depth orb
          val midLeftCenter = Offset(w * 0.08f, h * 0.44f)
          drawCircle(
            brush =
              Brush.radialGradient(
                colors = listOf(secondaryOrbColor, secondaryOrbColor.copy(alpha = 0.07f), Color.Transparent),
                center = midLeftCenter,
                radius = w * 0.68f,
              ),
            center = midLeftCenter,
            radius = w * 0.68f,
          )

          // Lower-right cool cyan-emerald ambient glow
          val bottomRightCenter = Offset(w * 0.82f, h * 0.84f)
          drawCircle(
            brush =
              Brush.radialGradient(
                colors = listOf(tertiaryOrbColor, tertiaryOrbColor.copy(alpha = 0.05f), Color.Transparent),
                center = bottomRightCenter,
                radius = w * 0.65f,
              ),
            center = bottomRightCenter,
            radius = w * 0.65f,
          )
        }
    )
  }
}

/**
 * Reusable frosted glass container with translucent diagonal sheen, prismatic hairline rim border,
 * and subtle elevation shadow.
 */
@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(26.dp),
  elevation: Dp = 8.dp,
  elevated: Boolean = false,
  customFillBrush: Brush? = null,
  customRimBrush: Brush? = null,
  shadowTint: Color? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  val semanticColors = LocalFundTrackColors.current
  val fillStart = if (elevated) semanticColors.glassElevatedStart else semanticColors.glassFillStart
  val fillEnd = if (elevated) semanticColors.glassElevatedEnd else semanticColors.glassFillEnd

  val fillBrush =
    customFillBrush
      ?: remember(fillStart, fillEnd) {
        Brush.linearGradient(colors = listOf(fillStart, fillEnd))
      }
  val rimBrush =
    customRimBrush
      ?: remember(semanticColors.glassRimStart, semanticColors.glassRimEnd) {
        Brush.linearGradient(colors = listOf(semanticColors.glassRimStart, semanticColors.glassRimEnd))
      }
  val resolvedShadow = shadowTint ?: semanticColors.glassShadow

  Box(
    modifier =
      modifier
        .shadow(
          elevation = elevation,
          shape = shape,
          clip = false,
          ambientColor = resolvedShadow,
          spotColor = resolvedShadow,
        )
        .clip(shape)
        .background(fillBrush)
        .border(width = 1.dp, brush = rimBrush, shape = shape),
    content = content,
  )
}
