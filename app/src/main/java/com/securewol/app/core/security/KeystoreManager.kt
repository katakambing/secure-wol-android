package com.securewol.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * KeystoreManager: Manages hardware-backed Android KeyStore integration
 * and Jetpack Security EncryptedSharedPreferences instances.
 */
object KeystoreManager {

    private const val PREFS_FILE_NAME = "secure_wol_encrypted_prefs"

    /**
     * Obtains or creates the MasterKey backed by Android Keystore (AES256-GCM).
     */
    fun getMasterKey(context: Context): MasterKey {
        return MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    /**
     * Creates an EncryptedSharedPreferences instance using the Keystore MasterKey.
     * All keys and values are AES-256-SIV and AES-256-GCM encrypted respectively.
     */
    fun getEncryptedPreferences(context: Context): SharedPreferences {
        val masterKey = getMasterKey(context)
        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Wipes the entire encrypted storage (used strictly during Device Re-Registration).
     */
    fun wipeEncryptedStorage(context: Context) {
        try {
            val prefs = getEncryptedPreferences(context)
            prefs.edit().clear().commit()
            SecureLogger.i("Encrypted storage wiped successfully during device re-registration")
        } catch (e: Exception) {
            SecureLogger.e("Failed to wipe encrypted preferences", e)
        }
    }
}
