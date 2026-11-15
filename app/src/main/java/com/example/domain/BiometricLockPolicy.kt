package com.example.domain

import android.os.Build

enum class BiometricAuthStrategy {
  /** Android 11+ (API 30+): BiometricPrompt supports BIOMETRIC_WEAK or DEVICE_CREDENTIAL directly. */
  BIOMETRIC_OR_CREDENTIAL_PROMPT,

  /** Android 7-10 (API 24-29): BiometricPrompt with BIOMETRIC_WEAK and negative Cancel button. */
  BIOMETRIC_ONLY_PROMPT,

  /** Android 7-10 (API 24-29): Biometric unavailable/unenrolled, but device PIN/Pattern/Password is set. */
  KEYGUARD_CREDENTIAL_FALLBACK,

  /** No biometric credentials enrolled and no device lock configured. */
  NOT_ENROLLED_NO_LOCK,

  /** Biometric hardware missing or unavailable and no device lock configured. */
  UNAVAILABLE_NO_LOCK,
}

object BiometricLockPolicy {
  // Constants mirror BiometricManager status codes so this policy can be unit-tested on the JVM.
  const val STATUS_SUCCESS = 0
  const val STATUS_ERROR_NO_HARDWARE = 12
  const val STATUS_ERROR_HW_UNAVAILABLE = 1
  const val STATUS_ERROR_NONE_ENROLLED = 11

  fun resolveStrategy(
    sdkInt: Int = Build.VERSION.SDK_INT,
    canAuthenticateCombinedStatus: Int,
    canAuthenticateWeakOnlyStatus: Int,
    isDeviceSecure: Boolean,
  ): BiometricAuthStrategy {
    if (sdkInt >= Build.VERSION_CODES.R) {
      return when (canAuthenticateCombinedStatus) {
        STATUS_SUCCESS -> BiometricAuthStrategy.BIOMETRIC_OR_CREDENTIAL_PROMPT
        STATUS_ERROR_NONE_ENROLLED -> {
          if (isDeviceSecure) {
            BiometricAuthStrategy.KEYGUARD_CREDENTIAL_FALLBACK
          } else {
            BiometricAuthStrategy.NOT_ENROLLED_NO_LOCK
          }
        }
        else -> {
          if (isDeviceSecure) {
            BiometricAuthStrategy.KEYGUARD_CREDENTIAL_FALLBACK
          } else {
            BiometricAuthStrategy.UNAVAILABLE_NO_LOCK
          }
        }
      }
    }

    return when (canAuthenticateWeakOnlyStatus) {
      STATUS_SUCCESS -> BiometricAuthStrategy.BIOMETRIC_ONLY_PROMPT
      STATUS_ERROR_NONE_ENROLLED -> {
        if (isDeviceSecure) {
          BiometricAuthStrategy.KEYGUARD_CREDENTIAL_FALLBACK
        } else {
          BiometricAuthStrategy.NOT_ENROLLED_NO_LOCK
        }
      }
      else -> {
        if (isDeviceSecure) {
          BiometricAuthStrategy.KEYGUARD_CREDENTIAL_FALLBACK
        } else {
          BiometricAuthStrategy.UNAVAILABLE_NO_LOCK
        }
      }
    }
  }

  fun requiresSystemAuthentication(strategy: BiometricAuthStrategy): Boolean =
    strategy == BiometricAuthStrategy.BIOMETRIC_OR_CREDENTIAL_PROMPT ||
      strategy == BiometricAuthStrategy.BIOMETRIC_ONLY_PROMPT ||
      strategy == BiometricAuthStrategy.KEYGUARD_CREDENTIAL_FALLBACK
}
