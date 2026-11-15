package com.example.security

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.domain.BiometricAuthStrategy
import com.example.domain.BiometricLockPolicy

object BiometricAuthenticator {
  fun evaluateStrategy(context: Context): BiometricAuthStrategy {
    val biometricManager = BiometricManager.from(context)
    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    val isDeviceSecure = keyguardManager?.isDeviceSecure == true

    val combinedStatus =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        biometricManager.canAuthenticate(
          BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
      } else {
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED
      }

    val weakOnlyStatus =
      biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)

    return BiometricLockPolicy.resolveStrategy(
      sdkInt = Build.VERSION.SDK_INT,
      canAuthenticateCombinedStatus = combinedStatus,
      canAuthenticateWeakOnlyStatus = weakOnlyStatus,
      isDeviceSecure = isDeviceSecure,
    )
  }

  fun authenticateWithBiometricPrompt(
    activity: FragmentActivity,
    strategy: BiometricAuthStrategy,
    onSucceeded: () -> Unit,
    onUseMpinSelected: () -> Unit,
    onCanceled: () -> Unit,
    onError: (String) -> Unit,
  ) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt =
      BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onSucceeded()
          }

          override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            when (errorCode) {
              BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onUseMpinSelected()
              BiometricPrompt.ERROR_USER_CANCELED,
              BiometricPrompt.ERROR_CANCELED -> onCanceled()
              else -> onError(errString.toString())
            }
          }
        },
      )

    val builder =
      BiometricPrompt.PromptInfo.Builder()
        .setTitle(activity.getString(R.string.biometric_prompt_title))
        .setSubtitle(activity.getString(R.string.biometric_prompt_subtitle))
        .setConfirmationRequired(false)

    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
        strategy == BiometricAuthStrategy.BIOMETRIC_OR_CREDENTIAL_PROMPT
    ) {
      builder.setAllowedAuthenticators(
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
          BiometricManager.Authenticators.DEVICE_CREDENTIAL
      )
    } else {
      builder
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .setNegativeButtonText(activity.getString(R.string.biometric_prompt_negative))
    }

    biometricPrompt.authenticate(builder.build())
  }

  @Suppress("DEPRECATION")
  fun createKeyguardCredentialIntent(context: Context): Intent? {
    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    return keyguardManager?.createConfirmDeviceCredentialIntent(
      context.getString(R.string.biometric_prompt_title),
      context.getString(R.string.biometric_prompt_subtitle),
    )
  }

  fun createEnrollBiometricIntent(): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
        putExtra(
          Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
          BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.BIOMETRIC_STRONG,
        )
      }
    } else {
      Intent(Settings.ACTION_SECURITY_SETTINGS)
    }
  }
}
