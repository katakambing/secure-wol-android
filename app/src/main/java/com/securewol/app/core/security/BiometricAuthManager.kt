package com.securewol.app.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * BiometricAuthManager: Wraps AndroidX BiometricPrompt for Strong Biometric & Device Credential authentication.
 */
class BiometricAuthManager(private val activity: FragmentActivity) {

    interface AuthCallback {
        fun onAuthSuccess()
        fun onAuthFailed()
        fun onAuthError(errorCode: Int, errString: CharSequence)
        fun onUsePinFallback()
    }

    private val biometricManager = BiometricManager.from(activity)

    /**
     * Checks if biometric hardware is present and enrolled.
     */
    fun canAuthenticateWithBiometrics(): Boolean {
        val status = biometricManager.canAuthenticate(BIOMETRIC_STRONG)
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Launches the official Android BiometricPrompt modal.
     */
    fun showBiometricPrompt(
        title: String = "Authenticate to continue",
        subtitle: String = "Verify identity to access PC controls",
        allowDeviceCredentialFallback: Boolean = false,
        callback: AuthCallback
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val promptCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                SecureLogger.i("Biometric authentication successful")
                callback.onAuthSuccess()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                SecureLogger.w("Biometric authentication failed (unrecognized biometric)")
                callback.onAuthFailed()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                SecureLogger.w("Biometric authentication error [$errorCode]: $errString")
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    callback.onUsePinFallback()
                } else {
                    callback.onAuthError(errorCode, errString)
                }
            }
        }

        val prompt = BiometricPrompt(activity, executor, promptCallback)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (allowDeviceCredentialFallback) {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        } else {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG)
            promptInfoBuilder.setNegativeButtonText("Use App PIN")
        }

        prompt.authenticate(promptInfoBuilder.build())
    }
}
