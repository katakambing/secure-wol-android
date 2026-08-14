package com.securewol.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.securewol.app.core.security.SecureLogger
import com.securewol.app.data.repository.PcRepository
import com.securewol.app.data.repository.SecurityRepository
import com.securewol.app.ui.navigation.AppNavigation
import com.securewol.app.ui.theme.SecureWolTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enforce anti-screenshot and anti-recents task preview protection (Threat Model Req 16)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        SecureLogger.i("MainActivity initialized with FLAG_SECURE")

        val app = application as SecureWolApplication
        val securityRepo = app.securityRepository
        val pcRepo = app.pcRepository

        setContent {
            SecureWolTheme {
                AppNavigation(
                    securityRepository = securityRepo,
                    pcRepository = pcRepo
                )
            }
        }
    }
}
