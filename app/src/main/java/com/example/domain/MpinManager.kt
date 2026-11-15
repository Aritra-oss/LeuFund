package com.example.domain

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object MpinManager {
  const val MPIN_LENGTH = 4
  const val MAX_FAILED_ATTEMPTS = 5
  const val LOCKOUT_DURATION_MS = 30_000L

  private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1"
  private const val PREFIX_PBKDF2 = "pbkdf2_sha1"
  private const val PREFIX_SHA256 = "sha256_iter"
  private const val ITERATIONS = 10_000
  private const val SALT_BYTES = 16
  private const val KEY_LENGTH_BITS = 256

  private val secureRandom = SecureRandom()

  fun isValidFormat(pin: String): Boolean =
    pin.length == MPIN_LENGTH && pin.all { it in '0'..'9' }

  fun hashPin(pin: String): String {
    require(isValidFormat(pin)) { "M-PIN must be exactly $MPIN_LENGTH digits" }
    val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
    val derived =
      derivePbkdf2(pin, salt, ITERATIONS)
        ?: return "$PREFIX_SHA256:$ITERATIONS:${salt.toHex()}:${deriveIteratedSha256(pin, salt, ITERATIONS).toHex()}"
    return "$PREFIX_PBKDF2:$ITERATIONS:${salt.toHex()}:${derived.toHex()}"
  }

  fun verifyPin(pin: String, storedRecord: String?): Boolean {
    if (!isValidFormat(pin) || storedRecord.isNullOrBlank()) return false
    val parts = storedRecord.split(':')
    if (parts.size != 4) return false

    val algo = parts[0]
    val iterations = parts[1].toIntOrNull() ?: return false
    if (iterations <= 0) return false
    val salt = parts[2].hexToBytes() ?: return false
    val expectedHash = parts[3].hexToBytes() ?: return false

    val actualHash =
      when (algo) {
        PREFIX_PBKDF2 -> derivePbkdf2(pin, salt, iterations) ?: return false
        PREFIX_SHA256 -> deriveIteratedSha256(pin, salt, iterations)
        else -> return false
      }
    return MessageDigest.isEqual(expectedHash, actualHash)
  }

  fun recordFailedAttempt(
    currentFailedAttempts: Int,
    nowMillis: Long = System.currentTimeMillis(),
  ): FailedAttemptOutcome {
    val nextAttempts = currentFailedAttempts + 1
    return if (nextAttempts >= MAX_FAILED_ATTEMPTS) {
      FailedAttemptOutcome(
        failedAttempts = nextAttempts,
        remainingAttemptsBeforeLockout = 0,
        lockoutUntilMillis = nowMillis + LOCKOUT_DURATION_MS,
      )
    } else {
      FailedAttemptOutcome(
        failedAttempts = nextAttempts,
        remainingAttemptsBeforeLockout = MAX_FAILED_ATTEMPTS - nextAttempts,
        lockoutUntilMillis = 0L,
      )
    }
  }

  fun remainingLockoutSeconds(
    lockoutUntilMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
  ): Int {
    val remainingMs = lockoutUntilMillis - nowMillis
    if (remainingMs <= 0L) return 0
    return ((remainingMs + 999L) / 1_000L).toInt()
  }

  private fun derivePbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray? =
    runCatching {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        try {
          SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
          spec.clearPassword()
        }
      }
      .getOrNull()

  private fun deriveIteratedSha256(pin: String, salt: ByteArray, iterations: Int): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(salt)
    var hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
    repeat(iterations - 1) {
      digest.reset()
      digest.update(salt)
      hash = digest.digest(hash)
    }
    return hash
  }

  private fun ByteArray.toHex(): String =
    joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xFF) }

  private fun String.hexToBytes(): ByteArray? {
    if (length % 2 != 0 || isEmpty()) return null
    val result = ByteArray(length / 2)
    for (i in result.indices) {
      val hi = Character.digit(this[i * 2], 16)
      val lo = Character.digit(this[i * 2 + 1], 16)
      if (hi == -1 || lo == -1) return null
      result[i] = ((hi shl 4) or lo).toByte()
    }
    return result
  }
}

data class FailedAttemptOutcome(
  val failedAttempts: Int,
  val remainingAttemptsBeforeLockout: Int,
  val lockoutUntilMillis: Long,
)
