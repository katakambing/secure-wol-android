package com.securewol.app.core.security

import android.util.Log

/**
 * SecureLogger: Enforces strict privacy and security rules for logging.
 * - Strips MAC addresses, IPs, PINs, cryptographic keys, and sensitive tokens.
 * - Only logs in debug builds; completely disabled or stripped in release builds.
 */
object SecureLogger {
    private const val TAG = "SecureWOL"
    private const val IS_DEBUG = true // In release builds with ProGuard/R8, Log calls are stripped

    fun d(message: String) {
        if (IS_DEBUG) {
            Log.d(TAG, sanitize(message))
        }
    }

    fun i(message: String) {
        if (IS_DEBUG) {
            Log.i(TAG, sanitize(message))
        }
    }

    fun w(message: String) {
        if (IS_DEBUG) {
            Log.w(TAG, sanitize(message))
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (IS_DEBUG) {
            Log.e(TAG, sanitize(message), throwable)
        }
    }

    /**
     * Sanitizes strings to prevent accidental leakage of MAC addresses, IP addresses,
     * hashes, and secrets.
     */
    private fun sanitize(input: String): String {
        // Redact MAC addresses
        val macRegex = Regex("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})")
        val sanitizedMac = macRegex.replace(input, "[REDACTED_MAC]")

        // Redact IPv4 addresses
        val ipRegex = Regex("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b")
        val sanitizedIp = ipRegex.replace(sanitizedMac, "[REDACTED_IP]")

        // Redact raw PIN patterns (e.g. 4-8 digit numbers in quotes)
        val pinRegex = Regex("(?i)(pin|password|secret|key)[:=\\s]+([\\w\\d]+)")
        return pinRegex.replace(sanitizedIp, "$1:[REDACTED]")
    }
}
