package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.BusinessThemePalette
import com.example.ui.theme.LocalFundTrackColors

/**
 * Tracks intro-animation signatures above `LazyColumn` so gauge/bar sweeps play on initial open,
 * business switches, or data updates—and render settled immediately with zero coroutine restarts
 * when scrolling back and forth across sections.
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
 * Serene, static classic frosted-glass backdrop compiled once in `drawWithCache` so scrolling
 * stays locked at full refresh rate with zero per-frame background ticks or grid clutter.
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
 * Classic frosted glass container with soft translucent sheen and crisp 1.dp rim border.
 */
@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(22.dp),
  elevation: Dp = 8.dp,
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

  val topSheenColor = Color.White.copy(alpha = if (elevated) 0.22f else 0.14f)

  Box(
    modifier =
      modifier
        .clip(shape)
        .background(fillBrush)
        .border(width = 1.dp, brush = rimBrush, shape = shape)
        .drawWithCache {
          val strokeW = 1.dp.toPx()
          val catchlightBrush =
            Brush.horizontalGradient(
              colors = listOf(Color.Transparent, topSheenColor, Color.Transparent),
              startX = size.width * 0.08f,
              endX = size.width * 0.92f,
            )
          onDrawWithContent {
            drawContent()
            drawLine(
              brush = catchlightBrush,
              start = Offset(size.width * 0.10f, strokeW),
              end = Offset(size.width * 0.90f, strokeW),
              strokeWidth = strokeW,
            )
          }
        },
    content = content,
  )
}

/**
 * Subtle classic press scale evaluated cleanly via `graphicsLayer` with zero `Modifier.composed`.
 */
@Composable
fun Modifier.futuristicPressScale(
  interactionSource: InteractionSource,
  pressedScale: Float = 0.985f,
): Modifier {
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (isPressed) pressedScale else 1f,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioNoBouncy,
          stiffness = Spring.StiffnessMedium,
        ),
      label = "classic_press_scale",
    )
  return this.graphicsLayer {
    scaleX = scale
    scaleY = scale
  }
}

/**
 * Combines standard Material ripple indication with subtle classic press scaling.
 */
@Composable
fun Modifier.futuristicClickable(
  role: Role? = null,
  pressedScale: Float = 0.985f,
  onClick: () -> Unit,
): Modifier {
  val interactionSource = remember { MutableInteractionSource() }
  val indication = LocalIndication.current
  return this.futuristicPressScale(
      interactionSource = interactionSource,
      pressedScale = pressedScale,
    )
    .clickable(
      interactionSource = interactionSource,
      indication = indication,
      role = role,
      onClick = onClick,
    )
}
