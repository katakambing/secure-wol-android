package com.securewol.app.data.model

import java.util.UUID

/**
 * PcDevice: Represents a configured target PC for Wake-on-LAN.
 * Stored securely in EncryptedSharedPreferences.
 */
data class PcDevice(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val macAddress: String,
    val ipAddress: String = "",
    val broadcastAddress: String = "255.255.255.255",
    val port: Int = 9,
    val secureOnPassword: String? = null,
    val createdAtEpoch: Long = System.currentTimeMillis()
) {
    /**
     * Returns a privacy-masked MAC address for display in UI before explicit full unmasking.
     * E.g. "54:04:••:••:••:8E"
     */
    fun maskedMac(): String {
        val parts = macAddress.split(":", "-")
        return if (parts.size == 6) {
            "${parts[0]}:${parts[1]}:••:••:••:${parts[5]}"
        } else {
            "••:••:••:••:••:••"
        }
    }
}
