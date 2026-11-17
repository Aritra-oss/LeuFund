package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.BusinessThemePalette
import com.example.ui.theme.LocalFundTrackColors

/**
 * Tracks intro-animation signatures so gauge/bar sweeps play once per business or data change.
 */
@Stable
class DashboardAnimMemory {
  private val playedSignatures = HashMap<String, Any>()

  fun consumeShouldAnimate(key: String, signature: Any): Boolean {
    val previous = playedSignatures[key]
    if (previous == signature) return false
    playedSignatures[key] = signature
    return true
  }
}

/**
 * Serene static classic backdrop compiled once in `drawWithCache` for zero per-frame invalidation.
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

  val topGlowColor =
    if (isDark) themePalette.accentLightTint.copy(alpha = 0.16f)
    else themePalette.accentLightTint.copy(alpha = 0.22f)
  val bottomWashColor =
    if (isDark) themePalette.gradientStart.copy(alpha = 0.22f)
    else themePalette.accent.copy(alpha = 0.12f)

  Box(modifier = modifier.fillMaxSize().background(baseGradient)) {
    Spacer(
      modifier =
        Modifier.fillMaxSize().drawWithCache {
          val w = size.width
          val h = size.height

          val topRightCenter = Offset(w * 0.84f, h * 0.12f)
          val r1 = w * 0.85f
          val brush1 =
            Brush.radialGradient(
              colorStops =
                arrayOf(
                  0.0f to topGlowColor,
                  0.45f to topGlowColor.copy(alpha = topGlowColor.alpha * 0.40f),
                  1.0f to Color.Transparent,
                ),
              center = topRightCenter,
              radius = r1,
            )

          val lowerLeftCenter = Offset(w * 0.14f, h * 0.62f)
          val r2 = w * 0.80f
          val brush2 =
            Brush.radialGradient(
              colorStops =
                arrayOf(
                  0.0f to bottomWashColor,
                  0.50f to bottomWashColor.copy(alpha = bottomWashColor.alpha * 0.35f),
                  1.0f to Color.Transparent,
                ),
              center = lowerLeftCenter,
              radius = r2,
            )

          onDrawBehind {
            drawCircle(brush = brush1, center = topRightCenter, radius = r1)
            drawCircle(brush = brush2, center = lowerLeftCenter, radius = r2)
          }
        }
    )
  }
}

/**
 * Classic frosted glass container rendered in a single lightweight clip + background + border pass.
 */
@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(22.dp),
  @Suppress("UNUSED_PARAMETER") elevation: Dp = 8.dp,
  elevated: Boolean = false,
  @Suppress("UNUSED_PARAMETER") futuristicReticles: Boolean = false,
  customFillBrush: Brush? = null,
  customRimBrush: Brush? = null,
  @Suppress("UNUSED_PARAMETER") shadowTint: Color? = null,
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

  Box(
    modifier =
      modifier
        .clip(shape)
        .background(fillBrush)
        .border(width = 1.dp, brush = rimBrush, shape = shape),
    content = content,
  )
}

@Suppress("UNUSED_PARAMETER")
fun Modifier.futuristicPressScale(
  interactionSource: InteractionSource,
  pressedScale: Float = 1f,
): Modifier = this

fun Modifier.futuristicClickable(
  role: Role? = null,
  @Suppress("UNUSED_PARAMETER") pressedScale: Float = 1f,
  onClick: () -> Unit,
): Modifier = this.clickable(role = role, onClick = onClick)
