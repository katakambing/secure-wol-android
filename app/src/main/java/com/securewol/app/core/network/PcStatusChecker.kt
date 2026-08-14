package com.securewol.app.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

enum class PcPowerStatus {
    ONLINE,
    OFFLINE,
    CHECKING
}

/**
 * PcStatusChecker: Checks whether target PC is reachable on local network (Online/Offline)
 * using ICMP echo and local socket probing.
 */
object PcStatusChecker {

    suspend fun checkStatus(ipAddress: String, timeoutMillis: Int = 1200): PcPowerStatus = withContext(Dispatchers.IO) {
        val cleanIp = ipAddress.trim()
        if (cleanIp.isBlank() || cleanIp == "255.255.255.255") {
            return@withContext PcPowerStatus.OFFLINE
        }

        try {
            val address = InetAddress.getByName(cleanIp)
            
            // 1. Fast ICMP Ping Probe
            if (address.isReachable(timeoutMillis)) {
                return@withContext PcPowerStatus.ONLINE
            }

            // 2. Fallback probe on common active Windows ports (445 SMB, 135 RPC, 3389 RDP)
            val portsToProbe = listOf(445, 135, 3389, 80)
            for (port in portsToProbe) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(address, port), 350)
                        return@withContext PcPowerStatus.ONLINE
                    }
                } catch (_: Exception) {
                    // Try next port
                }
            }
            PcPowerStatus.OFFLINE
        } catch (e: Exception) {
            PcPowerStatus.OFFLINE
        }
    }
}
