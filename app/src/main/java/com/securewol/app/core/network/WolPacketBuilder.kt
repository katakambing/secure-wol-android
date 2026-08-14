package com.securewol.app.core.network

/**
 * WolPacketBuilder: Constructs standard Wake-on-LAN Magic Packets.
 *
 * Magic Packet Format:
 * - 6 bytes of 0xFF (Synchronization Stream)
 * - 16 iterations of the 6-byte target MAC address (96 bytes)
 * - Total size: 102 bytes
 * - Optional: 6-byte SecureOn password suffix (108 bytes total)
 */
object WolPacketBuilder {

    /**
     * Parses a MAC address string in various formats and returns a 6-byte array.
     */
    fun parseMacAddress(macStr: String): ByteArray {
        val cleanMac = macStr.trim()
            .replace(":", "")
            .replace("-", "")
            .replace(" ", "")

        require(cleanMac.length == 12) {
            "Invalid MAC address length: expected 12 hexadecimal characters, got ${cleanMac.length}"
        }

        require(cleanMac.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            "Invalid hexadecimal characters in MAC address"
        }

        val macBytes = ByteArray(6)
        for (i in 0 until 6) {
            val hexByte = cleanMac.substring(i * 2, i * 2 + 2)
            macBytes[i] = hexByte.toInt(16).toByte()
        }
        return macBytes
    }

    /**
     * Builds the 102-byte (or 108-byte with SecureOn) Magic Packet payload.
     */
    fun buildMagicPacket(macStr: String, secureOnPasswordHex: String? = null): ByteArray {
        val macBytes = parseMacAddress(macStr)

        val hasSecureOn = !secureOnPasswordHex.isNullOrBlank()
        val secureOnBytes = if (hasSecureOn) {
            parseMacAddress(secureOnPasswordHex!!) // SecureOn is 6 bytes
        } else {
            null
        }

        val totalSize = 6 + (16 * 6) + (if (hasSecureOn) 6 else 0)
        val packet = ByteArray(totalSize)

        // 1. Fill first 6 bytes with 0xFF
        for (i in 0 until 6) {
            packet[i] = 0xFF.toByte()
        }

        // 2. Repeat 6-byte MAC address 16 times
        var offset = 6
        for (i in 0 until 16) {
            System.arraycopy(macBytes, 0, packet, offset, 6)
            offset += 6
        }

        // 3. Append optional 6-byte SecureOn password
        if (secureOnBytes != null) {
            System.arraycopy(secureOnBytes, 0, packet, offset, 6)
        }

        return packet
    }

    /**
     * Helper to validate a MAC address format.
     */
    fun isValidMac(macStr: String): Boolean {
        return try {
            parseMacAddress(macStr)
            true
        } catch (e: Exception) {
            false
        }
    }
}
