package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.BiometricAuthStrategy
import com.example.domain.BiometricLockPolicy
import com.example.domain.BusinessThemePalette
import com.example.domain.MpinManager
import com.example.ui.theme.LocalFundTrackColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BiometricLockScreen(
  themePalette: BusinessThemePalette,
  strategy: BiometricAuthStrategy,
  biometricLockEnabled: Boolean,
  statusMessage: String?,
  forceMpinSignal: Int,
  isDark: Boolean,
  onToggleTheme: () -> Unit,
  onUnlockClick: () -> Unit,
  onVerifyMpin: suspend (String) -> Boolean,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val semanticColors = LocalFundTrackColors.current
  val scope = rememberCoroutineScope()

  val canUseBiometrics =
    biometricLockEnabled && BiometricLockPolicy.requiresSystemAuthentication(strategy)
  var showMpinMode by rememberSaveable(canUseBiometrics) { mutableStateOf(!canUseBiometrics) }

  LaunchedEffect(forceMpinSignal) {
    if (forceMpinSignal > 0) {
      showMpinMode = true
    }
  }

  var enteredPin by remember { mutableStateOf("") }
  var failedAttempts by rememberSaveable { mutableIntStateOf(0) }
  var lockoutUntilMillis by rememberSaveable { mutableLongStateOf(0L) }
  var lockoutSecondsLeft by remember { mutableIntStateOf(0) }
  var mpinErrorMessage by remember { mutableStateOf<String?>(null) }
  var isVerifying by remember { mutableStateOf(false) }
  var shakeSignal by remember { mutableIntStateOf(0) }

  LaunchedEffect(lockoutUntilMillis) {
    if (lockoutUntilMillis <= 0L) {
      lockoutSecondsLeft = 0
      return@LaunchedEffect
    }
    while (true) {
      val remaining = MpinManager.remainingLockoutSeconds(lockoutUntilMillis)
      lockoutSecondsLeft = remaining
      if (remaining <= 0) {
        lockoutUntilMillis = 0L
        failedAttempts = 0
        mpinErrorMessage = null
        break
      }
      delay(500L)
    }
  }

  val isLockedOut = lockoutSecondsLeft > 0

  val heroSealBrush =
    remember(themePalette) {
      Brush.linearGradient(colors = listOf(themePalette.gradientStart, themePalette.gradientEnd))
    }

  fun onPinDigit(digit: Char) {
    if (isVerifying || isLockedOut || enteredPin.length >= MpinManager.MPIN_LENGTH) return
    mpinErrorMessage = null
    val next = enteredPin + digit
    enteredPin = next
    if (next.length == MpinManager.MPIN_LENGTH) {
      isVerifying = true
      scope.launch {
        val valid = onVerifyMpin(next)
        isVerifying = false
        if (valid) {
          enteredPin = ""
          failedAttempts = 0
          lockoutUntilMillis = 0L
          mpinErrorMessage = null
        } else {
          enteredPin = ""
          shakeSignal += 1
          val outcome = MpinManager.recordFailedAttempt(failedAttempts)
          failedAttempts = outcome.failedAttempts
          if (outcome.lockoutUntilMillis > 0L) {
            lockoutUntilMillis = outcome.lockoutUntilMillis
            mpinErrorMessage = null
          } else {
            mpinErrorMessage =
              context.resources.getQuantityString(
                R.plurals.mpin_incorrect_attempts_left,
                outcome.remainingAttemptsBeforeLockout,
                outcome.remainingAttemptsBeforeLockout,
              )
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
        .testTag("biometric_lock_screen"),
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
      shadowTint = themePalette.accent.copy(alpha = 0.30f),
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .drawBehind {
              val glowCenter = Offset(size.width * 0.5f, size.height * 0.14f)
              drawCircle(
                brush =
                  Brush.radialGradient(
                    colors = listOf(themePalette.accent.copy(alpha = 0.16f), Color.Transparent),
                    center = glowCenter,
                    radius = size.width * 0.58f,
                  ),
                center = glowCenter,
                radius = size.width * 0.58f,
              )
            }
            .padding(horizontal = 22.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp),
      ) {
        // Frosted Privacy Badge
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
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = themePalette.accent,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = stringResource(R.string.biometric_lock_badge).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            fontWeight = FontWeight.Bold,
            color = themePalette.accent,
          )
        }

        AnimatedContent(
          targetState = showMpinMode,
          transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(150)) },
          label = "lock_mode_content",
        ) { mpinActive ->
          if (mpinActive) {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(68.dp)
                    .clip(CircleShape)
                    .background(heroSealBrush)
                    .border(
                      width = 1.5.dp,
                      brush =
                        Brush.linearGradient(
                          colors =
                            listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.14f))
                        ),
                      shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Outlined.Pin,
                  contentDescription = stringResource(R.string.mpin_unlock_title),
                  tint = Color.White,
                  modifier = Modifier.size(34.dp),
                )
              }

              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = stringResource(R.string.mpin_unlock_title),
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center,
                )
                Text(
                  text =
                    if (canUseBiometrics) stringResource(R.string.mpin_unlock_subtitle_choice)
                    else stringResource(R.string.mpin_unlock_subtitle_fallback),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                )
              }

              MpinDotsIndicator(
                enteredLength = enteredPin.length,
                isError = mpinErrorMessage != null || isLockedOut,
                shakeSignal = shakeSignal,
                accentColor = themePalette.accent,
              )

              val activePinNotice =
                when {
                  isLockedOut -> stringResource(R.string.mpin_lockout_wait, lockoutSecondsLeft)
                  mpinErrorMessage != null -> mpinErrorMessage
                  else -> null
                }

              if (activePinNotice != null) {
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
                    text = activePinNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = semanticColors.softRedContent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                  )
                }
              }

              MpinKeypad(
                enabled = !isVerifying && !isLockedOut,
                accentColor = themePalette.accent,
                onDigitClick = ::onPinDigit,
                onClearClick = {
                  mpinErrorMessage = null
                  enteredPin = ""
                },
                onBackspaceClick = {
                  mpinErrorMessage = null
                  if (enteredPin.isNotEmpty()) {
                    enteredPin = enteredPin.dropLast(1)
                  }
                },
              )

              if (canUseBiometrics) {
                TextButton(
                  onClick = {
                    showMpinMode = false
                    enteredPin = ""
                    mpinErrorMessage = null
                    onUnlockClick()
                  },
                  modifier = Modifier.heightIn(min = 48.dp).testTag("biometric_use_instead_btn"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = null,
                    tint = themePalette.accent,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = stringResource(R.string.biometric_use_instead),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = themePalette.accent,
                  )
                }
              }
            }
          } else {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              // Luminous Fingerprint Seal
              Box(
                modifier =
                  Modifier.size(84.dp)
                    .clip(CircleShape)
                    .background(heroSealBrush)
                    .border(
                      width = 1.5.dp,
                      brush =
                        Brush.linearGradient(
                          colors =
                            listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.14f))
                        ),
                      shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Outlined.Fingerprint,
                  contentDescription = stringResource(R.string.biometric_lock_title),
                  tint = Color.White,
                  modifier = Modifier.size(44.dp),
                )
              }

              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = stringResource(R.string.biometric_lock_title),
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center,
                )
                Text(
                  text = stringResource(R.string.biometric_lock_subtitle),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                )
              }

              if (statusMessage != null) {
                Box(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(16.dp))
                      .background(semanticColors.softAmberContainer)
                      .border(
                        width = 1.dp,
                        color = semanticColors.softAmberBorder,
                        shape = RoundedCornerShape(16.dp),
                      )
                      .padding(horizontal = 14.dp, vertical = 12.dp)
                      .testTag("biometric_status_message"),
                ) {
                  Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = semanticColors.softAmberContent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                  )
                }
              }

              val ctaLabel =
                if (strategy == BiometricAuthStrategy.KEYGUARD_CREDENTIAL_FALLBACK) {
                  stringResource(R.string.biometric_unlock_device_button)
                } else {
                  stringResource(R.string.biometric_unlock_button)
                }

              Button(
                onClick = onUnlockClick,
                shape = RoundedCornerShape(18.dp),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = themePalette.accent,
                    contentColor = Color.White,
                  ),
                contentPadding = PaddingValues(vertical = 15.dp, horizontal = 20.dp),
                modifier =
                  Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("unlock_app_button"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Fingerprint,
                  contentDescription = null,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = ctaLabel,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
              }

              OutlinedButton(
                onClick = { showMpinMode = true },
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, themePalette.accent.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = themePalette.accent),
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 20.dp),
                modifier =
                  Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("mpin_use_instead_btn"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Pin,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = stringResource(R.string.mpin_use_instead),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
  }
}
