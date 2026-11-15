package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.BusinessThemePalette
import com.example.domain.MpinManager
import com.example.ui.theme.LocalFundTrackColors
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun MpinDotsIndicator(
  enteredLength: Int,
  isError: Boolean,
  shakeSignal: Int,
  accentColor: Color,
  modifier: Modifier = Modifier,
  totalLength: Int = MpinManager.MPIN_LENGTH,
) {
  val semanticColors = LocalFundTrackColors.current
  val shakeOffset = remember { Animatable(0f) }

  LaunchedEffect(shakeSignal) {
    if (shakeSignal > 0) {
      val offsets = listOf(-14f, 14f, -10f, 10f, -5f, 0f)
      for (target in offsets) {
        shakeOffset.animateTo(target, animationSpec = tween(durationMillis = 42))
      }
    }
  }

  val cdText = stringResource(R.string.mpin_dots_cd, enteredLength, totalLength)
  Row(
    modifier =
      modifier
        .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
        .semantics { contentDescription = cdText }
        .testTag("mpin_dots_indicator"),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    repeat(totalLength) { index ->
      val isFilled = index < enteredLength
      val dotSize by
        animateDpAsState(
          targetValue = if (isFilled) 16.dp else 10.dp,
          animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
          label = "mpin_dot_size_$index",
        )
      val boxBorderColor =
        when {
          isError -> semanticColors.softRedContent.copy(alpha = 0.7f)
          isFilled -> accentColor.copy(alpha = 0.85f)
          else -> semanticColors.cardBorder
        }
      val boxFillColor =
        when {
          isError -> semanticColors.softRedContainer
          isFilled -> accentColor.copy(alpha = 0.16f)
          else -> semanticColors.glassFillStart.copy(alpha = 0.72f)
        }
      val dotColor =
        when {
          isError -> semanticColors.softRedContent
          isFilled -> accentColor
          else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
        }

      Box(
        modifier =
          Modifier.size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(boxFillColor)
            .border(width = 1.2.dp, color = boxBorderColor, shape = RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Box(modifier = Modifier.size(dotSize).clip(CircleShape).background(dotColor))
      }
    }
  }
}

@Composable
fun MpinKeypad(
  enabled: Boolean,
  accentColor: Color,
  onDigitClick: (Char) -> Unit,
  onClearClick: () -> Unit,
  onBackspaceClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val rows = remember {
    listOf(
      listOf('1', '2', '3'),
      listOf('4', '5', '6'),
      listOf('7', '8', '9'),
    )
  }

  Column(
    modifier = modifier.fillMaxWidth().widthIn(max = 312.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    rows.forEach { rowDigits ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        rowDigits.forEach { digit ->
          MpinDigitKey(
            digit = digit,
            enabled = enabled,
            accentColor = accentColor,
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onDigitClick(digit)
            },
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      MpinActionKey(
        label = stringResource(R.string.mpin_key_clear),
        enabled = enabled,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onClearClick()
        },
        testTag = "mpin_key_clear",
        modifier = Modifier.weight(1f),
      )

      MpinDigitKey(
        digit = '0',
        enabled = enabled,
        accentColor = accentColor,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onDigitClick('0')
        },
        modifier = Modifier.weight(1f),
      )

      MpinBackspaceKey(
        enabled = enabled,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onBackspaceClick()
        },
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun MpinDigitKey(
  digit: Char,
  enabled: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  val shape = RoundedCornerShape(18.dp)
  val fillBrush =
    remember(semanticColors.glassFillStart, semanticColors.glassFillEnd, accentColor) {
      Brush.linearGradient(
        colors = listOf(semanticColors.glassFillStart, semanticColors.glassFillEnd)
      )
    }
  val rimBrush =
    remember(semanticColors.glassRimStart, semanticColors.glassRimEnd) {
      Brush.linearGradient(colors = listOf(semanticColors.glassRimStart, semanticColors.glassRimEnd))
    }

  Box(
    modifier =
      modifier
        .heightIn(min = 56.dp)
        .clip(shape)
        .background(fillBrush)
        .border(width = 1.dp, brush = rimBrush, shape = shape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .testTag("mpin_key_$digit"),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = digit.toString(),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.SemiBold,
      color =
        if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
    )
  }
}

@Composable
private fun MpinActionKey(
  label: String,
  enabled: Boolean,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(18.dp)
  Box(
    modifier =
      modifier
        .heightIn(min = 56.dp)
        .clip(shape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
      color =
        if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
    )
  }
}

@Composable
private fun MpinBackspaceKey(
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(18.dp)
  Box(
    modifier =
      modifier
        .heightIn(min = 56.dp)
        .clip(shape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .testTag("mpin_key_backspace"),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = Icons.AutoMirrored.Outlined.Backspace,
      contentDescription = stringResource(R.string.mpin_key_backspace_cd),
      tint =
        if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
      modifier = Modifier.size(22.dp),
    )
  }
}

@Composable
fun MpinSetupScreen(
  themePalette: BusinessThemePalette,
  isDark: Boolean,
  onToggleTheme: () -> Unit,
  onCompleteSetup: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val semanticColors = LocalFundTrackColors.current
  var step by rememberSaveable { mutableIntStateOf(1) }
  var firstPin by rememberSaveable { mutableStateOf("") }
  var confirmPin by rememberSaveable { mutableStateOf("") }
  var isMismatchError by rememberSaveable { mutableStateOf(false) }
  var shakeSignal by rememberSaveable { mutableIntStateOf(0) }

  val currentPin = if (step == 1) firstPin else confirmPin
  val heroSealBrush =
    remember(themePalette) {
      Brush.linearGradient(colors = listOf(themePalette.gradientStart, themePalette.gradientEnd))
    }

  fun onDigit(digit: Char) {
    isMismatchError = false
    if (step == 1) {
      if (firstPin.length < MpinManager.MPIN_LENGTH) {
        val next = firstPin + digit
        firstPin = next
        if (next.length == MpinManager.MPIN_LENGTH) {
          step = 2
          confirmPin = ""
        }
      }
    } else {
      if (confirmPin.length < MpinManager.MPIN_LENGTH) {
        val next = confirmPin + digit
        confirmPin = next
        if (next.length == MpinManager.MPIN_LENGTH) {
          if (next == firstPin) {
            onCompleteSetup(next)
          } else {
            isMismatchError = true
            shakeSignal += 1
            confirmPin = ""
          }
        }
      }
    }
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp)
        .testTag("mpin_setup_screen"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    // Top bar with brand emblem and quick theme toggle
    Row(
      modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Image(
          painter = painterResource(R.drawable.leufunds_icon),
          contentDescription = stringResource(R.string.app_name),
          contentScale = ContentScale.Crop,
          modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)),
        )
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      QuickThemeToggleButton(isDark = isDark, onToggle = onToggleTheme)
    }

    Spacer(modifier = Modifier.height(16.dp))

    GlassCard(
      modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
      shape = RoundedCornerShape(30.dp),
      elevation = 12.dp,
      elevated = true,
      shadowTint = themePalette.accent.copy(alpha = 0.28f),
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .drawBehind {
              val glowCenter = Offset(size.width * 0.5f, size.height * 0.12f)
              drawCircle(
                brush =
                  Brush.radialGradient(
                    colors = listOf(themePalette.accent.copy(alpha = 0.16f), Color.Transparent),
                    center = glowCenter,
                    radius = size.width * 0.55f,
                  ),
                center = glowCenter,
                radius = size.width * 0.55f,
              )
            }
            .padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier =
            Modifier.clip(RoundedCornerShape(999.dp))
              .background(themePalette.accent.copy(alpha = 0.14f))
              .border(
                width = 1.dp,
                color = themePalette.accent.copy(alpha = 0.30f),
                shape = RoundedCornerShape(999.dp),
              )
              .padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = themePalette.accent,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = stringResource(R.string.mpin_setup_badge).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            fontWeight = FontWeight.Bold,
            color = themePalette.accent,
          )
        }

        // Step Pills
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          SetupStepChip(
            label = stringResource(R.string.mpin_step_create),
            isActive = step == 1,
            isDone = step == 2,
            accentColor = themePalette.accent,
          )
          SetupStepChip(
            label = stringResource(R.string.mpin_step_confirm),
            isActive = step == 2,
            isDone = false,
            accentColor = themePalette.accent,
          )
        }

        Box(
          modifier =
            Modifier.size(64.dp)
              .clip(CircleShape)
              .background(heroSealBrush)
              .border(
                width = 1.5.dp,
                brush =
                  Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.14f))
                  ),
                shape = CircleShape,
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Outlined.Pin,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp),
          )
        }

        AnimatedContent(
          targetState = step,
          transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(150)) },
          label = "mpin_setup_copy",
        ) { activeStep ->
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text =
                if (activeStep == 1) stringResource(R.string.mpin_setup_title_create)
                else stringResource(R.string.mpin_setup_title_confirm),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center,
            )
            Text(
              text =
                if (activeStep == 1) stringResource(R.string.mpin_setup_subtitle_create)
                else stringResource(R.string.mpin_setup_subtitle_confirm),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }

        MpinDotsIndicator(
          enteredLength = currentPin.length,
          isError = isMismatchError,
          shakeSignal = shakeSignal,
          accentColor = themePalette.accent,
        )

        if (isMismatchError) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(semanticColors.softRedContainer)
                .border(
                  width = 1.dp,
                  color = semanticColors.softRedContent.copy(alpha = 0.35f),
                  shape = RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("mpin_error_message"),
          ) {
            Text(
              text = stringResource(R.string.mpin_setup_mismatch_error),
              style = MaterialTheme.typography.bodySmall,
              color = semanticColors.softRedContent,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }

        MpinKeypad(
          enabled = true,
          accentColor = themePalette.accent,
          onDigitClick = ::onDigit,
          onClearClick = {
            isMismatchError = false
            if (step == 1) firstPin = "" else confirmPin = ""
          },
          onBackspaceClick = {
            isMismatchError = false
            if (step == 1 && firstPin.isNotEmpty()) {
              firstPin = firstPin.dropLast(1)
            } else if (step == 2 && confirmPin.isNotEmpty()) {
              confirmPin = confirmPin.dropLast(1)
            }
          },
        )

        if (step == 2) {
          TextButton(
            onClick = {
              step = 1
              firstPin = ""
              confirmPin = ""
              isMismatchError = false
            },
            modifier = Modifier.heightIn(min = 48.dp),
          ) {
            Text(
              text = stringResource(R.string.mpin_setup_start_over),
              style = MaterialTheme.typography.labelLarge,
              color = themePalette.accent,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
  }
}

@Composable
private fun SetupStepChip(
  label: String,
  isActive: Boolean,
  isDone: Boolean,
  accentColor: Color,
) {
  val semanticColors = LocalFundTrackColors.current
  val fillColor =
    when {
      isActive -> accentColor.copy(alpha = 0.16f)
      isDone -> semanticColors.softGreenContainer
      else -> semanticColors.glassFillStart.copy(alpha = 0.7f)
    }
  val textColor =
    when {
      isActive -> accentColor
      isDone -> semanticColors.softGreenContent
      else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
  val borderColor =
    when {
      isActive -> accentColor.copy(alpha = 0.45f)
      isDone -> semanticColors.softGreenContent.copy(alpha = 0.3f)
      else -> semanticColors.cardBorder
    }

  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(999.dp))
        .background(fillColor)
        .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(999.dp))
        .padding(horizontal = 12.dp, vertical = 5.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Medium,
      color = textColor,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeMpinBottomSheet(
  accentColor: Color,
  onVerifyCurrentPin: suspend (String) -> Boolean,
  onChangeMpin: suspend (currentPin: String, newPin: String) -> Boolean,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val semanticColors = LocalFundTrackColors.current
  val scope = rememberCoroutineScope()

  var step by rememberSaveable { mutableIntStateOf(1) } // 1=Current, 2=New, 3=Confirm
  var currentPinVerified by rememberSaveable { mutableStateOf("") }
  var newPinDraft by rememberSaveable { mutableStateOf("") }
  var inputPin by rememberSaveable { mutableStateOf("") }
  var errorResId by remember { mutableStateOf<Int?>(null) }
  var isBusy by remember { mutableStateOf(false) }
  var shakeSignal by remember { mutableIntStateOf(0) }

  val titleResId =
    when (step) {
      1 -> R.string.change_mpin_step_current
      2 -> R.string.change_mpin_step_new
      else -> R.string.change_mpin_step_confirm
    }
  val subtitleResId =
    when (step) {
      1 -> R.string.change_mpin_subtitle_current
      2 -> R.string.change_mpin_subtitle_new
      else -> R.string.change_mpin_subtitle_confirm
    }

  fun onDigit(digit: Char) {
    if (isBusy || inputPin.length >= MpinManager.MPIN_LENGTH) return
    errorResId = null
    val next = inputPin + digit
    inputPin = next
    if (next.length == MpinManager.MPIN_LENGTH) {
      when (step) {
        1 -> {
          isBusy = true
          scope.launch {
            val valid = onVerifyCurrentPin(next)
            isBusy = false
            if (valid) {
              currentPinVerified = next
              inputPin = ""
              step = 2
            } else {
              errorResId = R.string.change_mpin_wrong_current
              shakeSignal += 1
              inputPin = ""
            }
          }
        }
        2 -> {
          if (next == currentPinVerified) {
            errorResId = R.string.change_mpin_same_as_old
            shakeSignal += 1
            inputPin = ""
          } else {
            newPinDraft = next
            inputPin = ""
            step = 3
          }
        }
        else -> {
          if (next != newPinDraft) {
            errorResId = R.string.mpin_setup_mismatch_error
            shakeSignal += 1
            inputPin = ""
          } else {
            isBusy = true
            scope.launch {
              val changed = onChangeMpin(currentPinVerified, next)
              isBusy = false
              if (changed) {
                onDismiss()
              } else {
                errorResId = R.string.change_mpin_wrong_current
                shakeSignal += 1
                inputPin = ""
                step = 1
              }
            }
          }
        }
      }
    }
  }

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
          .padding(horizontal = 20.dp)
          .padding(bottom = 24.dp)
          .testTag("change_mpin_sheet"),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = stringResource(titleResId),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )
      Text(
        text = stringResource(subtitleResId),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      MpinDotsIndicator(
        enteredLength = inputPin.length,
        isError = errorResId != null,
        shakeSignal = shakeSignal,
        accentColor = accentColor,
      )

      errorResId?.let { errRes ->
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(semanticColors.softRedContainer)
              .border(
                width = 1.dp,
                color = semanticColors.softRedContent.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp),
              )
              .padding(horizontal = 12.dp, vertical = 10.dp)
              .testTag("mpin_error_message"),
        ) {
          Text(
            text = stringResource(errRes),
            style = MaterialTheme.typography.bodySmall,
            color = semanticColors.softRedContent,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }

      MpinKeypad(
        enabled = !isBusy,
        accentColor = accentColor,
        onDigitClick = ::onDigit,
        onClearClick = {
          errorResId = null
          inputPin = ""
        },
        onBackspaceClick = {
          errorResId = null
          if (inputPin.isNotEmpty()) inputPin = inputPin.dropLast(1)
        },
      )

      TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(text = stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}
