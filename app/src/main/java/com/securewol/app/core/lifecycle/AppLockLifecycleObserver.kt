package com.securewol.app.core.lifecycle

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.securewol.app.core.security.SecureLogger
import com.securewol.app.core.security.SessionManager
import com.securewol.app.data.repository.SecurityRepository

/**
 * AppLockLifecycleObserver: Monitors app foreground/background transitions using ProcessLifecycleOwner.
 * Automatically invalidates the active session when background duration exceeds the configured threshold.
 */
class AppLockLifecycleObserver(
    private val context: Context,
    private val securityRepository: SecurityRepository
) : DefaultLifecycleObserver {

    private var backgroundTimestamp: Long = 0L

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        backgroundTimestamp = System.currentTimeMillis()
        val timeout = securityRepository.getSecuritySettings().autoLockTimeout
        SecureLogger.i("App entered background at $backgroundTimestamp. Timeout mode: ${timeout.name}")

        if (timeout.timeoutMillis == 0L) {
            // Immediate lock
            SessionManager.invalidateSession()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (backgroundTimestamp > 0) {
            val elapsed = System.currentTimeMillis() - backgroundTimestamp
            val timeout = securityRepository.getSecuritySettings().autoLockTimeout
            SecureLogger.i("App returned to foreground. Elapsed background time: ${elapsed}ms. Threshold: ${timeout.timeoutMillis}ms")

            if (elapsed >= timeout.timeoutMillis) {
                SessionManager.invalidateSession()
            }
        }
    }
}
