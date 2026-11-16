package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.BusinessThemePalette
import com.example.ui.theme.LocalFundTrackColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-bleed classic-futuristic ambient backdrop behind the translucent glass UI.
 * Eliminates expensive RenderEffect offscreen blur passes that stall scrolling, replacing them
 * with GPU-cached 5-stop radial plasma halos, gentle orbital drift, and precision HUD geometry.
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
    if (isDark) themePalette.accentLightTint.copy(alpha = 0.24f)
    else themePalette.accentLightTint.copy(alpha = 0.30f)
  val secondaryOrbColor =
    if (isDark) themePalette.gradientStart.copy(alpha = 0.36f)
    else themePalette.accent.copy(alpha = 0.19f)
  val tertiaryOrbColor =
    if (isDark) Color(0xFF0E7490).copy(alpha = 0.20f)
    else Color(0xFF38BDF8).copy(alpha = 0.14f)
  val gridLineColor =
    if (isDark) themePalette.accentLightTint.copy(alpha = 0.045f)
    else themePalette.accent.copy(alpha = 0.045f)
  val ringColor =
    if (isDark) Color.White.copy(alpha = 0.055f)
    else themePalette.accent.copy(alpha = 0.065f)

  val infiniteTransition = rememberInfiniteTransition(label = "backdrop_drift")
  val driftPhase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = (Math.PI * 2.0).toFloat(),
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 18_000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "backdrop_orbital_phase",
    )

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(baseGradient)
        .graphicsLayer()
        .drawBehind {
          val w = size.width
          val h = size.height
          val dx = cos(driftPhase) * w * 0.045f
          val dy = sin(driftPhase) * h * 0.03f

          // Top-right luminous theme plasma halo (5-stop soft radial gradient — zero RenderEffect stall)
          val topRightCenter = Offset(w * 0.86f + dx, h * 0.13f + dy)
          val r1 = w * 0.85f
          drawCircle(
            brush =
              Brush.radialGradient(
                colorStops =
                  arrayOf(
                    0.0f to primaryOrbColor,
                    0.35f to primaryOrbColor.copy(alpha = primaryOrbColor.alpha * 0.52f),
                    0.70f to primaryOrbColor.copy(alpha = primaryOrbColor.alpha * 0.14f),
                    1.0f to Color.Transparent,
                  ),
                center = topRightCenter,
                radius = r1,
              ),
            center = topRightCenter,
            radius = r1,
          )

          // Mid-left rich depth orb
          val midLeftCenter = Offset(w * 0.10f - dx * 0.8f, h * 0.44f - dy)
          val r2 = w * 0.78f
          drawCircle(
            brush =
              Brush.radialGradient(
                colorStops =
                  arrayOf(
                    0.0f to secondaryOrbColor,
                    0.40f to secondaryOrbColor.copy(alpha = secondaryOrbColor.alpha * 0.45f),
                    0.75f to secondaryOrbColor.copy(alpha = secondaryOrbColor.alpha * 0.10f),
                    1.0f to Color.Transparent,
                  ),
                center = midLeftCenter,
                radius = r2,
              ),
            center = midLeftCenter,
            radius = r2,
          )

          // Lower-right cool cyan ambient glow
          val bottomRightCenter = Offset(w * 0.80f + dx * 0.6f, h * 0.84f - dy * 0.7f)
          val r3 = w * 0.72f
          drawCircle(
            brush =
              Brush.radialGradient(
                colorStops =
                  arrayOf(
                    0.0f to tertiaryOrbColor,
                    0.45f to tertiaryOrbColor.copy(alpha = tertiaryOrbColor.alpha * 0.40f),
                    1.0f to Color.Transparent,
                  ),
                center = bottomRightCenter,
                radius = r3,
              ),
            center = bottomRightCenter,
            radius = r3,
          )

          // Classic-futuristic precision architectural grid (subtle hairline matrix)
          val step = 44.dp.toPx()
          val strokeW = 0.75.dp.toPx()
          var x = step * 0.5f
          while (x < w) {
            drawLine(gridLineColor, Offset(x, 0f), Offset(x, h), strokeWidth = strokeW)
            x += step
          }
          var y = step * 0.5f
          while (y < h) {
            drawLine(gridLineColor, Offset(0f, y), Offset(w, y), strokeWidth = strokeW)
            y += step
          }

          // Subtle concentric orbital chronometer rings in upper sky
          val orbitCenter = Offset(w * 0.92f, h * 0.08f)
          drawCircle(
            color = ringColor,
            radius = w * 0.46f,
            center = orbitCenter,
            style = Stroke(width = 1.dp.toPx()),
          )
          drawCircle(
            color = ringColor,
            radius = w * 0.68f,
            center = orbitCenter,
            style = Stroke(width = 0.8.dp.toPx()),
          )
        }
  )
}

/**
 * Reusable frosted glass container with translucent diagonal sheen, prismatic hairline rim border,
 * and subtle corner HUD accents — zero expensive dynamic shadow geometry passes during scroll.
 */
@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(26.dp),
  elevation: Dp = 8.dp,
  elevated: Boolean = false,
  futuristicReticles: Boolean = true,
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
  val reticleColor =
    remember(shadowTint, semanticColors.glassRimStart) {
      (shadowTint ?: semanticColors.glassRimStart).copy(alpha = 0.42f)
    }

  val topSheenColor = Color.White.copy(alpha = if (elevated) 0.24f else 0.15f)

  Box(
    modifier =
      modifier
        .clip(shape)
        .background(fillBrush)
        .border(width = 1.dp, brush = rimBrush, shape = shape)
        .drawWithCache {
          val cornerInset = 14.dp.toPx()
          val bracketLen = 10.dp.toPx()
          val strokeW = 1.25.dp.toPx()
          val catchlightBrush =
            Brush.horizontalGradient(
              colors = listOf(Color.Transparent, topSheenColor, Color.Transparent),
              startX = size.width * 0.08f,
              endX = size.width * 0.92f,
            )
          onDrawWithContent {
            drawContent()
            // Specular top-edge prism catchlight
            drawLine(
              brush = catchlightBrush,
              start = Offset(size.width * 0.10f, strokeW),
              end = Offset(size.width * 0.90f, strokeW),
              strokeWidth = strokeW,
            )
            if (futuristicReticles && size.width > cornerInset * 4f && size.height > cornerInset * 4f) {
              // Subtle top-left & bottom-right micro HUD corner reticles
              drawLine(
                color = reticleColor,
                start = Offset(cornerInset, cornerInset),
                end = Offset(cornerInset + bracketLen, cornerInset),
                strokeWidth = strokeW,
              )
              drawLine(
                color = reticleColor,
                start = Offset(cornerInset, cornerInset),
                end = Offset(cornerInset, cornerInset + bracketLen),
                strokeWidth = strokeW,
              )
              val rx = size.width - cornerInset
              val by = size.height - cornerInset
              drawLine(
                color = reticleColor,
                start = Offset(rx, by),
                end = Offset(rx - bracketLen, by),
                strokeWidth = strokeW,
              )
              drawLine(
                color = reticleColor,
                start = Offset(rx, by),
                end = Offset(rx, by - bracketLen),
                strokeWidth = strokeW,
              )
            }
          }
        },
    content = content,
  )
}

/**
 * Pure render-layer spring press bounce (`scaleX`/`scaleY` inside `graphicsLayer`) so tapping
 * pills and cards feels responsive with zero layout re-measurement.
 */
fun Modifier.futuristicPressScale(
  interactionSource: InteractionSource,
  pressedScale: Float = 0.965f,
): Modifier = composed {
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (isPressed) pressedScale else 1f,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMediumLow,
        ),
      label = "futuristic_press_scale",
    )
  this.graphicsLayer {
    scaleX = scale
    scaleY = scale
  }
}

/**
 * Combines standard Material ripple indication with pure render-layer spring press scaling.
 */
fun Modifier.futuristicClickable(
  role: Role? = null,
  pressedScale: Float = 0.965f,
  onClick: () -> Unit,
): Modifier = composed {
  val interactionSource = remember { MutableInteractionSource() }
  val indication = LocalIndication.current
  this.futuristicPressScale(interactionSource = interactionSource, pressedScale = pressedScale)
    .clickable(
      interactionSource = interactionSource,
      indication = indication,
      role = role,
      onClick = onClick,
    )
}
