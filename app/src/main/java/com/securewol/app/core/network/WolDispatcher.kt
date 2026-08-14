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
 * using multi-path broadcast (Global, Subnet Broadcast, and Unicast) across UDP ports 9 and 7.
 */
object WolDispatcher {

    sealed class WolResult {
        object Success : WolResult()
        data class Failure(val errorMessage: String) : WolResult()
        data class SecurityDenied(val reason: String) : WolResult()
    }

    /**
     * Dispatches a Wake-on-LAN Magic Packet to the target PC using multi-target broadcast.
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

        SecureLogger.i("Wake-on-LAN request initiated for PC [${targetPc.name}]")

        // Step 3: Collect all destination targets for maximum router & AP penetration
        val destinationAddresses = mutableSetOf<String>()
        
        // 1. Global Broadcast
        destinationAddresses.add("255.255.255.255")

        // 2. User configured broadcast
        if (targetPc.broadcastAddress.isNotBlank()) {
            destinationAddresses.add(targetPc.broadcastAddress.trim())
        }

        // 3. Subnet broadcast calculated from IP (e.g. 192.168.0.12 -> 192.168.0.255)
        if (targetPc.ipAddress.isNotBlank()) {
            val ipParts = targetPc.ipAddress.trim().split(".")
            if (ipParts.size == 4) {
                destinationAddresses.add("${ipParts[0]}.${ipParts[1]}.${ipParts[2]}.255")
            }
            // 4. Direct unicast to target IP
            destinationAddresses.add(targetPc.ipAddress.trim())
        }

        val targetPort = if (targetPc.port in 1..65535) targetPc.port else 9
        val portsToSend = if (targetPort != 7) listOf(targetPort, 7) else listOf(7, 9)

        var socket: DatagramSocket? = null
        var packetsSentCount = 0

        try {
            val payload = WolPacketBuilder.buildMagicPacket(targetPc.macAddress, targetPc.secureOnPassword)

            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = 3000
            }

            for (destHost in destinationAddresses) {
                try {
                    val destAddress = InetAddress.getByName(destHost)
                    for (p in portsToSend) {
                        val packet = DatagramPacket(payload, payload.size, destAddress, p)
                        // Send 3 bursts per target
                        repeat(3) {
                            socket.send(packet)
                            packetsSentCount++
                        }
                    }
                } catch (e: Exception) {
                    SecureLogger.w("Failed transmitting to WoL target $destHost: ${e.message}")
                }
            }

            if (packetsSentCount > 0) {
                SecureLogger.i("Wake-on-LAN Magic Packets transmitted successfully ($packetsSentCount bursts across ${destinationAddresses.size} routes)")
                WolResult.Success
            } else {
                WolResult.Failure("Failed to resolve any network broadcast targets.")
            }
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
