package com.securewol.app.data.repository

import android.content.Context
import com.securewol.app.core.security.DeviceBindingManager
import com.securewol.app.core.security.KeystoreManager
import com.securewol.app.core.security.LockoutController
import com.securewol.app.core.security.PinCryptoHelper
import com.securewol.app.core.security.SecureLogger
import com.securewol.app.core.security.SessionManager
import com.securewol.app.data.model.AuthState
import com.securewol.app.data.model.AutoLockTimeout
import com.securewol.app.data.model.SecuritySettings

/**
 * SecurityRepository: Centralized repository for all authentication, encryption,
 * lockout, and device-binding states.
 */
class SecurityRepository(private val context: Context) {

    companion object {
        private const val KEY_PIN_HASH = "owner_pin_hash"
        private const val KEY_PIN_SALT = "owner_pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "setting_biometric_enabled"
        private const val KEY_AUTOLOCK_TIMEOUT = "setting_autolock_timeout"
        private const val KEY_REQUIRE_AUTH_POWER_ON = "setting_require_auth_power_on"
        private const val KEY_FAILED_PROTECTION = "setting_failed_protection"
    }

    private val prefs by lazy { KeystoreManager.getEncryptedPreferences(context) }
    val lockoutController by lazy { LockoutController(context) }
    val deviceBindingManager by lazy { DeviceBindingManager(context) }

    /**
     * Checks if initial owner setup (device registration & PIN creation) has occurred.
     */
    fun isOwnerEnrolled(): Boolean {
        val hasPin = !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()
        val isRegistered = deviceBindingManager.isDeviceRegistered()
        return hasPin && isRegistered
    }

    /**
     * Registers owner with newly created PIN and binds device.
     */
    @Synchronized
    fun enrollOwner(pinChars: CharArray): Boolean {
        return try {
            val salt = PinCryptoHelper.generateSalt()
            val hash = PinCryptoHelper.hashPin(pinChars, salt)

            prefs.edit()
                .putString(KEY_PIN_HASH, PinCryptoHelper.encodeBase64(hash))
                .putString(KEY_PIN_SALT, PinCryptoHelper.encodeBase64(salt))
                .apply()

            deviceBindingManager.registerDevice()
            lockoutController.recordSuccessfulAuth()
            SessionManager.createSession()
            SecureLogger.i("Owner enrollment completed successfully")
            true
        } catch (e: Exception) {
            SecureLogger.e("Owner enrollment failed", e)
            false
        } finally {
            PinCryptoHelper.clearChars(pinChars)
        }
    }

    /**
     * Verifies entered PIN against stored cryptographic hash.
     */
    @Synchronized
    fun verifyAppPin(enteredPinChars: CharArray): Boolean {
        if (lockoutController.isLockedOut()) {
            SecureLogger.w("PIN verification attempted during active lockout period")
            return false
        }

        val storedHash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        val storedSalt = prefs.getString(KEY_PIN_SALT, "") ?: ""

        val isValid = PinCryptoHelper.verifyPin(enteredPinChars, storedHash, storedSalt)
        PinCryptoHelper.clearChars(enteredPinChars)

        if (isValid) {
            lockoutController.recordSuccessfulAuth()
            SessionManager.createSession()
        } else {
            lockoutController.recordFailedAttempt()
        }

        return isValid
    }

    /**
     * Changes existing PIN after verifying old PIN.
     */
    @Synchronized
    fun changePin(oldPinChars: CharArray, newPinChars: CharArray): Boolean {
        SessionManager.validateSessionOrThrow()
        
        val storedHash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        val storedSalt = prefs.getString(KEY_PIN_SALT, "") ?: ""

        val oldValid = PinCryptoHelper.verifyPin(oldPinChars, storedHash, storedSalt)
        PinCryptoHelper.clearChars(oldPinChars)

        if (!oldValid) {
            PinCryptoHelper.clearChars(newPinChars)
            return false
        }

        val newSalt = PinCryptoHelper.generateSalt()
        val newHash = PinCryptoHelper.hashPin(newPinChars, newSalt)
        PinCryptoHelper.clearChars(newPinChars)

        prefs.edit()
            .putString(KEY_PIN_HASH, PinCryptoHelper.encodeBase64(newHash))
            .putString(KEY_PIN_SALT, PinCryptoHelper.encodeBase64(newSalt))
            .apply()

        SecureLogger.i("App PIN updated successfully")
        return true
    }

    /**
     * Records biometric authentication success.
     */
    fun recordBiometricSuccess() {
        lockoutController.recordSuccessfulAuth()
        SessionManager.createSession()
    }

    /**
     * Records biometric authentication failure.
     */
    fun recordBiometricFailure() {
        lockoutController.recordFailedAttempt()
    }

    /**
     * Evaluates the current auth state.
     */
    fun getEffectiveAuthState(): AuthState {
        if (!isOwnerEnrolled()) {
            return AuthState.SetupRequired
        }
        if (lockoutController.isLockedOut()) {
            return AuthState.LockedOut(lockoutController.getRemainingLockoutSeconds())
        }
        if (SessionManager.isSessionValid()) {
            return AuthState.Authenticated
        }
        return AuthState.Unauthenticated
    }

    // --- Settings Getters & Setters ---

    fun getSecuritySettings(): SecuritySettings {
        return SecuritySettings(
            isBiometricEnabled = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true),
            autoLockTimeout = AutoLockTimeout.fromName(prefs.getString(KEY_AUTOLOCK_TIMEOUT, AutoLockTimeout.IMMEDIATELY.name)),
            requireAuthBeforePowerOn = prefs.getBoolean(KEY_REQUIRE_AUTH_POWER_ON, true),
            failedAttemptProtection = prefs.getBoolean(KEY_FAILED_PROTECTION, true)
        )
    }

    fun updateSecuritySettings(settings: SecuritySettings) {
        SessionManager.validateSessionOrThrow()
        prefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, settings.isBiometricEnabled)
            .putString(KEY_AUTOLOCK_TIMEOUT, settings.autoLockTimeout.name)
            .putBoolean(KEY_REQUIRE_AUTH_POWER_ON, settings.requireAuthBeforePowerOn)
            .putBoolean(KEY_FAILED_PROTECTION, settings.failedAttemptProtection)
            .apply()
        SecureLogger.i("Security settings updated")
    }
}
