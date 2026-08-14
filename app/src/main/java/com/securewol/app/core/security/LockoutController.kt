package com.securewol.app.core.security

import android.content.Context

/**
 * LockoutController: Enforces progressive delays against brute-force authentication attempts.
 *
 * Escalation table:
 * - 1..2 failures: No lockout
 * - 3..4 failures: 30 seconds delay
 * - 5..9 failures: 2 minutes delay (120s)
 * - 10+ failures: 5 minutes delay (300s)
 */
class LockoutController(private val context: Context) {

    companion object {
        private const val KEY_FAILED_ATTEMPTS = "auth_failed_attempts"
        private const val KEY_LOCKOUT_EXPIRY = "auth_lockout_expiry_epoch"

        const val DELAY_3_FAILS_SECONDS = 30L
        const val DELAY_5_FAILS_SECONDS = 120L
        const val DELAY_10_FAILS_SECONDS = 300L
    }

    private val prefs by lazy { KeystoreManager.getEncryptedPreferences(context) }

    /**
     * Calculates the lockout penalty duration in seconds for a given failure count.
     */
    fun calculateLockoutDurationSeconds(failureCount: Int): Long {
        return when {
            failureCount >= 10 -> DELAY_10_FAILS_SECONDS
            failureCount >= 5 -> DELAY_5_FAILS_SECONDS
            failureCount >= 3 -> DELAY_3_FAILS_SECONDS
            else -> 0L
        }
    }

    /**
     * Records an authentication failure, computes progressive penalty, and saves state to encrypted storage.
     * @return remaining seconds of lockout imposed, or 0 if under threshold.
     */
    @Synchronized
    fun recordFailedAttempt(): Long {
        val currentFails = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val lockoutSeconds = calculateLockoutDurationSeconds(currentFails)
        
        val expiryEpoch = if (lockoutSeconds > 0) {
            System.currentTimeMillis() + (lockoutSeconds * 1000)
        } else {
            0L
        }

        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, currentFails)
            .putLong(KEY_LOCKOUT_EXPIRY, expiryEpoch)
            .apply()

        SecureLogger.w("Authentication failure recorded. Total consecutive failures: $currentFails, lockout penalty: ${lockoutSeconds}s")
        return lockoutSeconds
    }

    /**
     * Resets the failure counter and clears lockout status. MUST only be called after verified successful authentication.
     */
    @Synchronized
    fun recordSuccessfulAuth() {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_EXPIRY, 0L)
            .apply()
        SecureLogger.i("Authentication success recorded. Failure counter reset to 0.")
    }

    /**
     * Checks if user is currently locked out.
     */
    @Synchronized
    fun isLockedOut(): Boolean {
        val expiry = prefs.getLong(KEY_LOCKOUT_EXPIRY, 0L)
        return expiry > System.currentTimeMillis()
    }

    /**
     * Returns remaining lockout time in seconds, or 0 if not locked out.
     */
    @Synchronized
    fun getRemainingLockoutSeconds(): Long {
        val expiry = prefs.getLong(KEY_LOCKOUT_EXPIRY, 0L)
        val now = System.currentTimeMillis()
        return if (expiry > now) {
            ((expiry - now) + 999) / 1000
        } else {
            0L
        }
    }

    /**
     * Returns current count of consecutive failures.
     */
    @Synchronized
    fun getFailedAttemptsCount(): Int {
        return prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
    }
}
