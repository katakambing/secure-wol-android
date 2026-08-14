package com.securewol.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.securewol.app.core.lifecycle.AppLockLifecycleObserver
import com.securewol.app.core.security.SecureLogger
import com.securewol.app.data.repository.PcRepository
import com.securewol.app.data.repository.SecurityRepository

class SecureWolApplication : Application() {

    val securityRepository: SecurityRepository by lazy {
        SecurityRepository(this)
    }

    val pcRepository: PcRepository by lazy {
        PcRepository(this)
    }

    override fun onCreate() {
        super.onCreate()
        SecureLogger.i("SecureWolApplication starting up")

        // Register global lifecycle observer to enforce auto-lock on app backgrounding
        val observer = AppLockLifecycleObserver(this, securityRepository)
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
    }
}
