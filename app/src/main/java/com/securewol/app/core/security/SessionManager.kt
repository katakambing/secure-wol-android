package com.securewol.app.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import java.util.UUID

/**
 * SessionToken: Nonce generated exclusively upon verified authentication.
 */
data class SessionToken(
    val id: String,
    val issuedAtMillis: Long,
    val maxAgeMillis: Long = 15 * 60 * 1000 // 15 minutes max lifetime per active session
) {
    fun isValid(): Boolean {
        return (System.currentTimeMillis() - issuedAtMillis) < maxAgeMillis
    }
}

/**
 * SessionManager: Strict in-memory session gatekeeper.
 * Required for any privileged operation including Wake-on-LAN transmission.
 */
object SessionManager {

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    @Volatile
    private var activeSessionToken: SessionToken? = null

    /**
     * Initializes a verified session upon biometric or PIN success.
     */
    @Synchronized
    fun createSession(): SessionToken {
        val randomBytes = ByteArray(16)
        SecureRandom().nextBytes(randomBytes)
        val token = SessionToken(
            id = UUID.randomUUID().toString(),
            issuedAtMillis = System.currentTimeMillis()
        )
        activeSessionToken = token
        _isAuthenticated.value = true
        SecureLogger.i("Authentication session initialized")
        return token
    }

    /**
     * Immediately destroys the active session and locks all capabilities.
     */
    @Synchronized
    fun invalidateSession() {
        activeSessionToken = null
        _isAuthenticated.value = false
        SecureLogger.i("Authentication session invalidated / App locked")
    }

    /**
     * Checks whether an active, non-expired session exists.
     */
    @Synchronized
    fun isSessionValid(): Boolean {
        val token = activeSessionToken
        val valid = token != null && token.isValid()
        if (!valid && activeSessionToken != null) {
            // Expired
            invalidateSession()
        }
        return valid
    }

    /**
     * Required gate for critical paths (e.g. sending WoL packet, modifying security settings).
     * Throws SecurityException if not authenticated.
     */
    @Synchronized
    fun validateSessionOrThrow(): SessionToken {
        val token = activeSessionToken
        if (token == null || !token.isValid()) {
            invalidateSession()
            throw SecurityException("Access Denied: Unauthenticated or expired session")
        }
        return token
    }
}
