package com.securewol.app.data.model

import java.util.UUID

/**
 * PcDevice: Represents a configured target PC for Wake-on-LAN and Remote Power Controls.
 * Stored securely in EncryptedSharedPreferences with hardware-backed Keystore AES-256-GCM.
 */
data class PcDevice(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val macAddress: String,
    val ipAddress: String = "",
    val broadcastAddress: String = "255.255.255.255",
    val port: Int = 9,
    val secureOnPassword: String? = null,
    val agentAuthToken: String? = null,
    val createdAtEpoch: Long = System.currentTimeMillis()
) {
    /**
     * Returns a privacy-masked MAC address for display in UI.
     * E.g. "34:5A:••:••:••:87"
     */
    fun maskedMac(): String {
        val parts = macAddress.split(":", "-")
        return if (parts.size == 6) {
            "${parts[0]}:${parts[1]}:••:••:••:${parts[5]}"
        } else {
            "••:••:••:••:••:••"
        }
    }

    /**
     * Returns a privacy-masked IP address for zero-exposure UI display.
     * E.g. "192.168.•••.12"
     */
    fun maskedIp(): String {
        if (ipAddress.isBlank()) return "Unset"
        val parts = ipAddress.split(".")
        return if (parts.size == 4) {
            "${parts[0]}.${parts[1]}.•••.${parts[3]}"
        } else {
            "•••.•••.•••.•••"
        }
    }
}
