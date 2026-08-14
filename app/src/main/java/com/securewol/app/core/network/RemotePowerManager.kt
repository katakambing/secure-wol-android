package com.securewol.app.core.network

import com.securewol.app.core.security.SecureLogger
import com.securewol.app.core.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

enum class RemotePowerAction(val endpoint: String, val displayName: String) {
    SLEEP("sleep", "Sleep"),
    RESTART("restart", "Restart"),
    SHUTDOWN("shutdown", "Shut Down")
}

sealed class RemoteCommandResult {
    object Success : RemoteCommandResult()
    data class Failure(val message: String) : RemoteCommandResult()
}

/**
 * RemotePowerManager: Sends authenticated local network commands to the PC Companion Agent
 * for Sleep, Restart, and Shutdown.
 */
object RemotePowerManager {

    suspend fun executePowerAction(
        targetIp: String,
        action: RemotePowerAction,
        agentPort: Int = 9090,
        authSecret: String? = null
    ): RemoteCommandResult = withContext(Dispatchers.IO) {
        // Enforce Session Gate
        try {
            SessionManager.validateSessionOrThrow()
        } catch (e: SecurityException) {
            return@withContext RemoteCommandResult.Failure("Authentication required.")
        }

        if (targetIp.isBlank() || targetIp == "255.255.255.255") {
            return@withContext RemoteCommandResult.Failure("Local IP address is required for remote power commands.")
        }

        try {
            val url = URL("http://$targetIp:$agentPort/${action.endpoint}")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3000
                readTimeout = 3000
                if (!authSecret.isNullOrBlank()) {
                    setRequestProperty("X-Auth-Token", authSecret)
                }
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                SecureLogger.i("Remote power command [${action.name}] executed successfully")
                RemoteCommandResult.Success
            } else {
                SecureLogger.w("Remote command returned HTTP $responseCode")
                RemoteCommandResult.Failure("PC Companion Agent rejected command (HTTP $responseCode)")
            }
        } catch (e: Exception) {
            SecureLogger.w("Failed to connect to PC Companion Agent: ${e.message}")
            RemoteCommandResult.Failure("Could not connect to PC Agent on $targetIp:$agentPort. Ensure the PC companion agent is running.")
        }
    }
}
