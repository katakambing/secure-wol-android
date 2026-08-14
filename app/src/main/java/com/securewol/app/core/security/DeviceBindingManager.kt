package com.securewol.app.core.security

import android.content.Context
import java.security.SecureRandom
import java.util.UUID

/**
 * DeviceBindingManager: Manages single-owner binding via cryptographically generated random UUID.
 * Never relies on restricted, spoofable, or privacy-invasive hardware identifiers (IMEI/MAC/Serial).
 */
class DeviceBindingManager(private val context: Context) {

    companion object {
        private const val KEY_DEVICE_UUID = "bound_device_uuid"
        private const val KEY_REGISTRATION_TIMESTAMP = "device_registration_timestamp"
        private const val KEY_IS_REGISTERED = "is_device_registered"
    }

    private val prefs by lazy { KeystoreManager.getEncryptedPreferences(context) }

    /**
     * Checks if this device has been registered and initialized by an owner.
     */
    fun isDeviceRegistered(): Boolean {
        return prefs.getBoolean(KEY_IS_REGISTERED, false) &&
                !prefs.getString(KEY_DEVICE_UUID, null).isNullOrBlank()
    }

    /**
     * Registers this device during First Launch with a cryptographically secure random UUID.
     */
    @Synchronized
    fun registerDevice(): String {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val deviceUuid = UUID.nameUUIDFromBytes(randomBytes).toString()
        val timestamp = System.currentTimeMillis()

        prefs.edit()
            .putString(KEY_DEVICE_UUID, deviceUuid)
            .putLong(KEY_REGISTRATION_TIMESTAMP, timestamp)
            .putBoolean(KEY_IS_REGISTERED, true)
            .apply()

        SecureLogger.i("Device securely bound to owner. Registration timestamp recorded.")
        return deviceUuid
    }

    /**
     * Gets the active device UUID fingerprint (for display in Security Settings).
     */
    fun getBoundDeviceId(): String? {
        return prefs.getString(KEY_DEVICE_UUID, null)
    }

    /**
     * Gets registration timestamp in epoch milliseconds.
     */
    fun getRegistrationTimestamp(): Long {
        return prefs.getLong(KEY_REGISTRATION_TIMESTAMP, 0L)
    }

    /**
     * Invalidates device registration and wipes all credentials and stored PCs.
     * Enforces Requirement 15 (Device Re-registration).
     */
    @Synchronized
    fun invalidateAndReRegister() {
        SessionManager.invalidateSession()
        KeystoreManager.wipeEncryptedStorage(context)
        SecureLogger.w("Device binding invalidated and storage reset for re-registration.")
    }
}
