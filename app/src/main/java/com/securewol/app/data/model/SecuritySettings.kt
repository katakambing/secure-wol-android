package com.securewol.app.data.model

/**
 * AutoLockTimeout options per Requirement 3:
 * - Immediately
 * - 30 seconds
 * - 1 minute
 * - 5 minutes
 */
enum class AutoLockTimeout(val displayName: String, val timeoutMillis: Long) {
    IMMEDIATELY("Immediately", 0L),
    THIRTY_SECONDS("30 seconds", 30_000L),
    ONE_MINUTE("1 minute", 60_000L),
    FIVE_MINUTES("5 minutes", 300_000L);

    companion object {
        fun fromName(name: String?): AutoLockTimeout {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: IMMEDIATELY
        }
    }
}

/**
 * SecuritySettings: Configurable security parameters.
 */
data class SecuritySettings(
    val isBiometricEnabled: Boolean = true,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.IMMEDIATELY,
    val requireAuthBeforePowerOn: Boolean = true,
    val failedAttemptProtection: Boolean = true
)
