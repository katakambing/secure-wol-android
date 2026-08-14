package com.securewol.app.core.network

import com.securewol.app.core.security.SecureLogger
import com.securewol.app.core.security.SessionManager
import com.securewol.app.data.model.PcDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * WolDispatcher: Strictly enforces internal authorization before sending WoL Magic Packets
 * over local UDP broadcast.
 */
object WolDispatcher {

    sealed class WolResult {
        object Success : WolResult()
        data class Failure(val errorMessage: String) : WolResult()
        data class SecurityDenied(val reason: String) : WolResult()
    }

    /**
     * Dispatches a Wake-on-LAN Magic Packet to the target PC.
     *
     * Strict Security Pipeline:
     * 1. Authentication Check: In-memory Session token must be valid and non-expired.
     * 2. Configuration Validation: Target MAC, broadcast address, and port are checked.
     * 3. Local Datagram Transmission: Dispatched on local broadcast socket.
     */
    suspend fun sendWakeOnLan(targetPc: PcDevice): WolResult = withContext(Dispatchers.IO) {
        // Step 1: Authentication & Authorization Gate
        try {
            SessionManager.validateSessionOrThrow()
        } catch (e: SecurityException) {
            SecureLogger.w("Blocked unauthorized WoL attempt: ${e.message}")
            return@withContext WolResult.SecurityDenied("Authentication required to send Wake-on-LAN.")
        }

        // Step 2: Validate Target PC parameters
        if (!WolPacketBuilder.isValidMac(targetPc.macAddress)) {
            SecureLogger.w("Invalid MAC address format encountered during WoL dispatch")
            return@withContext WolResult.Failure("Invalid MAC address format.")
        }

        val destinationHost = targetPc.broadcastAddress.ifBlank { "255.255.255.255" }
        val targetPort = if (targetPc.port in 1..65535) targetPc.port else 9

        SecureLogger.i("Wake-on-LAN request initiated for PC [${targetPc.name}]")

        // Step 3: Build Packet & Transmit over local UDP broadcast
        var socket: DatagramSocket? = null
        try {
            val payload = WolPacketBuilder.buildMagicPacket(targetPc.macAddress, targetPc.secureOnPassword)
            val destinationAddress = InetAddress.getByName(destinationHost)

            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = 3000
            }

            val packet = DatagramPacket(payload, payload.size, destinationAddress, targetPort)
            
            // Send multiple packet bursts (standard best practice for WoL over UDP)
            repeat(3) {
                socket.send(packet)
            }

            SecureLogger.i("Wake-on-LAN Magic Packet transmitted successfully")
            WolResult.Success
        } catch (e: Exception) {
            SecureLogger.e("Wake-on-LAN packet transmission failed", e)
            WolResult.Failure("Network transmission failed: ${e.localizedMessage ?: "Unknown network error"}")
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }
}
